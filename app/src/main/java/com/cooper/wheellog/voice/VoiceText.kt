package com.cooper.wheellog.voice

import android.content.Context
import android.content.res.Configuration
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.Constants.ALARM_TYPE
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

data class VoiceSample(val mac: String, val capturedAt: Long, val speedKmh: Double,
    val battery: Int, val distanceKm: Double, val voltage: Double, val current: Double,
    val power: Double, val temperature: Int, val motorTemperature: Int, val pwm: Double,
    val totalMetres: Long = 0, val tripMetres: Long = 0,
    val averageMovingSpeedKmh: Double = Double.NaN, val averageSpeedKmh: Double = Double.NaN,
    val rideTime: String = "")

object VoiceText {
    fun language(appLanguage: String) = if (appLanguage == "ru") "ru" else "en"
    fun localized(context: Context, language: String): Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) })

    fun alarm(context: Context, type: ALARM_TYPE, value: Double): String = when (type) {
        ALARM_TYPE.SPEED1, ALARM_TYPE.SPEED2, ALARM_TYPE.SPEED3 -> context.getString(R.string.voice_alarm_speed)
        ALARM_TYPE.PWM -> context.getString(R.string.voice_alarm_pwm, number(context, value * 100, 0))
        ALARM_TYPE.CURRENT -> context.getString(R.string.voice_alarm_current, number(context, abs(value), 0))
        ALARM_TYPE.TEMPERATURE -> context.getString(R.string.voice_alarm_temperature, number(context, value, 0))
        ALARM_TYPE.BATTERY -> context.getString(R.string.voice_alarm_battery, number(context, value, 0))
        ALARM_TYPE.WHEEL -> context.getString(R.string.voice_alarm_wheel)
    }

    fun report(context: Context, sample: VoiceSample, fields: Set<String>, miles: Boolean): String {
        val parts = mutableListOf<String>()
        if ("speed" in fields && sample.speedKmh.isFinite()) parts += context.getString(
            if (miles) R.string.voice_value_speed_mph else R.string.voice_value_speed_kmh,
            number(context, abs(sample.speedKmh) * if (miles) 0.621371 else 1.0, 0))
        if ("average_moving_speed" in fields && sample.averageMovingSpeedKmh.isFinite()) parts += context.getString(
            if (miles) R.string.voice_value_average_moving_mph else R.string.voice_value_average_moving_kmh,
            number(context, abs(sample.averageMovingSpeedKmh) * if (miles) 0.621371 else 1.0, 1))
        if ("average_speed" in fields && sample.averageSpeedKmh.isFinite()) parts += context.getString(
            if (miles) R.string.voice_value_average_mph else R.string.voice_value_average_kmh,
            number(context, abs(sample.averageSpeedKmh) * if (miles) 0.621371 else 1.0, 1))
        if ("distance" in fields && sample.distanceKm.isFinite() && sample.distanceKm >= 0) parts += quantity(context,
            if (miles) R.plurals.voice_distance_mi else R.plurals.voice_distance_km,
            sample.distanceKm * if (miles) 0.621371 else 1.0, 1)
        if ("ride_time" in fields) duration(context, sample.rideTime)?.let { parts += context.getString(R.string.voice_value_time, it) }
        if ("battery" in fields && sample.battery in 0..100) parts += quantity(context, R.plurals.voice_battery_percent, sample.battery.toDouble(), 0)
        if ("voltage" in fields && sample.voltage.isFinite() && sample.voltage > 0) parts += context.getString(R.string.voice_value_voltage, number(context, sample.voltage, 1))
        if ("current" in fields && sample.current.isFinite()) parts += context.getString(R.string.voice_value_current, number(context, sample.current, 0))
        if ("power" in fields && sample.power.isFinite()) parts += context.getString(R.string.voice_value_power, number(context, sample.power, 0))
        if ("pwm" in fields && sample.pwm.isFinite()) parts += context.getString(R.string.voice_value_pwm, number(context, sample.pwm, 0))
        if ("temperature" in fields) parts += context.getString(R.string.voice_value_temperature, number(context, sample.temperature.toDouble(), 0))
        if ("motor_temperature" in fields && sample.motorTemperature != 0) parts += context.getString(R.string.voice_value_motor_temperature, number(context, sample.motorTemperature.toDouble(), 0))
        return parts.joinToString(" ")
    }

    private fun duration(context: Context, time: String): String? {
        val values = time.split(':').map { it.toIntOrNull() ?: return null }
        if (values.size != 3 || values[0] < 0 || values[1] !in 0..59 || values[2] !in 0..59) return null
        val units = listOf(R.plurals.voice_time_hours, R.plurals.voice_time_minutes, R.plurals.voice_time_seconds)
        return values.mapIndexedNotNull { index, value ->
            if (value > 0 || index == 2 && values.all { it == 0 })
                context.resources.getQuantityString(units[index], value, value) else null
        }.joinToString(" ")
    }

    private fun number(context: Context, value: Double, digits: Int): String = NumberFormat.getNumberInstance(
        context.resources.configuration.localesCompat()).apply {
        isGroupingUsed = false; maximumFractionDigits = digits; minimumFractionDigits = 0
    }.format(value)

    private fun quantity(context: Context, id: Int, value: Double, digits: Int): String {
        val rounded = java.math.BigDecimal.valueOf(value).setScale(digits, java.math.RoundingMode.HALF_EVEN).stripTrailingZeros()
        val count = if (rounded.scale() > 0) 2 else abs(rounded.toInt())
        return context.resources.getQuantityString(id, count, number(context, value, digits))
    }

    @Suppress("DEPRECATION")
    private fun Configuration.localesCompat(): Locale = if (android.os.Build.VERSION.SDK_INT >= 24) locales[0] else locale
}
