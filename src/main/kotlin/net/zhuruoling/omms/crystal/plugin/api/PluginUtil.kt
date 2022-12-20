package net.zhuruoling.omms.crystal.plugin.api

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.zhuruoling.omms.crystal.command.CommandSourceStack
import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.permission.Permission
import net.zhuruoling.omms.crystal.permission.PermissionManager
import net.zhuruoling.omms.crystal.plugin.PluginManager
import org.apache.commons.io.FileUtils
import java.io.File


object PlayerUtil {
    @JvmStatic
    fun getPlayerPermission(player: String): Permission {
        return PermissionManager.getPermission(player)
    }
}

object CommandUtil {
    @JvmStatic
    fun literal(literal: String): LiteralArgumentBuilder<CommandSourceStack> {
        return LiteralArgumentBuilder.literal(literal)
    }

    @JvmStatic
    fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSourceStack, T> {
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

object EventUtil {
    @JvmStatic
    fun getPluginDeclaredEvent(pluginId: String, eventID: String): Event {
        val map = SharedConstants.pluginRegisteredEventTable[pluginId]
            ?: throw IllegalArgumentException("Specified pluginId($pluginId) not exist.")
        return map[eventID] ?: throw IllegalArgumentException("Specified eventId($eventID) not exist.")
    }
}

object PluginUtil {
    fun invokePluginDeclaredApiMethod(pluginId: String, methodName: String, vararg args: Any): Any {
        val methods = SharedConstants.pluginDeclaredApiMethodMap[pluginId]
            ?: throw NoSuchElementException("Plugin $pluginId Not Exist or Not Loaded.")
        val argTypes = arrayListOf<Class<*>>()
        args.forEach {
            argTypes.add(it::class.java)
        }
        val method = methods[Pair(methodName, argTypes)]
            ?: throw NoSuchMethodException("Method $methodName with args $argTypes does not exist.")
        return method(PluginManager.getPluginInstance(pluginId)!!.pluginInstance.pluginMain, *args)
    }
}





