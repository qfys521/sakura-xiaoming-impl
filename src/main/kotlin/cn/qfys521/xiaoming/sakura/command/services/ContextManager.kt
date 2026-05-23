package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.Conversation
import cn.qfys521.xiaoming.sakura.config.ConversationEntry
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import java.io.File

class ContextManager(
    private val conversationsDir: File,
    private val mapper: ObjectMapper,
    private val logger: Logger
) {
    init {
        conversationsDir.mkdirs()
    }

    fun getConversation(personaName: String, scope: String, scopeId: String): Conversation {
        val file = convFile(personaName, scope, scopeId)
        if (!file.exists()) return Conversation(personaName, scope, scopeId)
        return try {
            mapper.readValue(file, Conversation::class.java)
        } catch (e: Exception) {
            logger.error("Failed to load conversation: ${file.name}", e)
            Conversation(personaName, scope, scopeId)
        }
    }

    fun saveConversation(conv: Conversation) {
        val file = convFile(conv.personaName, conv.scope, conv.scopeId)
        file.parentFile.mkdirs()
        mapper.writeValue(file, conv)
    }

    fun addMessage(personaName: String, scope: String, scopeId: String, role: String, content: String) {
        val conv = getConversation(personaName, scope, scopeId)
        conv.messages += ConversationEntry(role = role, content = content)
        saveConversation(conv)
    }

    fun clearConversation(personaName: String, scope: String, scopeId: String) {
        val file = convFile(personaName, scope, scopeId)
        if (file.exists()) file.delete()
        logger.info("Conversation cleared: $personaName/$scope/$scopeId")
    }

    fun trimToRounds(conv: Conversation, maxRounds: Int): List<ConversationEntry> {
        val messages = conv.messages
        if (messages.isEmpty()) return emptyList()

        val entries = mutableListOf<ConversationEntry>()
        var rounds = 0
        for (i in messages.indices.reversed()) {
            entries.add(0, messages[i])
            if (messages[i].role == "user") rounds++
            if (rounds >= maxRounds) break
        }
        return entries
    }

    fun getSummary(personaName: String, scope: String, scopeId: String): String {
        val conv = getConversation(personaName, scope, scopeId)
        if (conv.messages.isEmpty()) return "📋 暂无对话历史"

        val rounds = conv.messages.count { it.role == "user" }
        return buildString {
            appendLine("📋 对话上下文: ${conv.personaName} | ${conv.scope}:${conv.scopeId}")
            appendLine("轮数: $rounds | 消息数: ${conv.messages.size}")
        }
    }

    private fun convFile(personaName: String, scope: String, scopeId: String): File {
        val dir = File(conversationsDir, personaName)
        return File(dir, "${scope}_${scopeId}.json")
    }
}
