package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.annotation.FilterParameter
import cn.chuanwise.xiaoming.annotation.Required
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain

class SkillCommands : SimpleInteractors<PluginMain>() {

    @Filter("/skill install {r:url}")
    @Required("sakura.command.admin.chat")
    fun install(event: XiaoMingUser<*>, @FilterParameter("url") url: String) {
        event.sendMessage("🔧 正在安装 skill: $url ...")

        val skill = if (url.contains("github.com")) {
            PluginMain.INSTANCE.skillManager.installFromGitHub(url)
        } else {
            PluginMain.INSTANCE.skillManager.installFromLocal(url)
        }

        if (skill != null) {
            PluginMain.INSTANCE.personaManager.addSkill("default", skill.name)
            event.sendMessage("✅ Skill 安装成功: ${skill.name} v${skill.version}\n${skill.description}\n已自动绑定到默认角色")
        } else {
            event.sendMessage("❌ 安装失败，请检查 URL 或路径是否正确，以及仓库是否包含 skill.json")
        }
    }

    @Filter("/skill list")
    fun list(event: XiaoMingUser<*>) {
        val skills = PluginMain.INSTANCE.skillManager.listSkills()
        if (skills.isEmpty()) {
            event.sendMessage("📋 暂无已安装的 skill")
            return
        }
        val msg = buildString {
            appendLine("📋 已安装的 Skill:")
            skills.forEach { s ->
                val status = if (s.enabled) "启用" else "禁用"
                appendLine("  - ${s.name} v${s.version} [$status]")
                if (s.description.isNotBlank()) appendLine("    ${s.description}")
            }
        }
        event.sendMessage(msg.trimEnd())
    }

    @Filter("/skill remove {r:name}")
    @Required("sakura.command.admin.chat")
    fun remove(event: XiaoMingUser<*>, @FilterParameter("name") name: String) {
        val ok = PluginMain.INSTANCE.skillManager.removeSkill(name)
        if (ok) {
            event.sendMessage("✅ Skill 已移除: $name")
        } else {
            event.sendMessage("❌ 未找到 skill: $name")
        }
    }

    @Filter("/skill enable {r:name}")
    fun enable(event: XiaoMingUser<*>, @FilterParameter("name") name: String) {
        val ok = PluginMain.INSTANCE.skillManager.setEnabled(name, true)
        if (ok) {
            event.sendMessage("✅ Skill 已启用: $name")
        } else {
            event.sendMessage("❌ 未找到 skill: $name")
        }
    }

    @Filter("/skill disable {r:name}")
    fun disable(event: XiaoMingUser<*>, @FilterParameter("name") name: String) {
        val ok = PluginMain.INSTANCE.skillManager.setEnabled(name, false)
        if (ok) {
            event.sendMessage("✅ Skill 已禁用: $name")
        } else {
            event.sendMessage("❌ 未找到 skill: $name")
        }
    }
}
