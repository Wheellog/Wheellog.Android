package com.cooper.wheellog.utils

import com.cooper.wheellog.BluetoothLeService
import com.cooper.wheellog.WheelData
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.round

class KingsongAdapterTest {

    private var adapter: KingsongAdapter = KingsongAdapter()
    private var header = byteArrayOf(0xAA.toByte(), 0x55)
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
            var result = adapter.decode(byteArray)

            // Assert.
            assertThat(result).isFalse()
        }
    }

    @Test
    @Ignore // TODO
    fun `decode with normal data`() {
        // Arrange.
        var voltage = 6000.toShort()
        var voltageBytes = ByteBuffer.allocate(2).putShort(voltage).array()
        var speed = 111.toShort()
        var speedBytes = ByteBuffer.allocate(2).putShort(speed).array()
        var temperature = 99.toShort()
        var temperatureBytes = ByteBuffer.allocate(2).putShort(temperature).array()
        var distance = 321
        var distanceBytes = ByteBuffer.allocate(4).putInt(distance).array()
        var type = 169.toByte(); // Live data
        var byteArray = header +
                voltageBytes +
                speedBytes +
                distanceBytes +
                byteArrayOf(10, 11) +
                temperatureBytes +
                byteArrayOf(14, 15, type, 17, 0, 0);

        // Act.
        var result = adapter.decode(byteArray)

        // Assert.
        assertThat(result).isTrue()
        assertThat(data.voltageDouble).isEqualTo(voltage / 100.0)
        var speedInKm = round(speed * 3.6 / 10).toInt()
        assertThat(abs(data.speed)).isEqualTo(speedInKm)
        assertThat(data.temperature).isEqualTo(35)
        assertThat(data.temperature2).isEqualTo(35)
        assertThat(data.wheelDistanceDouble).isEqualTo(distance / 1000.0)
        assertThat(data.batteryLevel).isEqualTo(54)
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
