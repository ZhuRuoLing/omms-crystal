package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.console.ConsoleHandler
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.main.SharedConstants.eventLoop
import net.zhuruoling.omms.crystal.plugin.PluginManager
import net.zhuruoling.omms.crystal.util.PRODUCT_NAME
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

private val logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {
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
    PluginManager.init()
    SharedConstants.eventDispatcher = EventDispatcher()
    eventLoop = EventLoop()
    eventLoop.start()
    init()
    ConsoleHandler().start()
    eventLoop.dispatch(ServerStartEvent, ServerStartEventArgs(Config.launchCommand, Config.serverWorkingDirectory))
}

fun exit() {
    eventLoop.exit()
}