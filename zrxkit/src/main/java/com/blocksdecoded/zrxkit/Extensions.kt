package com.blocksdecoded.zrxkit

import org.bouncycastle.util.encoders.Hex

internal fun ByteArray?.toHexString(): String {
    return this?.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    } ?: ""
}

@Throws(NumberFormatException::class)
internal fun String.hexStringToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

internal fun String.prefixed(): String = "0x$this"

internal fun String.decodePrefixedHex(): ByteArray = Hex.decode(this.clearPrefix())

internal fun String.clearPrefix(): String = this.substring(2)
