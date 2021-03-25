package com.cooper.wheellog.utils

enum class ThemeEnum(val value: Int) {
    Original(0),
    AJDM(1);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}