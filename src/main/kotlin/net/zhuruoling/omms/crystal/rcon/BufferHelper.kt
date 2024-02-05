package net.zhuruoling.omms.crystal.rcon

import java.nio.charset.StandardCharsets

private val HEX_CHARS_LOOKUP: CharArray =
    charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun getString(buf: ByteArray, i: Int, j: Int): String {
    val k = j - 1
    var l = if (i > k) k else i
    while (0 != buf[l].toInt() && l < k) {
        ++l
    }

    return String(buf, i, l - i, StandardCharsets.UTF_8)
}
fun getIntLE(buf: ByteArray, start: Int): Int {
    return getIntLE(buf, start, buf.size)
}
fun getIntLE(buf: ByteArray, start: Int, limit: Int): Int {
    return if (0 > limit - start - 4) 0 else buf[start + 3].toInt() shl 24 or ((buf[start + 2].toInt() and 255) shl 16) or ((buf[start + 1].toInt() and 255) shl 8) or (buf[start].toInt() and 255)
}
fun getIntBE(buf: ByteArray, start: Int, limit: Int): Int {
    return if (0 > limit - start - 4) 0 else buf[start].toInt() shl 24 or ((buf[start + 1].toInt() and 255) shl 16) or ((buf[start + 2].toInt() and 255) shl 8) or (buf[start + 3].toInt() and 255)
}
fun toHex(b: Byte): String {
    val var10000 = HEX_CHARS_LOOKUP[b.toInt() and 240 ushr 4]
    return "" + var10000 + HEX_CHARS_LOOKUP[b.toInt() and 15]
}
