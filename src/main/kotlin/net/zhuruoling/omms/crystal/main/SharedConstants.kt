package net.zhuruoling.omms.crystal.main

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.zhuruoling.omms.crystal.console.ConsoleHandler
import net.zhuruoling.omms.crystal.console.command.CommandSourceStack
import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.event.EventDispatcher
import net.zhuruoling.omms.crystal.event.EventHandler
import net.zhuruoling.omms.crystal.event.EventLoop
import net.zhuruoling.omms.crystal.server.ServerHandler
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object SharedConstants {
    var serverHandler: ServerHandler? = null
    lateinit var eventDispatcher: EventDispatcher
    lateinit var eventLoop: EventLoop
    lateinit var consoleHandler: ConsoleHandler
    val pluginCommandTable: HashMap<String, ArrayList<LiteralArgumentBuilder<CommandSourceStack>>> = hashMapOf()
    val pluginEventTable: HashMap<String, ArrayList<Event>> = hashMapOf()
    val pluginEventHandlerTable: HashMap<String, ArrayList<EventHandler>> = hashMapOf()
    var needExit = true
}

object DebugOptions {
    /*
    #debug options:
 #   N/O:None/Off
 #   A:All
 #   E:Event
 #   M:Main
 #   P:Plugin
 #   S:Server
     */
    private var off = true
    private var all = false
    private var event = false
    private var main = false
    private var plugin = false
    private var server = false
    fun parse(options: String) {
        val o = options.uppercase(Locale.getDefault())
        off = o.contains("N") or o.contains("O")
        all = o.contains("A")
        event = o.contains("E")
        main = o.contains("M")
        plugin = o.contains("P")
        server = o.contains("S")
    }

    fun allDebug() = !off and all
    fun eventDebug() = !off and (all or event)
    fun mainDebug() = !off and (all or main)
    fun pluginDebug() = !off and (all or plugin)
    fun serverDebug() = !off and (all or server)

    override fun toString(): String {
        return (
                if (off)
                    "OFF "
                else (
                        if (allDebug())
                            "ALL "
                        else (
                                (
                                        if (eventDebug()) "EVENT " else "")
                                        + (if (mainDebug()) "MAIN " else "")
                                        + (if (pluginDebug()) "PLUGIN " else "")
                                        + (if (serverDebug()) "SERVER " else "")
                                )
                        )
                )
    }
}