package com.cooper.wheellog

import com.wheellog.shared.SmartDouble

class WearData {
    var speed: SmartDouble = SmartDouble()
    var voltage: SmartDouble = SmartDouble()
    var current: SmartDouble = SmartDouble()
    var pwm: SmartDouble = SmartDouble()
    var temperature: SmartDouble = SmartDouble()
    var power: SmartDouble = SmartDouble()
    var distance: Double = 0.0
    var battery: Int = 0
    var batteryLowest: Int = 0
    var mainUnit: String = "kmh"
    var currentOnDial: Boolean = false
    var alarmSpeed: Boolean = false
    var alarmTemp: Boolean = false
    var alarmCurrent: Boolean = false
    var timeStamp: Long = -1
    var timeString: String = ""
}