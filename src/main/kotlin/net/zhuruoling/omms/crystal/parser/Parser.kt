package net.zhuruoling.omms.crystal.parser

interface Parser {}
class Info(val info: String, val thread:String, val level: org.slf4j.event.Level)

data class CommonInfo(
    val raw: String,
    val info: String
)

data class PlayerInfo(
    val raw: String,
    val info: String,
    val isNotSecure: Boolean,
    val player: String,
    val content: String
)

data class PlayerJoin(
    val raw: String,
    val player: String
)

data class PlayerLeft(
    val raw: String,
    val player: String
)

data class ServerOverloadInfo(
    val raw: String,
    val ticks: Long,
    val time: Double
)

data class ServerStartInfo(val version: String)
data class ServerStartingInfo(val ip: String, val port: Int)
data class ServerStartedInfo(val timeElapsed: Long)
class ServerStopping

abstract class MinecraftParser : Parser {
    abstract fun parseToBareInfo(raw: String):Info?
    abstract fun parseServerStart(raw: String): ServerStartInfo?
    abstract fun parseServerStarted(raw: String): ServerStartedInfo?
    abstract fun parseServerCommonInfo(raw: String): CommonInfo?
    abstract fun parseServerPlayerInfo(raw: String): PlayerInfo?
    abstract fun parseServerOverloadInfo(raw: String): ServerOverloadInfo?
    abstract fun parseServerStartingInfo(raw: String): ServerStartingInfo?
    abstract fun parsePlayerJoin(raw: String): PlayerJoin?
    abstract fun parsePlayerLeft(raw: String): PlayerLeft?
    abstract fun parseByPriority(raw: String): Any?
}

class UnableToParseException : UnsupportedOperationException()
