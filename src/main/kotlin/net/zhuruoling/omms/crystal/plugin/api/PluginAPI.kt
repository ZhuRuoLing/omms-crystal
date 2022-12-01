package net.zhuruoling.omms.crystal.plugin.api

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.zhuruoling.omms.crystal.console.command.CommandSourceStack
import org.apache.commons.io.FileUtils
import java.io.File
import javax.print.attribute.standard.Destination

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

object IOUtil{
    @JvmStatic
    fun invokeWebGetRequest(url: String): String?{
        return null
    }

    @JvmStatic
    fun copyFile(source:String, destination: String){
        FileUtils.copyFile(File(source),File(destination))
    }

    @JvmStatic
    fun copyFolder(source:String, destination: String){
        FileUtils.copyDirectory(File(source),File(destination))
    }

    @JvmStatic
    fun getWorkingDir():String{
        return net.zhuruoling.omms.crystal.util.getWorkingDir()
    }

    @JvmStatic
    fun joinFilePaths(vararg pathComponents: String):String{
        return net.zhuruoling.omms.crystal.util.joinFilePaths(*pathComponents)
    }


}





