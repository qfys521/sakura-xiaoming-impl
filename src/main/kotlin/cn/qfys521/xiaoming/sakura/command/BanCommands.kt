package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.annotation.FilterParameter
import cn.chuanwise.xiaoming.annotation.Required
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain

class BanCommands : SimpleInteractors<PluginMain>() {

    @Filter("/屏蔽-u {r:user}")
    @Required("sakura.command.admin.ban.user")
    fun banUser(event: XiaoMingUser<*>, @FilterParameter("user") user: Long) {
        PluginMain.INSTANCE.essentialsConfig.banedUser += user
        event.sendMessage("🚫 用户 $user 已被屏蔽")
    }

    @Filter("/unban-u {r:user}")
    @Required("sakura.command.admin.unban.user")
    fun unbanUser(event: XiaoMingUser<*>, @FilterParameter("user") user: Long) {
        PluginMain.INSTANCE.essentialsConfig.banedUser -= user
        event.sendMessage("✅ 用户 $user 已解除屏蔽")
    }

    @Filter("/屏蔽-g {r:group}")
    @Required("sakura.command.admin.ban.group")
    fun banGroup(event: XiaoMingUser<*>, @FilterParameter("group") group: Long) {
        PluginMain.INSTANCE.essentialsConfig.banedGroup += group
        event.sendMessage("🚫 群组 $group 已被屏蔽")
    }

    @Filter("/unban-g {r:group}")
    @Required("sakura.command.admin.unban.group")
    fun unbanGroup(event: XiaoMingUser<*>, @FilterParameter("group") group: Long) {
        PluginMain.INSTANCE.essentialsConfig.banedGroup -= group
        event.sendMessage("✅ 群组 $group 已解除屏蔽")
    }
}
