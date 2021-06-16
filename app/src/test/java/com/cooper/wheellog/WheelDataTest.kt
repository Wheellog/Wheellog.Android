package com.cooper.wheellog

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class WheelDataTest {
    private lateinit var data: WheelData

    @Before
    fun setUp() {
        data = spyk(WheelData())
        every { data.bluetoothLeService.applicationContext } returns mockkClass(Context::class, relaxed = true)
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Battery per km | 95 to 94`() {
        // Arrange.
        data.setBatteryPercent(95)
        data.totalDistance = 1_000

        // Act.
        data.setBatteryPercent(94)
        data.totalDistance += 2_000

        // Assert.
        assertThat(data.batteryPerKm).isEqualTo(0.5)
        assertThat(data.remainingDistance).isEqualTo(188)
    }

    @Test
    fun `Battery per km | 50 to 0`() {
        // Arrange.
        data.setBatteryPercent(50)
        data.totalDistance = 0

        // Act.
        data.setBatteryPercent(0)
        data.totalDistance += 25_000

        // Assert.
        assertThat(data.batteryPerKm).isEqualTo(2)
        assertThat(data.remainingDistance).isEqualTo(0)
    }
}