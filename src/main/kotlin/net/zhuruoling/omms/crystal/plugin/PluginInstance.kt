package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.event.EventArgs
import net.zhuruoling.omms.crystal.event.getEventById
import net.zhuruoling.omms.crystal.i18n.*
import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.parser.MinecraftParser
import net.zhuruoling.omms.crystal.plugin.api.annotations.EventHandler
import net.zhuruoling.omms.crystal.plugin.api.annotations.InjectArgument
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependency
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependencyRequirement
import net.zhuruoling.omms.crystal.plugin.metadata.PluginMetadata
import net.zhuruoling.omms.crystal.plugin.resources.PluginResource
import net.zhuruoling.omms.crystal.util.createLogger
import net.zhuruoling.omms.crystal.util.joinFilePaths
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.io.path.Path

class PluginInstance(
    private val urlClassLoader: URLClassLoader,
    private val fileFullPath: String,
    private val pluginStateChangeListener: PluginInstance.(PluginState, PluginState) -> Unit = { _, _ -> }
) {
    var pluginMetadata: PluginMetadata = PluginMetadata()
    private lateinit var pluginClazz: Class<*>
    private lateinit var instance: PluginInitializer
    private var _pluginState = PluginState.WAIT
    private lateinit var pluginConfigPath: Path
    private var pluginState
        set(value) {
            pluginStateChangeListener(_pluginState, value)
            _pluginState = value
        }
        get() = _pluginState

    val eventListeners = mutableListOf<Pair<Event, (EventArgs) -> Unit>>()
    private val logger = createLogger("PluginInstance")
    val pluginParsers = mutableMapOf<String, MinecraftParser>()
    val pluginResources = mutableMapOf<String, PluginResource>()
    fun loadPluginMetadata() {
        pluginState = PluginState.ERROR
        try {
            useFileInJar("crystal.plugin.json") {
                pluginMetadata = PluginMetadata.fromJson(readAllBytes().decodeToString())
                if (pluginMetadata.pluginDependencies != null) {
                    pluginMetadata.pluginDependencies!!.forEach { it2 ->
                        it2.parseRequirement()
                    }
                }
                checkMetadata()
            }
            pluginConfigPath = Path(joinFilePaths("config", pluginMetadata.id!!))
        } catch (e: Exception) {
            throw PluginException("Cannot read plugin jar file.", e)
        }
        pluginState = PluginState.PRE_LOAD
    }

    fun loadPluginClasses() {
        pluginState = PluginState.ERROR

        if (pluginMetadata.pluginInitializerClass != null) {
            try {
                pluginClazz = urlClassLoader.loadClass(pluginMetadata.pluginInitializerClass)
                val ins = pluginClazz.getConstructor().newInstance()
                if (ins !is PluginInitializer) {
                    throw PluginException("Plugin initializer class did not implement PluginInitializer.")
                }
                instance = ins
            } catch (e: Exception) {
                throw PluginException("Cannot load plugin initializer.", e)
            }
        }
        if (pluginMetadata.pluginEventHandlers != null) {
            if (pluginMetadata.pluginEventHandlers!!.isNotEmpty()) {
                val classes = mutableListOf<Class<out Any>>()
                pluginMetadata.pluginEventHandlers!!.forEach {
                    try {
                        classes += urlClassLoader.loadClass(it)
                    } catch (e: ClassNotFoundException) {
                        throw PluginException("Cannot load event handler class $it", e)
                    }
                }
                classes.map {
                    return@map it.getDeclaredConstructor().apply { isAccessible = true }.newInstance() to
                            Arrays.stream(it.declaredMethods)
                                .filter { it3 -> it3.annotations.any { it2 -> it2 is EventHandler } }
                                .map { it2 -> it2.getAnnotation(EventHandler::class.java).run { this.event to it2 } }
                                .toList()
                }.forEach { p ->
                    p.second.forEach { (s, m) ->
                        try {
                            m.isAccessible = true
                            val event = getEventById(s)
                            eventListeners += event to { args ->
                                try {
                                    m.invoke(p.first, args)
                                } catch (e: Exception) {
                                    logger.error(
                                        "Cannot invoke plugin(${pluginMetadata.id}) event listener ${m.toGenericString()}.",
                                        e
                                    )
                                }
                            }
                        } catch (e: IllegalArgumentException) {
                            throw PluginException("Cannot transform method ${m.toGenericString()} into EventHandler", e)
                        } catch (e: Exception) {
                            throw PluginException("", e)
                        }
                    }
                }
            }
        }
        if (pluginMetadata.pluginMinecraftParsers != null && pluginMetadata.pluginMinecraftParsers!!.isNotEmpty()) {
            pluginMetadata.pluginMinecraftParsers!!.forEach {
                try {
                    val clazz = urlClassLoader.loadClass(it.value)
                    val instance = clazz.getConstructor().newInstance()
                    if (instance !is MinecraftParser) {
                        throw PluginException("Plugin declared Minecraft parser class is not derived from MinecraftParser.")
                    }
                    pluginParsers += it.key to instance
                } catch (e: ClassNotFoundException) {
                    throw PluginException("Cannot load class ${it.value},", e)
                } catch (e: NoSuchMethodException) {
                    throw PluginException("Cannot load class ${it.value},", e)
                }
            }
        }

        pluginState = PluginState.INITIALIZED
    }

    fun injectArguments() {
        for (field in pluginClazz.declaredFields) {
            field.isAccessible = true
            if (field.annotations.any { it::class.java == InjectArgument::class.java }) {
                val name = field.getAnnotation(InjectArgument::class.java).name
                when (name) {
                    "pluginConfig" -> {
                        if (field.type != Path::class.java) {
                            throw IllegalArgumentException("Illegal field type of pluginConfig injection.(Require java.nio.file.Path, but found ${field.type.name}) ")
                        }
                        field.set(instance, pluginConfigPath)
                    }

                    else -> throw IllegalArgumentException("Illegal injection type $name")
                }
            }
        }
    }

    private fun checkMetadata() {
        if (pluginMetadata.id == null) {
            throw PluginException("plugin $fileFullPath: plugin id is null")
        }
        if (pluginMetadata.version == null) {
            throw PluginException("plugin $fileFullPath: plugin version is null")
        }
    }

    fun checkPluginDependencyRequirements(dependencies: List<PluginDependency>): List<PluginDependencyRequirement> {
        pluginState = PluginState.ERROR
        var result = mutableListOf<PluginDependencyRequirement>()
        if (pluginMetadata.pluginDependencies != null) {
            result =
                pluginMetadata.pluginDependencies!!.filter { dependencies.none { it2 -> it.requirementMatches(it2) } }
                    .toMutableList()
        }
        pluginState = PluginState.INITIALIZED
        return result
    }

    fun onInitialize() {
        pluginState = PluginState.ERROR
        try {
            instance.onInitialize()
        } catch (e: Exception) {
            throw PluginException("onInitialize", e)
        }
        pluginState = PluginState.LOADED
    }

    private fun <R> useFileInJar(fileName: String, consumer: InputStream.() -> R): R =
        ZipFile(File(fileFullPath)).use {
            try {
                val entry = it.getEntry(fileName)
                val inputStream = it.getInputStream(entry)
                consumer(inputStream)
            } catch (e: PluginException) {
                throw e
            } catch (e: ZipException) {
                throw PluginException("ZIP format error occurred while reading plugin jar file.", e)
            } catch (e: IOException) {
                throw PluginException("I/O error occurred while reading plugin jar file.", e)
            } catch (e: Exception) {
                throw PluginException("Cannot read plugin jar file.", e)
            }
        }

    fun loadPluginResources() {
        if (DebugOptions.pluginDebug()) logger.info("Loading plugin ${pluginMetadata.id} resources.")
        if (pluginMetadata.resources != null) {
            pluginMetadata.resources!!.forEach {
                logger.info("${pluginMetadata.id}: ${it.key} <- ${it.value}")
                useFileInJar(it.value) {
                    pluginResources[it.key] = PluginResource.fromReader(it.key, reader(StandardCharsets.UTF_8))
                }
            }
        }

        if (pluginResources.isEmpty() and DebugOptions.pluginDebug()) {
            logger.info("[DEBUG] Plugin ${pluginMetadata.id} has no resources.")
        }
        pluginResources.forEach {
            if (DebugOptions.pluginDebug())
                logger.info("[DEBUG] Resource ${it.value}")
            val resType = it.value.resMetaDataMap["type"] ?: return@forEach
            val namespace = it.value.resMetaDataMap["namespace"] ?: return@forEach
            if (resType == "lang") {
                val lang = it.key
                TranslateManager.getOrCreateDefaultLanguageProvider(lang).apply {
                    it.value.resDataMap.forEach { (k, v) ->
                        val translateKey = TranslateKey(lang, namespace, k)
                        if (DebugOptions.pluginDebug()) logger.info("[DEBUG] Translation: $k -> $v")
                        this.addTranslateKey(translateKey, v)
                    }
                }
            }
        }
    }
}