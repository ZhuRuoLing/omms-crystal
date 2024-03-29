package icu.takeneko.omms.crystal.plugin

import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.event.EventArgs
import icu.takeneko.omms.crystal.i18n.*
import icu.takeneko.omms.crystal.main.DebugOptions
import icu.takeneko.omms.crystal.parser.MinecraftParser
import icu.takeneko.omms.crystal.plugin.api.annotations.Config
import icu.takeneko.omms.crystal.plugin.api.annotations.EventHandler
import icu.takeneko.omms.crystal.plugin.api.annotations.InjectArgument
import icu.takeneko.omms.crystal.plugin.metadata.PluginDependency
import icu.takeneko.omms.crystal.plugin.metadata.PluginDependencyRequirement
import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata
import icu.takeneko.omms.crystal.plugin.resources.PluginResource
import icu.takeneko.omms.crystal.util.createLogger
import icu.takeneko.omms.crystal.util.joinFilePaths
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.properties.Delegates

class PluginInstance(
    private val classLoader: JarClassLoader,
    private val fileFullPath: String,
    private val pluginStateChangeListener: PluginInstance.(PluginState, PluginState) -> Unit = { _, _ -> }
) {
    var pluginMetadata: PluginMetadata = PluginMetadata()
    private lateinit var pluginClazz: Class<*>
    private lateinit var instance: PluginInitializer
    private var _pluginState = PluginState.WAIT
    private lateinit var pluginConfigPath: Path
    private lateinit var pluginConfigFile: File
    private var pluginState by Delegates.observable(PluginState.ERROR) { _, before, after ->
        pluginStateChangeListener(this, before, after)
    }

    val eventListeners = mutableListOf<Pair<Event, (EventArgs) -> Unit>>()
    private val logger = createLogger("PluginInstance")
    val pluginParsers = mutableMapOf<String, MinecraftParser>()
    val pluginResources = mutableMapOf<String, PluginResource>()
    fun loadPluginMetadata() {
        pluginState = PluginState.ERROR
        try {
            useInJarFile("crystal.plugin.json") {
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
            throw icu.takeneko.omms.crystal.plugin.PluginException("Cannot read plugin jar file.", e)
        }
        pluginState = PluginState.PRE_LOAD
    }

    fun loadPluginClasses() {
        pluginState = PluginState.ERROR
        if (pluginMetadata.pluginInitializerClass != null) {
            try {
                pluginClazz = classLoader.loadClass(pluginMetadata.pluginInitializerClass)
                val ins = pluginClazz.getConstructor().newInstance()
                if (ins !is PluginInitializer) {
                    throw icu.takeneko.omms.crystal.plugin.PluginException("Plugin initializer class did not implement PluginInitializer.")
                }
                instance = ins
            } catch (e: Exception) {
                throw icu.takeneko.omms.crystal.plugin.PluginException("Cannot load plugin initializer.", e)
            }
        }
        if (pluginMetadata.pluginEventHandlers != null) {
            if (pluginMetadata.pluginEventHandlers!!.isNotEmpty()) {
                val classes = mutableListOf<Class<out Any>>()
                pluginMetadata.pluginEventHandlers!!.forEach {
                    try {
                        classes += classLoader.loadClass(it)
                    } catch (e: ClassNotFoundException) {
                        throw icu.takeneko.omms.crystal.plugin.PluginException("Cannot load event handler class $it", e)
                    }
                }
                classes.map {
                    return@map it.getDeclaredConstructor().apply { isAccessible = true }.newInstance() to
                            Arrays.stream(it.declaredMethods)
                                .filter { it3 -> it3.annotations.any { it2 -> it2 is icu.takeneko.omms.crystal.plugin.api.annotations.EventHandler } }
                                .map { it2 ->
                                    it2.getAnnotation(icu.takeneko.omms.crystal.plugin.api.annotations.EventHandler::class.java).run { this.event.java to it2 }
                                }
                                .toList()
                }.forEach { p ->
                    p.second.forEach { (s, m) ->
                        try {
                            m.isAccessible = true
                            val event = findPluginEventInstance(s)
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
                            throw icu.takeneko.omms.crystal.plugin.PluginException(
                                "Cannot transform method ${m.toGenericString()} into EventHandler",
                                e
                            )
                        } catch (e: Exception) {
                            throw icu.takeneko.omms.crystal.plugin.PluginException("", e)
                        }
                    }
                }
            }
        }
        if (pluginMetadata.pluginMinecraftParsers != null && pluginMetadata.pluginMinecraftParsers!!.isNotEmpty()) {
            pluginMetadata.pluginMinecraftParsers!!.forEach {
                try {
                    val clazz = classLoader.loadClass(it.value)
                    val instance = clazz.getConstructor().newInstance()
                    if (instance !is MinecraftParser) {
                        throw icu.takeneko.omms.crystal.plugin.PluginException("Plugin declared Minecraft parser class is not derived from MinecraftParser.")
                    }
                    pluginParsers += it.key to instance
                } catch (e: ClassNotFoundException) {
                    throw icu.takeneko.omms.crystal.plugin.PluginException("Cannot load class ${it.value},", e)
                } catch (e: NoSuchMethodException) {
                    throw icu.takeneko.omms.crystal.plugin.PluginException("Cannot load class ${it.value},", e)
                }
            }
        }

        pluginState = PluginState.INITIALIZED
    }

    private fun findPluginEventInstance(clazz: Class<out Event>) =
        if (clazz.declaredFields.any { it.name == "INSTANCE" && it.type == clazz }) {
            clazz.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
        } else {
            clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        } as Event


    fun injectArguments() {
        pluginState = PluginState.ERROR
        if (DebugOptions.pluginDebug()) logger.info("[DEBUG] Injecting argument")
        for (field in pluginClazz.declaredFields) {
            field.isAccessible = true
            if (field.isAnnotationPresent(icu.takeneko.omms.crystal.plugin.api.annotations.InjectArgument::class.java)) {
                when (val name = field.getAnnotation(icu.takeneko.omms.crystal.plugin.api.annotations.InjectArgument::class.java).name) {
                    "pluginConfig" -> {
                        if (DebugOptions.pluginDebug()) logger.info("[DEBUG] Injecting pluginConfig into $field")
                        if (field.type != Path::class.java) {
                            throw IllegalArgumentException("Illegal field type of pluginConfig injection.(Require java.nio.file.Path, but found ${field.type.name}) ")
                        }
                        field.set(instance, pluginConfigPath)
                    }

                    else -> throw IllegalArgumentException("Illegal injection type $name")
                }
                continue
            }
            if (field.isAnnotationPresent(icu.takeneko.omms.crystal.plugin.api.annotations.Config::class.java) and field.isAnnotationPresent(
                    icu.takeneko.omms.crystal.plugin.api.annotations.InjectArgument::class.java)) {
                throw IllegalArgumentException("@Config cannot be used simultaneously with @InjectArgument (at field $field in class $pluginClazz).")
            }
            if (field.isAnnotationPresent(icu.takeneko.omms.crystal.plugin.api.annotations.Config::class.java)) {
                val configClass = field.type
                val defaultConfig = try {
                    configClass.getDeclaredField("DEFAULT").get(null)
                } catch (_: NoSuchFieldException) {
                    try {
                        configClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
                    } catch (e: Exception) {
                        throw icu.takeneko.omms.crystal.plugin.PluginException("Cannot create default config.", e)
                    }
                }
                pluginConfigFile = (pluginConfigPath / "${pluginMetadata.id}.json").toFile()
                if (!pluginConfigFile.exists()) {
                    pluginConfigFile.createNewFile()
                    pluginConfigFile.writer().use {
                        gsonForPluginMetadata.toJson(defaultConfig, it)
                    }
                }
                if (pluginConfigFile.exists()) {
                    val configInstance = pluginConfigFile.reader().use {
                        fillFieldsUseDefault(gsonForPluginMetadata.fromJson(it, configClass), defaultConfig)
                    }
                    field.set(instance, configInstance)
                }
            }
        }
        pluginState = PluginState.INITIALIZED
    }

    private fun <T> fillFieldsUseDefault(t: T, default: T): T {
        t!!::class.java.declaredFields.forEach {
            if (Modifier.isStatic(it.modifiers)) return@forEach
            if (it.get(t) == null) {
                it.set(t, it.get(default))
            }
        }
        return t
    }

    private fun checkMetadata() {
        if (pluginMetadata.id == null) {
            throw icu.takeneko.omms.crystal.plugin.PluginException("plugin $fileFullPath: plugin id is null")
        }
        if (pluginMetadata.version == null) {
            throw icu.takeneko.omms.crystal.plugin.PluginException("plugin $fileFullPath: plugin version is null")
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
            throw icu.takeneko.omms.crystal.plugin.PluginException("onInitialize", e)
        }
        pluginState = PluginState.LOADED
    }

    fun <R> useInJarFile(fileName: String, consumer: InputStream.() -> R): R =
        ZipFile(File(fileFullPath)).use {
            try {
                val entry = it.getEntry(fileName)
                val inputStream = it.getInputStream(entry)
                val r = consumer(inputStream)
                inputStream.close()
                r
            } catch (e: icu.takeneko.omms.crystal.plugin.PluginException) {
                throw e
            } catch (e: ZipException) {
                throw icu.takeneko.omms.crystal.plugin.PluginException(
                    "ZIP format error occurred while reading plugin jar file.",
                    e
                )
            } catch (e: IOException) {
                throw icu.takeneko.omms.crystal.plugin.PluginException(
                    "I/O error occurred while reading plugin jar file.",
                    e
                )
            } catch (e: Exception) {
                throw icu.takeneko.omms.crystal.plugin.PluginException("Cannot read plugin jar file.", e)
            }
        }

    fun getInJarFileStream(path: String) = ZipFile(File(fileFullPath)).use {
        val entry = it.getEntry(path)
        it.getInputStream(entry)
    }

    fun onFinalize() {
        pluginState = PluginState.ERROR
        try {
            instance.onFinalize()
        } catch (e: Exception) {
            throw icu.takeneko.omms.crystal.plugin.PluginException("onFinalize", e)
        }
        pluginState = PluginState.LOADED
    }

    fun loadPluginResources() {
        if (DebugOptions.pluginDebug()) logger.info("Loading plugin ${pluginMetadata.id} resources.")
        if (pluginMetadata.resources != null) {
            pluginMetadata.resources!!.forEach {
                if (DebugOptions.pluginDebug()) {
                    logger.info("[DEBUG] ${pluginMetadata.id}: ${it.key} <- ${it.value}")
                }

                useInJarFile(it.value) {
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