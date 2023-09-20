package com.cooper.wheellog.utils

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [30, 33])
@RunWith(RobolectricTestRunner::class)
class NotificationContentTest {

    @Test
    fun `prefix only notification`() {
        assertEquals("*", buildNotification(RuntimeEnvironment.getApplication()) {
            prefix = "*"
        })
    }

    @Test
    fun `prefix, separator ignored, battery level, celsius`() {
        assertEquals("* 37℃", buildNotification(RuntimeEnvironment.getApplication()) {
            prefix = "* "
            separator = " | "
            temperatureDegreesOfCelsius = 37
        })
    }

    @Test
    fun `prefix, separator, battery level, distance, celsius, kilometer`() {
        assertEquals("* 11.1 km | 37℃ | 80%", buildNotification(RuntimeEnvironment.getApplication()) {
            prefix = "* "
            separator = " | "
            distanceKm = 11.1
            temperatureDegreesOfCelsius = 37
            batteryLevelPct = 80
        })
    }

    @Test
    fun `prefix, separator, battery level, distance, fahrenheit, miles`() {
        assertEquals("* 6.9 mi | 99℉ | 80%", buildNotification(RuntimeEnvironment.getApplication()) {
            prefix = "* "
            separator = " | "
            useMiles = true
            useFahrenheits = true

            distanceKm = 11.1
            temperatureDegreesOfCelsius = 37
            batteryLevelPct = 80

        })
    }
}
