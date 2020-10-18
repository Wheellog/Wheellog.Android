package com.cooper.wheellog.utils

class StringUtil {
    companion object {
        @JvmStatic
        fun inArray(value: String, array: Array<String>): Boolean {
            return value in array
        }
    }
}