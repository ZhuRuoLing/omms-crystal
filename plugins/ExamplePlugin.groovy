import net.zhuruoling.omms.crystal.plugin.*
import net.zhuruoling.omms.crystal.plugin.api.*
import net.zhuruoling.omms.crystal.plugin.api.annotations.*
import net.zhuruoling.omms.crystal.text.*
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.parser.*

import java.lang.module.ModuleDescriptor

class MyPlugin extends PluginMain {
    PluginLogger logger = null

    @Override
    void onLoad(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()
        logger.info("Example Plugin Loaded!")
        logger.info(reverseStringFromApi("KONNICHIWA ZAWARUDO!"))
    }

    @Override
    void onUnload(ServerInterface serverInterface) {
        logger = serverInterface.getLogger()
        logger.info("Example Plugin Unloaded!")
    }

    @Override
    PluginMetadata getPluginMetadata() {
        return new PluginMetadata("example_plugin", ModuleDescriptor.Version.parse("0.0.1"), "ZhuRuoLing")
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

    String reverseStringFromApi(String s){
        return PluginUtil.INSTANCE.invokePluginDeclaredApiMethod("example_plugin","reverseString",s)
    }

    @Parser(name="custom_parser")
    class MyCustomParser extends MinecraftParser{
        Info parseToBareInfo(String raw) {
            return null
        }
        ServerStartedInfo parseServerStartedInfo(String raw){
            return null
        }
        PlayerInfo parsePlayerInfo(String raw){
            return null
        }
        ServerOverloadInfo parseServerOverloadInfo(String raw){
            return null
        }
        ServerStartingInfo parseServerStartingInfo(String raw){
            return null
        }
        PlayerJoinInfo parsePlayerJoinInfo(String raw){
            return null
        }
        PlayerLeftInfo parsePlayerLeftInfo(String raw) {
            return null
        }
        ServerStoppingInfo parseServerStoppingInfo(String raw) {
            return null
        }
    }
}