@file:Suppress("unused")

package cn.qfys521.xiaoming.sakura

import cn.chuanwise.xiaoming.plugin.JavaPlugin
import cn.qfys521.xiaoming.sakura.command.SakuraCommands
import cn.qfys521.xiaoming.sakura.config.ChatConfig
import cn.qfys521.xiaoming.sakura.config.JrrpConfig
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class PluginMain : JavaPlugin() {
    companion object {
        val INSTANCE = PluginMain()
    }

    var jrrpConfig: JrrpConfig = JrrpConfig()
        private set

    var chatConfig: ChatConfig = ChatConfig()
        private set

    override fun onLoad() {
        super.onLoad()
        logger.info("Sakura XiaoMing Plugin loaded successfully!")

        val dataFolder: File = dataFolder
        dataFolder.mkdirs()
        val jrrpConfig = File(dataFolder, "jrrp-config.json")
        val chatConfig = File(dataFolder, "chat-config.json")

        if (!jrrpConfig.exists()) {
            logger.info("JrrpConfig file not found, creating a new one with default values.")
            this@PluginMain.jrrpConfig = JrrpConfig()
            jrrpConfig.writeText(Json.encodeToString(this@PluginMain.jrrpConfig))
        } else {
            logger.info("Loading JrrpConfig from file.")
            this@PluginMain.jrrpConfig = Json.decodeFromString(jrrpConfig.readText())
        }
        if (!chatConfig.exists()) {
            logger.info("JrrpConfig file not found, creating a new one with default values.")
            this@PluginMain.chatConfig = ChatConfig()
            chatConfig.writeText(Json.encodeToString(Json.encodeToString(this@PluginMain.chatConfig)))
        } else {
            logger.info("Loading ChatConfig from file.")
            this@PluginMain.chatConfig = Json.decodeFromString(chatConfig.readText())
        }

        xiaoMingBot.interactorManager.registerInteractors(SakuraCommands(), this@PluginMain)
    }

    override fun onDisable() {
        super.onDisable()
        logger.info("Sakura XiaoMing Plugin disabled successfully!")
    }

}