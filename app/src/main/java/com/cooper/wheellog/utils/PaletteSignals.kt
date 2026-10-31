package com.cooper.wheellog.utils

/** Visual signals only: these checks do not trigger sound, vibration or wheel commands. */
object PaletteSignals {
    fun isSpeedWarning(customPalette: Boolean, pwmBasedAlarms: Boolean, speedTenths: Int, alarmSpeed: Int): Boolean =
        customPalette && !pwmBasedAlarms && alarmSpeed > 0 && speedTenths >= alarmSpeed * 10

    fun isBatteryWarning(customPalette: Boolean, battery: Int, threshold: Int): Boolean =
        customPalette && battery in 0..100 && battery <= threshold
}
