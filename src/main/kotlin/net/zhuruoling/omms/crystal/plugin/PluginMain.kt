package net.zhuruoling.omms.crystal.plugin

abstract class PluginMain {
    abstract fun onLoad(crystalInterface: CrystalInterface?)
    abstract fun onUnload(crystalInterface: CrystalInterface?)
    abstract fun getPluginMetadata() : PluginMetadata
}