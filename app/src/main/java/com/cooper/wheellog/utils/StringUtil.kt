package com.cooper.wheellog.utils

import android.content.Context

class StringUtil {
    companion object {
        @JvmStatic
        fun inArray(value: String, array: Array<String>): Boolean {
            return value in array
        }

        @JvmStatic
        fun toHexString(buffer: ByteArray): String {
            return "[" + toHexStringRaw(buffer) + "]"
        }

        @JvmStatic
        fun toHexStringRaw(buffer: ByteArray): String {
            return buffer.joinToString("") { "%02x".format(it) }
        }

        @JvmStatic
        fun getRawTextResource(context: Context, resId: Int): String {
            return context.resources.openRawResource(resId)
                    .bufferedReader().use { it.readText() }
        }

        @JvmStatic
        fun deleteFirstSentence(text: CharSequence): CharSequence {
            val indexOfNewLine = text.indexOfFirst { r -> r == '\n' }
            return if (indexOfNewLine != -1) {
                text.substring(indexOfNewLine + 1)
            } else {
                text
            }
        }

        @JvmStatic
        fun isCorrectMac(mac: String): Boolean {
            return Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$").matches(mac)
        }
    }
}