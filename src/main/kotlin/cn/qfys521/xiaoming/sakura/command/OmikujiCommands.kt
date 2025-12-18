@file:Suppress("unused")

package cn.qfys521.xiaoming.sakura.command

import cn.chuanwise.xiaoming.annotation.Filter
import cn.chuanwise.xiaoming.interactor.SimpleInteractors
import cn.chuanwise.xiaoming.user.XiaoMingUser
import cn.qfys521.xiaoming.sakura.PluginMain
import cn.qfys521.xiaoming.sakura.command.omikuji.OmikujiConsolePrinter
import cn.qfys521.xiaoming.sakura.command.omikuji.OmikujiGenerator

class OmikujiCommands : SimpleInteractors<PluginMain>() {


    @Filter("/omikuji")
    @Filter("/おみくじ" )
    @Filter("/抽御神签")
    fun onCommand(event: XiaoMingUser<*>) {
        val key = PluginMain.INSTANCE.jrrpConfig.key
        val generator = OmikujiGenerator(
            salt = key
        )

        val result = generator.generate(userId = event.code)

        event.sendMessage(
            OmikujiConsolePrinter.print(result)
        )
    }

}