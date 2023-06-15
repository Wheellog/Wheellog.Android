package com.cooper.wheellog.utils

import android.os.Build

object Calculator {
    private const val maxPowerMillis = 10_000 // 10 sec
    private const val lastPackageShouldNotNeLater = 2_000 // 2 sec
    private var powerArray = mutableListOf<Triple<Long, Int, Double>>() // time, distance, power

    private val isActual: Boolean
        get() = powerArray.size != 0
                && System.currentTimeMillis() - powerArray.last().first < lastPackageShouldNotNeLater

    val powerHour: Double
        get() {
            if (!isActual) {
                return 0.0
            }
            val elapsedTime = powerArray.last().first - powerArray.first().first
            val powerDelta = powerArray.sumOf { p -> p.third } / powerArray.size
            return powerDelta * elapsedTime / 3600.0
        }

    val whByKm: Double
        get() {
            if (!isActual) {
                return 0.0
            }
            val distance = powerArray.last().second - powerArray.first().second
            if (distance == 0) {
                return 0.0
            }
            return powerHour * 1000 / distance
        }

    fun pushPower(power: Double, distance: Int) {
        val time = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            powerArray.removeIf { p -> p.first < time - maxPowerMillis }
        } else {
            powerArray.removeAll(powerArray.filter { p -> p.first < time - maxPowerMillis })
        }
        powerArray.add(Triple(time, distance, power))
    }
}