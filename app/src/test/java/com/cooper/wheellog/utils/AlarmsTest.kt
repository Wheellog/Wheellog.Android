package com.cooper.wheellog.utils

import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.WheelData
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class AlarmsTest : KoinTest {
    private val appConfig = mockkClass(AppConfig::class, relaxed = true)
    private val wheelData = mockk<WheelData>(relaxed = true)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { appConfig }
                }
            )
        }
        every { appConfig.alarmBattery } returns 20
        every { appConfig.disablePhoneVibrate } returns true
        every { appConfig.disablePhoneBeep } returns true
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns wheelData
    }

    @After
    fun tearDown() {
        Alarms.stop()
        unmockkAll()
        stopKoin()
    }

    @Test
    fun `Battery alarm is not triggered when wheel is disconnected`() {
        // Arrange.
        every { wheelData.isConnected } returns false
        every { wheelData.batteryLevel } returns 10

        // Act.
        val executed = Alarms.checkAlarm(0.0, mockk(relaxed = true))

        // Assert.
        assertThat(executed).isFalse()
        assertThat(Alarms.alarm and 0x08).isEqualTo(0)
    }
}
