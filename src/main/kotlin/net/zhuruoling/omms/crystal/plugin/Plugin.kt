package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.event.PluginLoadEvent
import net.zhuruoling.omms.crystal.event.PluginLoadEventArgs
import net.zhuruoling.omms.crystal.event.PluginUnloadEvent
import net.zhuruoling.omms.crystal.event.PluginUnloadEventArgs
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.util.Manager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("PluginManager")

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
        SharedConstants.eventDispatcher.registerHandler(pluginLoadEvent){e ->
            e as PluginLoadEventArgs
            e.pluginInstance.pluginInstance.onLoad(e.serverInterface)
        }
        SharedConstants.eventDispatcher.registerHandler(pluginUnloadEvent){e ->
            e as PluginUnloadEventArgs
            e.pluginInstance.pluginInstance.onUnload(e.serverInterface)
        }
        pair
    }){
    fun load(id: String){
        if (this.map.containsKey(id)){
            if (this.map[id]!!.pluginInstance.pluginStatus != PluginStatus.UNLOADED){
                SharedConstants.eventLoop.dispatch(PluginLoadEvent(id), PluginLoadEventArgs(id, ServerInterface(id), this.map[id]!!))
            }else{
                throw PluginAlreadyLoadedException("Plugin $id already loaded.")
            }
        }else{
            throw PluginNotExistException("Plugin $id does not exist")
        }
    }

    fun loadAll(){
        this.map.forEach { (t, u) ->
            SharedConstants.eventLoop.dispatch(PluginLoadEvent(t), PluginLoadEventArgs(t, ServerInterface(t), u))
        }
    }

    fun unloadAll(){
        this.map.forEach { (t, u) ->
            SharedConstants.eventLoop.dispatch(PluginLoadEvent(t), PluginLoadEventArgs(t, ServerInterface(t), u))
        }
    }
}

class PluginNotExistException(message: String?) : RuntimeException(message)

class PluginAlreadyLoadedException(message: String?) : RuntimeException(message)
