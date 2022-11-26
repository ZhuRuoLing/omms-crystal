package net.zhuruoling.omms.crystal.console

import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.main.SharedConstants.serverHandler

class ConsoleHandler : Thread("ConsoleHandler") {


    override fun run() {
        while (true) {
            val str = readLine() ?: break
            if (str.startsWith(Config.commandPrefix)) {
                // TODO: resolve command
                if (str == ".fuck") {
                    SharedConstants.eventLoop.dispatch(ServerStopEvent, ServerStopEventArgs("crystal", true))
                    break
                }
                if (str == ".stop") {
                    SharedConstants.eventLoop.dispatch(ServerStopEvent, ServerStopEventArgs("console", false))
                }
                if (str == ".bye") {
                    SharedConstants.eventLoop.dispatch(ServerStopEvent, ServerStopEventArgs("crystal", false))
                    break
                }
                if (str == ".start") {
                    SharedConstants.eventLoop.dispatch(
                        ServerStartEvent,
                        ServerStartEventArgs(Config.launchCommand, Config.serverWorkingDirectory)
                    )
                }
            } else {
                serverHandler?.input(str)
            }

        }
    }
}