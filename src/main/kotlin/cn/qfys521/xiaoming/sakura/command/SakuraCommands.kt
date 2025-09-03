@file:Suppress("unused")

package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.annotation.FilterParameter
import cn.chuanwise.xiaoming.annotation.Required
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.PrivateXiaoMingUser
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain
import cn.qfys521.xiaoming.sakura.config.ChatConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.Base64
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import top.mrxiaom.overflow.message.data.Markdown

class SakuraCommands : SimpleInteractors<PluginMain>() {

    @Filter("/jrrp")
    @Filter("/今日运势")
    fun jrrp(event: XiaoMingUser<*>) {
        val qq = event.code
        val key = plugin.jrrpConfig.key

        val luckValue = LuckAlgorithm.get(qq, key)

        event.sendMessage(
            """
            
            |🎰 今日运势：$luckValue
            |${getJrrpComment(luckValue)}
        """.trimIndent()
        )
    }

    @Filter("/resetJrrp")
    @Required("sakura.command.admin.resetJrrp")
    fun resetJrrp(event: XiaoMingUser<*>) {
        plugin.jrrpConfig.key = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().toByteArray())
        event.sendMessage(
            """
            
            |🔑 密钥已重置！
            |🔄 今日运势重置成功！
            |新的密钥已生成，请重新运行 /jrrp 获取今日运势。
        """.trimIndent()
        )
    }

    @Filter("/屏蔽-u {r:user}")
    @Required("sakura.command.admin.ban.user")
    fun banUser(event: XiaoMingUser<*>, @FilterParameter("user") user: Long) {
        plugin.essentialsConfig.banedUser += user
        event.sendMessage(
            """
            
            |🚫 用户 $user 已被屏蔽！
            |请注意，屏蔽用户后，他们将无法使用任何命令。
            |如果需要取消屏蔽，请联系管理员。
        """.trimIndent()
        )

    }

    @Filter("/markdown {r:markdown}")
    @Required("sakura.command.admin.markdown")
    fun markdown(event: XiaoMingUser<*>, @FilterParameter("markdown") markdown: String) {
        event.sendMessage(Markdown(markdown))
    }

    @Filter("/屏蔽-g {r:group}")
    @Required("sakura.command.admin.ban.group")
    fun banGroup(event: XiaoMingUser<*>, @FilterParameter("group") group: Long) {
        plugin.essentialsConfig.banedGroup += group
        event.sendMessage(
            """
            |🚫 群组 $group 已被屏蔽！
            |请注意，屏蔽群组后，所有成员将无法使用任何命令。
            |如果需要取消屏蔽，请联系管理员。
        """.trimIndent()
        )
    }


    @Filter("/unban-u {r:user}")
    @Required("sakura.command.admin.unban.user")
    fun unbanUser(event: XiaoMingUser<*>, @FilterParameter("user") user: Long) {
        plugin.essentialsConfig.banedUser -= user
        event.sendMessage(
            """
            
            |✅ 用户 $user 已被取消屏蔽！
            |现在他们可以重新使用所有命令。
        """.trimIndent()
        )
    }

    @Filter("/unban-g {r:group}")
    @Required("sakura.command.admin.unban.group")
    fun unbanGroup(event: XiaoMingUser<*>, @FilterParameter("group") group: Long) {
        plugin.essentialsConfig.banedGroup -= group
        event.sendMessage(
            """
            
            |✅ 群组 $group 已被取消屏蔽！
            |现在所有成员可以重新使用所有命令。
        """.trimIndent()
        )
    }


    @Filter("/chat {r:chat}")
    @Required("sakura.command.admin.chat")
    fun chat(
        event: XiaoMingUser<*>,
        @FilterParameter("chat") chat: String
    ) {
        val msg = sendMessage(
            message = chat,
            config = plugin.chatConfig
        )
        event.sendMessage(msg.ifEmpty { "🤖 未收到回复，请稍后再试。" })
    }

    @Filter("/chat.set temperature {r:value}")
    @Required("sakura.command.admin.chat.set.temperature")
    fun setTemperature(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        val v = value.trim().toFloatOrNull()?.coerceIn(0f, 2f)
        if (v == null) {
            event.sendMessage("参数错误：temperature 需为 0~2 的数字")
            return
        }
        plugin.chatConfig.temperature = v
        event.sendMessage("已更新 temperature=$v")
    }

    @Filter("/chat.set topP {r:value}")
    @Required("sakura.command.admin.chat.set.topP")
    fun setTopP(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        val v = value.trim().toDoubleOrNull()?.coerceIn(0.0, 1.0)
        if (v == null) {
            event.sendMessage("参数错误：topP 需为 0~1 的数字")
            return
        }
        plugin.chatConfig.topP = v
        event.sendMessage("已更新 topP=$v")
    }

    @Filter("/chat.set maxTokens {r:value}")
    @Required("sakura.command.admin.chat.set.maxTokens")
    fun setMaxTokens(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        val v = value.trim().toIntOrNull()
        if (v == null || v <= 0) {
            event.sendMessage("参数错误：maxTokens 需为正整数")
            return
        }
        plugin.chatConfig.maxTokens = v
        event.sendMessage("已更新 maxTokens=$v")
    }

    @Filter("/chat.set modelName {r:value}")
    @Required("sakura.command.admin.chat.set.modelName")
    fun setModelName(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        val v = value.trim()
        if (v.isEmpty()) {
            event.sendMessage("参数错误：modelName 不能为空")
            return
        }
        plugin.chatConfig.modelName = v
        event.sendMessage("已更新 modelName=$v")
    }

    @Filter("/chat.set token {r:value}")
    @Required("sakura.command.admin.chat.set.token")
    fun setToken(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        val v = value.trim()
        if (v.isEmpty()) {
            event.sendMessage("参数错误：token 不能为空")
            return
        }
        plugin.chatConfig.token = v
        event.sendMessage("已更新 token（已隐藏）")
    }

    @Filter("/chat.set apiUrl {r:value}")
    @Required("sakura.command.admin.chat.set.apiUrl")
    fun setApiUrl(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        if (value.isEmpty() || !(value.startsWith("http://") || value.startsWith("https://"))) {
            event.sendMessage("参数错误：apiUrl 必须以 http:// 或 https:// 开头")
            return
        }
        plugin.chatConfig.apiUrl = value
        event.sendMessage("已更新 apiUrl=$value")
    }

    val config = plugin.chatConfig

    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()
    private val mediaType = "application/json".toMediaType()

    /**
     * @param message 提问的消息
     * @param config  调用配置
     * @return api返回的消息
     **/
    fun sendMessage(message: String, config: ChatConfig): String {
        // 构建请求体
        val payload = mapOf(
            "model" to config.modelName,
            "messages" to listOf(
                mapOf("role" to "user", "content" to message)
            ),
            "temperature" to config.temperature,
            "max_tokens" to config.maxTokens,
            "top_p" to config.topP
        )

        val body = mapper.writeValueAsString(payload).toRequestBody(mediaType)

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
            // OpenAI 兼容模式返回格式：choices[0].message.content
            return root["choices"]?.get(0)?.get("message")?.get("content")?.asText() ?: ""
        }
    }


    @Filter("/chat.set enableSearch {r:value}")
    @Required("sakura.command.admin.chat.set.enableSearch")
    fun setEnableSearch(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        val v = when (value.trim().lowercase()) {
            "true", "1", "yes", "y", "on", "是", "启用" -> true
            "false", "0", "no", "n", "off", "否", "关闭" -> false
            else -> {
                event.sendMessage("参数错误：enableSearch 仅支持 true/false")
                return
            }
        }
        plugin.chatConfig.enableSearch = v
        event.sendMessage("已更新 enableSearch=$v")
    }

    private fun getJrrpComment(value: Int): String {
        return when (value) {
            in 80..100 -> "🌟 今天运气爆棚！适合做任何事情！"
            in 70..79 -> "✨ 运气很不错，可以尝试一些挑战！"
            in 50..69 -> "😊 运气还可以，保持平常心！"
            in 40..49 -> "😐 运气一般般，小心行事！"
            in 30..39 -> "😕 运气不太好，建议低调行事！"
            in 20..29 -> "😰 运气有点糟糕，今天要谨慎！"
            in 1..19 -> "💀 运气很差，建议在家躺平！"
            else -> "🔥 今天千万别出门！"
        }
    }


}

object LuckAlgorithm {

    fun get(identifier: Long, key: String?): Int {
        return get(Date(), identifier, key)
    }

    fun get(date: Date?, identifier: Long, key: String?): Int {
        return get(getDay(date), identifier, key)
    }

    fun get(day: Int, identifier: Long, key: String?): Int {
        val code = rfc4226(getSeed(day, identifier), key, 8)
        // 返回值是均匀分布的1到100
        return code % 101
    }

    fun getDay(date: Date?): Int {
        val calendar: Calendar = Calendar.getInstance()
        if (date != null) calendar.setTime(date)
        val year: Int = calendar.get(Calendar.YEAR) - 1
        var day = year * 365 + year / 4 - year / 100 + year / 400 // 闰年调整
        day += calendar.get(Calendar.DAY_OF_YEAR)
        return day
    }

    // 二进制拼接日期与QQ号，生成64bit种子，范围约为公元45900年和12位QQ号
    fun getSeed(day: Int, identifier: Long): Long {
        return (identifier and 0x000000FFFFFFFFFFL) or ((day.toLong()) shl 40)
    }

    @JvmOverloads
    fun rfc4226(seed: Long, key: String?, digits: Int = 6): Int {
        return rfc4226(seed, Base64.getDecoder().decode(key), digits)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun rfc4226(seed: Long, key: ByteArray, digits: Int): Int {
        var digits = digits
        val seedBytes = byteArrayOf(
            (seed shr 56).toByte(),
            (seed shr 48).toByte(),
            (seed shr 40).toByte(),
            (seed shr 32).toByte(),
            (seed shr 24).toByte(),
            (seed shr 16).toByte(),
            (seed shr 8).toByte(),
            (seed).toByte(),
        )
        try {

            val keySpec = SecretKeySpec(key, "HMACSHA1")
            val mac: Mac = Mac.getInstance("HMACSHA1")
            mac.init(keySpec)
            val hash: ByteArray = mac.doFinal(seedBytes)
            val index = hash[19].toInt() and 0xF
            var code = 0
            for (i in 0..3) {
                var num = hash[index + i].toInt()
                if (num < 0) num += 256
                if (i == 0) num = num and 0x7F
                code = num or (code shl 8)
            }
            if (digits > 9) digits = 9
            if (digits < 1) digits = 1
            var divider = 10
            for (i in 1..<digits) divider *= 10
            code %= divider
            return code
        } catch (ex: IllegalArgumentException) {
            return -1
        } catch (ex: NoSuchAlgorithmException) {
            return -1
        } catch (ex: InvalidKeyException) {
            return -1
        }
    }
}

// 小工具扩展：安全处理可空字符串
private fun String?.orElse(fallback: String) = this ?: fallback