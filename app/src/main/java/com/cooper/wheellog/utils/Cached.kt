package com.cooper.wheellog.utils

object Cached {
    private val expiredFunctions = hashMapOf<String, Long>()
    private val doubleValues = hashMapOf<String, Double>()

    fun inCache(funName: String): Boolean {
        val time = expiredFunctions[funName] ?: 0L
        return time > SomeUtil.getNow()
    }

    fun getDouble(funName: String): Double {
        return doubleValues[funName] ?: 0.0
    }

    fun setDouble(funName: String, value: Double, cacheInMillis: Long) {
        doubleValues[funName] = value
        expiredFunctions[funName] = SomeUtil.getNow() + cacheInMillis
    }
}