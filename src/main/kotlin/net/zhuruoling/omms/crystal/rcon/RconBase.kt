package net.zhuruoling.omms.crystal.rcon


import net.zhuruoling.omms.crystal.util.UncaughtExceptionHandler
import net.zhuruoling.omms.crystal.util.createLogger
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

private val THREAD_COUNTER = AtomicInteger(0)

abstract class RconBase(protected val description: String) : Runnable {
    @Volatile
    var isRunning: Boolean = false
        protected set
    protected lateinit var thread: Thread
    private val logger: Logger = createLogger("RconBase")

    @Synchronized
    open fun start(): Boolean {
        if (this.isRunning) {
            return true
        } else {
            this.isRunning = true
            val desc = this.description
            this.thread = Thread(this, desc + " #" + THREAD_COUNTER.incrementAndGet())
            thread.uncaughtExceptionHandler = UncaughtExceptionHandler(logger)
            thread.start()
            logger.info("Thread {} started", description as Any)
            return true
        }
    }

    @Synchronized
    open fun stop() {
        this.isRunning = false
        if (thread.isAlive) {
            thread.interrupt()
        }
        logger.info("Thread {} stopped", description as Any)
    }

}
