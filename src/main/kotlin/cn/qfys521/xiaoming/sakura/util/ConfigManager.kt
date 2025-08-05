package cn.qfys521.xiaoming.sakura.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class ConfigManager(
    val objectMapper: ObjectMapper,
    val logger: org.slf4j.Logger
) {
    /**
     * 自动处理配置文件的加载和创建
     * @param configFile 配置文件
     * @param defaultConfig 默认配置实例
     * @param configName 配置名称（用于日志）
     * @return 加载的配置实例
     */
    inline fun <reified T> loadOrCreateConfig(
        configFile: File,
        defaultConfig: T,
        configName: String
    ): T {
        return if (!configFile.exists()) {
            logger.info("$configName file not found, creating a new one with default values.")
            objectMapper.writeValue(configFile, defaultConfig)
            defaultConfig
        } else {
            logger.info("Loading $configName from file.")
            objectMapper.readValue(configFile)
        }
    }

    /**
     * 保存配置到文件
     * @param configFile 配置文件
     * @param config 要保存的配置实例
     */
    fun <T> saveConfig(configFile: File, config: T) {
        objectMapper.writeValue(configFile, config)
    }
}
