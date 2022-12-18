package net.zhuruoling.omms.crystal.plugin

import groovy.lang.GroovyClassLoader
import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.event.EventHandler
import net.zhuruoling.omms.crystal.event.getEventById
import net.zhuruoling.omms.crystal.plugin.api.annotations.Api
import net.zhuruoling.omms.crystal.util.PluginUtil
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.jvm.kotlinFunction

open class GroovyPluginInstance(private val pluginFilePath: String) {
    private val groovyClassLoader: GroovyClassLoader
    lateinit var pluginMain: PluginMain
    var pluginStatus = PluginStatus.NONE
    lateinit var metadata: PluginMetadata
    val apiMethods: HashMap<Pair<String, MutableList<Class<*>>>, Method> = hashMapOf()
    val eventHandlers = hashMapOf<Event, EventHandler>()
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
            val clazz = groovyClassLoader.parseClass(File(pluginFilePath)) as Class<out PluginMain>
            this.pluginMain = clazz.getDeclaredConstructor().newInstance() as PluginMain
            this.metadata = pluginMain.getPluginMetadata()
            clazz.declaredMethods.forEach {
                for (annotation in it.annotations) {
                    if (annotation == Api()){
                        val parameters = mutableListOf<Class<*>>()
                        it.parameters.forEach { parameter ->
                            parameters.add(parameter.type)
                        }
                        it.isAccessible = true
                        apiMethods[Pair<String, MutableList<Class<*>>>(it.name, parameters)] = it
                    }
                }

                val eventHandlerMap = PluginUtil.getPluginDeclaredEventHandlerMethod(clazz)// event handler
                eventHandlerMap.forEach { p -> //convert
                    eventHandlers[getEventById(p.key)] = { args ->
                        p.value(this.pluginMain,ServerInterface(this.metadata.id!!) ,args)
                    }
                }
                //SharedConstants.pluginDeclaredEventHandlerMap[metadata.id!!] = map
            }
        } catch (e: MultipleCompilationErrorsException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return this
    }

    fun invokeMethod(methodName: String?, vararg params: Any): Any? {
        val clazz: Class<out PluginMain> = pluginMain.javaClass
        val paramTypes = ArrayList<Class<*>>()
        for (param in params) {
            paramTypes.add(param.javaClass)
        }
        return try {
            val method = methodName?.let { clazz.getMethod(it, *paramTypes.toArray(arrayOf())) }
            if (method != null) {
                method.isAccessible = true
                method.invoke(pluginMain, *params)
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
        pluginMain.onLoad(serverInterface)
        pluginStatus = PluginStatus.LOADED
    }

    fun onUnload(serverInterface: ServerInterface?) {
        pluginMain.onUnload(serverInterface)
        pluginStatus = PluginStatus.UNLOADED
    }

    override fun toString(): String {
        return "GroovyPluginInstance(pluginFilePath='$pluginFilePath', groovyClassLoader=$groovyClassLoader, instance=$pluginMain, pluginStatus=$pluginStatus, metadata=$metadata)"
    }
}