package net.zhuruoling.omms.crystal.rcon

import net.zhuruoling.omms.crystal.command.CommandManager
import net.zhuruoling.omms.crystal.command.CommandSource
import net.zhuruoling.omms.crystal.command.CommandSourceStack
import net.zhuruoling.omms.crystal.permission.Permission
import net.zhuruoling.omms.crystal.util.createLogger
import org.slf4j.Logger
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets

class RconClient(password: String, private val socket: Socket) :
    RconBase("RCON Client " + socket.inetAddress) {
    private var authenticated = false
    private val packetBuffer = ByteArray(1460)
    private val password: String
    private val logger: Logger = createLogger("RconClient")

    init {
        try {
            socket.soTimeout = 0
        } catch (var5: Exception) {
            this.isRunning = false
        }
        this.password = password
    }

    override fun run() {
        while (true) {
            try {
                if (this.isRunning) {
                    val bufferedInputStream = BufferedInputStream(socket.getInputStream())
                    val totalReadBytesCount = bufferedInputStream.read(this.packetBuffer, 0, 1460)
                    if (totalReadBytesCount < 10) {
                        return
                    }

                    var ptr = 0
                    val length: Int = getIntLE(this.packetBuffer, 0, totalReadBytesCount)
                    if (length == totalReadBytesCount - 4) {
                        ptr += 4
                        val requestId: Int = getIntLE(this.packetBuffer, ptr, totalReadBytesCount)
                        ptr += 4
                        val type: Int = getIntLE(this.packetBuffer, ptr)
                        ptr += 4
                        when (type) {
                            2 -> {
                                if (this.authenticated) {
                                    val command: String = getString(this.packetBuffer, ptr, totalReadBytesCount)
                                    try {
                                        this.respond(requestId, executeRconCommand(command))
                                    } catch (var15: Exception) {
                                        this.respond(requestId, "Error executing: " + command + " (" + var15.message + ")")
                                    }
                                    continue
                                }
                                this.fail()
                                continue
                            }

                            3 -> {
                                val password: String = getString(this.packetBuffer, ptr, totalReadBytesCount)
                                if (password.isNotEmpty() && password == this.password) {
                                    this.authenticated = true
                                    this.respond(requestId, 2, "")
                                    continue
                                }
                                this.authenticated = false
                                this.fail()
                                continue
                            }

                            else -> {
                                this.respond(requestId, String.format("Unknown request %s", Integer.toHexString(type)))
                                continue
                            }
                        }
                    }

                    return
                }
            } catch (_: IOException) {
            } catch (var17: Exception) {
                logger.error("Exception whilst parsing RCON input", var17 as Throwable)
            }
            this.close()
            logger.info("Thread {} shutting down", description as Any)
            this.isRunning = false
            return
        }
    }

    private fun respond(sessionToken: Int, responseType: Int, message: String) {
        val byteArrayOutputStream = ByteArrayOutputStream(1248)
        val dataOutputStream = DataOutputStream(byteArrayOutputStream)
        val bs = message.toByteArray(StandardCharsets.UTF_8)
        dataOutputStream.writeInt(Integer.reverseBytes(bs.size + 10))
        dataOutputStream.writeInt(Integer.reverseBytes(sessionToken))
        dataOutputStream.writeInt(Integer.reverseBytes(responseType))
        dataOutputStream.write(bs)
        dataOutputStream.write(0)
        dataOutputStream.write(0)
        socket.getOutputStream().write(byteArrayOutputStream.toByteArray())
    }

    private fun fail() {
        this.respond(-1, 2, "")
    }

    private fun respond(sessionToken: Int, message: String) {
        var msg = message
        var i = msg.length
        do {
            val j = if (4096 <= i) 4096 else i
            this.respond(sessionToken, 0, msg.substring(0, j))
            msg = msg.substring(j)
            i = msg.length
        } while (0 != i)
    }

    override fun stop() {
        this.isRunning = false
        this.close()
        super.stop()
    }

    private fun close() {
        try {
            socket.close()
        } catch (var2: IOException) {
            logger.warn("Failed to close socket", var2 as Throwable)
        }
    }

    private fun executeRconCommand(command: String): String {
        val src = CommandSourceStack(CommandSource.REMOTE, permissionLevel = Permission.OWNER)
        CommandManager.execute(command, src)
        return src.feedbackText.joinToString("\n")
    }

}
