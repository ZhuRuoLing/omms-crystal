import net.zhuruoling.omms.crystal.plugin.*
import net.zhuruoling.omms.crystal.plugin.api.*

import java.lang.module.ModuleDescriptor

class wdnmd extends PluginMain {
    PluginLogger logger = null

    @Override
    void onLoad(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()

        logger.info("KONNICHIWA ZAWARUDOOOOOOOOOOOO!")
        logger.info(reverseString("wdnmd sb"))
        logger.info("i hope it works.")
    }

    @Override
    void onUnload(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()
    }

    @Override
    PluginMetadata getPluginMetadata() {
        return new PluginMetadata("my_plugin2", ModuleDescriptor.Version.parse("0.0.1"), "ZhuRuoLing")
    }

    String reverseString(String s){
        return PluginUtil.INSTANCE.invokePluginDeclaredApiMethod("my_plugin","reverseString",s)
    }
}