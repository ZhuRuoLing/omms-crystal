import net.zhuruoling.omms.crystal.plugin.*

import java.lang.module.ModuleDescriptor

class MyPlugin extends PluginMain {
    PluginLogger logger = null

    @Override
    void onLoad(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()
        logger.info("KONNICHIWA ZAWARUDOOOOOOOOOOOO!")
    }

    @Override
    void onUnload(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()
    }

    @Override
    PluginMetadata getPluginMetadata() {
        return new PluginMetadata("my_plugin2", ModuleDescriptor.Version.parse("0.0.1"), "ZhuRuoLing")
    }

}