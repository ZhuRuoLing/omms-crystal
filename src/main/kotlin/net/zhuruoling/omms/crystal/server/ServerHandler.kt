package net.zhuruoling.omms.crystal.server


import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.main.SharedConstants.serverHandler
import net.zhuruoling.omms.crystal.parser.ParserManager
import net.zhuruoling.omms.crystal.util.createLogger
import net.zhuruoling.omms.crystal.util.createServerLogger
import net.zhuruoling.omms.crystal.util.resolveCommand
import org.slf4j.MarkerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.StringTokenizer
import java.util.concurrent.ConcurrentHashMap

enum class LaunchParameter

class ServerHandler(
    private val launchCommand: String,
    private val workingDir: String,
    vararg launchParameters: LaunchParameter?
) :
    Thread("ServerHandler") {

    private val launchParameters: Array<out LaunchParameter?>
    private val logger = createLogger("ServerHandler")
    private lateinit var out: OutputStream
    private lateinit var input: InputStream
    private val inputMap = ConcurrentHashMap<Int, String>()
    private var who = "crystal"
    private var process: Process? = null

    init {
        this.launchParameters = launchParameters
    }

    override fun run() {
        try {
            process = Runtime.getRuntime().exec(resolveCommand(launchCommand), null, File(workingDir))
            out = process!!.outputStream
            input = process!!.inputStream
        } catch (e: Exception) {
            logger.error("Cannot start server.", e)
            SharedConstants.eventLoop.dispatch(ServerStoppedEvent, ServerStoppedEventArgs(Integer.MIN_VALUE, who))
            return
        }
        val handler = ServerOutputHandler(process!!, *launchParameters)
        handler.start()
        val writer = out.writer(Charset.defaultCharset())
        while (process!!.isAlive) {
            if (inputMap.isNotEmpty()) {
                inputMap.forEach {
                    inputMap.remove(it.key, it.value)
                    logger.info("Handling input ${it.value}")
                    writer.write(it.value + "\n")
                    writer.flush()
                }
            }
            sleep(10)
        }
        val exitCode = process!!.exitValue()
        //logger.info("Server exited with exit code $exitCode.")
        serverHandler = null
        SharedConstants.eventLoop.dispatch(ServerStoppedEvent, ServerStoppedEventArgs(exitCode, who))
    }

    fun input(str: String) {
        inputMap[str.hashCode()] = str
    }

    fun stopServer(force: Boolean = false, who: String = "crystal") {
        this.who = who
        if (force) {
            process!!.destroyForcibly()
        } else {
            input("stop")
        }
    }
}


class ServerOutputHandler(private val serverProcess: Process, vararg launchParameters: LaunchParameter?) :
    Thread("ServerOutputHandler") {
    private val launchParameters: Array<out LaunchParameter?>
    private val logger = createServerLogger()
    private lateinit var input: InputStream
    private val parser = ParserManager.getParser(Config.parserName)
        ?: throw IllegalArgumentException("Specified parser ${Config.parserName} does not exist.")

    init {
        this.launchParameters = launchParameters
    }

    override fun run() {
        try {
            input = serverProcess.inputStream
            val reader = input.bufferedReader(Charset.forName("GBK"))
            while (serverProcess.isAlive) {
                sleep(10)
                val string = reader.readLine()
                if (string != null) {
                    val info = parser.parseToBareInfo(string)
                    if (info == null) {
                        println(string)
                    } else {
                        //dispatch a global info first
                        SharedConstants.eventLoop.dispatch(ServerInfoEvent, ServerInfoEventArgs(info))
                        //and then started to parse
                        parseAndDispatch(info.info)
                        when (info.level) {
                            Level.DEBUG -> logger.debug(MarkerFactory.getMarker(info.thread), info.info)
                            Level.ERROR -> logger.error(MarkerFactory.getMarker(info.thread), info.info)
                            Level.INFO -> logger.info(MarkerFactory.getMarker(info.thread), info.info)
                            Level.TRACE -> logger.trace(MarkerFactory.getMarker(info.thread), info.info)
                            Level.WARN -> logger.warn(MarkerFactory.getMarker(info.thread), info.info)
                        }
                    }
                }
            }
        } catch (ignored: InterruptedException) {
            //logger.detachAndStopAllAppenders()
        }
    }

    private fun parseAndDispatch(processedInfo: String) {
        val serverStartingInfo = parser.parseServerStartingInfo(processedInfo)
        if (serverStartingInfo != null) {
            dispatchEvent(ServerStartingEvent, ServerStartingEventArgs(serverProcess.pid(), serverStartingInfo.version))
            return
        }
        val serverStartedInfo = parser.parseServerStartedInfo(processedInfo)
        if (serverStartedInfo != null) {
            dispatchEvent(ServerStartedEvent, ServerStartedEventArgs(timeUsed = serverStartedInfo.timeElapsed))
            return
        }
        val serverOverloadInfo = parser.parseServerOverloadInfo(processedInfo)
        if (serverOverloadInfo != null) {
            dispatchEvent(
                ServerOverloadEvent,
                ServerOverloadEventArgs(serverOverloadInfo.ticks, serverOverloadInfo.time)
            )
            return
        }
        val serverStoppingInfo = parser.parseServerStoppingInfo(processedInfo)
        if (serverStoppingInfo != null) {
            dispatchEvent(ServerStoppingEvent, ServerStoppingEventArgs())
            return
        }
        val playerJoinInfo = parser.parsePlayerJoinInfo(processedInfo)
        if (playerJoinInfo != null) {
            dispatchEvent(PlayerJoinEvent, PlayerJoinEventArgs(player = playerJoinInfo.player))
            return
        }
        val playerInfo = parser.parsePlayerInfo(processedInfo)
        if (playerInfo != null) {
            dispatchEvent(
                PlayerInfoEvent,
                PlayerInfoEventArgs(content = playerInfo.player, player = playerInfo.content)
            )
            return
        }
        val playerLeftInfo = parser.parsePlayerLeftInfo(processedInfo)
        if (playerLeftInfo != null) {
            dispatchEvent(PlayerLeftEvent, PlayerLeftEventArgs(player = playerLeftInfo.player))
            return
        }
        return
    }

    private fun dispatchEvent(e: Event, args: EventArgs) {
        SharedConstants.eventLoop.dispatch(e, args)
    }
}