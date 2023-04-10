package com.cooper.wheellog.map

import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

class LogGeoPoint(latitude: Double, longitude: Double, altitude: Double): GeoPoint(latitude, longitude, altitude) {
    var speed: Double = 0.0
    var voltage: Double = 0.0
    var battery: Int = 0
    var distance: Int = 0
    var temperature: Int = 0
    var timeString: String = ""
    val timeDate: Date?
        get() {
            return if (timeString.isNotEmpty()) {
                val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
                sdf.parse(timeString)
            } else {
                null
            }
        }
}