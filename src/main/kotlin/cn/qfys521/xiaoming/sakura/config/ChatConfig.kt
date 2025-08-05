package cn.qfys521.xiaoming.sakura.config

import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    var modelName: String = "qwen-turbo",
    var temperature: Float = 0.7f,
    var maxTokens: Int = 4096,
    var topP: Double = 1.0,
    var token: String = "",
    var enableSearch: Boolean = true,

    )