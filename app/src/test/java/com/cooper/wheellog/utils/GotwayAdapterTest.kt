package com.cooper.wheellog.utils

import android.content.Context
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.Utils.Companion.hexToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.round

class GotwayAdapterTest {

    private var adapter: GotwayAdapter = GotwayAdapter()
    private var header = byteArrayOf(0x55, 0xAA.toByte())
    private lateinit var data: WheelData

    @Before
    fun setUp() {
        data = spyk(WheelData())
        every { data.bluetoothLeService.applicationContext } returns mockkClass(Context::class, relaxed = true)
        data.wheelType = Constants.WHEEL_TYPE.GOTWAY
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
        every { WheelLog.AppConfig.gotwayNegative } returns "1"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `decode with corrupted data 1-30 units`() {
        // Arrange.
        var byteArray = byteArrayOf()
        for (i in 0..29) {
            byteArray += i.toByte()

            // Act.
            val result = adapter.decode(byteArray)

            // Assert.
            assertThat(result).isFalse()
        }
    }

    @Test
    fun `decode with normal data`() {
        // Arrange.
        val voltage = 6000.toShort()
        val speed = (-1111).toShort()
        val temperature = 99.toShort()
        val distance = 3231.toShort()
        val phaseCurrent = (-8322).toShort()
        val byteArray = header +
                MathsUtil.getBytes(voltage) +
                MathsUtil.getBytes(speed) +
                byteArrayOf(0, 0) +
                MathsUtil.getBytes(distance) +
                MathsUtil.getBytes(phaseCurrent) +
                MathsUtil.getBytes(temperature) +
                byteArrayOf(14, 15, 16, 17, 0, 0x18, 0x5A, 0x5A, 0x5A, 0x5A)
        // Act.
        val result = adapter.decode(byteArray)

        // Assert.
        assertThat(result).isTrue()

        val speedInKm = round(speed * 3.6 / 10).toInt()
        assertThat(data.speed).isEqualTo(speedInKm)
        assertThat(data.temperature).isEqualTo(36)
        assertThat(data.phaseCurrentDouble).isEqualTo(phaseCurrent / 100.0)
        assertThat(data.voltageDouble).isEqualTo(voltage / 100.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(distance / 1000.0)
        assertThat(data.batteryLevel).isEqualTo(54)
    }

    @Test
    fun `decode with 2020 board data`() {
        // Arrange.
        val byteArray1 = "55AA19C1000000000000008CF0000001FFF80018".hexToByteArray()
        val byteArray2 = "5A5A5A5A55AA000060D248001C20006400010007".hexToByteArray()
        val byteArray3 = "000804185A5A5A5A".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(result3).isFalse()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(24)
        assertThat(data.voltageDouble).isEqualTo(65.93)
        assertThat(data.phaseCurrentDouble).isEqualTo(1.4)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.totalDistance).isEqualTo(24786)
        assertThat(data.batteryLevel).isEqualTo(100)
    }

    @Test
    fun `decode strange board data`() {
        // Arrange.
        val byteArray1 = "55AA19A0000C00000000032AF8150001FFF80018".hexToByteArray()
        val byteArray2 = "5A5A5A5A".hexToByteArray()
        val byteArray3 = "55AA000026E324001C19001E0001000700080418".hexToByteArray()
        val byteArray4 = "5A5A5A5A".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(abs(data.speed)).isEqualTo(4)
        assertThat(data.temperature).isEqualTo(30)
        assertThat(data.voltageDouble).isEqualTo(65.6)
        assertThat(data.phaseCurrentDouble).isEqualTo(8.1)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.totalDistance).isEqualTo(9955)
        assertThat(data.batteryLevel).isEqualTo(97)
    }

    @Test
    fun `update pedals mode`() {
        // Arrange.
        mockkConstructor(android.os.Handler::class)
        every { anyConstructed<android.os.Handler>().postDelayed(any(), any()) } returns true

        // Act.
        adapter.updatePedalsMode(0)
        adapter.updatePedalsMode(1)
        adapter.updatePedalsMode(2)

        // Assert.
        verify { anyConstructed<android.os.Handler>().postDelayed(any(), any()) }
        verify { data.bluetoothCmd("h".toByteArray()) }
        verify { data.bluetoothCmd("f".toByteArray()) }
        verify { data.bluetoothCmd("s".toByteArray()) }
    }
}
