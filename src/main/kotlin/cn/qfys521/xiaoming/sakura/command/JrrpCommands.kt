package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.annotation.Required
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain
import cn.qfys521.xiaoming.sakura.command.tools.LuckAlgorithm
import java.util.*

class JrrpCommands : SimpleInteractors<PluginMain>() {

    @Filter("/jrrp")
    @Filter("/今日运势")
    fun jrrp(event: XiaoMingUser<*>) {
        val qq = event.code
        val key = PluginMain.INSTANCE.jrrpConfig.key

        val luckValue = LuckAlgorithm.get(qq, key)

        event.sendMessage(
            """
            |🎰 今日运势：$luckValue
            |${getComment(luckValue)}
            """.trimIndent()
        )
    }

    @Filter("/resetJrrp")
    @Required("sakura.command.admin.resetJrrp")
    fun reset(event: XiaoMingUser<*>) {
        PluginMain.INSTANCE.jrrpConfig.key =
            Base64.getEncoder().encodeToString(UUID.randomUUID().toString().toByteArray())

        event.sendMessage("🔑 今日运势密钥已重置")
    }

    private fun getComment(value: Int) = when (value) {
        in 80..100 -> "🌟 今天运气爆棚！"
        in 60..79 -> "✨ 运气不错"
        in 40..59 -> "😐 普普通通"
        in 20..39 -> "😕 运气不佳"
        else -> "💀 建议躺平"
    }
}
