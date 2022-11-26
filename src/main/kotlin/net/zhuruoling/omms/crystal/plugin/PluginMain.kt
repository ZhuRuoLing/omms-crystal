package net.zhuruoling.omms.crystal.plugin

abstract class PluginMain {
    abstract fun onLoad(serverInterface: ServerInterface?)
    abstract fun onUnload(serverInterface: ServerInterface?)
    abstract fun getPluginMetadata() : PluginMetadata
}