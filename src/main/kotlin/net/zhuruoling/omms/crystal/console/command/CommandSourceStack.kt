package net.zhuruoling.omms.crystal.console.command

enum class CommandSource{
    CONSOLE,SERVER,REMOTE,CENTRAL
}

class CommandSourceStack(val from: CommandSource) {
}