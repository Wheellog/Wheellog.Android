package com.cooper.wheellog.utils

import android.content.Context
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.Utils.Companion.hexToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.math.abs

class VeteranAdapterTest: KoinTest {

    private lateinit var adapter: VeteranAdapter
    private var header = byteArrayOf(0xDC.toByte(), 0x5A.toByte(), 0x5C.toByte(), 0x20.toByte())
    private lateinit var data: WheelData
    private val appConfig = mockkClass(AppConfig::class, relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { appConfig }
                    single { mockContext }
                }
            )
        }
        adapter = VeteranAdapter()
        every { appConfig.gotwayNegative } returns "1"
        data = spyk(WheelData())
        data.wheelType = Constants.WHEEL_TYPE.VETERAN
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
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
        val version = 3210.toShort()
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
                MathsUtil.getBytes(version) +
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
        assertThat(data.version).isEqualTo("003.2.10")
    }

    @Test
    fun `decode with normal data - Patton`() {
        // Arrange.
        val voltage = 11500.toShort()
        val speed = (-1111).toShort()
        val temperature = 3599.toShort()
        val distance = 3231.toShort()
        val phaseCurrent = (-8322).toShort()
        val version = 4210.toShort()
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
                MathsUtil.getBytes(version) +
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
        assertThat(data.batteryLevel).isEqualTo(65)
        assertThat(data.version).isEqualTo("004.2.10")
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
        assertThat(data.version).isEqualTo("000.0.00")
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
        assertThat(data.version).isEqualTo("001.0.51")
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
        assertThat(data.angle).isEqualTo(0.2)
        assertThat(data.version).isEqualTo("001.0.58")
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
    fun `decode veteran abrams`() {
        // Arrange.
        val byteArray1 = "dc5a5c20266d00004aaf00004aaf000000000d9e".hexToByteArray()
        val byteArray2 = "0b8800000af00af007d2000300050004".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(34)
        assertThat(data.voltageDouble).isEqualTo(98.37)
        assertThat(data.phaseCurrentDouble).isEqualTo(0)
        assertThat(data.wheelDistanceDouble).isEqualTo(19.119)
        assertThat(data.totalDistance).isEqualTo(19119)
        assertThat(data.batteryLevel).isEqualTo(98)
        assertThat(data.angle).isEqualTo(0.05)
        assertThat(data.version).isEqualTo("002.0.02")
    }

    @Test
    fun `decode veteran abrams 2`() {
        // Arrange.
        val byteArray1 = "dc5a5c20268000004aaf00004aaf000000040ac0".hexToByteArray()
        val byteArray2 = "0dff00000af00af007d20003fff80004".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(27)
        assertThat(data.voltageDouble).isEqualTo(98.56)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.4)
        assertThat(data.wheelDistanceDouble).isEqualTo(19.119)
        assertThat(data.totalDistance).isEqualTo(19119)
        assertThat(data.batteryLevel).isEqualTo(99)
        assertThat(data.angle).isEqualTo(-0.08)
        assertThat(data.version).isEqualTo("002.0.02")
    }

    @Test
    fun `decode veteran abrams 3`() {
        // Arrange.
        val byteArray1 = "dc5a5c202719022208af000008af00000005102c".hexToByteArray()
        val byteArray2 = "0e1000000af00af007d3000100091851".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(abs(data.speed)).isEqualTo(546)
        assertThat(data.temperature).isEqualTo(41)
        assertThat(data.voltageDouble).isEqualTo(100.09)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.5)
        assertThat(data.wheelDistanceDouble).isEqualTo(2.223)
        assertThat(data.totalDistance).isEqualTo(2223)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(0.09)
        assertThat(data.version).isEqualTo("002.0.03")
    }

    @Test
    fun `decode veteran sherman s 1`() {
        // Arrange.
        val byteArray1 = "DC5A5C22266200000084000017A2000000000C38".hexToByteArray()
        val byteArray2 = "0B03000000C600E40BBD0003188B0000006F".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(31)
        assertThat(data.voltageDouble).isEqualTo(98.26)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.132)
        assertThat(data.totalDistance).isEqualTo(6050)
        assertThat(data.batteryLevel).isEqualTo(97)
        assertThat(data.angle).isEqualTo(62.83)
        assertThat(data.version).isEqualTo("003.0.05")
    }

    @Test
    fun `decode veteran patton`() {
        // Arrange.
        val byteArray1 = "dc5a5c26302b00001fdc00002038000000000d15".hexToByteArray()
        val byteArray2 = "0a79000000fa01900fa700031b690000006fffff".hexToByteArray()
        val byteArray3 = "5678".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(33)
        assertThat(data.voltageDouble).isEqualTo(123.31)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(8.156)
        assertThat(data.totalDistance).isEqualTo(8248)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(70.17)
        assertThat(data.version).isEqualTo("004.0.07")
    }

    @Test
    fun `decode veteran patton crc`() {
        // Arrange.
        val byteArray1 = "dc5a5c452abe00003edc00008562003500000b5c".hexToByteArray()
        val byteArray2 = "0dfe000002bc07d00fac000219fb0000006f0000".hexToByteArray()
        val byteArray3 = "80808080808004000014ffffffffff32ee029109".hexToByteArray()
        val byteArray4 = "df0fd303cb000000006f9a79c2".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(29)
        assertThat(data.voltageDouble).isEqualTo(109.42)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(16.092)
        assertThat(data.totalDistance).isEqualTo(3507554)
        assertThat(data.batteryLevel).isEqualTo(42)
        assertThat(data.angle).isEqualTo(66.51)
        assertThat(data.version).isEqualTo("004.0.12")
    }

    @Test
    fun `decode veteran lynx crc`() {
        // Arrange.
        val byteArray1 = "dc5a5c53391b000006d000000770000000260bcc".hexToByteArray()
        val byteArray2 = "0e08000000fa00c8138c00b4000b014c80c80000".hexToByteArray()
        val byteArray3 = "808080808080010008808080800fee0fee0fee0f".hexToByteArray()
        val byteArray4 = "ee0fef0fe80fef0fef0ff00ff00ff00fea0fef0f".hexToByteArray()
        val byteArray5 = "ef0fefdab22518".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(30)
        assertThat(data.voltageDouble).isEqualTo(146.19)
        assertThat(data.phaseCurrentDouble).isEqualTo(3.8)
        assertThat(data.wheelDistanceDouble).isEqualTo(1.744)
        assertThat(data.totalDistance).isEqualTo(1904)
        assertThat(data.batteryLevel).isEqualTo(94)
        assertThat(data.angle).isEqualTo(0.11)
        assertThat(data.version).isEqualTo("005.0.04")
    }

    @Test
    fun `decode veteran sherman l`() {
        // Arrange.
        val byteArray1 = "dc5a5c53397afffe0aa400000df10000000a0b3d".hexToByteArray()
        val byteArray2 = "0e0e0000037a035217730064000e00b480c80000".hexToByteArray()
        val byteArray3 = "808080808080058080808080800ff30ff50ff50f".hexToByteArray()
        val byteArray4 = "f50ff50fef0ff20ff30ff30ff30ff30fed0ff30f".hexToByteArray()
        val byteArray5 = "f40ff5378c5145".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isTrue()
        assertThat(abs(data.speed)).isEqualTo(2)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.voltageDouble).isEqualTo(147.14)
        assertThat(data.phaseCurrentDouble).isEqualTo(1.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(2.724)
        assertThat(data.totalDistance).isEqualTo(3569)
        assertThat(data.batteryLevel).isEqualTo(97)
        assertThat(data.angle).isEqualTo(0.14)
        assertThat(data.version).isEqualTo("006.0.03")
    }

    @Test
    fun `decode veteran patton s`() {
        // Arrange.
        val byteArray1 = "dc5a5c532c4c0000615400006406000000000ce0".hexToByteArray()
        val byteArray2 = "01a0000002bc02ee1b5900821a63000080c80000".hexToByteArray()
        val byteArray3 = "808080808080068080808080800ec20ec20ec20e".hexToByteArray()
        val byteArray4 = "c30ec20ebd0ec20ec30ec20ec30ec30ebd0ec30e".hexToByteArray()
        val byteArray5 = "c30ec08edbd62e".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(32)
        assertThat(data.voltageDouble).isEqualTo(113.40)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(24.916)
        assertThat(data.totalDistance).isEqualTo(25606)
        assertThat(data.batteryLevel).isEqualTo(59)
        assertThat(data.angle).isEqualTo(67.55)
        assertThat(data.version).isEqualTo("007.0.01")
    }

    @Test
    fun `decode veteran patton s 2`() {
        // Arrange.
        val byteArray1 = "dc5a5c5331490000ceba0002c160000300000b40".hexToByteArray()
        val byteArray2 = "03810000025802581b5a00961bfc000080c80000".hexToByteArray()
        val byteArray3 = "8080808080800500000000801e10591058105910".hexToByteArray()
        val byteArray4 = "5a1059105310591058105a105a105b1055105510".hexToByteArray()
        val byteArray5 = "57105105bf349e".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.voltageDouble).isEqualTo(126.17)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(183.994)
        assertThat(data.totalDistance).isEqualTo(246112)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(71.64)
        assertThat(data.version).isEqualTo("007.0.02")
    }


    @Test
    fun `decode veteran patton s 3`() {
        // Arrange.
        val byteArray1 = "dc5a5c47314d0000ceba0002c160000300000b3f".hexToByteArray()
        val byteArray2 = "03820000025802581b5a00961bf7000080c80000".hexToByteArray()
        val byteArray3 = "8080808080800300000000000000000000000000".hexToByteArray()
        val byteArray4 = "0000000000000000000000b2ce8d4a".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.voltageDouble).isEqualTo(126.21)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(183.994)
        assertThat(data.totalDistance).isEqualTo(246112)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(71.59)
        assertThat(data.version).isEqualTo("007.0.02")
    }


    @Test
    fun `decode veteran patton s 4`() {
        // Arrange.
        val byteArray1 = "dc5a5c4931490000ceba0002c160000300000b42".hexToByteArray()
        val byteArray2 = "03810000025802581b5a00961bfc000080c80000".hexToByteArray()
        val byteArray3 = "80808080808004000003ffffffffff3211025c09".hexToByteArray()
        val byteArray4 = "560eb004700000000000010000df90aaa0".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.voltageDouble).isEqualTo(126.17)
        assertThat(data.phaseCurrentDouble).isEqualTo(0.0)
        assertThat(data.wheelDistanceDouble).isEqualTo(183.994)
        assertThat(data.totalDistance).isEqualTo(246112)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(71.64)
        assertThat(data.version).isEqualTo("007.0.02")
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
