package cn.qfys521.xiaoming.sakura.config

data class ChatConfig(

    var modelName: String = "qwen-turbo",

    var temperature: Float = 0.7f,

    var maxTokens: Int = 4096,

    var topP: Double = 1.0,

    var token: String = "",

    var systemPrompt: String? = null,

    var apiUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",

    var enableSearch: Boolean = true
)