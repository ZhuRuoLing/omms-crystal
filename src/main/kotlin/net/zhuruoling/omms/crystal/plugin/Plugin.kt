package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.command.CommandManager
import net.zhuruoling.omms.crystal.event.PluginLoadEvent
import net.zhuruoling.omms.crystal.event.PluginLoadEventArgs
import net.zhuruoling.omms.crystal.event.PluginUnloadEvent
import net.zhuruoling.omms.crystal.event.PluginUnloadEventArgs
import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.parser.ParserManager
import net.zhuruoling.omms.crystal.util.Manager
import net.zhuruoling.omms.crystal.util.createLogger
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
        if (DebugOptions.pluginDebug()) {
            logger.info("Plugin metadata of $it is ${instance.metadata}")
        }
        val pair = Pair(instance.metadata.id!!, PluginInstance(instance.metadata.id!!, instance))

        if (DebugOptions.pluginDebug()) {
            logger.info("Registering plugin ${instance.metadata.id} declared api methods:")
            instance.apiMethods.forEach { (_, method) ->
                logger.info("\tPlugin ${instance.metadata.id} declared method: ${method.name}")
            }
        }
        SharedConstants.pluginDeclaredApiMethodMap[instance.metadata.id!!] =
            instance.apiMethods
        SharedConstants.pluginDeclaredEventHandlerMap[instance.metadata.id!!] =
            instance.eventHandlers
        SharedConstants.pluginDeclaredParserMap[instance.metadata.id!!] =
            instance.parsers

        instance.eventHandlers.forEach { (t, u) ->
            SharedConstants.eventDispatcher.registerHandler(t, u)
        }
        instance.parsers.forEach { (s, p) ->
            ParserManager.registerParser(s, p)
        }

        val pluginLoadEvent = PluginLoadEvent(instance.metadata.id!!)
        val pluginUnloadEvent = PluginUnloadEvent(instance.metadata.id!!)
        SharedConstants.eventDispatcher.registerHandler(pluginLoadEvent) { eventArgs ->
            eventArgs as PluginLoadEventArgs
            logger.info("Loading plugin ${eventArgs.pluginId}")
            if (eventArgs.pluginInstance.pluginInstance.pluginStatus == PluginStatus.LOADED) {
                logger.warn("Plugin ${eventArgs.pluginId} already loaded!")
                return@registerHandler
            }
            eventArgs.pluginInstance.pluginInstance.onLoad(eventArgs.crystalInterface)
        }

        SharedConstants.eventDispatcher.registerHandler(pluginUnloadEvent) { eventArgs ->
            eventArgs as PluginUnloadEventArgs
            logger.info("Unloading plugin ${eventArgs.pluginId}")
            if (eventArgs.pluginInstance.pluginInstance.pluginStatus == PluginStatus.UNLOADED) {
                logger.warn("Plugin ${eventArgs.pluginId} already unloaded!")
                return@registerHandler
            }
            eventArgs.pluginInstance.pluginInstance.onUnload(eventArgs.crystalInterface)
            PluginManager.doPluginCleanup(eventArgs.pluginId)
        }
        pair
    }) {
    fun load(id: String) {
        if (this.map.containsKey(id)) {
            if (this.map[id]!!.pluginInstance.pluginStatus != PluginStatus.UNLOADED) {
                SharedConstants.eventLoop.dispatch(
                    PluginLoadEvent(id),
                    PluginLoadEventArgs(id, CrystalInterface(id), this.map[id]!!)
                )
            } else {
                throw PluginAlreadyLoadedException("Plugin $id already loaded.")
            }
        } else {
            throw PluginNotExistException("Plugin $id does not exist")
        }
    }

    fun loadAll() {
        logger.info("Loading all Plugins.")
        this.map.forEach { (t, u) ->
            SharedConstants.eventLoop.dispatch(PluginLoadEvent(t), PluginLoadEventArgs(t, CrystalInterface(t), u))
        }
    }

    fun unloadAll() {
        logger.info("Unloading all Plugins.")
        this.map.forEach { (t, u) ->
            SharedConstants.eventLoop.dispatch(PluginUnloadEvent(t), PluginUnloadEventArgs(t, CrystalInterface(t), u))
        }
    }

    private fun doPluginCleanup(id: String) {
        if (SharedConstants.pluginDeclaredEventHandlerMap.containsKey(id)) {
            SharedConstants.pluginDeclaredEventHandlerMap[id]!!.forEach { (k, v) ->
                SharedConstants.eventDispatcher.unregisterHandler(k, v)
            }
            SharedConstants.pluginDeclaredEventHandlerMap.remove(id)
        }
        if (SharedConstants.pluginDeclaredParserMap.containsKey(id)){
            SharedConstants.pluginDeclaredParserMap[id]!!.forEach {(t, _) ->
                ParserManager.unregisterParser(t)
            }
        }
        SharedConstants.pluginRegisteredEventTable.remove(id)
        SharedConstants.pluginRegisteredCommandTable[id]?.forEach {
            CommandManager.unregister(it)
        }
        SharedConstants.pluginRegisteredCommandTable.remove(id)
        SharedConstants.pluginDeclaredApiMethodMap.remove(id)
    }

    fun getPluginInstance(id: String): PluginInstance? {
        return map[id]
    }
}

class PluginNotExistException(message: String?) : RuntimeException(message)

class PluginAlreadyLoadedException(message: String?) : RuntimeException(message)