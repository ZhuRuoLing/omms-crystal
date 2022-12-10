import net.zhuruoling.omms.crystal.plugin.*
import net.zhuruoling.omms.crystal.plugin.api.*

import java.lang.module.ModuleDescriptor

class MyPlugin extends PluginMain {
    PluginLogger logger = null

    HashMap<String, Object> defaultConfig = [
            "slot1":"",
            "slot2":"",
            "slot3":""
    ]

    @Override
    void onLoad(ServerInterface serverInterface) {
        logger.info("XBackUp Loaded!")
    }

    @Override
    void onUnload(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()
        logger.info("XBackUp Unloaded!")
    }

    @Override
    PluginMetadata getPluginMetadata() {
        return new PluginMetadata("xbu", ModuleDescriptor.Version.parse("0.0.1"), "ZhuRuoLing")
    }

}