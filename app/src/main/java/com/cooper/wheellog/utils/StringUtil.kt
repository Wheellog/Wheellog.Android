package com.cooper.wheellog.utils

import android.content.Context
import org.json.JSONObject

class StringUtil {
    companion object {
        @JvmStatic
        fun inArray(value: String, array: Array<String>): Boolean {
            return value in array
        }

        @JvmStatic
        fun toHexString(buffer: ByteArray): String {
            return "[" + buffer.joinToString("") { "%02x".format(it) } + "]"
        }

        @JvmStatic
        fun getRawTextResource(context: Context, resId: Int): String {
            return context.resources.openRawResource(resId)
                    .bufferedReader().use { it.readText() }
        }
    }
}