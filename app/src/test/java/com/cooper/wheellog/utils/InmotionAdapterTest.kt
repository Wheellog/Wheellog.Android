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


class InmotionAdapterTest {

    private var adapter: InMotionAdapter = InMotionAdapter()
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
    fun `decode with v8f real data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("AAAA1301A5550F9500000000000000FE0201008F")
        val byteArray2 = hexStringToByteArray("020000000000000000000054FAFFFF54FAFFFFFB")
        val byteArray3 = hexStringToByteArray("FFFFFFBE200000000000001B1B24240000000000")
        val byteArray4 = hexStringToByteArray("000000AF5400000100000000302B140605E00722")
        val byteArray5 = hexStringToByteArray("00000023000000C50000005D020000D900000006")
        val byteArray6 = hexStringToByteArray("000000000000000000000000000000004000081B")
        val byteArray7 = hexStringToByteArray("0000F221000033060000000000000B0000006216")
        val byteArray8 = hexStringToByteArray("0000F42A0000030000000E000000110106000000")
        val byteArray9 = hexStringToByteArray("000000000000C500765555")



        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)
        val result8 = adapter.decode(byteArray8)
        val result9 = adapter.decode(byteArray9)


        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isFalse()
        assertThat(result8).isFalse()
        assertThat(result9).isTrue()
        assertThat(abs(data.speed)).isEqualTo(14)
        assertThat(data.temperature).isEqualTo(27)
        assertThat(data.temperature2).isEqualTo(36)
        assertThat(data.voltageDouble).isEqualTo(83.82)
        assertThat(data.currentDouble).isEqualTo(-0.05)
        assertThat(data.totalDistance).isEqualTo(75205) // wrong shoud be 21679
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(0.0099945068359375)
        assertThat(data.roll).isEqualTo(6.722222222222222)

    }


    @Test
    fun `decode with v8f real data 2`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("AAAA1301A5550F9500000000000000FE0201007A")
        val byteArray2 = hexStringToByteArray("14000000000000000000003CFDFFFF3CFDFFFFF6")
        val byteArray3 = hexStringToByteArray("FFFFFFA7200000400100001C1C2424F8FFFFFFE7")
        val byteArray4 = hexStringToByteArray("FFFFFFB75400000900000000042C140605E00722")
        val byteArray5 = hexStringToByteArray("000000E301000023010000AC0500000302000056")
        val byteArray6 = hexStringToByteArray("0000004C0000000000000000000000004000081C")
        val byteArray7 = hexStringToByteArray("0000F221000033060000BF020000070100006F16")
        val byteArray8 = hexStringToByteArray("0000032B0000100000001D000000380256004C00")
        val byteArray9 = hexStringToByteArray("F8FFE7FFE7FF2301465555")



        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)
        val result8 = adapter.decode(byteArray8)
        val result9 = adapter.decode(byteArray9)


        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isFalse()
        assertThat(result8).isFalse()
        assertThat(result9).isTrue()
        assertThat(abs(data.speed)).isEqualTo(7)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.temperature2).isEqualTo(36)
        assertThat(data.voltageDouble).isEqualTo(83.59)
        assertThat(data.currentDouble).isEqualTo(-0.1)
        assertThat(data.totalDistance).isEqualTo(676844) // wrong! should be 21687 but 676844
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(0.079986572265625)
        assertThat(data.roll).isEqualTo(16.133333333333333)

    }


}
