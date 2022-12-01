package net.zhuruoling.omms.crystal.parser

interface Parser {}

abstract class MinecraftParser : Parser {
    abstract fun parseToBareInfo(raw: String): Info?
    abstract fun parseServerStarted(raw: String): ServerStartedInfo?
    abstract fun parseServerPlayerInfo(raw: String): PlayerInfo?
    abstract fun parseServerOverloadInfo(raw: String): ServerOverloadInfo?
    abstract fun parseServerStartingInfo(raw: String): ServerStartingInfo?
    abstract fun parsePlayerJoin(raw: String): PlayerJoinInfo?
    abstract fun parsePlayerLeft(raw: String): PlayerLeftInfo?
}

class UnableToParseException : UnsupportedOperationException()
object ParserManager {
    private val parser: HashMap<String, MinecraftParser> = hashMapOf()
    fun registerParser(id: String, minecraftParser: MinecraftParser, override: Boolean = false) {
        if (parser.containsKey(id) and !override) {
            throw java.lang.UnsupportedOperationException("This parser($minecraftParser) already exists.")
        } else {
            parser[id] = minecraftParser
        }
    }

    fun getParser(id: String): MinecraftParser? {
        return parser[id]
    }

    init {
        parser["builtin"] = BuiltinParser()
    }
}