package com.cooper.wheellog

import android.content.Context
import com.cooper.wheellog.utils.Calculator
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class CalculatorTest {
    private lateinit var data: WheelData

    @Before
    fun setUp() {
        data = spyk(WheelData())
        every { data.bluetoothService.applicationContext } returns mockkClass(Context::class, relaxed = true)
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Sets powers`() {
        // Arrange. Act.
        data.apply {
            totalDistance = 0
            setPower(500_000)
            Thread.sleep(200)
            totalDistance = 500
            setPower(1000_000)
        }

        // Assert.
        assertThat(Calculator.whByKm).isGreaterThan(800)
    }
}