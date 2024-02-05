package net.zhuruoling.omms.crystal.config

import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.server.ServerPropertiesAccess
import net.zhuruoling.omms.crystal.util.createLogger
import net.zhuruoling.omms.crystal.util.joinFilePaths
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.io.path.Path

val configContentBase: String
    get() = """
    #OMMS Crystal properties
    #${SimpleDateFormat("EEE MMM dd hh:mm:ss z YYYY", Locale.ENGLISH).format(Date())}
    #The working dir of your server folder
    workingDirectory=server
    launchCommand=java -Xmx4G -Xms1G -jar server.jar nogui
    pluginDirectory=plugins
    #Server Types:vanilla(builtin)
    serverType=vanilla
    #encoding used in parsing server output
    encoding=GBK
    commandPrefix=.
    enableRcon=false
    rconPort=25575
    lang=en_us
    rconPassword=
    rconServerPassword=12345678
    rconServerPort=25576
    #debug options:
    #   N:None/Off
    #   A:All
    #   E:Event
    #   O:Main
    #   P:Plugin
    #   S:Server
    debugOptions=N
""".trimIndent()

object Config {
    private val logger = createLogger("Config")
    var serverWorkingDirectory: String = "server"
    var launchCommand: String = ""
    var pluginDirectory = ""
    var serverType = ""
    var commandPrefix = "."
    var parserName = ""
    var encoding = "UTF-8"
    var lang = "en_us"
    var enableRcon = false
    var rconPort = "25575"
    var rconPassword = ""
    var rconServerPassword = "12345678"
    var rconServerPort = 25576
    private var debugOptString = "N"
    fun load(): Boolean {
        var isInit = false
        val configPath = joinFilePaths("config.properties")
        if (!Files.exists(Path(configPath))) {
            isInit = true
            Files.createFile(Path(configPath))
            val writer = FileWriter(File(configPath))
            writer.write(configContentBase)
            writer.flush()
            writer.close()
        }
        val properties = Properties()
        val reader = FileReader(configPath)
        properties.load(reader)
        serverWorkingDirectory = properties["workingDirectory"] as String
        serverType = properties["serverType"] as String
        launchCommand = properties["launchCommand"] as String
        pluginDirectory = properties["pluginDirectory"] as String
        commandPrefix = properties["commandPrefix"] as String
        parserName = if (serverType == "vanilla") "builtin" else serverType
        encoding = properties["encoding"] as String
        lang = properties["lang"] as String? ?: "en_us"
        debugOptString = properties["debugOptions"] as String? ?: "N"
        DebugOptions.parse(debugOptString)
        enableRcon = (properties["enableRcon"] as String?).toBoolean()
        val port = properties["rconPort"] as String? ?: ""
        val password = properties["rconPassword"] as String? ?: ""
        rconServerPassword = properties["rconServerPassword"] as String? ?: "12345678"
        rconServerPort = (properties["rconServerPort"] as String?)?.toInt() ?: 25576
        reader.close()
        if (enableRcon and (port.isBlank() || password.isBlank())) {
            logger.error(
                "Rcon is enabled and no ${
                    if (port.isBlank() and password.isBlank())
                        "rcon password or port"
                    else
                        if (password.isBlank())
                            "password"
                        else
                            "port"
                } provided!"
            )
            logger.info("Attempt to fill config with server.properties")
            try {
                val serverProperties = ServerPropertiesAccess.tryAccess()
                enableRcon = (serverProperties["enable-rcon"] as String?).toBoolean()
                rconPassword = serverProperties["rcon.password"] as String? ?: ""
                rconPort = serverProperties["rcon.port"] as String? ?: "25575"
            } catch (e: Exception) {
                throw RuntimeException("Bad config file, cannot fill config with detected environment.", e)
            }
        }
        write()
        return isInit
    }

    private fun write() {
        val properties = Properties()
        properties["workingDirectory"] = serverWorkingDirectory
        properties["launchCommand"] = launchCommand
        properties["pluginDirectory"] = pluginDirectory
        properties["serverType"] = serverType
        properties["commandPrefix"] = commandPrefix
        properties["encoding"] = encoding
        properties["lang"] = lang
        properties["enableRcon"] = enableRcon.toString()
        properties["rconPort"] = rconPort
        properties["rconPassword"] = rconPassword
        properties["rconServerPassword"] = rconServerPassword
        properties["rconServerPort"] = rconServerPort.toString()
        properties["debugOptions"] = debugOptString
        properties.store(FileWriter("config.properties"), "OMMS Crystal properties")
    }
}