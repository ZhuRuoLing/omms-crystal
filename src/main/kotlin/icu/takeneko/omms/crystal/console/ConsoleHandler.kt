package icu.takeneko.omms.crystal.console

import icu.takeneko.omms.crystal.command.CommandManager
import icu.takeneko.omms.crystal.command.CommandSource
import icu.takeneko.omms.crystal.command.CommandSourceStack
import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.main.SharedConstants.serverThreadDaemon
import icu.takeneko.omms.crystal.util.createLogger
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder



class ConsoleHandler : Thread("ConsoleHandler") {
    private val terminal: Terminal = TerminalBuilder.builder().system(true).dumb(true).build()
    private lateinit var lineReader: LineReader
    private val logger by lazy {
        createLogger("ConsoleHandler")
    }

    fun reload() {
        lineReader = LineReaderBuilder.builder().terminal(terminal).completer(CommandManager.completer()).build()
    }

    init {
        logger
    }

    override fun run() {
        while (true) {
            try {
                reload()
                val str = lineReader.readLine()
                if (str.startsWith(Config.commandPrefix)) {
                    try {
                        CommandManager.execute(str, CommandSourceStack(CommandSource.CONSOLE))
                    } catch (e: Exception) {
                        logger.error(e.message)
                    }
                } else {
                    serverThreadDaemon?.input(str)
                }
            } catch (e: Throwable) {
                logger.error("",e)
                break
            }
        }
    }
}