package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.console.ConsoleHandler
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.main.SharedConstants.consoleHandler
import net.zhuruoling.omms.crystal.main.SharedConstants.eventLoop
import net.zhuruoling.omms.crystal.main.SharedConstants.serverHandler
import net.zhuruoling.omms.crystal.plugin.PluginManager
import net.zhuruoling.omms.crystal.server.ServerHandler
import net.zhuruoling.omms.crystal.util.PRODUCT_NAME
import net.zhuruoling.omms.crystal.util.createLogger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory



fun exit() {
    eventLoop.exit()
    consoleHandler.interrupt()
}

fun init() {
    val logger = createLogger("EventHandler")
    SharedConstants.eventDispatcher.registerHandler(ServerStoppedEvent) {
        it as ServerStoppedEventArgs
        logger.info("Server exited with return value ${it.retValue}")
        serverHandler = null
        if (it.who == "crystal") {
            logger.info("Bye.")
            exit()
        }
    }
    SharedConstants.eventDispatcher.registerHandler(ServerStopEvent) {
        it as ServerStopEventArgs
        if (serverHandler == null) {
            logger.warn("Server is not running!")
        } else {
            serverHandler!!.stopServer(who = it.id, force = it.force)
        }
    }
    SharedConstants.eventDispatcher.registerHandler(ServerStartEvent) {

        val args = (it as ServerStartEventArgs)
        if (serverHandler != null) {
            logger.warn("Server already running!")
        }
        logger.info("Starting server using command ${args.startupCmd} at dir: ${args.workingDir}")
        serverHandler =
            ServerHandler(args.startupCmd, args.workingDir)
        serverHandler!!.start()
    }
    SharedConstants.eventDispatcher.registerHandler(ServerStartingEvent) {
        it as ServerStartingEventArgs
        logger.info("Server is running at pid ${it.pid}")
    }
}


fun main(args: Array<String>) {
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
    }
    logger.info("Config:")
    logger.info("\tServerWorkingDirectory: ${Config.serverWorkingDirectory}")
    logger.info("\tLaunchCommand: ${Config.launchCommand}")
    logger.info("\tPluginDirectory: ${Config.pluginDirectory}")
    logger.info("\tServerType: ${Config.serverType}")
    logger.info("\tDebugOptions: $DebugOptions")

    SharedConstants.eventDispatcher = EventDispatcher()
    eventLoop = EventLoop()
    eventLoop.start()
    PluginManager.init()
    PluginManager.loadAll()
    init()
    consoleHandler = ConsoleHandler()
    consoleHandler.start()
    PluginManager.loadAll()
    eventLoop.dispatch(ServerStartEvent, ServerStartEventArgs(Config.launchCommand, Config.serverWorkingDirectory))
}

