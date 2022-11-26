package net.zhuruoling.omms.crystal.plugin

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PluginLogger(private val pluginName: String) {
    private val logger: Logger = LoggerFactory.getLogger("InitServerInterface")
    fun info(content: String?) {
        logger.info("[$pluginName] $content")
    }

    fun debug(content: String?) {
        logger.debug("[$pluginName] $content")
    }

    fun error(content: String?) {
        logger.error("[$pluginName] $content")
    }

    fun warn(content: String?) {
        logger.warn("[$pluginName] $content")
    }
}