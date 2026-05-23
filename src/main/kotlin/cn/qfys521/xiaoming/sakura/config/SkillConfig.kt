package cn.qfys521.xiaoming.sakura.config

data class SkillAction(
    val tag: String,
    val description: String = "",
    val type: String = "shell",
    val template: String = "",
    val workingDir: String? = null,
    val timeoutMs: Long = 30000
)

data class SkillConfig(
    val name: String,
    val version: String = "0.0.0",
    val description: String = "",
    val author: String = "",
    val enabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis(),
    val actions: List<SkillAction> = emptyList(),
    val scripts: List<String> = emptyList()
)

data class SkillIndex(
    val skills: MutableMap<String, SkillConfig> = mutableMapOf()
)
