package com.wheellog.shared

class Constants {
    companion object {
        // settings
        const val wearPages = "wearos_pages"

        // for wearOs transport
        const val wearOsDataItemPath = "/wheel_data"
        const val wearOsPagesItemPath = "/page_settings"
        const val wearOsStartPath = "/start/wearos"
        const val wearOsDataMessagePath = "/messages"
        const val wearOsPingMessage = "ping"
        const val wearOsPongMessage = "pong"
        const val wearOsFinishMessage = "finish"
        const val wearOsHornMessage = "horn"
        const val wearOsLightMessage = "light"

        // wearOs dataSet keys
        const val wearOsSpeedData = "speed"
        const val wearOsMaxSpeedData = "max_speed"
        const val wearOsVoltageData = "voltage"
        const val wearOsCurrentData = "current"
        const val wearOsMaxCurrentData = "max_current"
        const val wearOsPowerData = "power"
        const val wearOsMaxPowerData = "max_power"
        const val wearOsPWMData = "pwm"
        const val wearOsMaxPWMData = "max_pwm"
        const val wearOsTemperatureData = "temperature"
        const val wearOsMaxTemperatureData = "max_temperature"
        const val wearOsBatteryData = "battery"
        const val wearOsBatteryLowData = "battery_lowest"
        const val wearOsDistanceData = "distance"
        const val wearOsUnitData = "main_unit"
        const val wearOsCurrentOnDialData = "current_on_dial"
        const val wearOsAlarmData = "alarm"
        const val wearOsTimestampData = "timestamp"
        const val wearOsTimeStringData = "time_string"
        const val wearOsPagesData = "pages"
    }
}