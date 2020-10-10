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


class NinebotZAdapterTest {

    private var adapter: NinebotZAdapter = NinebotZAdapter()
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
    fun `decode z10 sn data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("5AA50E143E04104E334F54433230323054303030")
        val byteArray2 = hexStringToByteArray("314BFC")

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()

        assertThat(data.serial).isEqualTo("N3OTC2020T0001")

    }

    @Test
    fun `decode z10 version data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("5AA502143E041A771006FF")

        // Act.
        val result1 = adapter.decode(byteArray1)

        // Assert.
        assertThat(result1).isTrue()
        assertThat(data.version).isEqualTo("0.7.7")
    }

    @Test
    fun `decode z10 bms1 sn data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("5AA522113E041034395945513138483151303432")
        val byteArray2 = hexStringToByteArray("33160180258025EC13AF00810100000000000001")
        val byteArray3 = hexStringToByteArray("256BF8")
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isTrue()
        assertThat(data.bms1SerialNumber).isEqualTo("49YEQ18H1Q0423")
        assertThat(data.bms1VersionNumber).isEqualTo("1.1.6")
        assertThat(data.bms1FactoryCap).isEqualTo(9600)
        assertThat(data.bms1ActualCap).isEqualTo(9600)
        assertThat(data.bms1FullCycles).isEqualTo(175)
        assertThat(data.bms1ChargeCount).isEqualTo(385)
        assertThat(data.bms1MfgDateStr).isEqualTo("01.08.2018")
    }

    @Test
    fun `decode z10 bms1 status data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("5AA518113E04300102BF25640011009A162F2E00")
        val byteArray2 = hexStringToByteArray("2000000000C025BD256200B2FA")

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(data.bms1Status).isEqualTo(513)
        assertThat(data.bms1RemCap).isEqualTo(9663)
        assertThat(data.bms1RemPerc).isEqualTo(100)
        assertThat(data.bms1Current).isEqualTo(0.17)
        assertThat(data.bms1Voltage).isEqualTo(57.86)
        assertThat(data.bms1Temp1).isEqualTo(27)
        assertThat(data.bms1Temp2).isEqualTo(26)
        assertThat(data.bms1BalanceMap).isEqualTo(8192)
        assertThat(data.bms1Health).isEqualTo(98)
    }

    @Test
    fun `decode z10 bms1 cells data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("5AA520113E0440341006103110381005103010FA")
        val byteArray2 = hexStringToByteArray("0F06103D104010011034103D1051100000000055")
        val byteArray3 = hexStringToByteArray("FB")
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isTrue()
        assertThat(data.bms1Cell1).isEqualTo(4.148)
        assertThat(data.bms1Cell2).isEqualTo(4.102)
        assertThat(data.bms1Cell3).isEqualTo(4.145)
        assertThat(data.bms1Cell4).isEqualTo(4.152)
        assertThat(data.bms1Cell5).isEqualTo(4.101)
        assertThat(data.bms1Cell6).isEqualTo(4.144)
        assertThat(data.bms1Cell7).isEqualTo(4.090)
        assertThat(data.bms1Cell8).isEqualTo(4.102)
        assertThat(data.bms1Cell9).isEqualTo(4.157)
        assertThat(data.bms1Cell10).isEqualTo(4.160)
        assertThat(data.bms1Cell11).isEqualTo(4.097)
        assertThat(data.bms1Cell12).isEqualTo(4.148)
        assertThat(data.bms1Cell13).isEqualTo(4.157)
        assertThat(data.bms1Cell14).isEqualTo(4.177)
        assertThat(data.bms1Cell15).isEqualTo(0.0)
        assertThat(data.bms1Cell16).isEqualTo(0.0)

    }

    @Test
    fun `decode z10 bms2 sn data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("5AA522123E041034395945513138483151303432")
        val byteArray2 = hexStringToByteArray("33160180258025EC13B000810100000000000001")
        val byteArray3 = hexStringToByteArray("2569F8")
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isTrue()
        assertThat(data.getbms2SerialNumber()).isEqualTo("49YEQ18H1Q0423")
        assertThat(data.getbms2VersionNumber()).isEqualTo("1.1.6")
        assertThat(data.getbms2FactoryCap()).isEqualTo(9600)
        assertThat(data.getbms2ActualCap()).isEqualTo(9600)
        assertThat(data.getbms2FullCycles()).isEqualTo(176)
        assertThat(data.getbms2ChargeCount()).isEqualTo(385)
        assertThat(data.getbms2MfgDateStr()).isEqualTo("01.08.2018")
    }

    @Test
    fun `decode z10 bms2 status data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("5AA518123E043001029C2564000000A0162F2E00")
        val byteArray2 = hexStringToByteArray("00000000009C259C25620044FB")

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(data.getbms2Status()).isEqualTo(513)
        assertThat(data.getbms2RemCap()).isEqualTo(9628)
        assertThat(data.getbms2RemPerc()).isEqualTo(100)
        assertThat(data.getbms2Current()).isEqualTo(0.0)
        assertThat(data.getbms2Voltage()).isEqualTo(57.92)
        assertThat(data.getbms2Temp1()).isEqualTo(27)
        assertThat(data.getbms2Temp2()).isEqualTo(26)
        assertThat(data.getbms2BalanceMap()).isEqualTo(0)
        assertThat(data.getbms2Health()).isEqualTo(98)
    }

    @Test
    fun `decode z10 bms2 cells data`() {
        // Arrange.
        val byteArray1 = hexStringToByteArray("5AA520123E04401B102C10221024102310211031")
        val byteArray2 = hexStringToByteArray("1030102F10271026103510321035100000000021")
        val byteArray3 = hexStringToByteArray("FC")
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isTrue()
        assertThat(data.getbms2Cell1()).isEqualTo(4.123)
        assertThat(data.getbms2Cell2()).isEqualTo(4.140)
        assertThat(data.getbms2Cell3()).isEqualTo(4.130)
        assertThat(data.getbms2Cell4()).isEqualTo(4.132)
        assertThat(data.getbms2Cell5()).isEqualTo(4.131)
        assertThat(data.getbms2Cell6()).isEqualTo(4.129)
        assertThat(data.getbms2Cell7()).isEqualTo(4.145)
        assertThat(data.getbms2Cell8()).isEqualTo(4.144)
        assertThat(data.getbms2Cell9()).isEqualTo(4.143)
        assertThat(data.getbms2Cell10()).isEqualTo(4.135)
        assertThat(data.getbms2Cell11()).isEqualTo(4.134)
        assertThat(data.getbms2Cell12()).isEqualTo(4.149)
        assertThat(data.getbms2Cell13()).isEqualTo(4.146)
        assertThat(data.getbms2Cell14()).isEqualTo(4.149)
        assertThat(data.getbms2Cell15()).isEqualTo(0.0)
        assertThat(data.getbms2Cell16()).isEqualTo(0.0)

    }

}
