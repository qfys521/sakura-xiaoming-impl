package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.ChatConfig
import cn.qfys521.xiaoming.sakura.config.ConversationEntry
import cn.qfys521.xiaoming.sakura.config.PersonaConfig
import cn.qfys521.xiaoming.sakura.config.SkillConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import java.io.File
import java.util.concurrent.TimeUnit

class AgentLoop(
    private val chatConfig: ChatConfig,
    private val personaManager: PersonaManager,
    private val contextManager: ContextManager,
    private val skillManager: SkillManager,
    private val actionExecutor: ActionExecutor,
    private val logger: Logger
) {
    private val client = OkHttpClient.Builder()
        .callTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .connectTimeout(300, TimeUnit.SECONDS)
        .build()
    private val mapper = jacksonObjectMapper()
    private val mediaType = "application/json".toMediaType()

    fun chat(
        userMessage: String,
        personaName: String,
        scope: String,
        scopeId: String
    ): String {
        val persona = personaManager.getPersona(personaName)
            ?: personaManager.getPersona("default")!!
        val enabledSkills = skillManager.getSkillsByNames(persona.enabledSkills)
        val conv = contextManager.getConversation(personaName, scope, scopeId)

        return agentLoop(userMessage, persona, enabledSkills, conv, scope, scopeId)
    }

    private fun agentLoop(
        userMessage: String,
        persona: PersonaConfig,
        skills: List<SkillConfig>,
        conv: cn.qfys521.xiaoming.sakura.config.Conversation,
        scope: String,
        scopeId: String
    ): String {
        val messages = buildMessages(userMessage, persona, skills, conv)
        var currentMessages = messages

        var iterations = 0
        val maxIterations = 5

        while (iterations < maxIterations) {
            val response = callApi(currentMessages, persona)
            val content = response ?: return ""

            val actions = actionExecutor.parseActionsFromOutput(content)

            if (actions.isEmpty()) {
                contextManager.addMessage(persona.name, scope, scopeId, "user", userMessage)
                contextManager.addMessage(persona.name, scope, scopeId, "assistant", content)
                return content
            }

            val actionResults = mutableListOf<String>()
            for ((tag, params) in actions) {
                val skillActions = skills.flatMap { it.actions }

                val result = actionExecutor.executeBuiltinTag(tag, params, "builtin")
                val finalResult = if (result.success) {
                    result
                } else {
                    actionExecutor.executeTag(tag, params, skillActions, skills.firstOrNull()?.name ?: "unknown")
                }

                if (finalResult.success) {
                    actionResults.add("[$tag] 执行结果:\n${finalResult.output}")
                } else {
                    actionResults.add("[$tag] 执行失败: ${finalResult.error}")
                }
            }

            val feedback = "系统反馈（你之前请求的操作已执行完毕，请基于以下结果继续回答用户，不要再输出操作标签）:\n\n${actionResults.joinToString("\n\n---\n\n")}"

            currentMessages = currentMessages + mapOf(
                "role" to "user",
                "content" to feedback
            )
            iterations++
        }

        val fallback = callApi(currentMessages, persona) ?: "操作超时，请稍后重试"
        contextManager.addMessage(persona.name, scope, scopeId, "user", userMessage)
        contextManager.addMessage(persona.name, scope, scopeId, "assistant", fallback)
        return fallback
    }

    private fun buildMessages(
        userMessage: String,
        persona: PersonaConfig,
        skills: List<SkillConfig>,
        conv: cn.qfys521.xiaoming.sakura.config.Conversation
    ): List<Map<String, Any?>> {
        val messages = mutableListOf<Map<String, Any?>>()

        val systemPrompt = buildSystemPrompt(persona, skills)
        if (systemPrompt.isNotBlank()) {
            messages.add(mapOf("role" to "system", "content" to systemPrompt))
        }

        val history = contextManager.trimToRounds(conv, persona.maxHistoryRounds)
        for (entry in history) {
            messages.add(mapOf("role" to entry.role, "content" to entry.content))
        }

        messages.add(mapOf("role" to "user", "content" to userMessage))
        return messages
    }

    private fun buildSystemPrompt(persona: PersonaConfig, skills: List<SkillConfig>): String {
        return buildString {
            appendLine(persona.systemPrompt)

            for (skill in skills) {
                appendLine()
                val skillMd = readSkillPrompt(skill.name)
                if (skillMd != null) {
                    appendLine("## Skill: ${skill.name}")
                    appendLine(skillMd)
                }
            }

            appendLine()
            appendLine(buildActionInstructions(skills))
        }.trim()
    }

    private fun buildActionInstructions(skills: List<SkillConfig>): String {
        val sb = StringBuilder()
        sb.appendLine("你可以使用以下标签执行操作（将标签放在回复末尾）:")

        for (tag in ActionExecutor.BUILTIN_TAGS) {
            when (tag) {
                "search" -> sb.appendLine("- <search>关键词</search> : 搜索网页")
                "fetch" -> sb.appendLine("- <fetch>URL</fetch> : 获取网页内容")
                "python" -> sb.appendLine("- <python>代码</python> : 执行 Python 代码")
                "shell" -> sb.appendLine("- <shell>命令</shell> : 执行 Shell 命令")
            }
        }

        for (skill in skills) {
            for (action in skill.actions) {
                if (action.tag !in ActionExecutor.BUILTIN_TAGS) {
                    sb.appendLine("- <${action.tag}>参数</${action.tag}> : ${action.description}")
                }
            }
        }

        return sb.toString()
    }

    private fun callApi(messages: List<Map<String, Any?>>, persona: PersonaConfig): String? {
        val model = persona.modelName ?: chatConfig.modelName
        val temperature = persona.temperature ?: chatConfig.temperature
        val maxTokens = persona.maxTokens ?: chatConfig.maxTokens
        val topP = persona.topP ?: chatConfig.topP

        val payload = mapOf(
            "model" to model,
            "messages" to messages,
            "temperature" to temperature,
            "max_tokens" to maxTokens,
            "top_p" to topP
        )

        val body = mapper.writeValueAsString(payload).toRequestBody(mediaType)
        val request = Request.Builder()
            .url("${chatConfig.apiUrl}/chat/completions")
            .addHeader("Authorization", "Bearer ${chatConfig.token}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("API call failed: ${response.code}")
                    return null
                }
                val json = response.body?.string() ?: return null
                val root = mapper.readTree(json)
                root["choices"]?.get(0)?.get("message")?.get("content")?.asText()
            }
        } catch (e: Exception) {
            logger.error("API call error", e)
            null
        }
    }

    private fun readSkillPrompt(skillName: String): String? {
        val skillDir = File(skillsDir(), skillName)
        val skillMd = File(skillDir, "SKILL.md")
        if (skillMd.exists()) return skillMd.readText()

        val promptMd = File(skillDir, "prompt.md")
        if (promptMd.exists()) return promptMd.readText()

        return null
    }

    private fun skillsDir(): File = skillManager.skillsDir
}
