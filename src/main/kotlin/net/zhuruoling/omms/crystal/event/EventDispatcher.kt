package net.zhuruoling.omms.crystal.event

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors


class EventDispatcher {
    private val eventMap: ConcurrentHashMap<Event, ArrayList<(EventArgs) -> Unit>> = ConcurrentHashMap()
    private val executor = Executors.newFixedThreadPool(4)
    val logger: org.slf4j.Logger = LoggerFactory.getLogger("EventDispatcher")
    fun dispatchEvent(e: Event, args: EventArgs) {
        val handlers = eventMap[e]
        if (handlers == null) {
            logger.warn("${e.id} not exist.")
        } else {
            handlers.forEach {
                executor.submit {
                    it.invoke(args)
                }
            }
        }

    }

    fun register(e: Event, handler: (EventArgs) -> Unit) {
        var handlers = eventMap[e]
        logger.info("Registering event ${e.id}")
        if (handlers == null) {
            handlers = arrayListOf()
            handlers.add(handler)
        } else {
            handlers.add(handler)
        }
        eventMap[e] = handlers

    }

    fun shutdown() {
        executor.shutdown()
    }
}