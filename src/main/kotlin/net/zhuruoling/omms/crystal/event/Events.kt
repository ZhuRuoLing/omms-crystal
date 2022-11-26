package net.zhuruoling.omms.crystal.event

import net.zhuruoling.omms.crystal.parser.Info
import java.util.StringTokenizer


//base
open class Event(val id: String, val priority: Int)
open class EventArgs

//plugin
class PluginEvent(id: String, val pluginId: String, priority: Int = 1) : Event(id, priority)
class PluginEventArgs : EventArgs() {
    private val hashMap: HashMap<String, Any> = hashMapOf()
    fun insert(key: String, value: Any) {
        hashMap[key] = value
    }

    fun get(key: String): Any? {
        return if (hashMap.containsKey(key)) hashMap[key] else throw NoSuchElementException("")
    }
}

/*
crystal.server.info         v
crystal.server.start        v
crystal.server.starting     v
crystal.server.started      v
crystal.server.stopping     v
crystal.server.stopped      v
crystal.server.player.info  v
crystal.server.player.join  v
crystal.server.player.left  v
crystal.plugin.load         v
crystal.plugin.unload       v
*/


//server
object ServerInfoEvent : Event("crystal.server.info", 1000)
class ServerInfoEventArgs(val info: Info) : EventArgs()

object ServerStartEvent : Event("crystal.server.start", 1000)
class ServerStartEventArgs(val startupCmd: String, val workingDir: String) : EventArgs()

object ServerStartingEvent : Event("crystal.server.starting", 1000)
class ServerStartingEventArgs(val pid: Long) : EventArgs()

object ServerStartedEvent : Event("crystal.server.started", 1000)
class ServerStartedEventArgs() : EventArgs()

object ServerStopEvent : Event("crystal.server.stop", 1000)
class ServerStopEventArgs(val id: String, val force: Boolean) : EventArgs()

object ServerStoppingEvent : Event("crystal.server.stopping", 1000)
class ServerStoppingEventArgs(val id: String) : EventArgs()

object ServerStoppedEvent : Event("crystal.server.stopped", 1000)
class ServerStoppedEventArgs(val retValue: Int, val who: String) : EventArgs()


//player
object PlayerInfoEvent : Event("crystal.server.player.info", 100)
class PlayerInfoEventArgs(val content: String, val player: String) : EventArgs()

object PlayerJoinEvent : Event("crystal.server.player.join", 100)
class PlayerJoinEventArgs(val player: String) : EventArgs()

object PlayerLeftEvent : Event("crystal.server.player.left", 100)
class PlayerLeftEventArgs(val player: String) : EventArgs()


//plugin
object PluginLoadEvent : Event("crystal.plugin.load", 10)
class PluginLoadEventArgs(pluginId: String) : EventArgs()

object PluginUnloadEvent : Event("crystal.plugin.unload", 10)
class PluginUnloadEventArgs(pluginId: String) : EventArgs()