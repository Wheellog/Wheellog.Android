package com.cooper.wheellog

import android.content.Context
import com.cooper.wheellog.utils.Calculator
import com.cooper.wheellog.utils.SomeUtil
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
        mockkObject(SomeUtil)
        var time = 0L
        every { SomeUtil.getNow() } answers {
            time += 250
            time
        }
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
            totalDistance = 500
            setPower(1000_000)
        }

        // Assert.
        assertThat(Calculator.whByKm).apply {
            isGreaterThan(1.0)
            isLessThan(2.0)
        }
    }

    @Test
    fun `Push powers over 10 sec`() {
        // Arrange. Act.
        data.apply {
            totalDistance = 0
            for (i in 1..50) {
                totalDistance = i * 10L
                setPower(500_000 + i * 1000)
            }
        }

        // Assert.
        val power = Calculator.powerHour
        val whByKm = Calculator.whByKm

        assertThat(power).apply {
            isGreaterThan(13.5)
            isLessThan(14.0)
        }
        assertThat(whByKm).apply {
            isGreaterThan(36.9)
            isLessThan(37.0)
        }
    }
}