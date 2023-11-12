package com.cooper.wheellog.utils.gotway

import kotlin.math.roundToInt

/**
 * Calculate the battery percentage for Begode, Gotway, and Extreme Bull wheels.
 */
class GotwayBatteryCalculator {
    // Todo check that this is correct for Begode/Gotway/Extreme Bull Wheels of varying voltages
    fun getBattery(useBetterPercents: Boolean, voltage: Int): Int {
        val battery: Int = if (useBetterPercents) {
            when {
                voltage > 6680 -> 100
                voltage > 5440 -> (voltage - 5380) / 13
                voltage > 5290 -> ((voltage - 5290) / 32.5).roundToInt()
                else -> 0
            }
        } else {
            when {
                voltage <= 5290 -> 0
                voltage >= 6580 -> 100
                else -> (voltage - 5290) / 13
            }
        }
        return battery
    }
}
