package com.cooper.wheellog.utils

import android.os.Build

object Calculator {
    private const val maxPowerMillis = 10_000 // 10 sec
    private const val lastPackageShouldNotNeLater = 1_000 // 1 sec
    private var powerArray = mutableListOf<Triple<Long, Int, Double>>() // time, distance, power

    val powerHour: Double
        get() {
            if (powerArray.size == 0 || powerArray.last().first < lastPackageShouldNotNeLater) {
                return 0.0
            }
            val elapsedTime = powerArray.last().first - powerArray.first().first
            val powerDelta = powerArray.sumOf { p -> p.third } / powerArray.size
            return powerDelta * elapsedTime / 3600.0
        }

    val whByKm: Double
        get() {
            if (powerArray.size == 0) {
                return 0.0
            }
            val distance = powerArray.first().second - powerArray.last().second
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