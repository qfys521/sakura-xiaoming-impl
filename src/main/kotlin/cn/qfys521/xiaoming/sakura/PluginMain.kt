@file:Suppress("unused")

package cn.qfys521.xiaoming.sakura

import cn.chuanwise.xiaoming.plugin.JavaPlugin
import cn.qfys521.xiaoming.sakura.command.BanCommands
import cn.qfys521.xiaoming.sakura.command.ChatCommands
import cn.qfys521.xiaoming.sakura.command.JrrpCommands
import cn.qfys521.xiaoming.sakura.command.OmikujiCommands
import cn.qfys521.xiaoming.sakura.command.PersonaCommands
import cn.qfys521.xiaoming.sakura.command.SkillCommands
import cn.qfys521.xiaoming.sakura.command.services.ActionExecutor
import cn.qfys521.xiaoming.sakura.command.services.AgentLoop
import cn.qfys521.xiaoming.sakura.command.services.ContextManager
import cn.qfys521.xiaoming.sakura.command.services.PersonaManager
import cn.qfys521.xiaoming.sakura.command.services.SkillManager
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
        var INSTANCE: PluginMain = PluginMain()
            private set
    }

    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    private lateinit var configManager: ConfigManager

    var jrrpConfig: JrrpConfig = JrrpConfig()
        private set
    var chatConfig: ChatConfig = ChatConfig()
        private set
    var essentialsConfig: EssentialsConfig = EssentialsConfig()
        private set

    lateinit var skillManager: SkillManager
        private set
    lateinit var personaManager: PersonaManager
        private set
    lateinit var contextManager: ContextManager
        private set
    lateinit var actionExecutor: ActionExecutor
        private set
    lateinit var agentLoop: AgentLoop
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

        skillManager = SkillManager(File(dataFolder, "skills"), objectMapper, logger)
        personaManager = PersonaManager(File(dataFolder, "personas"), objectMapper, logger)
        contextManager = ContextManager(File(dataFolder, "conversations"), objectMapper, logger)
        actionExecutor = ActionExecutor(File(dataFolder, "skills"), logger)

        try {
            jrrpConfig = configManager.loadOrCreateConfig(jrrpConfigFile, JrrpConfig(), "JrrpConfig")
            chatConfig = configManager.loadOrCreateConfig(chatConfigFile, ChatConfig(), "ChatConfig")
            essentialsConfig =
                configManager.loadOrCreateConfig(essentialsConfigFile, EssentialsConfig(), "EssentialsConfig")
        } catch (ex: Exception) {
            logger.error("Sakura XiaoMing Plugin load failed!", ex)
        }

        agentLoop = AgentLoop(chatConfig, personaManager, contextManager, skillManager, actionExecutor, logger)

        xiaoMingBot.interactorManager.registerInteractors(BanCommands(), INSTANCE)
        xiaoMingBot.interactorManager.registerInteractors(ChatCommands(), INSTANCE)
        xiaoMingBot.interactorManager.registerInteractors(JrrpCommands(), INSTANCE)
        xiaoMingBot.interactorManager.registerInteractors(OmikujiCommands() , INSTANCE)
        xiaoMingBot.interactorManager.registerInteractors(SkillCommands(), INSTANCE)
        xiaoMingBot.interactorManager.registerInteractors(PersonaCommands(), INSTANCE)
        xiaoMingBot.eventManager.registerListeners(CommandListener(), INSTANCE)
    }

    override fun onDisable() {
        super.onDisable()
        logger.info("Sakura XiaoMing Plugin disabled successfully!")
        configManager.saveConfig(File(dataFolder, "jrrp-config.json"), jrrpConfig)
        configManager.saveConfig(File(dataFolder, "chat-config.json"), chatConfig)
        configManager.saveConfig(File(dataFolder, "essentials-config.json"), essentialsConfig)
        logger.info("Configuration files saved successfully!")
    }
}
