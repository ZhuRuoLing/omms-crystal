package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.config.ConfigManager

data class wdnmd(val string: String)


fun main() {
    ConfigManager.init()
    val defaultConfig: MutableMap<String, Any> = mutableMapOf("a" to "B", "c" to "D", "e" to 114514)
    var wdnmd = ConfigManager.getConfig(pluginId = "wdnmd", createIfNotExist = true, defaultConfig = defaultConfig)
    println(wdnmd)
    ConfigManager.setConfigValue("wdnmd", "KKKK","ZZZZ")
    wdnmd = ConfigManager.getConfig(pluginId = "wdnmd")
    println(wdnmd)
    ConfigManager.removeConfigKey("wdnmd", "KKKK")
    wdnmd = ConfigManager.getConfig(pluginId = "wdnmd")
    println(wdnmd)
    //ConfigManager.getPluginConfig("wdnmd")
}