package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.event.ServerStartEvent
import icu.takeneko.omms.crystal.event.ServerStartEventArgs
import icu.takeneko.omms.crystal.event.ServerStopEvent
import icu.takeneko.omms.crystal.event.ServerStopEventArgs
import icu.takeneko.omms.crystal.i18n.withTranslateContext
import icu.takeneko.omms.crystal.main.SharedConstants
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.permission.comparePermission
import icu.takeneko.omms.crystal.permission.resolvePermissionLevel
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.text.Color
import icu.takeneko.omms.crystal.text.Text
import icu.takeneko.omms.crystal.text.TextGroup
import icu.takeneko.omms.crystal.util.createLogger

private val logger = createLogger("Command")

fun literal(literal: String): LiteralArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack> {
    return LiteralArgumentBuilder.literal(literal)
}

private fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack, T> {
    return RequiredArgumentBuilder.argument(name, type)
}

fun integerArgument(name: String): RequiredArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack, Int> {
    return icu.takeneko.omms.crystal.command.argument(name, IntegerArgumentType.integer())
}

fun wordArgument(name: String): RequiredArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack, String> {
    return icu.takeneko.omms.crystal.command.argument(name, StringArgumentType.word())
}

fun greedyStringArgument(name: String): RequiredArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack, String> {
    return icu.takeneko.omms.crystal.command.argument(name, StringArgumentType.greedyString())
}

fun getWord(context: CommandContext<icu.takeneko.omms.crystal.command.CommandSourceStack>, name: String): String {
    return StringArgumentType.getString(context, name)
}

fun getInteger(context: CommandContext<icu.takeneko.omms.crystal.command.CommandSourceStack>, name: String): Int {
    return IntegerArgumentType.getInteger(context, name)
}

val helpCommand: LiteralArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack> = icu.takeneko.omms.crystal.command.literal(
    Config.commandPrefix + "help"
).then(
    icu.takeneko.omms.crystal.command.greedyStringArgument("filter")
        .executes {
            val filter = icu.takeneko.omms.crystal.command.getWord(it, "filter")
            icu.takeneko.omms.crystal.command.CommandHelpManager.displayFiltered(it.source) {
                filter in this
            }
            1
        }
).executes {
    icu.takeneko.omms.crystal.command.CommandHelpManager.displayAll(it.source)
    1
}


val permissionCommand: LiteralArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack> = icu.takeneko.omms.crystal.command.literal(
    Config.commandPrefix + "permission"
).then(
    icu.takeneko.omms.crystal.command.literal("set").then(
        icu.takeneko.omms.crystal.command.wordArgument("player")
            .then(
                icu.takeneko.omms.crystal.command.wordArgument("permissionLevel").requires {
                    if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
                        comparePermission(it.permissionLevel!!, Permission.OWNER)
                    else
                        true
                }.executes {
                    PermissionManager.setPermission(
                        icu.takeneko.omms.crystal.command.getWord(it, "player"),
                        resolvePermissionLevel(icu.takeneko.omms.crystal.command.getWord(it, "permissionLevel"))
                    )
                    1
                }
            )
            .requires {
                if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
                    comparePermission(it.permissionLevel!!, Permission.OWNER)
                else
                    true
            }.executes {
                PermissionManager.setPermission(icu.takeneko.omms.crystal.command.getWord(it, "player"))
                1
            }
    )
).then(
    icu.takeneko.omms.crystal.command.literal("delete").then(
        icu.takeneko.omms.crystal.command.wordArgument("player").requires {
            if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
                comparePermission(it.permissionLevel!!, Permission.OWNER)
            else
                true
        }.executes {
            PermissionManager.deletePlayer(icu.takeneko.omms.crystal.command.getWord(it, "player"))

            1
        }
    )
).then(
    icu.takeneko.omms.crystal.command.literal("list").executes {
        val permissionStorage = PermissionManager.convertToPermissionStorage()

        val textOwner = TextGroup(
            Text("  Owners: ").withColor(Color.light_purple),
            Text(permissionStorage.owner.joinToString(separator = ", ")).withColor(Color.reset)
        )
        val textAdmin = TextGroup(
            Text("  Admins: ").withColor(Color.yellow),
            Text(permissionStorage.admin.joinToString(separator = ", ")).withColor(Color.reset)
        )
        val textUser = TextGroup(
            Text("  Users: ").withColor(Color.aqua),
            Text(permissionStorage.user.joinToString(separator = ", ")).withColor(Color.reset)
        )
        val textGuest = TextGroup(
            Text("  Guests: ").withColor(Color.blue),
            Text(permissionStorage.guest.joinToString(separator = ", ")).withColor(Color.reset)
        )
        it.source.sendFeedback(Text("Permissions:"))
        it.source.sendFeedback(textOwner)
        it.source.sendFeedback(textAdmin)
        it.source.sendFeedback(textUser)
        it.source.sendFeedback(textGuest)
        1
    }
).then(
    icu.takeneko.omms.crystal.command.literal("save").requires {
        if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.OWNER)
        else
            true
    }.executes {
        PermissionManager.writePermission()
        1
    }
)

val startCommand: LiteralArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack> = icu.takeneko.omms.crystal.command.literal(
    Config.commandPrefix + "start"
).requires {
    if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
        comparePermission(it.permissionLevel!!, Permission.ADMIN)
    else
        true
}.executes {
    SharedConstants.eventLoop.dispatch(
        ServerStartEvent,
        ServerStartEventArgs(Config.launchCommand, Config.serverWorkingDirectory)
    )
    1
}

val stopCommand: LiteralArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack> = icu.takeneko.omms.crystal.command.literal(
    Config.commandPrefix + "stop"
).then(
    icu.takeneko.omms.crystal.command.literal("force").requires {
        if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.ADMIN)
        else
            true
    }.executes {
        SharedConstants.eventLoop.dispatch(
            ServerStopEvent,
            ServerStopEventArgs(
                when (it.source.from) {
                    CommandSource.PLAYER -> it.source.player!!
                    CommandSource.PLUGIN -> "plugin"
                    CommandSource.REMOTE -> "central"
                    CommandSource.CONSOLE -> "console"
                },
                true
            )
        )
        1
    }
)
    .requires {
        if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.ADMIN)
        else
            true
    }.executes {
        SharedConstants.eventLoop.dispatch(
            ServerStopEvent,
            ServerStopEventArgs(
                when (it.source.from) {
                    CommandSource.PLAYER -> it.source.player!!
                    CommandSource.PLUGIN -> "plugin"
                    CommandSource.REMOTE -> "central"
                    CommandSource.CONSOLE -> "console"
                },
                false
            )
        )
        1
    }

val executeCommand: LiteralArgumentBuilder<CommandSourceStack> =
    literal(Config.commandPrefix + "execute")
        .requires { it.from == CommandSource.CONSOLE || it.from == CommandSource.REMOTE }
        .then(
            icu.takeneko.omms.crystal.command.literal("as").then(
                icu.takeneko.omms.crystal.command.wordArgument("player")
            )
        )

val pluginCommand: LiteralArgumentBuilder<icu.takeneko.omms.crystal.command.CommandSourceStack> = icu.takeneko.omms.crystal.command.literal(
    Config.commandPrefix + "plugin"
)
//    .then(literal("load").then(wordArgument("plugin").requires {
//        if (it.from == CommandSource.PLAYER)
//            comparePermission(it.permissionLevel!!, Permission.ADMIN)
//        else
//            true
//    }.executes {
//        PluginManager.load(getWord(it,"plugin"))
//        1
//    }))
//    .then(literal("unload").then(wordArgument("plugin").requires {
//        if (it.from == CommandSource.PLAYER)
//            comparePermission(it.permissionLevel!!, Permission.ADMIN)
//        else
//            true
//    }.executes {
//        PluginManager.unload(getWord(it,"plugin"))
//        1
//    }))
    .then(
        icu.takeneko.omms.crystal.command.literal("reload")
            .then(icu.takeneko.omms.crystal.command.wordArgument("plugin").requires {
        if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.ADMIN)
        else
            true
    }.executes {
        icu.takeneko.omms.crystal.command.logger.warn("Plugin reloading is highly experimental, in some cases it can cause severe problems.")
        icu.takeneko.omms.crystal.command.logger.warn("Reloading plugin ${
            icu.takeneko.omms.crystal.command.getWord(
                it,
                "plugin"
            )
        }!")
        PluginManager.reload(icu.takeneko.omms.crystal.command.getWord(it, "plugin"))
        1
    }))
    .then(icu.takeneko.omms.crystal.command.literal("reloadAll").requires {
        if (it.from == icu.takeneko.omms.crystal.command.CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.ADMIN)
        else
            true
    }.executes {
        icu.takeneko.omms.crystal.command.logger.warn("Plugin reloading is highly experimental, in some cases it can cause severe problems.")
        icu.takeneko.omms.crystal.command.logger.warn("Reloading all plugins!")
        PluginManager.reloadAllPlugins()
        1
    })
//
//

private val commands = listOf(
    icu.takeneko.omms.crystal.command.helpCommand,
    icu.takeneko.omms.crystal.command.permissionCommand,
    icu.takeneko.omms.crystal.command.startCommand,
    icu.takeneko.omms.crystal.command.stopCommand,
    icu.takeneko.omms.crystal.command.executeCommand,
    icu.takeneko.omms.crystal.command.pluginCommand
)


fun registerBuiltinCommandHelp() {
    val dispatcher = CommandDispatcher<icu.takeneko.omms.crystal.command.CommandSourceStack>()
    icu.takeneko.omms.crystal.command.commands.forEach(dispatcher::register)
    val usage = dispatcher.getAllUsage(
        dispatcher.root,
        icu.takeneko.omms.crystal.command.CommandSourceStack(
            from = icu.takeneko.omms.crystal.command.CommandSource.PLAYER,
            player = "",
            permissionLevel = Permission.OWNER
        ),
        false
    ).toList()
    val help = usage.map {
        it to it.removePrefix(Config.commandPrefix).split(" ")
            .joinToString(separator = ".")
            .run { "help.$this" }
    }
    help.forEach { (cmd, help) ->
        icu.takeneko.omms.crystal.command.CommandHelpManager.registerHelpMessage(cmd) {
            withTranslateContext("crystal") {
                tr(help)
            }
        }
    }
}