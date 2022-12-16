import net.zhuruoling.omms.crystal.plugin.*
import net.zhuruoling.omms.crystal.plugin.api.*
import net.zhuruoling.omms.crystal.plugin.api.annotations.*
import net.zhuruoling.omms.crystal.event.*

import java.lang.module.ModuleDescriptor

class MyPlugin extends PluginMain {
    PluginLogger logger = null

    @Override
    void onLoad(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()
        logger.info("MyPlugin Loaded!")
        logger.info("I HAVE A reverseString API Method!")
    }

    @Override
    void onUnload(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()
        logger.info("MyPlugin Unloaded!")
    }

    @Override
    PluginMetadata getPluginMetadata() {
        return new PluginMetadata("my_plugin", ModuleDescriptor.Version.parse("0.0.1"), "ZhuRuoLing")
    }

    @Api
    String reverseString(String s){
        return s.reverse()
    }
    @net.zhuruoling.omms.crystal.plugin.api.annotations.EventListener(event = "crystal.server.overload")
    void serverOverloadEventHandler(ServerInterface serverInterface, ServerOverloadEventArgs eventArgs){

    }
}