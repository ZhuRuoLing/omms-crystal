import cn.hutool.core.exceptions.ExceptionUtil
import cn.hutool.core.io.FileUtil
import net.zhuruoling.omms.crystal.event.ServerInfoEventArgs
import net.zhuruoling.omms.crystal.event.ServerStoppedEventArgs
import net.zhuruoling.omms.crystal.permission.Permission
import net.zhuruoling.omms.crystal.plugin.CrystalInterface
import net.zhuruoling.omms.crystal.plugin.PluginLogger
import net.zhuruoling.omms.crystal.plugin.PluginMain
import net.zhuruoling.omms.crystal.plugin.PluginMetadata
import net.zhuruoling.omms.crystal.plugin.api.annotations.EventHandler
import net.zhuruoling.omms.crystal.text.Color
import net.zhuruoling.omms.crystal.text.HoverAction
import net.zhuruoling.omms.crystal.text.HoverEvent
import net.zhuruoling.omms.crystal.text.Text
import net.zhuruoling.omms.crystal.util.UtilKt
import org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.Path

import static net.zhuruoling.omms.crystal.permission.PermissionKt.comparePermission
import static net.zhuruoling.omms.crystal.plugin.api.CommandUtil.literal

class SimpleBackup extends PluginMain {

    class FileNameFilter implements FileFilter {
        List<String> fileNames = new ArrayList<>()
        boolean allow = false

        FileNameFilter(boolean allow, String... fileNames) {
            this.fileNames = new ArrayList<>()
            for (final def f in fileNames) {
                this.fileNames.add(f)
            }
            this.allow = allow
        }

        @Override
        boolean accept(File pathname) {
            if (FileUtil.getName(pathname) in fileNames)
                return !allow
            return allow
        }
    }


    PluginLogger logger = null
    boolean gameSaved = false
    boolean autosaveEnabled = false
    boolean requireConfirm = false
    boolean confirmed = false
    Path backupPath = Path.of(UtilKt.joinFilePaths("backup"))
    HashMap<String, Object> defaultConfig = [
            "slot1": "",
            "slot2": "",
            "slot3": ""
    ]

    Map<String, Object> config = new HashMap<>()

    @Override
    PluginMetadata getPluginMetadata() {
        return new PluginMetadata("simple_backup", "0.0.1", "ZhuRuoLing")
    }

    @Override
    void onLoad(CrystalInterface crystalInterface) {
        logger = crystalInterface.logger
        config = crystalInterface.loadConfig(true, defaultConfig)
        crystalInterface.registerPluginCommand(
                literal(crystalInterface.getCommandPrefix() + "backup").then(
                        literal("make").requires { return comparePermission(it.permissionLevel, Permission.USER) }
                                .executes {
                                    var thread = createBackupThread(crystalInterface)
                                    thread.start()
                                    return 1
                                }
                ).then(literal("restore")
                        .then(literal("confirm").requires { return comparePermission(it.permissionLevel, Permission.ADMIN) }
                                .executes {
                                    if (requireConfirm){

                                    }else {
                                        it.source.sendFeedback(new Text("Nothing need to confirm.").withColor(Color.aqua))
                                    }

                                    return 1
                                })
                        .requires { return comparePermission(it.permissionLevel, Permission.ADMIN) }
                        .executes {
                            it.source.sendFeedback(new Text("Type \"${crystalInterface.getCommandPrefix()}backup restore confirm\" to confirm restore").withColor(Color.yellow))
                            requireConfirm = true
                            return 1
                        }
                )
        )
        logger.info("SimpleBackup Loaded")
    }

    @Override
    void onUnload(CrystalInterface crystalInterface) {

    }

    @EventHandler(event = "crystal.server.info")
    void onInfo(CrystalInterface crystalInterface, ServerInfoEventArgs eventArgs) {
        String info = eventArgs.info.info
        switch (info) {
            case "Saved the game" -> {
                gameSaved = true
            }
            case "Automatic saving is now disabled" -> {
                autosaveEnabled = false
            }
            case "Automatic saving is now enabled" -> {
                autosaveEnabled = true
            }
        }
    }

    @EventHandler(event = "crystal.server.stopped")
    void onServerStopped(CrystalInterface crystalInterface, ServerStoppedEventArgs eventArgs) {

    }

    void waitForGameSaved() {
        while (!gameSaved) {
        }
    }


    Thread createBackupThread(CrystalInterface crystalInterface) {
        var thread = new Thread({
            try {
                crystalInterface.broadcast(new Text("Making backup..").withbold(true).withColor(Color.aqua))
                crystalInterface.serverConsoleInput("save-off")
                crystalInterface.serverConsoleInput("save-all flush")
                waitForGameSaved()
                Long now = System.currentTimeMillis()
                if (Files.exists(backupPath)) {
                    FileUtils.deleteDirectory(backupPath.toAbsolutePath().toFile())
                }
                Files.createDirectory(backupPath)
                FileUtils.copyDirectory(
                        Path.of(UtilKt.joinFilePaths(crystalInterface.getCrystalConfig().serverWorkingDirectory, "world")).toAbsolutePath().toFile(),
                        Path.of(UtilKt.joinFilePaths("backup", "world")).toAbsolutePath().toFile(),
                        new FileNameFilter(true, "session.lock")
                )
                Long after = System.currentTimeMillis()
                crystalInterface.serverConsoleInput("save-on")
                crystalInterface.broadcast(new Text("Backup complete, ${(after - now)}ms used"))
            }
            catch (Exception e) {
                e.printStackTrace()
                var reason = ExceptionUtil.getMessage(e)
                var trace = ExceptionUtil.stacktraceToString(e)
                crystalInterface.broadcast(new Text("Cannot make backup:").withbold(true).withColor(Color.red))
                crystalInterface.broadcast(new Text(reason).withColor(Color.red).withHoverEvent(new HoverEvent(HoverAction.show_text, new Text(trace).withColor(Color.red), new Text(trace).withColor(Color.red))))
            }
        })
        return thread
    }

}
