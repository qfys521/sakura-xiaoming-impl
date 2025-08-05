@file:Suppress("unused")

package cn.qfys521.xiaoming.sakura

import cn.chuanwise.xiaoming.plugin.JavaPlugin
import cn.qfys521.xiaoming.sakura.command.SakuraCommands
import cn.qfys521.xiaoming.sakura.config.ChatConfig
import cn.qfys521.xiaoming.sakura.config.EssentialsConfig
import cn.qfys521.xiaoming.sakura.config.JrrpConfig
import cn.qfys521.xiaoming.sakura.listener.CommandListener
import cn.qfys521.xiaoming.sakura.util.ConfigManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

open class PluginMain : JavaPlugin() {
    companion object {
        val INSTANCE = PluginMain()
    }

    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    private lateinit var configManager: ConfigManager

    var jrrpConfig: JrrpConfig = JrrpConfig()
        private set

    var chatConfig: ChatConfig = ChatConfig()
        private set

    var essentialsConfig: EssentialsConfig = EssentialsConfig()
        private set

    override fun onLoad() {
        super.onLoad()
        logger.info("Sakura XiaoMing Plugin loaded successfully!")

        configManager = ConfigManager(objectMapper, logger)

        val dataFolder: File = dataFolder
        dataFolder.mkdirs()

        val jrrpConfigFile = File(dataFolder, "jrrp-config.json")
        val chatConfigFile = File(dataFolder, "chat-config.json")
        val essentialsConfigFile = File(dataFolder, "essentials-config.json")

        jrrpConfig = configManager.loadOrCreateConfig(jrrpConfigFile, JrrpConfig(), "JrrpConfig")
        chatConfig = configManager.loadOrCreateConfig(chatConfigFile, ChatConfig(), "ChatConfig")
        essentialsConfig =
            configManager.loadOrCreateConfig(essentialsConfigFile, EssentialsConfig(), "EssentialsConfig")

        xiaoMingBot.interactorManager.registerInteractors(SakuraCommands(), this@PluginMain)
        xiaoMingBot.eventManager.registerListeners(CommandListener(), this@PluginMain)
    }

    override fun onDisable() {
        super.onDisable()
        logger.info("Sakura XiaoMing Plugin disabled successfully!")
        // 保存配置文件
        configManager.saveConfig(File(dataFolder, "jrrp-config.json"), jrrpConfig)
        configManager.saveConfig(File(dataFolder, "chat-config.json"), chatConfig)
        configManager.saveConfig(File(dataFolder, "essentials-config.json"), essentialsConfig)

        logger.info("Configuration files saved successfully!")
    }
}