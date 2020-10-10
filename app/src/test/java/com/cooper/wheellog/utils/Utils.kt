package com.cooper.wheellog.utils

import okio.ByteString.Companion.decodeHex

class Utils {
    companion object {
        fun String.hexToByteArray(): ByteArray {
            return this.decodeHex().toByteArray()
        }
    }
}