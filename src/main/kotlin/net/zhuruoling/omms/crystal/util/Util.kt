package net.zhuruoling.omms.crystal.util

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.function.Consumer



const val PRODUCT_NAME = "Oh My Minecraft Server Crystal"

fun getWorkingDir(): String? {
    val directory = File("")
    return directory.absolutePath
}

fun joinFilePaths(vararg pathComponent: String): String {
    val paths: Array<out String?> = pathComponent
    val path = StringBuilder()
    path.append(getWorkingDir())
    Arrays.stream(paths).toList().forEach(Consumer { x: String? ->
        path.append(File.separator)
        path.append(x)
    })
    return path.toString()
}

fun resloveCommand(command:String): Array<out String>{
    if (command.isEmpty()) throw IllegalArgumentException("Illegal command $command, to short or empty!")
    val stringTokenizer = StringTokenizer(command)
    val list = mutableListOf<String>()
    while (stringTokenizer.hasMoreTokens()){
        list.add(stringTokenizer.nextToken())
    }
    return list.toTypedArray()
}

fun createServerLogger(): Logger{
    val logger = LoggerFactory.getLogger("ServerLogger") as Logger
    val loggerContext: LoggerContext = logger.loggerContext
    loggerContext.reset()
    val encoder  = PatternLayoutEncoder()
    encoder.context = loggerContext
    encoder.pattern = "[%cyan(%d{yyyy-MM-dd HH:mm:ss.SSS})] [%boldYellow(%marker)/%highlight(%level)]: %msg%n"
    encoder.start()
    val appender: ConsoleAppender<ILoggingEvent> = ConsoleAppender<ILoggingEvent>()
    appender.context = loggerContext
    appender.encoder = encoder
    appender.start()
    logger.addAppender(appender)
    return logger
}