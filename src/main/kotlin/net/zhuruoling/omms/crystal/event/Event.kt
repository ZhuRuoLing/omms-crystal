package net.zhuruoling.omms.crystal.event

import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.main.exit
import net.zhuruoling.omms.crystal.server.ServerHandler
import org.slf4j.LoggerFactory
import java.util.concurrent.PriorityBlockingQueue

class EventLoop : Thread("EventLoop-1") {
    val logger = LoggerFactory.getLogger("EventDispatcher")
    private val queue =
        PriorityBlockingQueue<Pair<Event, EventArgs>>(128) { p1, p2 -> p1.first.priority - p2.first.priority }
    private var stopped = false
    override fun run() {
        while (!stopped) {
            while (queue.isNotEmpty()) {
                val pair = queue.poll()
                SharedConstants.eventDispatcher.dispatchEvent(pair.first, pair.second)
            }
            sleep(10)
        }
        SharedConstants.eventDispatcher.shutdown()
    }

    fun exit() {
        stopped = true
    }

    fun dispatch(e: Event, args: EventArgs) {
        queue.put(Pair(e, args))
    }
}

fun init() {
    SharedConstants.eventDispatcher.register(ServerStoppedEvent){
        val logger = LoggerFactory.getLogger("EventDispatcher")
        it as ServerStoppedEventArgs
        logger.info("Server exited with return value ${it.retValue}")
        if (it.who == "crystal") {
            exit()
        }
    }
    SharedConstants.eventDispatcher.register(ServerStopEvent) {
        it as ServerStopEventArgs
        SharedConstants.serverHandler!!.stopServer(who = it.id)
    }
    SharedConstants.eventDispatcher.register(ServerStartEvent) {
        val logger = LoggerFactory.getLogger("EventDispatcher")
        val args = (it as ServerStartEventArgs)
        logger.info("Starting server using command ${args.startupCmd} at dir: ${args.workingDir}")
        SharedConstants.serverHandler =
            ServerHandler(args.startupCmd, args.workingDir)
        SharedConstants.serverHandler?.start()
    }
    SharedConstants.eventDispatcher.register(ServerStartingEvent) {
        val logger = LoggerFactory.getLogger("EventDispatcher")
        it as ServerStartingEventArgs
        logger.info("Server is running at pid ${it.pid}")
    }
    SharedConstants.eventDispatcher.register(ServerInfoEvent) {
        val logger = LoggerFactory.getLogger("EventDispatcher")
        it as ServerInfoEventArgs
        logger.info(it.info.info)
    }
}

