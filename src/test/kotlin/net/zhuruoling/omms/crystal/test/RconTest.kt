package icu.takeneko.omms.crystal.test

import net.kyori.adventure.text.Component
import icu.takeneko.omms.crystal.command.CommandManager
import icu.takeneko.omms.crystal.command.literal
import icu.takeneko.omms.crystal.rcon.RconListener
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals

class RconTest {
    @Test
    fun rconTest() {
        CommandManager.register(literal("testCommand").executes {
            it.source.sendFeedback(Component.text("TEST COMMAND"))
            1
        })
        ServerSocket(11451, 0, InetAddress.getByName("0.0.0.0")).apply {
            setSoTimeout(500)
            val listener = RconListener(this, "12345678")
            listener.start()
            val socketChannel = SocketChannel.open(InetSocketAddress(11451))
            val rcon = nl.vv32.rcon.Rcon.newBuilder()
                .withChannel(socketChannel)
                .withCharset(StandardCharsets.UTF_8)
                .withReadBufferCapacity(8192)
                .withWriteBufferCapacity(8192)
                .build()
            rcon.tryAuthenticate("12345678")
            Thread.sleep(1000)
            val result = rcon.sendCommand("testCommand")
            println(result)
            Thread.sleep(1000)
            assertEquals("TEST COMMAND\nao", result)
            rcon.close()
            listener.stop()
        }

    }
}