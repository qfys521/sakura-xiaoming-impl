package cn.qfys521.xiaoming.sakura.util

import java.io.File
import java.util.concurrent.TimeUnit

object RuntimeExec {

    private val OS: String = System.getProperty("os.name").lowercase()
    val isWindows: Boolean = OS.contains("win")
    val isMac: Boolean = OS.contains("mac")
    val isLinux: Boolean = OS.contains("nix") || OS.contains("nux") || OS.contains("aix")

    data class ExecResult(
        val success: Boolean,
        val output: String,
        val exitCode: Int = -1,
        val error: String = ""
    )

    fun listDir(path: String = "."): String {
        val cmd = if (isWindows) listOf("cmd", "/c", "dir", "/b", path)
        else listOf("sh", "-c", "ls -la '$path'")
        return exec(cmd).output
    }

    fun readFile(path: String): String {
        return try {
            File(path).readText().take(4000)
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    fun writeFile(path: String, content: String): String {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            "File written: $path (${content.length} bytes)"
        } catch (e: Exception) {
            "Error writing file: ${e.message}"
        }
    }

    fun exec(command: String, workDir: File? = null, timeoutMs: Long = 30000): ExecResult {
        return try {
            val cmd = if (isWindows) listOf("cmd", "/c", command)
            else listOf("sh", "-c", command)

            val pb = ProcessBuilder(cmd)
                .directory(workDir ?: File("."))
                .redirectErrorStream(true)

            val proc = pb.start()
            val completed = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)

            if (!completed) {
                proc.destroyForcibly()
                return ExecResult(false, "", -1, "Timed out after ${timeoutMs}ms")
            }

            val stdout = proc.inputStream.bufferedReader().readText()
            ExecResult(proc.exitValue() == 0, stdout.take(4000), proc.exitValue())
        } catch (e: Exception) {
            ExecResult(false, "", -1, e.message ?: "Unknown error")
        }
    }

    fun exec(cmd: List<String>, workDir: File? = null, timeoutMs: Long = 30000): ExecResult {
        return try {
            val pb = ProcessBuilder(cmd)
                .directory(workDir ?: File("."))
                .redirectErrorStream(true)

            val proc = pb.start()
            val completed = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)

            if (!completed) {
                proc.destroyForcibly()
                return ExecResult(false, "", -1, "Timed out after ${timeoutMs}ms")
            }

            val stdout = proc.inputStream.bufferedReader().readText()
            ExecResult(proc.exitValue() == 0, stdout.take(4000), proc.exitValue())
        } catch (e: Exception) {
            ExecResult(false, "", -1, e.message ?: "Unknown error")
        }
    }
}
