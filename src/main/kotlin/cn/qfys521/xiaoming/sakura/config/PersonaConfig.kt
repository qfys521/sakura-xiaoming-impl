package cn.qfys521.xiaoming.sakura.config

data class PersonaConfig(
    val name: String,
    val displayName: String = "",
    val systemPrompt: String = "You are a helpful assistant.",
    val modelName: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val enabledSkills: List<String> = emptyList(),
    val maxHistoryRounds: Int = 20,
    val createdAt: Long = System.currentTimeMillis()
)

data class PersonaIndex(
    val personas: MutableMap<String, PersonaConfig> = mutableMapOf(),
    var defaultPersona: String = "default"
)

data class PersonaContext(
    val contexts: MutableMap<String, String> = mutableMapOf()
)
