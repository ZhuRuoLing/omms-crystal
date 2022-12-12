package net.zhuruoling.omms.crystal.command

import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.permission.Permission
import net.zhuruoling.omms.crystal.text.TextGroup
import net.zhuruoling.omms.crystal.text.TextSerializer
import net.zhuruoling.omms.crystal.util.createLogger

enum class CommandSource{
    CONSOLE,CENTRAL,PLAYER,PLUGIN
}

private val logger = createLogger("CommandSourceStack")

class CommandSourceStack(val from: CommandSource, val player: String?, val permissionLevel: Permission?)
{
    fun sendFeedBack(text: TextGroup){
        when(from){
            CommandSource.PLAYER -> {
                assert(SharedConstants.serverHandler != null)
                SharedConstants.serverHandler!!.runCatching {
                    this.input("tellraw $player ${TextSerializer.serialize(text)}")
                }
            }
            CommandSource.CONSOLE -> {
                logger.info(text.toRawString())
            }
            else -> {

            }
        }
    }
}