package net.zhuruoling.omms.crystal.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder


private fun literal(literal: String): LiteralArgumentBuilder<CommandSourceStack> {
    return LiteralArgumentBuilder.literal(literal)
}

private fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSourceStack, T> {
    return RequiredArgumentBuilder.argument(name, type)
}

private fun integerArgument(name: String):RequiredArgumentBuilder<CommandSourceStack, Int>{
    return argument(name, IntegerArgumentType.integer())
}

private fun wordArgument(name: String):RequiredArgumentBuilder<CommandSourceStack, String>{
    return argument(name, StringArgumentType.word())
}

private fun greedyStringArgument(name: String):RequiredArgumentBuilder<CommandSourceStack,String>{
    return argument(name, StringArgumentType.greedyString())
}

val helpCommand = literal("help").then(
    argument("page", IntegerArgumentType.integer())
        .executes {
            return@executes 1
        }
).executes {
    return@executes 1
}


