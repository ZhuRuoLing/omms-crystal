package net.zhuruoling.omms.crystal.plugin

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.zhuruoling.omms.crystal.command.CommandSourceStack
import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.config.ConfigManager
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.main.SharedConstants

class ServerInterface(private val pluginName: String) {
    private val logger: PluginLogger = PluginLogger(pluginName)

    fun getLogger():PluginLogger{
        return logger
    }

    @Synchronized
    fun registerEventHandler(e: Event, handler: EventHandler) {
        SharedConstants.eventDispatcher.registerHandler(e, handler)
        if (SharedConstants.pluginEventHandlerTable[pluginName] == null) {
            SharedConstants.pluginEventHandlerTable[pluginName] = arrayListOf(Pair(e, handler))
        } else {
            SharedConstants.pluginEventHandlerTable[pluginName]!!.add(Pair(e, handler))
        }
    }

    @Synchronized
    fun registerPluginEvent(eventId: String, priority: Int = 1) {
        if (SharedConstants.pluginEventTable[pluginName] == null) {
            SharedConstants.pluginEventTable[pluginName] = hashMapOf()
            SharedConstants.pluginEventTable[pluginName]!![eventId] = PluginEvent(eventId, pluginName, priority)
        } else {
            SharedConstants.pluginEventTable[pluginName]!![eventId] = PluginEvent(eventId, pluginName, priority)
        }
    }

    @Synchronized
    fun registerPluginCommand(commandSourceStackLiteralArgumentBuilder: LiteralArgumentBuilder<CommandSourceStack>) {
        SharedConstants.commandDispatcher.register(commandSourceStackLiteralArgumentBuilder)
        if (SharedConstants.pluginCommandTable[pluginName] == null) {
            SharedConstants.pluginCommandTable[pluginName] = arrayListOf()
            SharedConstants.pluginCommandTable[pluginName]!!.add(commandSourceStackLiteralArgumentBuilder)
        } else {
            if (!SharedConstants.pluginCommandTable[pluginName]!!.contains(commandSourceStackLiteralArgumentBuilder))
                SharedConstants.pluginCommandTable[pluginName]!!.add(commandSourceStackLiteralArgumentBuilder)
        }
        SharedConstants.consoleHandler.reload()
    }

    @Synchronized
    fun loadConfig(
        createIfNotExist: Boolean = true,
        defaultConfig: MutableMap<String, Any>? = null
    ): MutableMap<String, Any> {
        return ConfigManager.getConfig(pluginName, createIfNotExist, defaultConfig)
    }


    @Synchronized
    fun startServer(): Boolean {
        if (SharedConstants.serverHandler == null) {
            SharedConstants.eventLoop.dispatch(
                ServerStartEvent, ServerStartEventArgs(
                    Config.launchCommand, Config.serverWorkingDirectory
                )
            )
            return true
        } else {
            logger.warn("Server is running!")
            return false
        }
    }

    @Synchronized
    fun stopServer(force: Boolean = false): Boolean {
        if (SharedConstants.serverHandler == null) {
            logger.warn("Server is not running")
            return false
        } else {
            SharedConstants.eventLoop.dispatch(ServerStopEvent, ServerStopEventArgs(pluginName, force))
            return true
        }
    }


}