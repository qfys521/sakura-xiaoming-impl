package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.ChatConfig
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
        val currentMessages = messages.toMutableList()

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
            for (action in actions) {
                val skillActions = skills.flatMap { it.actions }
                val result = actionExecutor.executeAction(action, skillActions, skills.firstOrNull()?.name ?: "builtin")

                if (result.success) {
                    actionResults.add("[${action.trigger}] result:\n${result.output}")
                } else {
                    actionResults.add("[${action.trigger}] failed: ${result.error}")
                }
            }

            val feedback = "Tool output (use this to answer, do NOT output more tool commands):\n\n${actionResults.joinToString("\n\n---\n\n")}"

            currentMessages.add(mapOf("role" to "user", "content" to feedback))
            iterations++
        }

        val fallback = callApi(currentMessages, persona) ?: "too many tool iterations"
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
            appendLine()
            appendLine(buildCapabilitySection(skills))
            appendLine()
            appendLine(buildInjectedKnowledge(skills))
        }.trim()
    }

    private fun buildCapabilitySection(skills: List<SkillConfig>): String {
        return """
## How to use tools

When you need to perform an action, use one of these formats at the END of your response:

!cmd: &lt;command&gt;           - Run a shell command (dir/ls, echo, etc.)
!python: &lt;code&gt;           - Execute Python code
!skill: &lt;name&gt; &lt;args&gt;   - Invoke a specific skill (see your knowledge below)
!fetch: &lt;url&gt;             - Fetch web page content

### Useful commands
- View directory: !cmd: dir /b   (Windows) or !cmd: ls -la
- Read file:     !cmd: type file.txt  or !cmd: cat file.txt
- Write file:    !python: open('path','w').write('content')

### Rules
- Only output tool commands when you genuinely need external data or to perform an action
- Do NOT output example commands — only when actually executing
- Place commands at the END of your response, one per line, as plain text
- After receiving tool results, answer the user based on those results — do NOT output more tool commands
""".trimIndent()
    }

    private fun buildInjectedKnowledge(skills: List<SkillConfig>): String {
        val lines = mutableListOf<String>()

        // built-in capabilities always available
        lines.add("python-run: Execute Python code. Use !python: followed by your code.")
        lines.add("command-run: Run shell commands. Use !cmd: followed by the command.")

        // injected skill knowledge
        for (skill in skills) {
            val prompt = readSkillSummary(skill)
            if (prompt != null) {
                lines.add("${skill.name}: $prompt")
            }
        }

        if (lines.isEmpty()) return ""
        return lines.joinToString("\n\n")
    }

    private fun readSkillSummary(skill: SkillConfig): String? {
        val skillDir = File(skillsDir(), skill.name)
        val skillMd = File(skillDir, "SKILL.md")
        val promptFile = if (skillMd.exists()) skillMd else File(skillDir, "prompt.md")
        if (!promptFile.exists()) return skill.description.takeIf { it.isNotBlank() }

        val raw = promptFile.readText()
        // extract the essence — first meaningful paragraph or description line
        val cleaned = raw
            .removePrefix("---")
            .lines()
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("---") }
            .joinToString(" ")

        return cleaned.take(800).ifBlank { skill.description }
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

    private fun skillsDir(): File = skillManager.skillsDir
}
