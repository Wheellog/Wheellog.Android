package com.cooper.wheellog.data

data class LogTick(
    /**
     * Time in 1/10 second
     */
    val time: Float,
    val timeString: String,
    /**
     * Time in 1/10 second + day of week
     */
    val timePlusDayOfWeek: Float,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val batteryLevel: Int,
    /**
     * Battery voltage in Volts
     */
    val voltage: Double,
    /**
     * Current in Ampere
     */
    val current: Double,
    /**
     * Power in Watts
     */
    val power: Double,
    /**
     * Speed in km/h
     */
    val speed: Double,
    /**
     * Speed in km/h by GPS
     */
    val speedGps: Double,
    /**
     * Temperature in Celsius
     */
    val temperature: Int,
    /**
     * PWM in %
     */
    val pwm: Double,
    /**
     * Distance in meters
     */
    val distance: Int,
    /**
     * Total distance in meters
     */
    val totalDistance: Int,
    val timestampMillis: Long = 0,
    val wheelDataAvailable: Boolean = true,
    val hasLocation: Boolean = latitude != 0.0 || longitude != 0.0,
    val gpsTimestampMillis: Long = timestampMillis,
    val gpsAccuracy: Float = 10f,
    val gpsDistanceMeters: Double = 0.0,
    val segmentStart: Boolean = false,
    val pwmAvailable: Boolean = wheelDataAvailable,
    /** Retain stock summary calculations for pre-extension WheelLog CSV files. */
    val legacyStatistics: Boolean = false,
    val phaseCurrent: Double? = null,
    val motorTemperature: Double? = null,
    val availablePower: Double? = power.takeIf { wheelDataAvailable },
    val availableCurrent: Double? = current.takeIf { wheelDataAvailable },
    val availableVoltage: Double? = voltage.takeIf { wheelDataAvailable },
    val availableTemperature: Double? = temperature.toDouble().takeIf { wheelDataAvailable },
    val gpsSpeedAvailable: Boolean = hasLocation,
    /** Active recording time excludes explicit/automatic pauses; never a sensor reading. */
    val recordingElapsedMillis: Long? = null,
)
