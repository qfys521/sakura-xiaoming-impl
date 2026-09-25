package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.annotation.FilterParameter
import cn.chuanwise.xiaoming.annotation.Required
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.PrivateXiaoMingUser
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain

class ChatCommands : SimpleInteractors<PluginMain>() {

    @Filter("/chat.clear")
    fun clearContext(event: XiaoMingUser<*>) {
        val (scope, id) = PersonaCommands.resolveScope(event)
        val personaName = PluginMain.INSTANCE.personaManager.getContextPersona(scope, id)
        PluginMain.INSTANCE.contextManager.clearConversation(personaName, scope, id)
        event.sendMessage("🧹 角色 '$personaName' 的上下文记忆已清除")
    }

    @Filter("/chat.history")
    fun history(event: XiaoMingUser<*>) {
        val (scope, id) = PersonaCommands.resolveScope(event)
        val personaName = PluginMain.INSTANCE.personaManager.getContextPersona(scope, id)
        val summary = PluginMain.INSTANCE.contextManager.getSummary(personaName, scope, id)
        event.sendMessage(summary)
    }

    @Filter("/chat {r:chat}")
    @Required("sakura.command.admin.chat")
    fun chat(event: XiaoMingUser<*>, @FilterParameter("chat") chat: String) {
        val (scope, id) = PersonaCommands.resolveScope(event)

        val (personaName, message) = parsePersonaArg(chat, scope, id)

        val reply = PluginMain.INSTANCE.agentLoop.chat(message, personaName, scope, id)
        sendReplyWithExpiredMessageFallback(
            event,
            reply.ifEmpty { "\uD83E\uDD16 \u672A\u6536\u5230\u56DE\u590D" }
        )
    }

    /**
     * QQ Bot reply message IDs have a limited lifetime. If the AI response
     * takes too long, retry through the proactive-message API instead.
     */
    private fun sendReplyWithExpiredMessageFallback(
        event: XiaoMingUser<*>,
        content: String
    ) {
        try {
            event.sendMessage(content)
        } catch (error: Throwable) {
            if (!isExpiredReplyMessageError(error)) {
                throw error
            }

            PluginMain.INSTANCE.logger.warn(
                "Reply message expired, retrying as a proactive message",
                error
            )

            try {
                event.contact.sendMessage(content)
            } catch (fallbackError: Throwable) {
                PluginMain.INSTANCE.logger.error(
                    "Failed to send proactive message after reply message expired",
                    fallbackError
                )
                throw fallbackError
            }
        }
    }

    private fun isExpiredReplyMessageError(error: Throwable): Boolean {
        var current: Throwable? = error
        var depth = 0

        while (current != null && depth++ < 8) {
            val text = "${current::class.java.name} ${current.message.orEmpty()}".lowercase()
            if (
                (text.contains("msg_id") || text.contains("msgid")) &&
                (text.contains("expired") || text.contains("\u8FC7\u671F"))
            ) {
                return true
            }
            current = current.cause
        }

        return false
    }

    @Filter("/chat.set temperature {r:value}")
    fun setTemperature(event: PrivateXiaoMingUser, @FilterParameter("value") value: String) {
        value.toFloatOrNull()?.coerceIn(0f, 2f)?.let {
            PluginMain.INSTANCE.chatConfig.temperature = it
            event.sendMessage("temperature=$it")
        } ?: event.sendMessage("参数错误")
    }

    private fun parsePersonaArg(raw: String, scope: String, id: String): Pair<String, String> {
        val match = Regex("^-p\\s+(\\S+)\\s+(.*)").find(raw)
        return if (match != null) {
            val name = match.groupValues[1]
            val msg = match.groupValues[2]
            Pair(name, msg)
        } else {
            val defaultName = PluginMain.INSTANCE.personaManager.getContextPersona(scope, id)
            Pair(defaultName, raw)
        }
    }
}
