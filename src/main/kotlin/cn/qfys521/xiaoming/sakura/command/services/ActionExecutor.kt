package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.SkillAction
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

data class ActionResult(
    val success: Boolean,
    val output: String,
    val error: String = ""
)

class ActionExecutor(
    private val skillsDir: File,
    private val logger: Logger
) {
    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun executeTag(tag: String, params: String, skillActions: List<SkillAction>, skillName: String): ActionResult {
        val action = skillActions.find { it.tag == tag }
        if (action == null) {
            return ActionResult(false, "", "No action defined for tag: $tag")
        }

        return when (action.type.lowercase()) {
            "http" -> executeHttp(action, params)
            "shell" -> executeShell(action, params, skillName)
            "python" -> executePython(action, params, skillName)
            else -> ActionResult(false, "", "Unknown action type: ${action.type}")
        }
    }

    fun hasBuiltinTagSupport(tag: String): Boolean {
        return tag in BUILTIN_TAGS
    }

    fun executeBuiltinTag(tag: String, params: String, skillName: String): ActionResult {
        return when (tag) {
            "search" -> webSearch(params)
            "fetch" -> webFetch(params)
            "python" -> runPython(params, skillName, 30000)
            "shell" -> runShell(params, skillName, 30000)
            else -> ActionResult(false, "", "Unknown builtin tag: $tag")
        }
    }

    fun parseActionsFromOutput(output: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val regex = Regex("<(\\w+)>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)
        for (match in regex.findAll(output)) {
            val tag = match.groupValues[1]
            val params = match.groupValues[2].trim()
            if (tag in BUILTIN_TAGS) {
                results.add(tag to params)
            }
        }
        return results
    }

    fun stripTagsFromOutput(output: String): String {
        return output.replace(Regex("<\\w+>.*?</\\w+>", RegexOption.DOT_MATCHES_ALL), "").trim()
    }

    private fun executeHttp(action: SkillAction, params: String): ActionResult {
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

    private fun executeShell(action: SkillAction, params: String, skillName: String): ActionResult {
        val cmd = action.template.replace("{{params}}", params)
        return runShell(cmd, skillName, action.timeoutMs)
    }

    private fun executePython(action: SkillAction, params: String, skillName: String): ActionResult {
        return runPython(params, skillName, action.timeoutMs)
    }

    private fun webSearch(query: String): ActionResult {
        return ActionResult(false, "", "Web search requires a search API configured. Install a search skill or configure an API key.")
    }

    private fun webFetch(url: String): ActionResult {
        return try {
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; sakura-xiaoming/1.0)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    ActionResult(false, "", "HTTP ${response.code}")
                } else {
                    val body = response.body?.string() ?: ""
                    ActionResult(true, body.take(4000))
                }
            }
        } catch (e: Exception) {
            ActionResult(false, "", e.message ?: "Fetch error")
        }
    }

    private fun runPython(code: String, skillName: String, timeoutMs: Long): ActionResult {
        val skillDir = File(skillsDir, skillName)
        val tmpFile = File(skillDir, "_exec_tmp.py")
        return runProcess(listOf("python", tmpFile.absolutePath), code, tmpFile, skillDir, timeoutMs)
    }

    private fun runShell(command: String, skillName: String, timeoutMs: Long): ActionResult {
        val skillDir = File(skillsDir, skillName)
        val tmpFile = File(skillDir, "_exec_tmp.bat")
        return runProcess(listOf("cmd", "/c", tmpFile.absolutePath), command, tmpFile, skillDir, timeoutMs)
    }

    private fun runProcess(cmd: List<String>, content: String, tmpFile: File, workDir: File, timeoutMs: Long): ActionResult {
        return try {
            tmpFile.parentFile.mkdirs()
            tmpFile.writeText(content)

            val pb = ProcessBuilder(cmd)
                .directory(workDir)
                .redirectErrorStream(true)

            val process = pb.start()
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)

            if (!completed) {
                process.destroyForcibly()
                tmpFile.delete()
                return ActionResult(false, "", "Execution timed out after ${timeoutMs}ms")
            }

            val stdout = process.inputStream.bufferedReader().readText()
            tmpFile.delete()

            if (process.exitValue() == 0) {
                ActionResult(true, stdout.take(4000))
            } else {
                ActionResult(false, stdout.take(4000), "Exit code: ${process.exitValue()}")
            }
        } catch (e: Exception) {
            tmpFile.delete()
            ActionResult(false, "", e.message ?: "Process error")
        }
    }

    companion object {
        val BUILTIN_TAGS = setOf("search", "fetch", "python", "shell")
    }
}
