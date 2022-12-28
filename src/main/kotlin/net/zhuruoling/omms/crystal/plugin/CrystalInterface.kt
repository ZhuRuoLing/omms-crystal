package net.zhuruoling.omms.crystal.plugin

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.zhuruoling.omms.crystal.command.CommandManager
import net.zhuruoling.omms.crystal.command.CommandSource
import net.zhuruoling.omms.crystal.command.CommandSourceStack
import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.config.ConfigManager
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.text.Text
import net.zhuruoling.omms.crystal.text.TextGroup
import net.zhuruoling.omms.crystal.text.TextSerializer

class CrystalInterface(private val pluginName: String) {
    private val logger: PluginLogger = PluginLogger(pluginName)

    fun getLogger():PluginLogger{
        return logger
    }

    @Synchronized
    fun registerPluginEvent(eventId: String, priority: Int = 1) {
        if (SharedConstants.pluginRegisteredEventTable[pluginName] == null) {
            SharedConstants.pluginRegisteredEventTable[pluginName] = hashMapOf()
            SharedConstants.pluginRegisteredEventTable[pluginName]!![eventId] = PluginEvent(eventId, pluginName, priority)
        } else {
            SharedConstants.pluginRegisteredEventTable[pluginName]!![eventId] = PluginEvent(eventId, pluginName, priority)
        }
    }

    @Synchronized
    fun registerPluginCommand(commandSourceStackLiteralArgumentBuilder: LiteralArgumentBuilder<CommandSourceStack>) {
        CommandManager.register(commandSourceStackLiteralArgumentBuilder)
        if (SharedConstants.pluginRegisteredCommandTable[pluginName] == null) {
            SharedConstants.pluginRegisteredCommandTable[pluginName] = arrayListOf()
            SharedConstants.pluginRegisteredCommandTable[pluginName]!!.add(commandSourceStackLiteralArgumentBuilder)
        } else {
            if (!SharedConstants.pluginRegisteredCommandTable[pluginName]!!.contains(commandSourceStackLiteralArgumentBuilder))
                SharedConstants.pluginRegisteredCommandTable[pluginName]!!.add(commandSourceStackLiteralArgumentBuilder)
        }
        SharedConstants.consoleHandler.reload()
    }

    fun getCommandPrefix(): String{
        return Config.commandPrefix
    }

    @Synchronized
    fun loadConfig(
        createIfNotExist: Boolean = true,
        defaultConfig: MutableMap<String, Any>? = null
    ): MutableMap<String, Any> {
        return ConfigManager.getConfig(pluginName, createIfNotExist, defaultConfig)
    }

    @Synchronized
    fun startServer(): Boolean {
        return if (SharedConstants.serverController == null) {
            SharedConstants.eventLoop.dispatch(
                ServerStartEvent, ServerStartEventArgs(
                    Config.launchCommand, Config.serverWorkingDirectory
                )
            )
            true
        } else {
            logger.warn("Server is running!")
            false
        }
    }

    @Synchronized
    fun stopServer(force: Boolean = false): Boolean {
        return if (SharedConstants.serverController == null) {
            logger.warn("Server is not running")
            false
        } else {
            SharedConstants.eventLoop.dispatch(ServerStopEvent, ServerStopEventArgs(pluginName, force))
            true
        }
    }

    @Synchronized
    fun serverConsoleInput(content:String):Boolean{
        return if (SharedConstants.serverController == null) {
            logger.warn("Server is not running")
            false
        } else {
            SharedConstants.eventLoop.dispatch(ServerConsoleInputEvent, ServerConsoleInputEventArgs(content))
            true
        }
    }

    fun runCommand(command: String){
        CommandManager.execute(command, CommandSourceStack(CommandSource.PLUGIN))
    }

    fun getCrystalConfig():Config{
        return Config
    }

    fun getDebugOption():DebugOptions{
        return DebugOptions
    }

    @Synchronized
    fun broadcast(text:Text, player:String){
        SharedConstants.serverController?.input("tellraw $player ${TextSerializer.serialize(text)}")
    }

    @Synchronized
    fun broadcast(text:TextGroup, player:String){
        SharedConstants.serverController?.input("tellraw $player ${TextSerializer.serialize(text)}")
    }

    @Synchronized
    fun broadcast(text:String, player:String){
        SharedConstants.serverController?.input("tellraw $player ${TextSerializer.serialize(Text(text))}")
    }

    @Synchronized
    fun broadcast(text:Text){
        SharedConstants.serverController?.input("tellraw @a ${TextSerializer.serialize(text)}")
    }

    @Synchronized
    fun broadcast(text:TextGroup){
        SharedConstants.serverController?.input("tellraw @a ${TextSerializer.serialize(text)}")
    }

    @Synchronized
    fun broadcast(text:String){
        SharedConstants.serverController?.input("tellraw @a ${TextSerializer.serialize(Text(text))}")
    }
}