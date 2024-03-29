package icu.takeneko.omms.crystal.main

import com.mojang.brigadier.exceptions.CommandSyntaxException
import icu.takeneko.omms.crystal.command.*
import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.console.ConsoleHandler
import icu.takeneko.omms.crystal.event.*
import icu.takeneko.omms.crystal.i18n.TranslateManager
import icu.takeneko.omms.crystal.main.SharedConstants.consoleHandler
import icu.takeneko.omms.crystal.main.SharedConstants.eventDispatcher
import icu.takeneko.omms.crystal.main.SharedConstants.eventLoop
import icu.takeneko.omms.crystal.main.SharedConstants.serverThreadDaemon
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.rcon.Rcon
import icu.takeneko.omms.crystal.server.ServerStatus
import icu.takeneko.omms.crystal.server.ServerThreadDaemon
import icu.takeneko.omms.crystal.server.serverStatus
import icu.takeneko.omms.crystal.text.Color
import icu.takeneko.omms.crystal.text.Text
import icu.takeneko.omms.crystal.text.TextGroup
import icu.takeneko.omms.crystal.util.BuildProperties
import icu.takeneko.omms.crystal.util.PRODUCT_NAME
import icu.takeneko.omms.crystal.util.createLogger
import icu.takeneko.omms.crystal.util.joinFilePaths
import java.lang.management.ManagementFactory
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.system.exitProcess


fun exit() {
    thread(start = true, name = "ShutdownThread") {
        //PluginManager.unloadAll()
        PermissionManager.writePermission()
        eventLoop.exit()
        eventDispatcher.shutdown()
        consoleHandler.interrupt()
    }

}


fun init() {
    val logger = createLogger("EventHandler")
    CommandManager.run {
        register(icu.takeneko.omms.crystal.command.helpCommand)
        register(icu.takeneko.omms.crystal.command.permissionCommand)
        register(icu.takeneko.omms.crystal.command.startCommand)
        register(icu.takeneko.omms.crystal.command.stopCommand)
        register(icu.takeneko.omms.crystal.command.pluginCommand)
    }

    eventDispatcher.run {
        registerHandler(ServerStoppingEvent){
            if (Config.enableRcon){
                Rcon.close()
            }
        }
        registerHandler(ServerStoppedEvent) {
            it as ServerStoppedEventArgs
            logger.info("Server exited with return value ${it.retValue}")
            serverThreadDaemon = null
            serverStatus = ServerStatus.STOPPED
            if (it.cause == "crystal") {
                Rcon.stopServer()
                logger.info("Bye.")
                exit()
            }
        }
        registerHandler(ServerStopEvent) {
            it as ServerStopEventArgs
            if (serverThreadDaemon == null) {
                logger.warn("Server is not running!")
            } else {
                serverThreadDaemon!!.stopServer(who = it.id, force = it.force)
                serverStatus = ServerStatus.STOPPING
            }
        }
        registerHandler(ServerStartingEvent) {
            it as ServerStartingEventArgs
            SharedConstants.serverVersion = it.version
        }
        registerHandler(ServerStartEvent) {
            val args = (it as ServerStartEventArgs)
            if (serverThreadDaemon != null) {
                logger.warn("Server already running!")
            }
            logger.info("Starting server using command ${args.startupCmd} at dir: ${args.workingDir}")

            serverThreadDaemon =
                ServerThreadDaemon(args.startupCmd, args.workingDir)
            serverThreadDaemon!!.start()
        }
        registerHandler(ServerStartingEvent) {
            it as ServerStartingEventArgs
            serverStatus = ServerStatus.STARTING
            logger.info("Server is running at pid ${it.pid}")
        }
        registerHandler(PlayerJoinEvent) {
            it as PlayerJoinEventArgs
            if (it.player !in PermissionManager) {
                PermissionManager[it.player] = PermissionManager.defaultPermissionLevel
            }
        }
        registerHandler(RconStartedEvent) {
            it as RconStartedEventArgs
            if (Config.enableRcon) {
                logger.info("Attempt to init rcon connection.")
                Rcon.connect()
                logger.info("Rcon connected.")
            }
        }
        registerHandler(ServerStartedEvent) {
            it as ServerStartedEventArgs
            serverStatus = ServerStatus.RUNNING
        }
        registerHandler(PlayerInfoEvent) {
            it as PlayerInfoEventArgs
            if (it.content.startsWith(Config.commandPrefix)) {
                val commandSourceStack =
                    CommandSourceStack(CommandSource.PLAYER, it.player, PermissionManager.getPermission(it.player))
                try {
                    CommandManager.execute(it.content, commandSourceStack)
                } catch (e: CommandSyntaxException) {
                    commandSourceStack.sendFeedback(
                        Text("Incomplete or invalid command${if (e.message != null) ", see errors below:" else ""}\n").withColor(
                            Color.red
                        )
                    )
                    if (e.message != null) {
                        commandSourceStack.sendFeedback(Text(e.message!!).withColor(Color.red))
                    }
                } catch (e: Exception) {
                    logger.error("An exception was thrown while processing command.", e)
                    commandSourceStack.sendFeedback(
                        TextGroup(
                            Text("Unexpected error occurred while executing command:\n").withColor(Color.red),
                            Text(e.message!!).withColor(Color.red)
                        )
                    )
                }
            }
        }
        registerHandler(ServerConsoleInputEvent) {
            it as ServerConsoleInputEventArgs
            if (serverThreadDaemon != null) {
                serverThreadDaemon!!.input(it.content)
            } else {
                logger.warn("Server is NOT running!")
            }
        }
    }
}

fun main(args: Array<String>) {
    val start = System.currentTimeMillis()
    println("Starting icu.takeneko.omms.crystal.main.MainKt.main()")
    Runtime.getRuntime().run {
        val thread = thread(name = "ShutdownThread", start = false) {
            if (serverThreadDaemon != null) {
                println("Stopping server because jvm is shutting down.")
                serverThreadDaemon!!.outputHandler.interrupt()
                serverThreadDaemon!!.stopServer(true)
            }
        }
        this.addShutdownHook(thread)
    }
    consoleHandler = ConsoleHandler()
    consoleHandler.start()
    //registerEvents()
    val logger = createLogger("Main")
    logger.info("Hello World!")
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    logger.info("$PRODUCT_NAME ${BuildProperties["version"]} is running on ${os.name} ${os.arch} ${os.version} at pid ${runtime.pid}")
    try {
        if (Config.load()) {
            logger.warn("First startup detected.")
            logger.warn("You may fill the config file to continue.")
            if (Files.exists(Path(joinFilePaths("server"))) || !Files.isDirectory(Path(joinFilePaths("server")))) {
                Files.createDirectory(Path(joinFilePaths("server")))
            }
            exitProcess(1)
        }
        if (DebugOptions.mainDebug()) {
            logger.info("Config:")
            logger.info("\tServerWorkingDirectory: ${Config.serverWorkingDirectory}")
            logger.info("\tLaunchCommand: ${Config.launchCommand}")
            logger.info("\tPluginDirectory: ${Config.pluginDirectory}")
            logger.info("\tServerType: ${Config.serverType}")
            logger.info("\tDebugOptions: $DebugOptions")
        }
        SharedConstants.language = Config.lang
        TranslateManager.init()
        CommandHelpManager.init()
        eventDispatcher = EventDispatcher()
        eventLoop = EventLoop()
        eventLoop.start()
        init()
        PluginManager.init()
        PluginManager.loadAll()
        PermissionManager.init()
        consoleHandler.reload()
        val end = System.currentTimeMillis()
        Rcon.startServer()
        logger.info("Startup preparations finished in ${end - start} milliseconds.")
        if (args.contains("--noserver")) {
            Thread.sleep(1500)
            exit()
            exitProcess(0)
        }
        eventLoop.dispatch(ServerStartEvent, ServerStartEventArgs(Config.launchCommand, Config.serverWorkingDirectory))
    } catch (e: Exception) {
        logger.error("Unexpected error occurred.", e)
        exitProcess(1)
    }
}
