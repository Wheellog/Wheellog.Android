package com.cooper.wheellog.utils

import android.content.Context
import com.cooper.wheellog.R
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun buildNotification(
    context: Context,
    config: NotificationContent.() -> Unit
): CharSequence {
    val notificationContent = NotificationContent()
    notificationContent.config()
    return notificationContent.buildNotification(context)
}


class NotificationContent {

    sealed class WhatToDisplay {
        data class DistanceKm(val value: Double) : WhatToDisplay()
        data class TemperatureDegreesOfCelsius(val value: Int) : WhatToDisplay()
        data class BatteryLevel(val value: Int) : WhatToDisplay()
        data class SpeedKmPh(val value: Double) : WhatToDisplay()
    }

    private val itemsDisplayed = mutableListOf<WhatToDisplay>()

    var useMiles: Boolean = false
    var useFahrenheits: Boolean = false
    var prefix: String = ""
    var separator: String = ""

    var distanceKm: Double by writeOnlyProperty { itemsDisplayed.add(WhatToDisplay.DistanceKm(it)) }
    var temperatureDegreesOfCelsius: Int by writeOnlyProperty {
        itemsDisplayed.add(WhatToDisplay.TemperatureDegreesOfCelsius(it))
    }
    var batteryLevelPct: Int by writeOnlyProperty { itemsDisplayed.add(WhatToDisplay.BatteryLevel(it)) }
    var speedKmPh: Double by writeOnlyProperty { itemsDisplayed.add(WhatToDisplay.SpeedKmPh(it)) }

    private fun <T> writeOnlyProperty(function: (t: T) -> Boolean): ReadWriteProperty<NotificationContent, T> {
        return object : ReadWriteProperty<NotificationContent, T> {
            override fun getValue(thisRef: NotificationContent, property: KProperty<*>): T {
                throw UnsupportedOperationException("Property ${property.name} is write-only")
            }

            override fun setValue(thisRef: NotificationContent, property: KProperty<*>, value: T) {
                function(value)
            }
        }
    }

    fun buildNotification(context: Context): CharSequence {
        return prefix + itemsDisplayed.joinToString(separator) {
            when (it) {
                is WhatToDisplay.BatteryLevel -> "${it.value}%"
                is WhatToDisplay.DistanceKm -> if (useMiles) {
                    "${
                        MathsUtil.kmToMiles(it.value).formatDecimal()
                    } ${context.getString(R.string.miles)}"
                } else {
                    "${it.value.formatDecimal()} ${context.getString(R.string.km)}"
                }

                is WhatToDisplay.TemperatureDegreesOfCelsius ->
                    if (useFahrenheits) {
                        "${
                            MathsUtil.celsiusToFahrenheit(it.value.toDouble()).formatDecimal(0)
                        }${context.getString(R.string.degrees_of_fahrenheit)}"
                    } else {
                        "${it.value}${context.getString(R.string.degrees_of_celsius)}"
                    }

                is WhatToDisplay.SpeedKmPh -> if (useMiles) {
                    "${
                        MathsUtil.kmToMiles(it.value).formatDecimal()
                    } ${context.getString(R.string.mph)}"
                } else {
                    "${it.value.formatDecimal()} ${context.getString(R.string.kmh)}"
                }
            }
        }

    }

    private fun Double.formatDecimal(maxFractionDigits: Int = 1): String =
        DecimalFormat().apply {
            isGroupingUsed = false
            minimumFractionDigits = 0
            maximumFractionDigits = maxFractionDigits
            isDecimalSeparatorAlwaysShown = false
            roundingMode = RoundingMode.HALF_UP
        }.format(this)
}
