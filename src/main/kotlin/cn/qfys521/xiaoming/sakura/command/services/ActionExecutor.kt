package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.SkillAction
import cn.qfys521.xiaoming.sakura.util.RuntimeExec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import java.io.File
import java.util.concurrent.TimeUnit

data class ActionResult(
    val success: Boolean,
    val output: String,
    val error: String = ""
)

data class ParsedAction(
    val trigger: String,
    val args: String
)

class ActionExecutor(
    private val skillsDir: File,
    private val logger: Logger
) {
    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun parseActionsFromOutput(output: String): List<ParsedAction> {
        val results = mutableListOf<ParsedAction>()

        // strip markdown code fences
        val cleaned = output
            .replace(Regex("```[\\w]*\\r?\\n?"), "\n")
            .replace(Regex("```"), "")
            .replace(Regex("`([^`]+)`")) { mr -> mr.groupValues[1] }

        // !cmd: block — single-line or multiline until next trigger or end
        val cmdRegex = Regex("""!cmd:\s*(.+?)(?=!\w+:|$)""", setOf(RegexOption.DOT_MATCHES_ALL))
        for (m in cmdRegex.findAll(cleaned)) {
            results.add(ParsedAction("cmd", m.groupValues[1].trim()))
        }

        // !python: block
        val pyRegex = Regex("""!python:\s*(.+?)(?=!\w+:|$)""", setOf(RegexOption.DOT_MATCHES_ALL))
        for (m in pyRegex.findAll(cleaned)) {
            results.add(ParsedAction("python", m.groupValues[1].trim()))
        }

        // !skill: block
        val skillRegex = Regex("""!skill:\s*(\S+)(?:\s+(.+?))?(?=!\w+:|$)""", setOf(RegexOption.DOT_MATCHES_ALL))
        for (m in skillRegex.findAll(cleaned)) {
            val skillName = m.groupValues[1].trim()
            val args = m.groupValues.getOrNull(2)?.trim() ?: ""
            results.add(ParsedAction("skill:$skillName", args))
        }

        // !fetch: block
        val fetchRegex = Regex("""!fetch:\s*(.+?)(?=!\w+:|$)""", setOf(RegexOption.DOT_MATCHES_ALL))
        for (m in fetchRegex.findAll(cleaned)) {
            results.add(ParsedAction("fetch", m.groupValues[1].trim()))
        }

        // fallback: legacy <tag> format for backward compat
        val tagRegex = Regex("<(\\w+)>(.*?)</\\1>", setOf(RegexOption.DOT_MATCHES_ALL))
        for (m in tagRegex.findAll(cleaned)) {
            val tag = m.groupValues[1]
            val params = m.groupValues[2].trim()
            if (tag in BUILTIN_TAGS) {
                results.add(ParsedAction(tag, params))
            }
        }

        return results
    }

    fun executeAction(action: ParsedAction, skillActions: List<SkillAction>, skillName: String): ActionResult {
        return when {
            action.trigger == "cmd" -> execCmd(action.args)
            action.trigger == "python" -> execPython(action.args, skillName)
            action.trigger == "fetch" -> execFetch(action.args)
            action.trigger.startsWith("skill:") -> {
                val name = action.trigger.removePrefix("skill:")
                execSkill(name, action.args, skillActions)
            }
            action.trigger in BUILTIN_TAGS -> executeLegacyTag(action.trigger, action.args)
            else -> ActionResult(false, "", "Unknown trigger: ${action.trigger}")
        }
    }

    private fun execCmd(command: String): ActionResult {
        val result = RuntimeExec.exec(command)
        return if (result.success) ActionResult(true, result.output)
        else ActionResult(false, result.output, result.error)
    }

    private fun execPython(code: String, skillName: String): ActionResult {
        val skillDir = File(skillsDir, skillName)
        skillDir.mkdirs()
        val tmpFile = File(skillDir, "_exec_tmp.py")
        return try {
            tmpFile.writeText(code)
            val result = if (RuntimeExec.isWindows) {
                RuntimeExec.exec(listOf("python", tmpFile.absolutePath), skillDir)
            } else {
                RuntimeExec.exec(listOf("python3", tmpFile.absolutePath), skillDir)
            }
            tmpFile.delete()
            if (result.success) ActionResult(true, result.output)
            else ActionResult(false, result.output, result.error)
        } catch (e: Exception) {
            tmpFile.delete()
            ActionResult(false, "", e.message ?: "Python error")
        }
    }

    private fun execSkill(name: String, args: String, skillActions: List<SkillAction>): ActionResult {
        val action = skillActions.find { it.tag == name }
        if (action == null) {
            return ActionResult(false, "", "Skill not found: $name. Use !cmd: or !python: for built-in operations.")
        }
        return when (action.type.lowercase()) {
            "http" -> execHttpAction(action, args)
            "shell" -> execCmd(action.template.replace("{{params}}", args))
            "python" -> execPython(args, name)
            else -> ActionResult(false, "", "Unknown action type: ${action.type}")
        }
    }

    private fun execHttpAction(action: SkillAction, params: String): ActionResult {
        return try {
            val url = action.template.replace("{{params}}", params)
            val request = Request.Builder().url(url)
                .header("User-Agent", "sakura-xiaoming-skill")
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                ActionResult(true, body.take(4000))
            }
        } catch (e: Exception) {
            ActionResult(false, "", e.message ?: "HTTP error")
        }
    }

    private fun executeLegacyTag(tag: String, params: String): ActionResult {
        return when (tag) {
            "search" -> ActionResult(false, "", "Use !skill: search <query> instead")
            "fetch" -> execFetch(params)
            "python" -> execPython(params, "builtin")
            "shell" -> execCmd(params)
            else -> ActionResult(false, "", "Unknown tag: $tag")
        }
    }

    private fun execFetch(url: String): ActionResult {
        return try {
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; sakura-xiaoming/1.0)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) ActionResult(false, "", "HTTP ${response.code}")
                else {
                    val body = response.body?.string() ?: ""
                    ActionResult(true, body.take(4000))
                }
            }
        } catch (e: Exception) {
            ActionResult(false, "", e.message ?: "Fetch error")
        }
    }

    companion object {
        val BUILTIN_TAGS = setOf("search", "fetch", "python", "shell")
    }
}
