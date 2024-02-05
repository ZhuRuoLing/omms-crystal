package net.zhuruoling.omms.crystal.rcon

import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.util.createLogger
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class RconListener(private val listener: ServerSocket, private val password: String): RconBase("RCON Listener") {
    private val clients: MutableList<RconClient> = arrayListOf()


    private fun removeStoppedClients() {
        clients.removeIf { client: RconClient -> !client.isRunning }
    }

    override fun run() {
        try {
            while (this.isRunning) {
                try {
                    val socket: Socket = listener.accept()
                    val rconClient = RconClient(this.password, socket)
                    rconClient.start()
                    clients.add(rconClient)
                    this.removeStoppedClients()
                } catch (e: SocketTimeoutException) {
                    this.removeStoppedClients()
                } catch (e: IOException) {
                    if (this.isRunning) {
                        logger.info("IO exception: ", e)
                    }
                }
            }
        } finally {
            this.closeSocket(this.listener)
        }
    }

    override fun stop() {
        this.isRunning = false
        this.closeSocket(this.listener)
        super.stop()
        for (client in clients) {
            if (client.isRunning) {
                client.stop()
            }
        }
        clients.clear()
    }

    private fun closeSocket(socket: ServerSocket) {
        logger.debug("closeSocket: {}", socket)
        try {
            socket.close()
        } catch (e: IOException) {
            logger.warn("Failed to close socket", e)
        }
    }

    companion object {
        private val logger = createLogger("RconListener")
        fun createRconListener(): RconListener? {
            val port: Int = Config.rconServerPort
            if (port in 1..65535) {
                val password: String = Config.rconServerPassword
                if (password.isEmpty()) {
                    //logger.warn("No rcon password set in config.properties, rcon disabled!")
                    return null
                } else {
                    try {
                        val serverSocket = ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"))
                        serverSocket.setSoTimeout(500)
                        val rconListener = RconListener(serverSocket, password)
                        if (!rconListener.start()) {
                            return null
                        } else {
                            logger.info("RCON running on 0.0.0.0:{}", port)
                            return rconListener
                        }
                    } catch (e: IOException) {
                        logger.warn("Unable to initialise RCON on 0.0.0.0:{}", port, e)
                        return null
                    }
                }
            } else {
                logger.warn("Invalid rcon port {} found in config.properties, rcon disabled!", port as Any)
                return null
            }
        }
    }
}
