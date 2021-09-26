package com.wheellog.shared

import java.util.*

enum class WearPage {
    Main,
    PWM,
    Temperature,
    Current,
    Voltage,
    Power,
    Distance;

    infix fun and(other: WearPage): WearPages = WearPages.of(this, other)

    companion object {
        private const val separator = ";"

        fun serialize(set: WearPages): String {
            return set.joinToString(separator)
        }

        fun deserialize(value: String): WearPages {
            val names = value.split(separator)
            val r = WearPages.noneOf(WearPage::class.java)
            names.forEach { r.add(valueOf(it)) }
            return r
        }
    }
}

typealias WearPages = EnumSet<WearPage>

infix fun WearPages.and(other: WearPage): WearPages {
    this.add(other)
    return this
}

fun WearPages.serialize(): String {
    return WearPage.serialize(this)
}

fun WearPages.deserialize(value: String): WearPages {
    return WearPage.deserialize(value)
}