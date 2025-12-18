package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.annotation.FilterParameter
import cn.chuanwise.xiaoming.annotation.Required
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.PrivateXiaoMingUser
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain
import cn.qfys521.xiaoming.sakura.command.services.ChatService

class ChatCommands : SimpleInteractors<PluginMain>() {

    private val chatService = ChatService()

    @Filter("/chat {r:chat}")
    @Required("sakura.command.admin.chat")
    fun chat(event: XiaoMingUser<*>, @FilterParameter("chat") chat: String) {
        val reply = chatService.sendMessage(chat, PluginMain.INSTANCE.chatConfig)
        event.sendMessage(reply.ifEmpty { "🤖 未收到回复" })
    }

    @Filter("/chat.set temperature {r:value}")
    fun setTemperature(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        value.toFloatOrNull()?.coerceIn(0f, 2f)?.let {
            PluginMain.INSTANCE.chatConfig.temperature = it
            event.sendMessage("temperature=$it")
        } ?: event.sendMessage("参数错误")
    }

}
