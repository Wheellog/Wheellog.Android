package com.cooper.wheellog.utils

object Calculator {
    private const val maxPowerMillis = 10_000 // 10 sec
    private const val lastPackageShouldNotNeLater = 2_000 // 2 sec
    private var powerArray = mutableListOf<Triple<Long, Int, Double>>() // time, distance, power

    private enum class CachedFunc {
        PowerHour,
        WhByKm
    }

    private val isActual: Boolean
        get() = powerArray.size != 0
                && SomeUtil.getNow() - powerArray.last().first < lastPackageShouldNotNeLater

    private fun removeOldElements() {
        val expiredTime = SomeUtil.getNow() - maxPowerMillis
        if (powerArray.first().first > expiredTime) {
            return
        }
        val listToRemove = powerArray.filter { p -> p.first < expiredTime }
        powerArray.removeAll(listToRemove)
    }

    val powerHour: Double
        get() {
            if (Cached.inCache(CachedFunc.PowerHour.name) || !isActual) {
                return Cached.getDouble(CachedFunc.PowerHour.name)
            }
            removeOldElements()
            val elapsedTime = powerArray.last().first - powerArray.first().first
            val powerDelta = powerArray.sumOf { p -> p.third } / powerArray.size
            val value = powerDelta * elapsedTime / 3600_000.0
            Cached.setDouble(CachedFunc.PowerHour.name, value, 1_000)
            return value
        }

    val whByKm: Double
        get() {
            if (Cached.inCache(CachedFunc.WhByKm.name) || !isActual) {
                return Cached.getDouble(CachedFunc.WhByKm.name)
            }
            val distance = powerArray.last().second - powerArray.first().second
            if (distance == 0) {
                return 0.0
            }
            val value = powerHour * 1000.0 / distance
            Cached.setDouble(CachedFunc.WhByKm.name, value, 1_000)
            return value
        }

    fun pushPower(power: Double, distance: Int) {
        val now = SomeUtil.getNow()
        powerArray.add(Triple(now, distance, power))
    }
}