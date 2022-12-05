package net.zhuruoling.omms.crystal.plugin.api

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.zhuruoling.omms.crystal.command.CommandSourceStack
import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.main.SharedConstants
import org.apache.commons.io.FileUtils
import java.io.File
import java.lang.IllegalArgumentException

object CommandUtil{
    @JvmStatic
    fun literal(literal: String): LiteralArgumentBuilder<CommandSourceStack> {
        return LiteralArgumentBuilder.literal(literal)
    }

    @JvmStatic
    fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSourceStack, T>{
        return RequiredArgumentBuilder.argument(name, type)
    }
}

object IOUtil {
    @JvmStatic
    fun invokeWebGetRequest(url: String): String? = null

    @JvmStatic
    fun copyFile(source: String, destination: String) {
        FileUtils.copyFile(File(source), File(destination))
    }

    @JvmStatic
    fun copyFolder(source: String, destination: String) {
        FileUtils.copyDirectory(File(source), File(destination))
    }

    @JvmStatic
    fun getWorkingDir(): String {
        return net.zhuruoling.omms.crystal.util.getWorkingDir()
    }

    @JvmStatic
    fun joinFilePaths(vararg pathComponents: String): String {
        return net.zhuruoling.omms.crystal.util.joinFilePaths(*pathComponents)
    }
}

object EventUtil{
    @JvmStatic
    fun getPluginDeclaredEvent(pluginId:String, eventID: String):Event{
        val map = SharedConstants.pluginEventTable[pluginId] ?: throw IllegalArgumentException("Specified pluginId($pluginId) not exist.")
        return map[eventID] ?: throw IllegalArgumentException("Specified eventId($eventID) not exist.")
    }
}





