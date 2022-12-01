package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.event.EventHandler
import net.zhuruoling.omms.crystal.main.SharedConstants

class ServerInterface(pluginName: String) {
    val logger: PluginLogger = PluginLogger(pluginName)
    fun registerEventHandler(e:Event, handler: EventHandler){
        SharedConstants.eventDispatcher.registerHandler(e, handler)

    }

    fun registerPluginEvent(id: String){

    }
}