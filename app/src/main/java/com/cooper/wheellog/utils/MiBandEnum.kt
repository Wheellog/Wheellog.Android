package com.cooper.wheellog.utils

enum class MiBandEnum(val value: Int) {
    Alarm(0),
    Min(1),
    Medium(2),
    Max(3);

    fun next() = if (this.ordinal == values().size - 1)
        values()[0]
    else
        values()[this.ordinal + 1]

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}