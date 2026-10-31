package com.cooper.wheellog

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class WheelDataTest: KoinTest {
    private lateinit var data: WheelData
    private val appConfig = mockkClass(AppConfig::class, relaxed = true)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { appConfig }
                }
            )
        }
        data = spyk(WheelData())
        every { data.bluetoothService.applicationContext } returns mockkClass(Context::class, relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    @Test
    fun `Wheel speed correction affects display and maximum while raw telemetry is preserved`() {
        data.speed = 10_000
        data.setTopSpeed(12_000)
        every { appConfig.appliedSpeedCorrectionPercent } returns -10.0
        assertThat(data.rawSpeedDouble).isEqualTo(100.0)
        assertThat(data.speedDouble).isEqualTo(90.0)
        assertThat(data.speed).isEqualTo(900)
        assertThat(data.topSpeedDouble).isEqualTo(108.0)
        assertThat(data.topSpeed).isEqualTo(10_800)
        every { appConfig.appliedSpeedCorrectionPercent } returns 10.0
        assertThat(data.speedDouble).isWithin(.00001).of(110.0)
        assertThat(data.rawSpeedDouble).isEqualTo(100.0)
        every { appConfig.appliedSpeedCorrectionPercent } returns 0.0
        assertThat(data.speedDouble).isEqualTo(100.0)
    }

    @Test fun `half percent correction reaches live maximum and captured CSV without integer rounding`() {
        every { appConfig.appliedSpeedCorrectionPercent } returns -6.5
        data.speed = 8_160
        data.setTopSpeed(8_160)
        assertThat(data.rawSpeedDouble).isEqualTo(81.6)
        assertThat(data.speedDouble).isWithin(.00001).of(76.296)
        assertThat(data.topSpeedDouble).isWithin(.00001).of(76.296)
        val sample = com.cooper.wheellog.data.NativeWheelSample.capture(data, 0, "")
        val cells = com.cooper.wheellog.data.NativeCsv.HEADER.split(',').zip(
            com.cooper.wheellog.data.NativeCsv.row(1_700_000_000_000L, null, sample, 0.0, true).split(',')).toMap()
        assertThat(cells.getValue("speed_correction_percent")).isEqualTo("-6.5")
        assertThat(cells.getValue("speed").toDouble()).isWithin(.00001).of(76.296)
        assertThat(cells.getValue("wheel_speed_raw").toDouble()).isEqualTo(81.6)
    }

    @Test
    fun `Battery per km | 95 to 94`() {
        // Arrange.
        data.setBatteryLevel(95)
        data.totalDistance = 1_000

        // Act.
        data.setBatteryLevel(94)
        data.totalDistance += 2_000

        // Assert.
        assertThat(data.batteryPerKm).isEqualTo(0.5)
        assertThat(data.remainingDistance).isEqualTo(188)
    }

    @Test fun `recorded distance origin is independent of session averages and speed correction`() {
        data.totalDistance = 100_000
        data.totalDistance = 105_000
        WheelData::class.java.getDeclaredField("mRidingTime").apply { isAccessible = true }.setInt(data, 600)
        WheelData::class.java.getDeclaredField("mRideTime").apply { isAccessible = true }.setInt(data, 900)
        data.setRecordingDistanceStart(90_000L)
        assertThat(data.distanceDouble).isEqualTo(15.0)
        assertThat(data.distance).isEqualTo(15_000)
        assertThat(data.getDistanceAtOdometer(104_000)).isEqualTo(14_000)
        assertThat(data.averageRidingSpeedDouble).isEqualTo(30.0)
        every { appConfig.appliedSpeedCorrectionPercent } returns -6.0
        assertThat(data.distanceDouble).isEqualTo(15.0)
        assertThat(data.averageRidingSpeedDouble).isWithin(.001).of(28.2)
        data.setRecordingDistanceStart(null)
        assertThat(data.distanceDouble).isEqualTo(0.0)
        data.totalDistance = 106_000
        assertThat(data.distanceDouble).isEqualTo(1.0)
        data.reset()
        assertThat(data.distanceDouble).isEqualTo(0.0)
    }

    @Test
    fun `Battery per km | 50 to 0`() {
        // Arrange.
        data.setBatteryLevel(50)
        data.totalDistance = 0

        // Act.
        data.setBatteryLevel(0)
        data.totalDistance += 25_000

        // Assert.
        assertThat(data.batteryPerKm).isEqualTo(2)
        assertThat(data.remainingDistance).isEqualTo(0)
    }

    @Test
    fun `Max power`() {
        // Arrange.
        data.setPower(50)

        // Act.
        data.setPower(100)
        data.setPower(75)

        // Assert.
        assertThat(data.powerDouble).isEqualTo(0.75)
        assertThat(data.maxPowerDouble).isEqualTo(1)
    }

    @Test
    fun `Max current`() {
        // Arrange.
        data.current = 50

        // Act.
        data.current = 100
        data.current = 75

        // Assert.
        assertThat(data.currentDouble).isEqualTo(0.75)
        assertThat(data.maxCurrentDouble).isEqualTo(1)
    }

    @Test fun `new session resets current and power peaks along with other readings`() {
        data.current = 7_120
        data.phaseCurrent = 12_000
        data.setPower(933_200)
        assertThat(data.maxCurrentDouble).isGreaterThan(0.0)
        assertThat(data.maxPowerDouble).isGreaterThan(0.0)
        data.reset()
        assertThat(data.maxCurrentDouble).isEqualTo(0.0)
        assertThat(data.maxPhaseCurrentDouble).isEqualTo(0.0)
        assertThat(data.maxPowerDouble).isEqualTo(0.0)
        data.current = 100
        data.setPower(200)
        assertThat(data.maxCurrentDouble).isEqualTo(1.0)
        assertThat(data.maxPowerDouble).isEqualTo(2.0)
    }

    @Test fun `minus six percent correction preserves GPS PWM and non speed telemetry in CSV`() {
        every { appConfig.appliedSpeedCorrectionPercent } returns -6.0
        data.speed = 10_000
        data.voltage = 14_000
        data.current = 1_000
        data.setPower(140_000)
        data.output = 6_800
        data.updatePwm()
        val sample = com.cooper.wheellog.data.NativeWheelSample.capture(data, 0, "")
        val gps = com.cooper.wheellog.track.TrackPoint(1_700_000_000_000L, 50.0, 10.0, null, 5f, 20f)
        val cells = com.cooper.wheellog.data.NativeCsv.HEADER.split(',').zip(
            com.cooper.wheellog.data.NativeCsv.row(gps.time, gps, sample, 0.0, true).split(',')).toMap()
        assertThat(cells.getValue("speed").toDouble()).isEqualTo(94.0)
        assertThat(cells.getValue("wheel_speed_raw").toDouble()).isEqualTo(100.0)
        assertThat(cells.getValue("gps_speed").toDouble()).isEqualTo(72.0)
        assertThat(cells.getValue("pwm").toDouble()).isEqualTo(data.calculatedPwm)
        assertThat(cells.getValue("voltage").toDouble()).isEqualTo(140.0)
        assertThat(cells.getValue("current").toDouble()).isEqualTo(10.0)
        assertThat(cells.getValue("power").toDouble()).isEqualTo(1400.0)
    }
}
