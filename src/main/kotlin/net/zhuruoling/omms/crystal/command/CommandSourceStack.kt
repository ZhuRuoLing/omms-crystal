package net.zhuruoling.omms.crystal.command

enum class CommandSource{
    CONSOLE,SERVER,REMOTE,CENTRAL
}

class CommandSourceStack(val from: CommandSource)