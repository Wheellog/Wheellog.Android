package com.cooper.wheellog.utils

import com.cooper.wheellog.WheelData
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.*
import kotlin.math.abs
import kotlin.math.round

class GotwayAdapterTest {

    private var adapter: GotwayAdapter = GotwayAdapter()
    private var header = byteArrayOf(0x55, 0xAA.toByte())
    private lateinit var data: WheelData

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
    fun `decode with normal data`() {
        // Arrange.
        val voltage = 6000.toShort()
        val speed = 111.toShort()
        val temperature = 99.toShort()
        val distance = 321.toShort()
        val byteArray = header +
                MathsUtil.getBytes(voltage) +
                MathsUtil.getBytes(speed) +
                byteArrayOf(6, 7) +
                MathsUtil.getBytes(distance) +
                byteArrayOf(10, 11) +
                MathsUtil.getBytes(temperature) +
                byteArrayOf(14, 15, 16, 17, 0, 0)

        // Act.
        val result = adapter.decode(byteArray)

        // Assert.
        assertThat(result).isTrue()
        val speedInKm = round(speed * 3.6 / 10).toInt()
        assertThat(abs(data.speed)).isEqualTo(speedInKm)
        assertThat(data.temperature).isEqualTo(35)
        assertThat(data.temperature2).isEqualTo(35)
        assertThat(data.voltageDouble).isEqualTo(voltage / 100.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(distance / 1000.0)
        assertThat(data.batteryLevel).isEqualTo(54)
    }

    @Test
    @Ignore // TODO
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
