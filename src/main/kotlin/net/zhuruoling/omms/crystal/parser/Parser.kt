package net.zhuruoling.omms.crystal.parser

interface Parser {}

abstract class MinecraftParser : Parser {
    abstract fun parseToBareInfo(raw: String): Info?// TODO:
    abstract fun parseServerStartedInfo(raw: String): ServerStartedInfo?// TODO:
    abstract fun parsePlayerInfo(raw: String): PlayerInfo?// TODO:
    abstract fun parseServerOverloadInfo(raw: String): ServerOverloadInfo?// TODO:
    abstract fun parseServerStartingInfo(raw: String): ServerStartingInfo?// TODO:
    abstract fun parsePlayerJoinInfo(raw: String): PlayerJoinInfo?// TODO:
    abstract fun parsePlayerLeftInfo(raw: String): PlayerLeftInfo?

    abstract fun parseServerStoppingInfo(raw: String):ServerStoppingInfo?
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