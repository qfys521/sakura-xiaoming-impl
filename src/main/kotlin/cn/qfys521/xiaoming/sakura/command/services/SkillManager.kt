package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.SkillConfig
import cn.qfys521.xiaoming.sakura.config.SkillIndex
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class SkillManager(
    val skillsDir: File,
    private val mapper: ObjectMapper,
    private val logger: Logger
) {
    private val indexFile = File(skillsDir, "skills.json")
    private val client = OkHttpClient.Builder()
        .callTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun loadIndex(): SkillIndex {
        if (!indexFile.exists()) return SkillIndex()
        return try {
            mapper.readValue(indexFile, SkillIndex::class.java)
        } catch (e: Exception) {
            logger.error("Failed to load skill index", e)
            SkillIndex()
        }
    }

    private fun saveIndex(index: SkillIndex) {
        skillsDir.mkdirs()
        mapper.writeValue(indexFile, index)
    }

    fun listSkills(): List<SkillConfig> {
        return loadIndex().skills.values.toList()
    }

    fun installFromGitHub(url: String): SkillConfig? {
        val (owner, repo) = parseGitHubUrl(url) ?: run {
            logger.error("Invalid GitHub URL: $url")
            return null
        }
        val defaultBranch = getDefaultBranch(owner, repo)
        val zipUrl = "https://github.com/$owner/$repo/archive/refs/heads/$defaultBranch.zip"

        logger.info("Downloading skill from $zipUrl")
        val zipBytes = downloadZip(zipUrl) ?: return null

        val tempZip = File.createTempFile("skill-", ".zip")
        tempZip.writeBytes(zipBytes)

        val extractDir = File(skillsDir, "temp-extract")
        extractDir.mkdirs()
        unzip(tempZip, extractDir)
        tempZip.delete()

        val innerDir = extractDir.listFiles()?.firstOrNull() ?: run {
            logger.error("Empty archive")
            extractDir.deleteRecursively()
            return null
        }

        val skillJsonFile = File(innerDir, "skill.json")
        val existingSkillJsonFile = File(innerDir, ".claude-plugin/plugin.json") //backward compat

        if (!skillJsonFile.exists() && !existingSkillJsonFile.exists()) {
            logger.error("No skill.json found in repo root")
            extractDir.deleteRecursively()
            return null
        }

        val skillConfig: SkillConfig = if (skillJsonFile.exists()) {
            val node = mapper.readTree(skillJsonFile)
            SkillConfig(
                name = node["name"]?.asText() ?: repo,
                version = node["version"]?.asText() ?: "0.0.0",
                description = node["description"]?.asText() ?: "",
                author = node["author"]?.asText() ?: owner
            )
        } else {
            val node = mapper.readTree(existingSkillJsonFile)
            SkillConfig(
                name = node["name"]?.asText() ?: repo,
                version = node["version"]?.asText() ?: "0.0.0",
                description = node["description"]?.asText() ?: "",
                author = node["author"]?.get("name")?.asText() ?: owner
            )
        }

        val targetDir = File(skillsDir, skillConfig.name)
        if (targetDir.exists()) targetDir.deleteRecursively()
        innerDir.renameTo(targetDir)
        extractDir.deleteRecursively()

        val index = loadIndex()
        index.skills[skillConfig.name] = skillConfig
        saveIndex(index)

        logger.info("Skill installed: ${skillConfig.name} v${skillConfig.version}")
        return skillConfig
    }

    fun installFromLocal(path: String): SkillConfig? {
        val source = File(path)
        if (!source.exists() || !source.isDirectory) {
            logger.error("Local directory not found: $path")
            return null
        }

        val skillJsonFile = File(source, "skill.json")
        val existingSkillJsonFile = File(source, ".claude-plugin/plugin.json")

        val skillConfig: SkillConfig = if (skillJsonFile.exists()) {
            val node = mapper.readTree(skillJsonFile)
            SkillConfig(
                name = node["name"]?.asText() ?: source.name,
                version = node["version"]?.asText() ?: "0.0.0",
                description = node["description"]?.asText() ?: "",
                author = node["author"]?.asText() ?: ""
            )
        } else if (existingSkillJsonFile.exists()) {
            val node = mapper.readTree(existingSkillJsonFile)
            SkillConfig(
                name = node["name"]?.asText() ?: source.name,
                version = node["version"]?.asText() ?: "0.0.0",
                description = node["description"]?.asText() ?: "",
                author = node["author"]?.get("name")?.asText() ?: ""
            )
        } else {
            logger.error("No skill.json found in $path")
            return null
        }

        val targetDir = File(skillsDir, skillConfig.name)
        if (targetDir.exists()) targetDir.deleteRecursively()
        source.copyRecursively(targetDir, overwrite = true)

        val index = loadIndex()
        index.skills[skillConfig.name] = skillConfig
        saveIndex(index)

        logger.info("Skill installed from local: ${skillConfig.name} v${skillConfig.version}")
        return skillConfig
    }

    fun removeSkill(name: String): Boolean {
        val index = loadIndex()
        if (name !in index.skills) return false

        index.skills.remove(name)
        saveIndex(index)

        val skillDir = File(skillsDir, name)
        if (skillDir.exists()) skillDir.deleteRecursively()

        logger.info("Skill removed: $name")
        return true
    }

    fun setEnabled(name: String, enabled: Boolean): Boolean {
        val index = loadIndex()
        val skill = index.skills[name] ?: return false
        index.skills[name] = skill.copy(enabled = enabled)
        saveIndex(index)
        return true
    }

    fun getEnabledSkills(): List<SkillConfig> {
        return loadIndex().skills.values.filter { it.enabled }
    }

    fun getSkillsByNames(names: List<String>): List<SkillConfig> {
        val index = loadIndex()
        return names.mapNotNull { index.skills[it] }.filter { it.enabled }
    }

    fun getSkillDir(name: String): File = File(skillsDir, name)

    private fun parseGitHubUrl(url: String): Pair<String, String>? {
        val regex = Regex("github\\.com/([^/]+)/([^/]+?)(?:\\.git)?$")
        val match = regex.find(url.trimEnd('/')) ?: return null
        return match.groupValues[1] to match.groupValues[2]
    }

    private fun getDefaultBranch(owner: String, repo: String): String {
        return try {
            val apiUrl = "https://api.github.com/repos/$owner/$repo"
            val request = Request.Builder().url(apiUrl)
                .addHeader("Accept", "application/vnd.github+json")
                .header("User-Agent", "sakura-xiaoming")
                .build()
            val json = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "main"
                response.body?.string() ?: return "main"
            }
            mapper.readTree(json)["default_branch"]?.asText() ?: "main"
        } catch (e: Exception) {
            logger.warn("Failed to get default branch for $owner/$repo, falling back to 'main'", e)
            "main"
        }
    }

    private fun downloadZip(url: String): ByteArray? {
        return try {
            val request = Request.Builder().url(url)
                .header("User-Agent", "sakura-xiaoming")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("Download failed: ${response.code}")
                    return null
                }
                response.body?.bytes()
            }
        } catch (e: Exception) {
            logger.error("Download error", e)
            null
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val entryFile = File(targetDir, entry!!.name)
                if (entry!!.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile.mkdirs()
                    FileOutputStream(entryFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
        }
    }
}
