package cn.qfys521.xiaoming.sakura.command.services

import cn.qfys521.xiaoming.sakura.config.PersonaConfig
import cn.qfys521.xiaoming.sakura.config.PersonaContext
import cn.qfys521.xiaoming.sakura.config.PersonaIndex
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import java.io.File

class PersonaManager(
    private val personasDir: File,
    private val mapper: ObjectMapper,
    private val logger: Logger
) {
    private val indexFile = File(personasDir, "personas.json")
    private val contextFile = File(personasDir, "contexts.json")

    init {
        personasDir.mkdirs()
        if (!indexFile.exists()) {
            val default = PersonaConfig(
                name = "default",
                displayName = "默认助手",
                systemPrompt = "You are a helpful assistant."
            )
            val index = PersonaIndex(personas = mutableMapOf("default" to default))
            mapper.writeValue(indexFile, index)
        }
    }

    fun createPersona(config: PersonaConfig): PersonaConfig {
        val index = loadIndex()
        if (config.name in index.personas) {
            throw IllegalStateException("Persona '${config.name}' already exists")
        }
        index.personas[config.name] = config
        saveIndex(index)
        logger.info("Persona created: ${config.name}")
        return config
    }

    fun deletePersona(name: String): Boolean {
        if (name == "default") return false
        val index = loadIndex()
        if (index.personas.remove(name) != null) {
            if (index.defaultPersona == name) {
                index.defaultPersona = "default"
            }
            saveIndex(index)
            logger.info("Persona deleted: $name")
            return true
        }
        return false
    }

    fun getPersona(name: String): PersonaConfig? {
        return loadIndex().personas[name]
    }

    fun listPersonas(): List<PersonaConfig> {
        return loadIndex().personas.values.toList()
    }

    fun setDefaultPersona(name: String): Boolean {
        val index = loadIndex()
        if (name !in index.personas) return false
        val updated = index.copy(defaultPersona = name)
        mapper.writeValue(indexFile, updated)
        return true
    }

    fun getDefaultPersonaName(): String {
        return loadIndex().defaultPersona
    }

    fun getContextPersona(scope: String, scopeId: String): String {
        val ctx = loadContexts()
        return ctx.contexts["${scope}_$scopeId"] ?: getDefaultPersonaName()
    }

    fun setContextPersona(scope: String, scopeId: String, personaName: String) {
        val ctx = loadContexts()
        ctx.contexts["${scope}_$scopeId"] = personaName
        saveContexts(ctx)
    }

    fun addSkill(personaName: String, skillName: String): Boolean {
        val index = loadIndex()
        val persona = index.personas[personaName] ?: return false
        if (skillName in persona.enabledSkills) return true
        index.personas[personaName] = persona.copy(
            enabledSkills = persona.enabledSkills + skillName
        )
        saveIndex(index)
        logger.info("Skill '$skillName' added to persona '$personaName'")
        return true
    }

    fun removeSkill(personaName: String, skillName: String): Boolean {
        val index = loadIndex()
        val persona = index.personas[personaName] ?: return false
        if (skillName !in persona.enabledSkills) return false
        index.personas[personaName] = persona.copy(
            enabledSkills = persona.enabledSkills - skillName
        )
        saveIndex(index)
        logger.info("Skill '$skillName' removed from persona '$personaName'")
        return true
    }

    private fun loadIndex(): PersonaIndex {
        return try {
            mapper.readValue(indexFile, PersonaIndex::class.java)
        } catch (e: Exception) {
            logger.error("Failed to load persona index", e)
            PersonaIndex()
        }
    }

    private fun saveIndex(index: PersonaIndex) {
        mapper.writeValue(indexFile, index)
    }

    private fun loadContexts(): PersonaContext {
        if (!contextFile.exists()) return PersonaContext()
        return try {
            mapper.readValue(contextFile, PersonaContext::class.java)
        } catch (e: Exception) {
            PersonaContext()
        }
    }

    private fun saveContexts(ctx: PersonaContext) {
        mapper.writeValue(contextFile, ctx)
    }
}
