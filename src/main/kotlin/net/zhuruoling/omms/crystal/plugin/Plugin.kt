package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.event.PluginLoadEvent
import net.zhuruoling.omms.crystal.event.PluginLoadEventArgs
import net.zhuruoling.omms.crystal.event.PluginUnloadEvent
import net.zhuruoling.omms.crystal.event.PluginUnloadEventArgs
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.util.Manager
import net.zhuruoling.omms.crystal.util.createLogger
import net.zhuruoling.omms.crystal.util.unregisterCommand
import org.slf4j.Logger

private val logger: Logger = createLogger("PluginLogger")

data class PluginInstance(val id: String, val pluginInstance: GroovyPluginInstance)

enum class PluginStatus {
    LOADED, UNLOADED, NONE
}

object PluginManager : Manager<String, PluginInstance>(
    scanFolder = "plugins",
    fileNameFilter = { s: String -> s.endsWith(".groovy") },
    beforeInit = null,
    afterInit = null,
    initializer = {
        val instance = GroovyPluginInstance(it).initPlugin()
        logger.info("Plugin metadata of $it is ${instance.metadata}")
        val pair = Pair(instance.metadata.id!!, PluginInstance(instance.metadata.id!!, instance))
        val pluginLoadEvent = PluginLoadEvent(instance.metadata.id!!)
        val pluginUnloadEvent = PluginUnloadEvent(instance.metadata.id!!)
        SharedConstants.eventDispatcher.registerHandler(pluginLoadEvent) { eventArgs ->
            eventArgs as PluginLoadEventArgs
            logger.info("Loading plugin ${eventArgs.pluginId}")
            if (eventArgs.pluginInstance.pluginInstance.pluginStatus == PluginStatus.LOADED) {
                logger.warn("Plugin ${eventArgs.pluginId} already loaded!")
                return@registerHandler
            }
            eventArgs.pluginInstance.pluginInstance.onLoad(eventArgs.serverInterface)
        }
        SharedConstants.eventDispatcher.registerHandler(pluginUnloadEvent) { eventArgs ->
            eventArgs as PluginUnloadEventArgs
            logger.info("Unoading plugin ${eventArgs.pluginId}")
            if (eventArgs.pluginInstance.pluginInstance.pluginStatus == PluginStatus.UNLOADED) {
                logger.warn("Plugin ${eventArgs.pluginId} already unloaded!")
                return@registerHandler
            }
            eventArgs.pluginInstance.pluginInstance.onUnload(eventArgs.serverInterface)
            PluginManager.doPluginCleanup(eventArgs.pluginId)
        }
        pair
    }) {
    fun load(id: String) {
        if (this.map.containsKey(id)) {
            if (this.map[id]!!.pluginInstance.pluginStatus != PluginStatus.UNLOADED) {
                SharedConstants.eventLoop.dispatch(
                    PluginLoadEvent(id),
                    PluginLoadEventArgs(id, ServerInterface(id), this.map[id]!!)
                )
            } else {
                throw PluginAlreadyLoadedException("Plugin $id already loaded.")
            }
        } else {
            throw PluginNotExistException("Plugin $id does not exist")
        }
    }

    fun loadAll() {
        this.map.forEach { (t, u) ->
            SharedConstants.eventLoop.dispatch(PluginLoadEvent(t), PluginLoadEventArgs(t, ServerInterface(t), u))
        }
    }

    fun unloadAll() {
        this.map.forEach { (t, u) ->
            SharedConstants.eventLoop.dispatch(PluginLoadEvent(t), PluginLoadEventArgs(t, ServerInterface(t), u))
        }
    }

    fun doPluginCleanup(id: String) {
        SharedConstants.pluginEventHandlerTable[id]?.forEach {
            SharedConstants.eventDispatcher.unregisterHandler(it.first, it.second)
        }
        SharedConstants.pluginEventHandlerTable.remove(id)
        SharedConstants.pluginEventTable.remove(id)
        SharedConstants.pluginCommandTable[id]?.forEach {
            unregisterCommand(it, SharedConstants.commandDispatcher)
        }
        SharedConstants.pluginCommandTable.remove(id)
    }
}

class PluginNotExistException(message: String?) : RuntimeException(message)

class PluginAlreadyLoadedException(message: String?) : RuntimeException(message)