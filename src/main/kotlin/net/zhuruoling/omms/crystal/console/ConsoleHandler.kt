package net.zhuruoling.omms.crystal.console

import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.event.ServerStartEvent
import net.zhuruoling.omms.crystal.event.ServerStartEventArgs
import net.zhuruoling.omms.crystal.event.ServerStopEvent
import net.zhuruoling.omms.crystal.event.ServerStopEventArgs
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.main.SharedConstants.serverHandler
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder

class ConsoleHandler : Thread("ConsoleHandler") {
    override fun run() {
        val terminal = TerminalBuilder.builder().system(true).dumb(true).build()
        val lineReader = LineReaderBuilder.builder().terminal(terminal).build()
        while (true) {
            try {
                val str = lineReader.readLine()
                if (str.startsWith(Config.commandPrefix)) {
                    // TODO: resolve command
                    if (str == ".fuck") {
                        SharedConstants.eventLoop.dispatch(ServerStopEvent, ServerStopEventArgs("crystal", true))
                    }
                    if (str == ".stop") {
                        SharedConstants.eventLoop.dispatch(ServerStopEvent, ServerStopEventArgs("someone", false))
                    }
                    if (str == ".bye") {
                        SharedConstants.eventLoop.dispatch(ServerStopEvent, ServerStopEventArgs("crystal", false))
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
            } catch (e: Exception) {
                break
            }
        }
    }
}