package net.zhuruoling.omms.crystal.text

class Color {

}


abstract class Text {
    abstract var literalString: String
    abstract val compoentText: List<out Text>
    abstract var style: Style
    abstract fun toRawString(): String
    abstract fun withStyle(style: Style): Text
}

class LiteralText private constructor(override var literalString: String = "") : Text() {

    companion object {
        fun of(content: String): Text {
            return LiteralText(content)
        }

        fun join(list: List<Text>, separator: Text) {
            TODO("Not yet implemented")
        }
    }

    override val compoentText: List<Text> = mutableListOf()
    override var style: Style = Style.empty

    override fun toRawString(): String {
        return literalString
    }

    override fun withStyle(style: Style): Text {
        return this
    }


}

class Style private constructor() {
    companion object {
        val empty: Style = Style()
    }
}