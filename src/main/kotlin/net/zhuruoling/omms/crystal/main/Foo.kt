package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.config.ConfigManager
import net.zhuruoling.omms.crystal.text.*

fun main() {
    val text = Text.of("wdnmd")
        .withColor(Color.aqua)
        .withItalic(true)
        .withHoverEvent(
            HoverEvent(
                HoverAction.show_text,
                value = Text.of("wdnmd")
                    .withColor(Color.green)
            )
        )
    println(TextSerializer.serialize(text))
    val textGroup = TextGroup(
        text, text.withColor(Color.yellow) , text.withClickEvent(
            ClickEvent(
                ClickAction.suggest_command,
                "/say wdnmd"
            )
        )
    )
    println(TextSerializer.serialize(textGroup))
    //ConfigManager.getPluginConfig("wdnmd")
}