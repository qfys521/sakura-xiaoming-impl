@file:Suppress("unused")

package cn.qfys521.xiaoming.sakura

import cn.chuanwise.xiaoming.plugin.JavaPlugin
import cn.qfys521.xiaoming.sakura.command.SakuraCommands
import cn.qfys521.xiaoming.sakura.config.ChatConfig
import cn.qfys521.xiaoming.sakura.config.JrrpConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

open class PluginMain : JavaPlugin() {
    companion object {
        val INSTANCE = PluginMain()
    }

    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    var jrrpConfig: JrrpConfig = JrrpConfig()
        private set

    var chatConfig: ChatConfig = ChatConfig()
        private set

    override fun onLoad() {
        super.onLoad()
        logger.info("Sakura XiaoMing Plugin loaded successfully!")

        val dataFolder: File = dataFolder
        dataFolder.mkdirs()
        val jrrpConfigFile = File(dataFolder, "jrrp-config.json")
        val chatConfigFile = File(dataFolder, "chat-config.json")

        if (!jrrpConfigFile.exists()) {
            logger.info("JrrpConfig file not found, creating a new one with default values.")
            this@PluginMain.jrrpConfig = JrrpConfig()
            objectMapper.writeValue(jrrpConfigFile, this@PluginMain.jrrpConfig)
        } else {
            logger.info("Loading JrrpConfig from file.")
            this@PluginMain.jrrpConfig = objectMapper.readValue(jrrpConfigFile)
        }

        if (!chatConfigFile.exists()) {
            logger.info("ChatConfig file not found, creating a new one with default values.")
            this@PluginMain.chatConfig = ChatConfig()
            objectMapper.writeValue(chatConfigFile, this@PluginMain.chatConfig)
        } else {
            logger.info("Loading ChatConfig from file.")
            this@PluginMain.chatConfig = objectMapper.readValue(chatConfigFile)
        }

        xiaoMingBot.interactorManager.registerInteractors(SakuraCommands(), this@PluginMain)
    }

    override fun onDisable() {
        super.onDisable()
        logger.info("Sakura XiaoMing Plugin disabled successfully!")
    }
}