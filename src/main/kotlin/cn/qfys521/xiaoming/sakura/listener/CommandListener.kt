@file:Suppress("unused")

package cn.qfys521.xiaoming.sakura.listener

import cn.chuanwise.xiaoming.annotation.EventListener
import cn.chuanwise.xiaoming.event.SimpleListeners
import cn.qfys521.xiaoming.sakura.PluginMain
import net.mamoe.mirai.event.events.GroupMessageSyncEvent


class CommandListener : SimpleListeners<PluginMain>() {

    @EventListener
    fun onGroupMessageEvent(event: GroupMessageSyncEvent) {
        if (event.sender.id in plugin.essentialsConfig.banedUser) {
            event.cancel()
            return
        }
        if (event.group.id in plugin.essentialsConfig.banedGroup) {
            event.cancel()
            return
        }
    }

}