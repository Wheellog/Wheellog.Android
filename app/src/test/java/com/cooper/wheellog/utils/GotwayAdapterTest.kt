package com.cooper.wheellog.utils

import com.cooper.wheellog.WheelData
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


class GotwayAdapterTest {

    private var adapter: GotwayAdapter = GotwayAdapter()
    private var header = byteArrayOf(0x55, 0xAA.toByte())
    private lateinit var data: WheelData

    @Before
    fun setUp() {
        data = spyk(WheelData())
        data.wheelType = Constants.WHEEL_TYPE.GOTWAY
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
        val byteArray1 = "55AA19C1000000000000008CF0000001FFF80018".hexToByteArray()
        val byteArray2 = "5A5A5A5A55AA000060D248001C20006400010007".hexToByteArray()
        val byteArray3 = "000804185A5A5A5A".hexToByteArray()

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
    fun `decode veteran old board data`() {
        // Arrange.
        val byteArray1 = "DC5A5C2025D600003BF500003BF50000FFDE1399".hexToByteArray()
        val byteArray2 = "0DEF0000024602460000000000000000".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isTrue()
        assertThat(result2).isFalse()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(50)
        assertThat(data.temperature2).isEqualTo(50)
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
        assertThat(result1).isTrue()
        assertThat(result2).isFalse()
        assertThat(abs(data.speed)).isEqualTo(274)
        assertThat(data.temperature).isEqualTo(45)
        assertThat(data.temperature2).isEqualTo(45)
        assertThat(data.voltageDouble).isEqualTo(90.98)
        assertThat(data.phaseCurrentDouble).isEqualTo(160.6)
        assertThat(data.wheelDistanceDouble).isEqualTo(4.634)
        assertThat(data.totalDistance).isEqualTo(347461)
        assertThat(data.batteryLevel).isEqualTo(60)
        assertThat(data.version).isEqualTo("4.27 (1051)")
    }

    @Test
    fun `getting corrected tiltback voltage on Veteran`() {
        // Arrange.
        data.wheelType = Constants.WHEEL_TYPE.VETERAN

        // Act.
        val veteran50 = adapter.getCorrectedTiltbackVoltage(50.0)
        val veteran72 = adapter.getCorrectedTiltbackVoltage(72.0)
        val veteran75 = adapter.getCorrectedTiltbackVoltage(75.0)
        val veteran79 = adapter.getCorrectedTiltbackVoltage(79.2)
        val veteran90 = adapter.getCorrectedTiltbackVoltage(90.0)
        val veteran100 = adapter.getCorrectedTiltbackVoltage(100.0)

        // Assert.
        assertThat(veteran50).isEqualTo(75.6) // correct?
        assertThat(veteran72).isEqualTo(72)
        assertThat(veteran75).isEqualTo(75)
        assertThat(veteran79).isEqualTo(79.2)
        assertThat(veteran90).isEqualTo(75.6) // correct?
        assertThat(veteran100).isEqualTo(75.6) // correct?
    }

    @Test
    fun `getting corrected tiltback voltage on Begode sacaler 0 - 67V`() {
        // Arrange.
        adapter.gotwayVoltageScaler = 0;

        // Act.
        val g30 = adapter.getCorrectedTiltbackVoltage(30.0)
        val g40 = adapter.getCorrectedTiltbackVoltage(40.0)
        val g48 = adapter.getCorrectedTiltbackVoltage(48.0)
        val g50 = adapter.getCorrectedTiltbackVoltage(50.0)
        val g52 = adapter.getCorrectedTiltbackVoltage(52.0)
        val g60 = adapter.getCorrectedTiltbackVoltage(60.0)
        val g72 = adapter.getCorrectedTiltbackVoltage(72.0)
        val g75 = adapter.getCorrectedTiltbackVoltage(75.0)

        // Assert.
        assertThat(g30).isEqualTo(52.8)
        assertThat(g40).isEqualTo(52.8)
        assertThat(g48).isEqualTo(48)
        assertThat(g50).isEqualTo(50)
        assertThat(g52).isEqualTo(52)
        assertThat(g60).isEqualTo(52.8)
        assertThat(g72).isEqualTo(52.8)
        assertThat(g75).isEqualTo(52.8)
    }

    @Test
    fun `getting corrected tiltback voltage on Begode sacaler 1 - 84V`() {
        // Arrange.
        adapter.gotwayVoltageScaler = 1;

        // Act.
        val g50 = adapter.getCorrectedTiltbackVoltage(50.0)
        val g60 = adapter.getCorrectedTiltbackVoltage(60.0)
        val g65 = adapter.getCorrectedTiltbackVoltage(65.0)
        val g72 = adapter.getCorrectedTiltbackVoltage(72.0)
        val g75 = adapter.getCorrectedTiltbackVoltage(75.0)

        // Assert.
        assertThat(g50).isEqualTo(66)
        assertThat(g60).isEqualTo(60)
        assertThat(g65).isEqualTo(65)
        assertThat(g72).isEqualTo(66)
        assertThat(g75).isEqualTo(66)
    }

    @Test
    fun `getting corrected tiltback voltage on Begode sacaler 2 - 100V`() {
        // Arrange.
        adapter.gotwayVoltageScaler = 2;

        // Act.
        val g60 = adapter.getCorrectedTiltbackVoltage(60.0)
        val g72 = adapter.getCorrectedTiltbackVoltage(72.0)
        val g75 = adapter.getCorrectedTiltbackVoltage(75.0)
        val g80 = adapter.getCorrectedTiltbackVoltage(80.0)

        // Assert.
        assertThat((g60 * 10).roundToInt() / 10.0).isEqualTo(79.2)
        assertThat(g72).isEqualTo(72)
        assertThat(g75).isEqualTo(75)
        assertThat((g80 * 10).roundToInt() / 10.0).isEqualTo(79.2)
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
