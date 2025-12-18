package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.ChatConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ChatService {

    private val client = OkHttpClient
        .Builder()
        .callTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .connectTimeout(300, TimeUnit.SECONDS)
        .build()
    private val mapper = jacksonObjectMapper()
    private val mediaType = "application/json".toMediaType()

    fun sendMessage(message: String, config: ChatConfig): String {
        // 构建请求体
        val payload = mapOf(
            "model" to config.modelName,
            "messages" to listOf(
                config.systemPrompt?.let {
                    mapOf(
                        "role" to "system",
                        "content" to it
                    )
                },
                mapOf(

                    "role" to "user",
                    "content" to message
                )
            ),
            "temperature" to config.temperature,
            "max_tokens" to config.maxTokens,
            "top_p" to config.topP
        )



        val body = mapper
            .writeValueAsString(payload)
            .toRequestBody(mediaType)

        val request = Request.Builder()
            .url("${config.apiUrl}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.token}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Unexpected code $response")
            }
            val json = response.body?.string() ?: ""
            val root = mapper.readTree(json)
            return root["choices"]?.get(0)?.get("message")?.get("content")?.asText() ?: ""
        }
    }
}
