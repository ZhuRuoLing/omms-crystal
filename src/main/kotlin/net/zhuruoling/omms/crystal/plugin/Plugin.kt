package net.zhuruoling.omms.crystal.plugin

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
    afterInit = {
        it.map.forEach { (s, pi) ->
            pi.pluginInstance.onLoad(ServerInterface(s))
        }
    },
    initializer = {
        val instance = GroovyPluginInstance(it).initPlugin()
        logger.info("Plugin metadata of $it is ${instance.metadata}")
        val pair = Pair(instance.metadata.id!!, PluginInstance(instance.metadata.id!!, instance))
        pair
    })

