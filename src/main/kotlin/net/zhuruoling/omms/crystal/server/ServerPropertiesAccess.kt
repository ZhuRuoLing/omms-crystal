package net.zhuruoling.omms.crystal.server

import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.util.joinFilePaths
import java.io.File
import java.io.FileNotFoundException
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets
import java.util.Properties

object ServerPropertiesAccess {
    private var properties: Properties? = null

    fun tryAccess(): Properties {
        return if (properties == null) {
            File(joinFilePaths(Config.serverWorkingDirectory, "server.properties")).run {
                if (exists()) {
                    reader(StandardCharsets.UTF_8).use {
                        properties = Properties()
                        synchronized(properties!!) {
                            properties!!.load(it)
                        }
                    }
                } else {
                    throw FileNotFoundException("${this.absolutePath} not exist.")
                }
            }
            properties!!
        } else {
            properties!!
        }
    }
}