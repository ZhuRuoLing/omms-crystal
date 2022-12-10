package net.zhuruoling.omms.crystal.command

import net.zhuruoling.omms.crystal.permission.Permission
import net.zhuruoling.omms.crystal.text.Text

enum class CommandSource{
    CONSOLE,CENTRAL,PLAYER,PLUGIN
}

class CommandSourceStack(val from: CommandSource, val player: String?, val permissionLevel: Permission?)
{
    fun sendFeedBack(text: Text){
        when(from){
            CommandSource.PLAYER -> {

            }
            CommandSource.CONSOLE -> {

            }
            else -> {

            }
        }
    }
}