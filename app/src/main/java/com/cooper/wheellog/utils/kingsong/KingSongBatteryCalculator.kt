package com.cooper.wheellog.utils.kingsong

import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.WheelData
import kotlin.math.roundToInt

class KingSongBatteryCalculator(
    private val wd: WheelData,
    private val appConfig: AppConfig,
) {
    fun calculateAndStoreBatteryLevel(voltage: Int) {
        val useBetterPercents = appConfig.useBetterPercents
        val battery = when {
            KingsongUtils.is84vWheel(wd) -> eightyFourVoltWheel(useBetterPercents, voltage)
            KingsongUtils.is100vWheel(wd) -> oneHundredVoltWheel(useBetterPercents, voltage)
            KingsongUtils.is126vWheel(wd) -> oneHundredTwentySixVoltWheel(
                useBetterPercents,
                voltage,
            )

            else -> unknownWheel(useBetterPercents, voltage)
        }
        wd.batteryLevel = battery
    }

    private fun unknownWheel(useBetterPercents: Boolean, voltage: Int): Int = if (useBetterPercents) {
        when {
            voltage > 6680 -> 100
            voltage > 5440 -> ((voltage - 5320) / 13.6).roundToInt()
            voltage > 5120 -> (voltage - 5120) / 36
            else -> 0
        }
    } else {
        when {
            voltage < 5000 -> 0
            voltage >= 6600 -> 100
            else -> (voltage - 5000) / 16
        }
    }

    /**
     * KS-18L, KS-16X, KS-16XF, RW, KS-18LH, KS-18LY, KS-S18
     */
    private fun eightyFourVoltWheel(useBetterPercents: Boolean, voltage: Int): Int =
        if (useBetterPercents) {
            when {
                voltage > 8350 -> 100
                voltage > 6800 -> (voltage - 6650) / 17
                voltage > 6400 -> (voltage - 6400) / 45
                else -> 0
            }
        } else {
            when {
                voltage < 6250 -> 0
                voltage >= 8250 -> 100
                else -> (voltage - 6250) / 20
            }
        }

    /**
     * S19
     */
    private fun oneHundredVoltWheel(useBetterPercents: Boolean, voltage: Int): Int =
        if (useBetterPercents) {
            when {
                voltage > 10020 -> 100
                voltage > 8160 -> ((voltage - 7980) / 20.4).roundToInt()
                voltage > 7680 -> ((voltage - 7680) / 54.0).roundToInt()
                else -> 0
            }
        } else {
            when {
                voltage < 7500 -> 0
                voltage >= 9900 -> 100
                else -> (voltage - 7500) / 24
            }
        }

    /**
     * S20, S22, S22 Pro
     */
    private fun oneHundredTwentySixVoltWheel(useBetterPercents: Boolean, voltage: Int): Int =
        if (useBetterPercents) {
            when {
                voltage > 12525 -> 100
                voltage > 10200 -> ((voltage - 9975) / 25.5).roundToInt()
                voltage > 9600 -> ((voltage - 9600) / 67.5).roundToInt()
                else -> 0
            }
        } else {
            when {
                voltage < 9375 -> 0
                voltage >= 12375 -> 100
                else -> (voltage - 9375) / 30
            }
        }
}
