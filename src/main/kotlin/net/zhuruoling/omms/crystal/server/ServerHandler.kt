package net.zhuruoling.omms.crystal.server


import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.main.SharedConstants.serverHandler
import net.zhuruoling.omms.crystal.parser.BasicParser
import net.zhuruoling.omms.crystal.util.createServerLogger
import net.zhuruoling.omms.crystal.util.resloveCommand
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

enum class LaunchParameter


class ServerHandler(
    private val launchCommand: String,
    private val workingDir: String,
    vararg launchParameters: LaunchParameter?
) :
    Thread("ServerHandler") {

    private val launchParameters: Array<out LaunchParameter?>
    private val logger = LoggerFactory.getLogger("ServerHandler")
    private lateinit var out: OutputStream
    private lateinit var input: InputStream
    private val inputMap = mutableMapOf<Int, String>()
    private var who = "crystal"
    private var process: Process? = null

    init {
        this.launchParameters = launchParameters
    }

    override fun run() {
        process = Runtime.getRuntime().exec(resloveCommand(launchCommand), null, File(workingDir))
        out = process!!.outputStream
        input = process!!.inputStream
        val handler = ServerOutputHandler(process!!, *launchParameters)
        handler.start()
        val writer = out.writer(Charset.defaultCharset())
        while (process!!.isAlive) {
            if (inputMap.isNotEmpty()) {
                inputMap.forEach {
                    inputMap.remove(it.key, it.value)
                    logger.debug("Handling input ${it.value}")
                    writer.write(it.value + "\n")
                    writer.flush()
                }
            }
            sleep(10)
        }
        val exitCode = process!!.exitValue()
        logger.info("Server exited with exit code $exitCode.")
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

    init {
        this.launchParameters = launchParameters
    }

    override fun run() {
        try {
            SharedConstants.eventLoop.dispatch(ServerStartingEvent, ServerStartingEventArgs(serverProcess.pid()))
            input = serverProcess.inputStream
            val reader = input.bufferedReader(Charset.forName("GBK"))
            while (serverProcess.isAlive) {
                sleep(10)
                val string = reader.readLine()
                if (string != null) {
                    val info = BasicParser().parseToBareInfo(string)
                    if (info == null) {
                        println(string)
                    }
                    else {
                        SharedConstants.eventLoop.dispatch(ServerInfoEvent, ServerInfoEventArgs(info))
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
        }
    }
}