package com.cooper.wheellog.utils

import android.content.Context
import com.cooper.wheellog.WheelLog
import java.util.*

object StringUtil {
    @JvmStatic
    fun inArray(value: String, array: Array<String>): Boolean {
        return value in array
    }

    /**
     * Converts supplied Int value to a temperature string.
     */
    @JvmStatic
    fun Int.toTempString(): String {
        return if (WheelLog.AppConfig.useFahrenheit) {
            String.format(Locale.US, "%02d℉", MathsUtil.celsiusToFahrenheit(this.toDouble()).toInt())
        } else {
            String.format(Locale.US, "%02d℃", this)
        }
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