package net.zhuruoling.omms.crystal.plugin

import groovy.lang.GroovyClassLoader
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path

open class GroovyPluginInstance(private val pluginFilePath: String) {
    private val groovyClassLoader: GroovyClassLoader
    lateinit var instance: PluginMain
    var pluginStatus = PluginStatus.NONE
    lateinit var metadata: PluginMetadata

    init {
        val config = CompilerConfiguration()
        config.sourceEncoding = "UTF-8"
        groovyClassLoader = GroovyClassLoader(Thread.currentThread().contextClassLoader, config)
    }

    fun initPlugin(): GroovyPluginInstance {
        if (!Files.exists(Path.of(pluginFilePath))) {
            throw FileNotFoundException("The specified plugin file $pluginFilePath does not exist.")
        }
        try {
            val clazz = groovyClassLoader.parseClass(File(pluginFilePath))
            this.instance = clazz.getDeclaredConstructor().newInstance() as PluginMain
            this.metadata = instance.getPluginMetadata()
        } catch (e: MultipleCompilationErrorsException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return this
    }

    fun invokeMethod(methodName: String?, vararg params: Any): Any? {
        val clazz: Class<out PluginMain> = instance!!.javaClass
        val paramTypes = ArrayList<Class<*>>()
        for (param in params) {
            paramTypes.add(param.javaClass)
        }
        return try {
            val method = methodName?.let { clazz.getMethod(it, *paramTypes.toArray(arrayOf())) }
            if (method != null) {
                method.isAccessible = true
                method.invoke(instance, *params)
            } else {
                null
            }

        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            null
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            null
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            null
        }
    }

    fun onLoad(serverInterface: ServerInterface?) {
        instance.onLoad(serverInterface)
        pluginStatus = PluginStatus.LOADED
    }

    fun onUnload(serverInterface: ServerInterface?) {
        instance.onUnload(serverInterface)
        pluginStatus = PluginStatus.UNLOADED
    }

    override fun toString(): String {
        return "GroovyPluginInstance(pluginFilePath='$pluginFilePath', groovyClassLoader=$groovyClassLoader, instance=$instance, pluginStatus=$pluginStatus, metadata=$metadata)"
    }
}