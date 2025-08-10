package cn.qfys521.xiaoming.sakura.config

import com.fasterxml.jackson.annotation.JsonProperty

data class ChatConfig(
    @field:JsonProperty("modelName")
    var modelName: String = "qwen-turbo",
    @field:JsonProperty("temperature")
    var temperature: Float = 0.7f,
    @field:JsonProperty("maxTokens")
    var maxTokens: Int = 4096,
    @field:JsonProperty("topP")
    var topP: Double = 1.0,
    @field:JsonProperty("token")
    var token: String = "",
    @field:JsonProperty("apiUrl")
    var apiUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    @field:JsonProperty("enableSearch")
    var enableSearch: Boolean = true
)