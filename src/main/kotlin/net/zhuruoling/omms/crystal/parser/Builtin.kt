package net.zhuruoling.omms.crystal.parser

import org.slf4j.event.Level


val regex =
    Regex("\\[(\\d{2}:\\d{2}:\\d{2})\\] \\[([A-Za-z0-9\\u0020！@#$%^&*\\(\\)+=_-]*)[/]([A-Za-z0-9_-]*)\\]: ([^\\f\\r\\n\\v]*\\w*)")


class BasicParser : MinecraftParser() {
    override fun parseToBareInfo(raw: String): Info? {
        val matcher = regex.toPattern().matcher(raw)
        if (!matcher.matches()) return null
        val level = Level.valueOf(matcher.group(3))
        return Info(matcher.group(4), matcher.group(2), level)
    }

    override fun parseServerStart(raw: String): ServerStartInfo? {
        TODO("Not yet implemented")
    }

    override fun parseServerStarted(raw: String): ServerStartedInfo? {
        TODO("Not yet implemented")
    }

    override fun parseServerCommonInfo(raw: String): CommonInfo? {
        TODO("Not yet implemented")
    }

    override fun parseServerPlayerInfo(raw: String): PlayerInfo? {
        TODO("Not yet implemented")
    }

    override fun parseServerOverloadInfo(raw: String): ServerOverloadInfo? {
        TODO("Not yet implemented")
    }

    override fun parseServerStartingInfo(raw: String): ServerStartingInfo? {
        TODO("Not yet implemented")
    }

    override fun parsePlayerJoin(raw: String): PlayerJoin? {
        TODO("Not yet implemented")
    }

    override fun parsePlayerLeft(raw: String): PlayerLeft? {
        TODO("Not yet implemented")
    }

    override fun parseByPriority(raw: String): Any? {
        TODO("Not yet implemented")
    }

}