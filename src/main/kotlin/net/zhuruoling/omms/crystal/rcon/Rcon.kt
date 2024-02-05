package net.zhuruoling.omms.crystal.rcon

import net.zhuruoling.omms.crystal.config.Config
import nl.vv32.rcon.Rcon
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

object Rcon {

    private lateinit var rcon: Rcon
    private var rconServer: RconListener? = null
    fun connect() {
        val socketChannel = SocketChannel.open(InetSocketAddress(Config.rconPort.toInt()))
        rcon = Rcon.newBuilder()
            .withChannel(socketChannel)
            .withCharset(StandardCharsets.UTF_8)
            .withReadBufferCapacity(8192)
            .withWriteBufferCapacity(8192)
            .build()
        rcon.tryAuthenticate(Config.rconPassword)
    }

    fun startServer() {
        rconServer = RconListener.createRconListener()
    }

    fun stopServer(){
        if (rconServer != null){
            rconServer!!.stop()
        }
    }

    fun close() {
        rcon.close()
    }

    fun <R> useNewRconSession(block: Rcon.() -> R): R {
        return Rcon.newBuilder()
            .withChannel(SocketChannel.open(InetSocketAddress(Config.rconPort.toInt())))
            .withCharset(StandardCharsets.UTF_8)
            .withReadBufferCapacity(8192)
            .withWriteBufferCapacity(8192)
            .build().run {
                tryAuthenticate(Config.rconPassword)
                use(block)
            }
    }

    fun executeCommand(command: String): String {
        return rcon.sendCommand(command)
    }

}