package net.zhuruoling.omms.crystal.event

import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.util.createLogger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors


class EventDispatcher {
    private val eventMap: ConcurrentHashMap<Event, ArrayList<(EventArgs) -> Unit>> = ConcurrentHashMap()
    private val executor = Executors.newFixedThreadPool(4)
    private val logger = createLogger("EventDispatcher", DebugOptions.eventDebug())
    fun dispatchEvent(e: Event, args: EventArgs) {
        if (DebugOptions.eventDebug()) logger.info("[DEBUG] Dispatching Event ${e.id} with args $args")
        val handlers = eventMap[e]
        if (handlers == null) {
            //logger.warn("${e.id} not exist.")
        } else {
            handlers.forEach {
                executor.submit {
                    it.invoke(args)
                }
            }
        }

    }

    fun registerHandler(e: Event, handler: (EventArgs) -> Unit) {
        var handlers = eventMap[e]
        //logger.info("Registering event ${e.id}")
        if (handlers == null) {
            handlers = arrayListOf()
            handlers.add(handler)
        } else {
            handlers.add(handler)
        }
        eventMap[e] = handlers
    }

    fun unregisterHandler(e: Event, handler: (EventArgs) -> Unit) {
        val handlers: ArrayList<(EventArgs) -> Unit> =
            eventMap[e] ?: throw NoSuchElementException("Event $e does not exist.")
        handlers.remove(handler)
        eventMap[e] = handlers
    }

    fun shutdown() {
        executor.shutdown()
    }
}