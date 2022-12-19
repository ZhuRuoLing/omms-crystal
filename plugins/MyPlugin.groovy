import net.zhuruoling.omms.crystal.plugin.*
import net.zhuruoling.omms.crystal.plugin.api.*
import net.zhuruoling.omms.crystal.plugin.api.annotations.*
import net.zhuruoling.omms.crystal.text.*
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

    @EventHandler(event = "crystal.server.overload")
    void serverOverloadEventHandler(ServerInterface serverInterface, ServerOverloadEventArgs eventArgs){
        serverInterface.broadcast(new Text("Server is overloading! Running ${eventArgs.time}ms or ${eventArgs.ticks} ticks behind").withColor(Color.red))
    }

    @EventHandler(event = "crystal.server.player.join")
    void serverOverloadEventHandler(ServerInterface serverInterface, PlayerJoinEventArgs eventArgs){
        serverInterface.broadcast(
                new TextGroup(new Text("${eventArgs.player}").withColor(Color.yellow),
                new Text(" joined the impart").withColor(Color.aqua))
        )
    }
}