package cn.qfys521.xiaoming.sakura.config

data class ConversationEntry(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Conversation(
    val personaName: String,
    val scope: String,
    val scopeId: String,
    val messages: MutableList<ConversationEntry> = mutableListOf()
)
