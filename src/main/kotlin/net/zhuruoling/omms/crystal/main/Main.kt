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
        return
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
    init()
    PluginManager.loadAll()
    consoleHandler = ConsoleHandler()
    consoleHandler.start()
    eventLoop.dispatch(ServerStartEvent, ServerStartEventArgs(Config.launchCommand, Config.serverWorkingDirectory))
}
/*
[2022-12-02 10:18:15.668] [main/INFO]: [DEBUG] Registering event net.zhuruoling.omms.crystal.event.PluginLoadEvent@a8641539 with handler (net.zhuruoling.omms.crystal.event.EventArgs) -> kotlin.Unit
[2022-12-02 10:18:15.671] [main/INFO]: [DEBUG] Registering event net.zhuruoling.omms.crystal.event.PluginUnloadEvent@5daaf292 with handler (net.zhuruoling.omms.crystal.event.EventArgs) -> kotlin.Unit
{my_plugin=PluginInstance(id=my_plugin, pluginInstance=GroovyPluginInstance(pluginFilePath='C:\Users\jkl-9\IdeaProjects\omms-crystal\plugins\MyPlugin.groovy', groovyClassLoader=groovy.lang.GroovyClassLoader@192d43ce, instance=MyPlugin@10afe71a, pluginStatus=NONE, metadata=PluginMetadata{id='my_plugin', version='0.0.1', author=[ZhuRuoLing], pluginDependencies=null}))}
[2022-12-02 10:18:15.675] [main/WARN]: wdnmd
[2022-12-02 10:18:15.679] [EventLoop-1/INFO]: [DEBUG] Dispatching Event crystal.plugin.load with args PluginLoadEventArgs(pluginId='my_plugin', serverInterface=net.zhuruoling.omms.crystal.plugin.ServerInterface@3ca85d7, pluginInstance=PluginInstance(id=my_plugin, pluginInstance=GroovyPluginInstance(pluginFilePath='C:\Users\jkl-9\IdeaProjects\omms-crystal\plugins\MyPlugin.groovy', groovyClassLoader=groovy.lang.GroovyClassLoader@192d43ce, instance=MyPlugin@10afe71a, pluginStatus=NONE, metadata=PluginMetadata{id='my_plugin', version='0.0.1', author=[ZhuRuoLing], pluginDependencies=null})))
[2022-12-02 10:18:15.706] [pool-2-thread-1/INFO]: [my_plugin] KONNICHIWA ZAWARUDO!
{my_plugin=PluginInstance(id=my_plugin, pluginInstance=GroovyPluginInstance(pluginFilePath='C:\Users\jkl-9\IdeaProjects\omms-crystal\plugins\MyPlugin.groovy', groovyClassLoader=groovy.lang.GroovyClassLoader@192d43ce, instance=MyPlugin@10afe71a, pluginStatus=LOADED, metadata=PluginMetadata{id='my_plugin', version='0.0.1', author=[ZhuRuoLing], pluginDependencies=null}))}
[2022-12-02 10:18:15.724] [EventLoop-1/INFO]: [DEBUG] Dispatching Event crystal.plugin.load with args PluginLoadEventArgs(pluginId='my_plugin', serverInterface=net.zhuruoling.omms.crystal.plugin.ServerInterface@4c3061e5, pluginInstance=PluginInstance(id=my_plugin, pluginInstance=GroovyPluginInstance(pluginFilePath='C:\Users\jkl-9\IdeaProjects\omms-crystal\plugins\MyPlugin.groovy', groovyClassLoader=groovy.lang.GroovyClassLoader@192d43ce, instance=MyPlugin@10afe71a, pluginStatus=LOADED, metadata=PluginMetadata{id='my_plugin', version='0.0.1', author=[ZhuRuoLing], pluginDependencies=null})))
[2022-12-02 10:18:15.725] [EventLoop-1/INFO]: [DEBUG] Dispatching Event crystal.server.start with args ServerStartEventArgs(startupCmd='C:\jdk-17.0.3.7-hotspot\bin\java.exe -Xmx4G -Xms1G -jar server.jar nogui', workingDir='server')
[2022-12-02 10:18:15.725] [pool-2-thread-2/WARN]: Plugin my_plugin already loaded!
FUCK
 */
