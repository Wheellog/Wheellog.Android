package com.cooper.wheellog.utils

import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.Utils.Companion.hexToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class InmotionAdapterV2Test {

    private var adapter: InmotionAdapterV2 = InmotionAdapterV2()
    private lateinit var data: WheelData

    @Before
    fun setUp() {
        data = spyk(WheelData())
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `decode with v11 full data`() {
        // Arrange.
        val byteArray1 = "AAAA110882010206010201009C".hexToByteArray() // wheel type
        val byteArray2 = "AAAA11178202313438304341313232323037303032420000000000FD".hexToByteArray() // s/n
        val byteArray3 = "AAAA111D820622080004030F000602214000010110000602230D00010107000001F3".hexToByteArray() //versions
        val byteArray4 = "AAAA141AA0207C15C800106464140000000058020000006400001500100010".hexToByteArray() // wtf
        val byteArray5 = "AAAA142B900001142614000000803E498AE00FB209D109CEB000C7DF010000BE720000AB1300008F040000AB0600004C".hexToByteArray() // probably statistics
        val byteArray6 = "AAAA141991E86C000066191C002DB2040064E60000974D050000C7DF01A4".hexToByteArray() // totals
        val byteArray7 = "AAAA143184E61EEB0561094A11AE04A004DF01402958CBB000CE004A010000D4FF7C15641900000000492B00000000000000000000C6".hexToByteArray()
// 18:30:42.272: 278.80 km, 18415.10, 3077.57, 16:23:00, 96:32:23, 50944, 479
// 18:30:42.306: 79.10 V, 15.15 A, 24.01 Km/h, 44.26 Nm, bat 1198 Wh, mot 1184 Wh, 4.79 km, rem 105.60 km, 88%, 0, mos 27 C, mot 0 C, bat -176 C, board 30 C, lamp -176 C, pith 3.3, pithAim 0.0, roll 654.9, spd lim 55.0, cur lim 65.00, bright 0, br light 0, cpu -176 C, imu -176 C
// pc 1, mc 1, mot 1, chrg 0, light 1, decor 1, lifted 0, tail 1, fan 1
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isTrue()
        assertThat(data.serial).isEqualTo("1480CA122207002B")
        assertThat(data.model).isEqualTo("Inmotion V11")
        assertThat(data.version).isEqualTo("rev: 2.1")


        assertThat(data.speedDouble).isEqualTo(24.01)
        assertThat(data.temperature).isEqualTo(27)
        assertThat(data.temperature2).isEqualTo(30)
        assertThat(data.imuTemp).isEqualTo(-176)
        assertThat(data.cpuTemp).isEqualTo(-176)
        assertThat(data.motorPower).isEqualTo(1184.0)
        assertThat(data.currentLimit).isEqualTo(65.00)
        assertThat(data.speedLimit).isEqualTo(55.00)
        assertThat(data.torque).isEqualTo(44.26)
        assertThat(data.voltageDouble).isEqualTo(79.10)
        assertThat(data.currentDouble).isEqualTo(15.15)
        assertThat(data.wheelDistanceDouble).isEqualTo(4.79)
        assertThat(data.totalDistance).isEqualTo(278800)
        assertThat(data.batteryLevel).isEqualTo(88)
        assertThat(data.powerDouble).isEqualTo(1198.0)
        assertThat(data.angle).isEqualTo(3.3)
        assertThat(data.roll).isEqualTo(-0.44)
    }


}
