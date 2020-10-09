package com.cooper.wheellog.utils

import com.cooper.wheellog.WheelData
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
//import sun.security.krb5.internal.KDCOptions.with
import kotlin.math.abs
import kotlin.math.round


class GotwayAdapterTest {

    private var adapter: GotwayAdapter = GotwayAdapter()
    private var header = byteArrayOf(0x55, 0xAA.toByte())
    private lateinit var data: WheelData

    fun hexStringToByteArray(s: String): ByteArray? {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

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
    fun `decode with 2020 board data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("55AA19C1000000000000008CF0000001FFF80018")
        val byteArray2 = hexStringToByteArray("5A5A5A5A55AA000060D248001C20006400010007")
        val byteArray3 = hexStringToByteArray("000804185A5A5A5A")

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isTrue()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(22)
        assertThat(data.temperature2).isEqualTo(22)
        assertThat(data.voltageDouble).isEqualTo(65.93)
        assertThat(data.phaseCurrentDouble).isEqualTo(-1.16)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.totalDistance).isEqualTo(24786)
        assertThat(data.batteryLevel).isEqualTo(100)
    }


    @Test
    fun `decode strange board data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("55AA19A0000C00000000032AF8150001FFF80018")
        val byteArray2 = hexStringToByteArray("5A5A5A5A")
        val byteArray3 = hexStringToByteArray("55AA000026E324001C19001E0001000700080418")
        val byteArray4 = hexStringToByteArray("5A5A5A5A")

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)

        // Assert.
        assertThat(result1).isTrue()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(abs(data.speed)).isEqualTo(4)
        assertThat(data.temperature).isEqualTo(29)
        assertThat(data.temperature2).isEqualTo(29)
        assertThat(data.voltageDouble).isEqualTo(65.6)
        assertThat(data.phaseCurrentDouble).isEqualTo(8.1)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.totalDistance).isEqualTo(0) // wrong
        assertThat(data.batteryLevel).isEqualTo(97)
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
