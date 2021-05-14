package com.cooper.wheellog

class WearData {
    var speed: Double = 0.0
    var maxSpeed: Int = 1
    var voltage: Double = 0.0
    var current: Double = 0.0
    var maxCurrent: Double = 1.0
    var pwm: Double = 0.0
    var maxPwm: Double = 0.0
    var temperature: Int = 0
    var maxTemperature: Int = 0
    var maxPower: Double = 0.0
    var battery: Int = 0
    var batteryLowest: Int = 0
    var mainUnit: String = "kmh"
    var currentOnDial: Boolean = false
    var alarm: Boolean = false
    var timeStamp: Long = -1
    var timeString: String = ""
}