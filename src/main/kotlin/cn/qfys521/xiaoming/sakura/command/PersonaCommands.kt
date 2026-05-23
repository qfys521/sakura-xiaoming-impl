package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.annotation.FilterParameter
import cn.chuanwise.xiaoming.annotation.Required
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain
import cn.qfys521.xiaoming.sakura.config.PersonaConfig

class PersonaCommands : SimpleInteractors<PluginMain>() {

    @Filter("/persona list")
    fun list(event: XiaoMingUser<*>) {
        val personas = PluginMain.INSTANCE.personaManager.listPersonas()
        val defaultName = PluginMain.INSTANCE.personaManager.getDefaultPersonaName()
        val msg = buildString {
            appendLine("🎭 可用角色:")
            personas.forEach { p ->
                val star = if (p.name == defaultName) "⭐" else "  "
                appendLine("$star ${p.displayName.ifEmpty { p.name }} (${p.name})")
                if (p.systemPrompt.length <= 60) {
                    appendLine("     ${p.systemPrompt}")
                } else {
                    appendLine("     ${p.systemPrompt.take(60)}...")
                }
            }
            appendLine("⭐ = 默认角色")
        }
        event.sendMessage(msg.trimEnd())
    }

    @Filter("/persona create {r:name} {r:prompt}")
    @Required("sakura.command.admin.chat")
    fun create(
        event: XiaoMingUser<*>,
        @FilterParameter("name") name: String,
        @FilterParameter("prompt") prompt: String
    ) {
        try {
            val config = PersonaConfig(
                name = name,
                displayName = name,
                systemPrompt = prompt
            )
            PluginMain.INSTANCE.personaManager.createPersona(config)
            event.sendMessage("✅ 角色创建成功: $name")
        } catch (e: IllegalStateException) {
            event.sendMessage("❌ 角色 '$name' 已存在")
        }
    }

    @Filter("/persona delete {r:name}")
    @Required("sakura.command.admin.chat")
    fun delete(event: XiaoMingUser<*>, @FilterParameter("name") name: String) {
        val ok = PluginMain.INSTANCE.personaManager.deletePersona(name)
        if (ok) {
            event.sendMessage("✅ 角色已删除: $name")
        } else {
            event.sendMessage("❌ 删除失败（角色不存在或不可删除）: $name")
        }
    }

    @Filter("/persona use {r:name}")
    fun use(event: XiaoMingUser<*>, @FilterParameter("name") name: String) {
        val persona = PluginMain.INSTANCE.personaManager.getPersona(name)
        if (persona == null) {
            event.sendMessage("❌ 角色不存在: $name")
            return
        }
        val (scope, id) = resolveScope(event)
        PluginMain.INSTANCE.personaManager.setContextPersona(scope, id, name)
        event.sendMessage("✅ 当前会话已切换至角色: ${persona.displayName.ifEmpty { persona.name }} (${persona.name})")
    }

    companion object {
        fun resolveScope(event: XiaoMingUser<*>): Pair<String, String> {
            val code = event.contact.code
            return if (event is cn.chuanwise.xiaoming.user.GroupXiaoMingUser) {
                "group" to "g$code"
            } else {
                "user" to "u$code"
            }
        }
    }

    @Filter("/persona info {r:name}")
    fun info(event: XiaoMingUser<*>, @FilterParameter("name") name: String) {
        val persona = PluginMain.INSTANCE.personaManager.getPersona(name)
        if (persona == null) {
            event.sendMessage("❌ 角色不存在: $name")
            return
        }
        val msg = buildString {
            appendLine("🎭 ${persona.displayName.ifEmpty { persona.name }} (${persona.name})")
            appendLine("System Prompt: ${persona.systemPrompt}")
            appendLine("Model: ${persona.modelName ?: "默认"}")
            appendLine("Temperature: ${persona.temperature ?: "默认"}")
            appendLine("Max History: ${persona.maxHistoryRounds} 轮")
            if (persona.enabledSkills.isNotEmpty()) {
                appendLine("Skills: ${persona.enabledSkills.joinToString(", ")}")
            } else {
                appendLine("Skills: (无)")
            }
        }
        event.sendMessage(msg.trimEnd())
    }

    @Filter("/persona skill add {r:persona} {r:skill}")
    @Required("sakura.command.admin.chat")
    fun skillAdd(
        event: XiaoMingUser<*>,
        @FilterParameter("persona") personaName: String,
        @FilterParameter("skill") skillName: String
    ) {
        val ok = PluginMain.INSTANCE.personaManager.addSkill(personaName, skillName)
        if (ok) {
            event.sendMessage("✅ Skill '$skillName' 已绑定到角色 '$personaName'")
        } else {
            event.sendMessage("❌ 角色 '$personaName' 不存在或 skill 已绑定")
        }
    }

    @Filter("/persona skill remove {r:persona} {r:skill}")
    @Required("sakura.command.admin.chat")
    fun skillRemove(
        event: XiaoMingUser<*>,
        @FilterParameter("persona") personaName: String,
        @FilterParameter("skill") skillName: String
    ) {
        val ok = PluginMain.INSTANCE.personaManager.removeSkill(personaName, skillName)
        if (ok) {
            event.sendMessage("✅ Skill '$skillName' 已从角色 '$personaName' 解绑")
        } else {
            event.sendMessage("❌ 角色 '$personaName' 不存在或 skill 未绑定")
        }
    }
}
