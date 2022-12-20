package net.zhuruoling.omms.crystal.main

import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.zhuruoling.omms.crystal.command.*
import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.config.ConfigManager
import net.zhuruoling.omms.crystal.console.ConsoleHandler
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.main.SharedConstants.consoleHandler
import net.zhuruoling.omms.crystal.main.SharedConstants.eventDispatcher
import net.zhuruoling.omms.crystal.main.SharedConstants.eventLoop
import net.zhuruoling.omms.crystal.main.SharedConstants.serverHandler
import net.zhuruoling.omms.crystal.permission.PermissionManager
import net.zhuruoling.omms.crystal.plugin.PluginManager
import net.zhuruoling.omms.crystal.server.ServerHandler
import net.zhuruoling.omms.crystal.text.Color
import net.zhuruoling.omms.crystal.text.Text
import net.zhuruoling.omms.crystal.text.TextGroup
import net.zhuruoling.omms.crystal.util.PRODUCT_NAME
import net.zhuruoling.omms.crystal.util.createLogger
import net.zhuruoling.omms.crystal.util.joinFilePaths
import java.lang.management.ManagementFactory
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.system.exitProcess


fun exit() {
    thread(start = true, name = "ShutdownThread") {
        val logger = createLogger("ShutDown")
        PluginManager.unloadAll()
        PermissionManager.writePermission()
        eventLoop.exit()
        eventDispatcher.shutdown()
        consoleHandler.interrupt()
    }

}


fun init() {
    val logger = createLogger("EventHandler")
    CommandManager.run {
        register(helpCommand)
        register(permissionCommand)
        register(startCommand)
        register(stopCommand)
        register(pluginCommand)
    }
    eventDispatcher.run {
        registerHandler(ServerStoppedEvent) {
            it as ServerStoppedEventArgs
            logger.info("Server exited with return value ${it.retValue} (who=${it.who})")
            serverHandler = null
            if (it.who == "crystal") {
                logger.info("Bye.")
                exit()
            }
        }
        registerHandler(ServerStopEvent) {
            it as ServerStopEventArgs
            if (serverHandler == null) {
                logger.warn("Server is not running!")
            } else {
                serverHandler!!.stopServer(who = it.id, force = it.force)
            }
        }
        registerHandler(ServerStartEvent) {
            val args = (it as ServerStartEventArgs)
            if (serverHandler != null) {
                logger.warn("Server already running!")
            }
            logger.info("Starting server using command ${args.startupCmd} at dir: ${args.workingDir}")
            serverHandler =
                ServerHandler(args.startupCmd, args.workingDir)
            serverHandler!!.start()
        }
        registerHandler(ServerStartingEvent) {
            it as ServerStartingEventArgs
            logger.info("Server is running at pid ${it.pid}")
        }
        registerHandler(PlayerJoinEvent) {
            it as PlayerJoinEventArgs
            if (!PermissionManager.playerExists(it.player)) {
                PermissionManager.setPermission(it.player)
            }
        }
        registerHandler(PlayerInfoEvent){
            it as PlayerInfoEventArgs
            if (it.content.startsWith(Config.commandPrefix)){
                val commandSourceStack = CommandSourceStack(CommandSource.PLAYER, it.player, PermissionManager.getPermission(it.player))
                try {
                    CommandManager.execute(it.content,commandSourceStack)
                }catch (e:CommandSyntaxException){
                    e.printStackTrace()
                    commandSourceStack.sendFeedBack(TextGroup(
                        Text("Incomplete or invalid command, see errors below:\n").withColor(Color.red),
                        Text(e.rawMessage.string).withColor(Color.red)
                    ))
                }
                catch (e:Exception){
                    e.printStackTrace()
                    commandSourceStack.sendFeedBack(TextGroup(
                        Text("Unexpected error occurred while executing command.:\n").withColor(Color.red),
                        Text(e.message!!).withColor(Color.red)
                    ))
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    println("Starting net.zhuruoling.omms.crystal.main.MainKt.main()")
    consoleHandler = ConsoleHandler()
    consoleHandler.start()
    registerEvents()
    val logger = createLogger("Main")
    logger.info("Hello World!")
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    logger.info(
        String.format(
            "$PRODUCT_NAME is running on %s %s %s at pid %d",
            os.name,
            os.arch,
            os.version,
            runtime.pid
        )
    )
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
    eventDispatcher = EventDispatcher()
    eventLoop = EventLoop()
    eventLoop.start()
    init()
    ConfigManager.init()
    PluginManager.init()
    PermissionManager.init()
    PluginManager.loadAll()
    consoleHandler.reload()
    if (args.contains("--noserver")){
        Thread.sleep(1500)
        exit()
        exitProcess(0)
    }
    eventLoop.dispatch(ServerStartEvent, ServerStartEventArgs(Config.launchCommand, Config.serverWorkingDirectory))
}

