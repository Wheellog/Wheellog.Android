package com.cooper.wheellog.utils

import android.app.Activity
import android.content.Context
import androidx.core.math.MathUtils
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.Utils.Companion.hexToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

class VeteranAdapterTest {

    private var adapter: VeteranAdapter = VeteranAdapter()
    private var header = byteArrayOf(0xDC.toByte(), 0x5A.toByte(), 0x5C.toByte(), 0x20.toByte())
    private lateinit var data: WheelData

    @Before
    fun setUp() {
        data = spyk(WheelData())
        every { data.bluetoothLeService.applicationContext } returns mockkClass(Context::class, relaxed = true)
        data.wheelType = Constants.WHEEL_TYPE.VETERAN
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
        every { WheelLog.AppConfig.gotwayNegative } returns 1
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
        val voltage = 9500.toShort()
        val speed = (-1111).toShort()
        val temperature = 3599.toShort()
        val distance = 3231.toShort()
        val phaseCurrent = (-8322).toShort()
        val byteArray = header +
                MathsUtil.getBytes(voltage) +
                MathsUtil.getBytes(speed) +
                MathsUtil.getBytes(distance) +
                byteArrayOf(0, 0) +
                MathsUtil.getBytes(distance) +
                byteArrayOf(0, 0) +
                MathsUtil.getBytes(phaseCurrent) +
                MathsUtil.getBytes(temperature) +
                byteArrayOf(0, 0) +
                byteArrayOf(0, 0) +
                byteArrayOf(0, 0) +
                byteArrayOf(0, 0) +
                byteArrayOf(43, 45) +
                byteArrayOf(0, 0) +
                byteArrayOf(0, 0) +
                byteArrayOf(0, 0)
        // Act.
        val result = adapter.decode(byteArray)

        // Assert.
        assertThat(result).isTrue()
        assertThat(data.speed).isEqualTo(speed)
        assertThat(data.temperature).isEqualTo(temperature/100)
        assertThat(data.voltageDouble).isEqualTo(voltage/100.0)
        assertThat(data.phaseCurrentDouble).isEqualTo(phaseCurrent/10.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(distance/1000.0)
        assertThat(data.totalDistance).isEqualTo(distance)
        assertThat(data.batteryLevel).isEqualTo(80)
        assertThat(data.version).isEqualTo("43.45 (11053)")
    }

    @Test
    fun `decode veteran old board data`() {
        // Arrange.
        val byteArray1 = "DC5A5C2025D600003BF500003BF50000FFDE1399".hexToByteArray()
        val byteArray2 = "0DEF0000024602460000000000000000".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(50)
        assertThat(data.voltageDouble).isEqualTo(96.86)
        assertThat(data.phaseCurrentDouble).isEqualTo(-3.4)
        assertThat(data.wheelDistanceDouble).isEqualTo(15.349)
        assertThat(data.totalDistance).isEqualTo(15349)
        assertThat(data.batteryLevel).isEqualTo(90)
        assertThat(data.version).isEqualTo("0.0 (0)")
    }

    @Test
    fun `decode veteran new board data`() {
        // Arrange.
        val byteArray1 = "DC5A5C20238A0112121A00004D450005064611F2".hexToByteArray()
        val byteArray2 = "0E1000000AF00AF0041B000300000000".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(abs(data.speed)).isEqualTo(274)
        assertThat(data.temperature).isEqualTo(45)
        assertThat(data.voltageDouble).isEqualTo(90.98)
        assertThat(data.phaseCurrentDouble).isEqualTo(160.6)
        assertThat(data.wheelDistanceDouble).isEqualTo(4.634)
        assertThat(data.totalDistance).isEqualTo(347461)
        assertThat(data.batteryLevel).isEqualTo(60)
        assertThat(data.version).isEqualTo("4.27 (1051)")
    }

    @Test
    fun `update pedals mode`() {
        // Arrange.
        every { data.bluetoothLeService.writeBluetoothGattCharacteristic(any()) } returns true
        mockkConstructor(android.os.Handler::class)
        every { anyConstructed<android.os.Handler>().postDelayed(any(), any()) } returns true

        // Act.
        adapter.updatePedalsMode(0)
        adapter.updatePedalsMode(1)
        adapter.updatePedalsMode(2)

        // Assert.
        verify { anyConstructed<android.os.Handler>().postDelayed(any(), any()) }
        verify { data.bluetoothLeService.writeBluetoothGattCharacteristic("h".toByteArray()) }
        verify { data.bluetoothLeService.writeBluetoothGattCharacteristic("f".toByteArray()) }
        verify { data.bluetoothLeService.writeBluetoothGattCharacteristic("s".toByteArray()) }
    }
}
