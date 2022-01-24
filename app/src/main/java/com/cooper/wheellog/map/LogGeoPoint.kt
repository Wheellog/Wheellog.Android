package com.cooper.wheellog.map

import org.osmdroid.util.GeoPoint

class LogGeoPoint(latitude: Double, longitude: Double, altitude: Double): GeoPoint(latitude, longitude, altitude) {
    var speed: Double = 0.0
    var voltage: Double = 0.0
    var battery: Int = 0
    var distance: Int = 0
    var temperature: Int = 0
    var timeString: String = ""

    override fun toString(): String {
        // TODO: localize me
        return String.format("Speed: %.2f km/h\nBattery: %s %%\nDistance by wheel: %.2f km\nVoltage: %.2f V\nTemperature: %s °C\nTime: %s",
            speed,
            battery,
            distance / 1000.0,
            voltage,
            temperature,
            timeString)
    }
}