package net.zhuruoling.omms.crystal.plugin

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.zhuruoling.omms.crystal.command.CommandSourceStack
import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.event.EventHandler
import net.zhuruoling.omms.crystal.event.PluginEvent
import net.zhuruoling.omms.crystal.main.SharedConstants

class ServerInterface(private val pluginName: String) {
    val logger: PluginLogger = PluginLogger(pluginName)
    @Synchronized
    fun registerEventHandler(e:Event, handler: EventHandler){
        SharedConstants.eventDispatcher.registerHandler(e, handler)
        if (SharedConstants.pluginEventHandlerTable[pluginName] == null){
            SharedConstants.pluginEventHandlerTable[pluginName] = arrayListOf(Pair(e,handler))
        }
        else{
            SharedConstants.pluginEventHandlerTable[pluginName]!!.add(Pair(e,handler))
        }
    }

    @Synchronized
    fun registerPluginEvent(eventId: String, priority: Int = 1){
        if(SharedConstants.pluginEventTable[pluginName] == null){
            SharedConstants.pluginEventTable[pluginName] = hashMapOf()
            SharedConstants.pluginEventTable[pluginName]!![eventId] = PluginEvent(eventId,pluginName,priority)
        }
        else{
            SharedConstants.pluginEventTable[pluginName]!![eventId] = PluginEvent(eventId,pluginName,priority)
        }
    }

    @Synchronized
    fun registerPluginCommand(commandSourceStackLiteralArgumentBuilder: LiteralArgumentBuilder<CommandSourceStack>){
        SharedConstants.commandDispatcher.register(commandSourceStackLiteralArgumentBuilder)
        if(SharedConstants.pluginCommandTable[pluginName] == null){
            SharedConstants.pluginCommandTable[pluginName] = arrayListOf()
            SharedConstants.pluginCommandTable[pluginName]!!.add(commandSourceStackLiteralArgumentBuilder)
        }
        else{
            if (!SharedConstants.pluginCommandTable[pluginName]!!.contains(commandSourceStackLiteralArgumentBuilder))
                SharedConstants.pluginCommandTable[pluginName]!!.add(commandSourceStackLiteralArgumentBuilder)
        }
    }
}