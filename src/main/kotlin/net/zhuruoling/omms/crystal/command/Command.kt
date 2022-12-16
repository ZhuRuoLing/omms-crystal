package net.zhuruoling.omms.crystal.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.event.ServerStartEvent
import net.zhuruoling.omms.crystal.event.ServerStartEventArgs
import net.zhuruoling.omms.crystal.event.ServerStopEvent
import net.zhuruoling.omms.crystal.event.ServerStopEventArgs
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.permission.Permission
import net.zhuruoling.omms.crystal.permission.PermissionManager
import net.zhuruoling.omms.crystal.permission.comparePermission
import net.zhuruoling.omms.crystal.permission.reslovePermissionLevel
import net.zhuruoling.omms.crystal.util.createLogger


fun literal(literal: String): LiteralArgumentBuilder<CommandSourceStack> {
    return LiteralArgumentBuilder.literal(literal)
}

private fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSourceStack, T> {
    return RequiredArgumentBuilder.argument(name, type)
}

fun integerArgument(name: String): RequiredArgumentBuilder<CommandSourceStack, Int> {
    return argument(name, IntegerArgumentType.integer())
}

fun wordArgument(name: String): RequiredArgumentBuilder<CommandSourceStack, String> {
    return argument(name, StringArgumentType.word())
}

fun greedyStringArgument(name: String): RequiredArgumentBuilder<CommandSourceStack, String> {
    return argument(name, StringArgumentType.greedyString())
}

fun getWord(context: CommandContext<CommandSourceStack>, name: String): String {
    return StringArgumentType.getString(context, name)
}

fun getInteger(context: CommandContext<CommandSourceStack>, name: String): Int {
    return IntegerArgumentType.getInteger(context, name)
}

val helpCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.commandPrefix + "help").then(
    integerArgument("page")
        .executes {
            1
        }
).executes {
    1
}

val permissionCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.commandPrefix + "permission").then(
    literal("set").then(
        wordArgument("player")
            .then(
                wordArgument("permissionLevel").requires {
                    if (it.from == CommandSource.PLAYER)
                        comparePermission(it.permissionLevel!!, Permission.OWNER)
                    else
                        true
                }.executes {
                    PermissionManager.setPermission(
                        getWord(it, "player"),
                        reslovePermissionLevel(getWord(it, "permissionLevel"))
                    )
                    1
                }
            )
            .requires {
                if (it.from == CommandSource.PLAYER)
                    comparePermission(it.permissionLevel!!, Permission.OWNER)
                else
                    true
            }.executes {
                PermissionManager.setPermission(getWord(it, "player"))
                1
            }
    )
).then(
    literal("delete").then(
        wordArgument("player").requires {
            if (it.from == CommandSource.PLAYER)
                comparePermission(it.permissionLevel!!, Permission.OWNER)
            else
                true
        }.executes {
            PermissionManager.deletePlayer(getWord(it, "player"))
            1
        }
    )
).then(
    literal("list").executes {
        val permissionStorage = PermissionManager.convertToPermissionStorage()
        val logger = createLogger("Command")
        logger.info("Permissions:")
        logger.info("\tOwners:${permissionStorage.owner.joinToString(separator = ", ")}")
        logger.info("\tAdmins:${permissionStorage.admin.joinToString(separator = ", ")}")
        logger.info("\tUsers:${permissionStorage.user.joinToString(separator = ", ")}")
        logger.info("\tGuests:${permissionStorage.guest.joinToString(separator = ", ")}")
        1
    }
).then(
    literal("save").requires {
        if (it.from == CommandSource.PLAYER)
            comparePermission(it.permissionLevel!!, Permission.OWNER)
        else
            true
    }.executes {
        PermissionManager.writePermission()
        1
    }
)

val startCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.commandPrefix + "start").requires {
    if (it.from == CommandSource.PLAYER)
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

val stopCommand: LiteralArgumentBuilder<CommandSourceStack> = literal(Config.commandPrefix + "stop").then(
    argument("force", BoolArgumentType.bool()).requires {
        if (it.from == CommandSource.PLAYER)
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
                    CommandSource.CENTRAL -> "central"
                    CommandSource.CONSOLE -> "console"
                },
                BoolArgumentType.getBool(it, "force")
            )
        )
        1
    }
)
    .requires {
        if (it.from == CommandSource.PLAYER)
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
                    CommandSource.CENTRAL -> "central"
                    CommandSource.CONSOLE -> "console"
                },
                false
            )
        )
        1
    }

val pluginCommand = literal(Config.commandPrefix + "plugin")




