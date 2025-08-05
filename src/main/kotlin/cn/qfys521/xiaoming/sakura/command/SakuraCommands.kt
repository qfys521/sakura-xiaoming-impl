@file:Suppress("unused")

package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.annotation.FilterParameter
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain
import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.aigc.generation.GenerationResult
import com.alibaba.dashscope.base.HalfDuplexServiceParam
import com.alibaba.dashscope.common.Message
import com.alibaba.dashscope.common.Role
import com.alibaba.dashscope.exception.ApiException
import com.alibaba.dashscope.exception.InputRequiredException
import com.alibaba.dashscope.exception.NoApiKeyException
import io.reactivex.Flowable
import io.reactivex.functions.Consumer
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.Base64
import java.util.Calendar
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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

    @Filter("/chat {r:chat}")
    fun chat(
        event: XiaoMingUser<*>,
        @FilterParameter("chat") chat: String
    ) {
        event.sendMessage(
            """
            ${callWithMessage(chat)}
        """.trimIndent()
        )
    }


    private fun getJrrpComment(value: Int): String {
        return when (value) {
            in 90..100 -> "🌟 今天运气爆棚！适合做任何事情！"
            in 80..89 -> "✨ 运气很不错，可以尝试一些挑战！"
            in 70..79 -> "😊 运气还可以，保持平常心！"
            in 60..69 -> "😐 运气一般般，小心行事！"
            in 50..59 -> "😕 运气不太好，建议低调行事！"
            in 30..49 -> "😰 运气有点糟糕，今天要谨慎！"
            in 10..29 -> "💀 运气很差，建议在家躺平！"
            else -> "🔥 今天千万别出门！"
        }
    }

    private fun handleGenerationResult(message: GenerationResult, fullContent: StringBuilder) {
        fullContent.append(message.output.text)
    }

    @Throws(ApiException::class, NoApiKeyException::class, InputRequiredException::class)
    fun callWithMessage(context: String): String? {

        val gen = Generation()
        val systemMsg0: Message? = Message.builder()
            .role(Role.SYSTEM.value)
            .content("你是一只白丝猫耳小萝莉")
            .build()

        val userMsg1: Message? = Message.builder()
            .role(Role.USER.value)
            .content(context)
            .build()

        val param: GenerationParam = GenerationParam.builder()
            .model("qwen-turbo")
            .apiKey(plugin.chatConfig.token)
            .messages(listOf(systemMsg0, userMsg1))
            .topP(plugin.chatConfig.topP)
            .temperature(plugin.chatConfig.temperature)
            .enableSearch(plugin.chatConfig.enableSearch)
            .build()

        val clientParams: HalfDuplexServiceParam = param
        val extraParams = HashMap<String?, Any?>()
        extraParams["enable_thinking"] = false
        extraParams["thinking_budget"] = 4000

        clientParams.setParameters(extraParams)
        val results: Flowable<GenerationResult?> = gen.streamCall(clientParams)
        val fullContent = StringBuilder()
        var msg: String? = null

        results.blockingForEach(Consumer { message: GenerationResult? ->
            handleGenerationResult(
                message!!,
                fullContent
            )
            msg = fullContent.toString()
        })
        return msg
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