package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.ChatConfig
import cn.qfys521.xiaoming.sakura.config.Conversation
import cn.qfys521.xiaoming.sakura.config.PersonaConfig
import cn.qfys521.xiaoming.sakura.config.SkillConfig
import org.slf4j.Logger
import java.io.File

class AgentLoop(
    private val chatConfig: ChatConfig,
    private val personaManager: PersonaManager,
    private val contextManager: ContextManager,
    private val skillManager: SkillManager,
    private val actionExecutor: ActionExecutor,
    private val logger: Logger,
    workspace: File
) : AutoCloseable {
    private val codex = CodexChatService(chatConfig, workspace, logger)

    fun chat(
        userMessage: String,
        personaName: String,
        scope: String,
        scopeId: String
    ): String {
        val persona = personaManager.getPersona(personaName)
            ?: personaManager.getPersona("default")!!
        val enabledSkills = skillManager.getSkillsByNames(persona.enabledSkills)
        val conv = contextManager.getConversation(persona.name, scope, scopeId)

        return agentLoop(userMessage, persona, enabledSkills, conv, scope, scopeId)
    }

    private fun agentLoop(
        userMessage: String,
        persona: PersonaConfig,
        skills: List<SkillConfig>,
        conv: Conversation,
        scope: String,
        scopeId: String
    ): String {
        val developerInstructions = buildSystemPrompt(persona, skills)
        val model = persona.modelName ?: chatConfig.modelName
        val thread = try {
            codex.startThread(developerInstructions, model)
        } catch (e: Exception) {
            logger.error("Failed to start Codex thread", e)
            return ""
        }

        var content = codex.run(thread, buildPrompt(userMessage, conv, persona.maxHistoryRounds))
            ?: return ""
        var iterations = 0
        val maxIterations = 5

        while (iterations < maxIterations) {
            val actions = actionExecutor.parseActionsFromOutput(content)
            if (actions.isEmpty()) {
                saveConversation(persona, scope, scopeId, userMessage, content)
                return content
            }

            val actionResults = mutableListOf<String>()
            val skillActions = skills.flatMap { it.actions }
            for (action in actions) {
                val result = actionExecutor.executeAction(
                    action,
                    skillActions,
                    skills.firstOrNull()?.name ?: "builtin"
                )
                if (result.success) {
                    actionResults.add("[${action.trigger}] ok:\n${result.output}")
                } else {
                    val detail = listOfNotNull(
                        result.error.takeIf { it.isNotBlank() },
                        result.output.takeIf { it.isNotBlank() }?.let { "output: $it" }
                    ).joinToString("; ")
                    actionResults.add(
                        "[${action.trigger}] FAILED" +
                            if (detail.isNotEmpty()) ": $detail" else ""
                    )
                }
            }

            val feedback = """
                The plugin-specific actions have completed. Use the results below to answer the user.
                Do not output another plugin action unless it is genuinely required.

                ${actionResults.joinToString("\n\n---\n\n")}
            """.trimIndent()
            content = codex.run(thread, feedback) ?: return ""
            iterations++
        }

        saveConversation(persona, scope, scopeId, userMessage, content)
        return content
    }

    private fun saveConversation(
        persona: PersonaConfig,
        scope: String,
        scopeId: String,
        userMessage: String,
        assistantMessage: String
    ) {
        contextManager.addMessage(persona.name, scope, scopeId, "user", userMessage)
        contextManager.addMessage(persona.name, scope, scopeId, "assistant", assistantMessage)
    }

    private fun buildPrompt(
        userMessage: String,
        conv: Conversation,
        maxHistoryRounds: Int
    ): String {
        val history = contextManager.trimToRounds(conv, maxHistoryRounds)
        return buildString {
            if (history.isNotEmpty()) {
                appendLine("Conversation history:")
                history.forEach { entry ->
                    appendLine("[${entry.role}] ${entry.content}")
                }
                appendLine()
            }
            appendLine("Current user message:")
            append(userMessage)
        }
    }

    private fun buildSystemPrompt(persona: PersonaConfig, skills: List<SkillConfig>): String {
        return buildString {
            appendLine(persona.systemPrompt)
            appendLine()
            appendLine(buildCapabilitySection())
            appendLine()
            appendLine(buildInjectedKnowledge(skills))
        }.trim()
    }

    private fun buildCapabilitySection(): String {
        return """
            ## How to use tools

            Codex native tools are available for shell commands, files, and web research. Prefer them
            whenever they can complete the request. When you need a plugin-specific action that is not
            covered by a native tool, put one of these formats at the END of your response:

            !cmd: <command>           - Run a shell command
            !python: <code>           - Execute Python code
            !skill: <name> <args>     - Invoke a configured XiaoMing skill
            !fetch: <url>             - Fetch web page content

            Rules:
            - Only output plugin actions when you genuinely need them
            - Do not output example actions
            - After receiving action results, answer the user instead of outputting more actions
        """.trimIndent()
    }

    private fun buildInjectedKnowledge(skills: List<SkillConfig>): String {
        val lines = mutableListOf<String>()

        for (skill in skills) {
            val prompt = readSkillSummary(skill)
            if (prompt != null) {
                lines.add("${skill.name}: $prompt")
            }
        }

        if (lines.isEmpty()) return ""
        return "## Configured XiaoMing skills\n\n" + lines.joinToString("\n\n")
    }

    private fun readSkillSummary(skill: SkillConfig): String? {
        val skillDir = File(skillManager.skillsDir, skill.name)
        val skillMd = File(skillDir, "SKILL.md")
        val promptFile = if (skillMd.exists()) skillMd else File(skillDir, "prompt.md")
        if (!promptFile.exists()) return skill.description.takeIf { it.isNotBlank() }

        val raw = promptFile.readText()
        val body = if (raw.startsWith("---")) {
            val second = raw.indexOf("---", 3)
            if (second >= 0) raw.substring(second + 3) else raw
        } else raw

        val cleaned = body
            .lines()
            .filter { line ->
                val text = line.trim()
                text.isNotBlank()
                    && !text.startsWith("#")
                    && !text.startsWith("---")
                    && !text.startsWith("license:")
                    && !text.startsWith("github:")
                    && !text.startsWith("name:")
                    && !text.startsWith("metadata:")
                    && !text.startsWith("author:")
                    && !text.startsWith("version:")
            }
            .joinToString(" ")

        val claudePatterns = listOf(
            "CLAUDE_SKILL_DIR", "cdp-proxy", "check-deps", "/eval",
            "/click", "/scroll", "/screenshot", "WebSearch", "WebFetch"
        )
        val isClaudeSkill = claudePatterns.any { cleaned.contains(it) }
        val summary = cleaned.take(800).ifBlank { skill.description }

        return if (isClaudeSkill) {
            "$summary\n\nNote: this skill was written for a different platform. Use the configured XiaoMing actions or Codex native tools for equivalent operations."
        } else summary
    }

    override fun close() {
        codex.close()
    }
}
