package com.cooper.wheellog.utils

import com.cooper.wheellog.WheelData
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.math.abs
import kotlin.math.round

class KingsongAdapterTest {

    private var adapter: KingsongAdapter = KingsongAdapter()
    private var header = byteArrayOf(0x55, 0xAA.toByte())
    private var data = spyk(WheelData())

    @Before
    fun setUp() {
        data = spyk(WheelData())
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
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
    fun `decode Live data`() {
        // Arrange.
        val voltage = 6000.toShort()
        val speed = 111.toShort()
        val temperature = 12345.toShort()
        val distance = 1234567890
        val type = 169.toByte() // Live data
        val byteArray = header +
                MathsUtil.getBytes(voltage) +
                MathsUtil.getBytes(speed) +
                MathsUtil.getBytes(distance) +
                byteArrayOf(10, 11) +
                MathsUtil.getBytes(temperature) +
                byteArrayOf(14, 15, 16, type, 0, 0)

        // Act.
        val result = adapter.decode(MathsUtil.reverseEvery2(byteArray))

        // Assert.
        assertThat(result).isTrue()
        assertThat(data.voltageDouble).isEqualTo(voltage / 100.0)
        val speedInKm = round(speed / 10.0).toInt()
        assertThat(abs(data.speed)).isEqualTo(speedInKm)
        assertThat(data.temperature).isEqualTo(temperature / 100)
        assertThat(data.temperature2).isEqualTo(0)
        assertThat(data.totalDistanceDouble).isEqualTo(distance / 1000.0)
        assertThat(data.batteryLevel).isEqualTo(62)
    }

    @Test
    fun `decode Distance|Time|Fan Data`() {
        // Arrange.
        val topSpeed = 30000.toShort()
        val distance = 1234567890
        val type = 185.toByte() // Distance|Time|Fan Data
        val fanStatus = 321.toByte()
        val byteArray = header +
                MathsUtil.getBytes(distance) +
                byteArrayOf(6, 7) +
                MathsUtil.getBytes(topSpeed) +
                byteArrayOf(10, 11, 12, fanStatus, 14, 15, 16, type, 0, 0)

        // Act.
        val result = adapter.decode(MathsUtil.reverseEvery2(byteArray))

        // Assert.
        assertThat(result).isTrue()
        assertThat(data.wheelDistanceDouble).isEqualTo(distance / 1000.0)
        assertThat(data.topSpeedDouble).isEqualTo(topSpeed / 100.0)
        assertThat(data.fanStatus).isEqualTo(fanStatus)
    }

    @Test
    fun `decode Name and Model data`() {
        // Arrange.
        val type = 187.toByte() // Name and Type data
        val name = "Super-Wheel12"
        val model = name.split("-")[0]
        val byteArray = header +
                MathsUtil.reverseEvery2(byteArrayOf(2) + name.toByteArray(Charsets.UTF_8)) +
                byteArrayOf(16) +
                byteArrayOf(type, 0, 0)

        // Act.
        val result = adapter.decode(MathsUtil.reverseEvery2(byteArray))

        // Assert.
        assertThat(result).isTrue()
        assertThat(data.name).isEqualTo(name)
        assertThat(data.model).isEqualTo(model)
    }

    @Test
    fun `decode Serial number`() {
        // Arrange.
        justRun { data.updateKSAlarmAndSpeed() }
        val type = 179.toByte() // Name and Type data
        val serial = "King1234567890123"
        var serialBytes = serial.toByteArray(Charsets.UTF_8)
        val byteArray = MathsUtil.reverseEvery2(header) +
                serialBytes.copyOfRange(0, 14) +
                type +
                serialBytes.copyOfRange(14, serialBytes.size)

        // Act.
        val result = adapter.decode(byteArray)

        // Assert.
        assertThat(result).isTrue()
        assertThat(data.serial.trimEnd('\u0000')).isEqualTo(serial)
    }

    @Test
    fun `decode max speed and alerts`() {
        // Arrange.
        val type = 181.toByte() // Name and Type data
        val byteArray = MathsUtil.reverseEvery2(header) +
                byteArrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, type, 17, 18, 19)

        // Act.
        val result = adapter.decode(byteArray)

        // Assert.
        assertThat(result).isTrue()
    }

    @Test
    @Ignore // TODO
    fun `update pedals mode`() {
        // Arrange.
        every { data.bluetoothLeService.writeBluetoothGattCharacteristic(any()) } returns true

        // Act.
        adapter.updatePedalsMode(0)
        adapter.updatePedalsMode(1)
        adapter.updatePedalsMode(2)

        // Assert.
        verify { data.bluetoothLeService.writeBluetoothGattCharacteristic(any()) }
    }
}
