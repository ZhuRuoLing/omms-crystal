package net.zhuruoling.omms.crystal.util

import org.slf4j.Logger

class UncaughtExceptionHandler(private val logger: Logger) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        logger.error("Caught previously unhandled exception :")
        logger.error(thread.name, throwable)
    }
}