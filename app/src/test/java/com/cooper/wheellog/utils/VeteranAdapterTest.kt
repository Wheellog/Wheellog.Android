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
    fun `decode veteran 58fw data`() {
        // Arrange.
        val byteArray1 = "dc5a5c2025cd0000071f0000c77800280000110b".hexToByteArray()
        val byteArray2 = "0e1000010af00af00422000300140000".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(43)
        assertThat(data.voltageDouble).isEqualTo(96.77)
        assertThat(data.phaseCurrentDouble).isEqualTo(0)
        assertThat(data.wheelDistanceDouble).isEqualTo(1.823)
        assertThat(data.totalDistance).isEqualTo(2672504)
        assertThat(data.batteryLevel).isEqualTo(89)
        assertThat(data.version).isEqualTo("4.34 (1058)")
    }


    @Test
    fun `decode veteran with fail data`() {
        // Arrange.
        val byteArray1 = "dc5a5c2024dc02130a0a00001179005200450f47".hexToByteArray()
        val byteArray2 = "0e1000000af00af0041d000300000000dc5a5c20".hexToByteArray()
        val byteArray3 = "2467020f0a0d0000117c005201070a1900001188".hexToByteArray()
        val byteArray4 = "0052ffe10f350e1000000af00af0041d00030000".hexToByteArray()
        val byteArray5 = "0000dc5a5c20251501fb0a1c0000118b0052ffe6".hexToByteArray()
        val byteArray6 = "0f350e1000000af00af0041d000300000000dc5a".hexToByteArray()
        val byteArray7 = "5c2024ef01f70a1f0000118e005200470f350e10".hexToByteArray()
        val byteArray8 = "00000af00af0041d000300000000dc5a5c2024fc".hexToByteArray()
        val byteArray9 = "01ec0a22000011910052001e0f3e0e1000000af0".hexToByteArray()
        val byteArray10 = "0af0041d000300000000dc5a5c2024b601e80a24".hexToByteArray()
        val byteArray11 = "000011930052dc5a5c2024e9000052023a0f3e0e".hexToByteArray()
        val byteArray12 = "1000000af00af0041d000300000000dc5a5c2023".hexToByteArray()
        val byteArray13 = "c302100a5100001100dc5a5c20248702320a5e00".hexToByteArray()
        val byteArray14 = "000af00af0041d000300000000dc5a5c20250220".hexToByteArray()
        val byteArray15 = "0a9800001207005201cf0f350e1000000af00af0".hexToByteArray()
        val byteArray16 = "041d000300000000dc5a5c2023e602250a9b0000".hexToByteArray()
        val byteArray17 = "120a005202610f2b0e1000000af00af0041d0003".hexToByteArray()
        val byteArray18 = "00000000dc5a5c20241202260a9e0000120d0052".hexToByteArray()
        val byteArray19 = "02010f350e1000000af00af0041d000300000000".hexToByteArray()
        val byteArray20 = "dc5a5c202405022e0aa100001210005201f70f2b".hexToByteArray()


        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(abs(data.speed)).isEqualTo(531)

        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()

        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        assertThat(result5).isFalse()
        assertThat(result6).isTrue()
        assertThat(abs(data.speed)).isEqualTo(507)

        val result7 = adapter.decode(byteArray7)
        val result8 = adapter.decode(byteArray8)
        assertThat(result7).isFalse()
        assertThat(result8).isTrue()
        assertThat(abs(data.speed)).isEqualTo(503)

        val result9 = adapter.decode(byteArray9)
        val result10 = adapter.decode(byteArray10)
        assertThat(result9).isFalse()
        assertThat(result10).isTrue()
        assertThat(abs(data.speed)).isEqualTo(492)

        val result11 = adapter.decode(byteArray11)
        val result12 = adapter.decode(byteArray12)
        assertThat(result11).isFalse()
        assertThat(result12).isFalse()

        val result13 = adapter.decode(byteArray13)
        val result14 = adapter.decode(byteArray14)
        assertThat(result13).isFalse()
        assertThat(result14).isFalse()

        val result15 = adapter.decode(byteArray15)
        val result16 = adapter.decode(byteArray16)
        assertThat(result15).isFalse()
        assertThat(result16).isFalse()

        val result17 = adapter.decode(byteArray17)
        val result18 = adapter.decode(byteArray18)
        assertThat(result17).isFalse()
        assertThat(result18).isTrue()
        assertThat(abs(data.speed)).isEqualTo(549)

        val result19 = adapter.decode(byteArray19)
        val result20 = adapter.decode(byteArray20)
        assertThat(result19).isTrue()
        assertThat(result20).isFalse()
        assertThat(abs(data.speed)).isEqualTo(550)

    }


    @Test
    fun `update pedals mode`() {
        // Arrange.
        mockkConstructor(android.os.Handler::class)

        // Act.
        adapter.updatePedalsMode(0)
        adapter.updatePedalsMode(1)
        adapter.updatePedalsMode(2)

        // Assert.
        verify { data.bluetoothCmd("SETh".toByteArray()) }
        verify { data.bluetoothCmd("SETm".toByteArray()) }
        verify { data.bluetoothCmd("SETs".toByteArray()) }
    }

    @Test
    fun `reset trip`() {
        // Arrange.
        mockkConstructor(android.os.Handler::class)

        // Act.
        adapter.resetTrip()

        // Assert.
        verify { data.bluetoothCmd("CLEARMETER".toByteArray()) }
    }
}
