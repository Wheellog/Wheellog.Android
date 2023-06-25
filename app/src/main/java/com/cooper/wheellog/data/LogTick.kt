package com.cooper.wheellog.data

data class LogTick(
    val time: Float,
    val timeString: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val batteryLevel: Int,
    val voltage: Double,
    val current: Double,
    val power: Double,
    val speed: Double,
    val speedGps: Double,
    val temperature: Int,
    val pwm: Double,
    val distance: Int,
)