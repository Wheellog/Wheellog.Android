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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class NinebotZAdapterTest: KoinTest {

    private lateinit var adapter: NinebotZAdapter
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
        adapter = NinebotZAdapter()
        data = spyk(WheelData())
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    @Test
    fun `decode z10 sn data`() {
        // Arrange.
        val byteArray1 = "5AA50E143E04104E334F54433230323054303030".hexToByteArray()
        val byteArray2 = "314BFC".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(data.serial).isEqualTo("N3OTC2020T0001")
    }

    @Test
    fun `decode z10 version data`() {
        // Arrange.
        val byteArray1 = "5AA506143E041A771019180000D1FE".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(data.version).isEqualTo("0.7.7")
        assertThat(data.error).isEqualTo("Err:25 VGM - Voltage < 10V\nErr:24 General voltage > 65V or < 40V")
    }

    @Test
    fun `decode z10 key data`() { // to think about
        // Arrange.
        val byteArray1 = "5aa510163e5b010000000000003cc76a7b1c7d91".hexToByteArray()
        val byteArray2 = "23de0527fb".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
    }

    @Test
    fun `decode z10 life data`() { // to think about
        // Arrange.
        val byteArray1 = "5aa520143e04b000000000489800004e009c0a7a".hexToByteArray()
        val byteArray2 = "059b97280023016d0472011a1892119c0a7a052a".hexToByteArray()
        val byteArray3 = "f8".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isTrue()
        assertThat(data.speedDouble).isEqualTo(27.16)
        assertThat(data.voltageDouble).isEqualTo(61.7)
        assertThat(data.currentDouble).isEqualTo(44.98)
        assertThat(data.temperature).isEqualTo(37)
        assertThat(data.totalDistance).isEqualTo(2660251)
        assertThat(data.powerDouble).isEqualTo(2775.27)
        assertThat(data.batteryLevel).isEqualTo(78)
    }

    @Test
    fun `decode z10 bms1 sn data`() {
        // Arrange.
        val byteArray1 = "5AA522113E041034395945513138483151303432".hexToByteArray()
        val byteArray2 = "33160180258025EC13AF00810100000000000001".hexToByteArray()
        val byteArray3 = "256BF8".hexToByteArray()
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(data.bms1.serialNumber).isEqualTo("49YEQ18H1Q0423")
        assertThat(data.bms1.versionNumber).isEqualTo("1.1.6")
        assertThat(data.bms1.factoryCap).isEqualTo(9600)
        assertThat(data.bms1.actualCap).isEqualTo(9600)
        assertThat(data.bms1.fullCycles).isEqualTo(175)
        assertThat(data.bms1.chargeCount).isEqualTo(385)
        assertThat(data.bms1.mfgDateStr).isEqualTo("01.08.2018")
    }

    @Test
    fun `decode z10 bms1 status data`() {
        // Arrange.
        val byteArray1 = "5AA518113E04300102BF25640011009A162F2E00".hexToByteArray()
        val byteArray2 = "2000000000C025BD256200B2FA".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(data.bms1.status).isEqualTo(513)
        assertThat(data.bms1.remCap).isEqualTo(9663)
        assertThat(data.bms1.remPerc).isEqualTo(100)
        assertThat(data.bms1.current).isEqualTo(0.17)
        assertThat(data.bms1.voltage).isEqualTo(57.86)
        assertThat(data.bms1.temp1).isEqualTo(27)
        assertThat(data.bms1.temp2).isEqualTo(26)
        assertThat(data.bms1.balanceMap).isEqualTo(8192)
        assertThat(data.bms1.health).isEqualTo(98)
    }

    @Test
    fun `decode z10 bms1 cells data`() {
        // Arrange.
        val byteArray1 = "5AA520113E0440341006103110381005103010FA".hexToByteArray()
        val byteArray2 = "0F06103D104010011034103D1051100000000055".hexToByteArray()
        val byteArray3 = "FB".hexToByteArray()
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(data.bms1.cells[0]).isEqualTo(4.148)
        assertThat(data.bms1.cells[1]).isEqualTo(4.102)
        assertThat(data.bms1.cells[2]).isEqualTo(4.145)
        assertThat(data.bms1.cells[3]).isEqualTo(4.152)
        assertThat(data.bms1.cells[4]).isEqualTo(4.101)
        assertThat(data.bms1.cells[5]).isEqualTo(4.144)
        assertThat(data.bms1.cells[6]).isEqualTo(4.090)
        assertThat(data.bms1.cells[7]).isEqualTo(4.102)
        assertThat(data.bms1.cells[8]).isEqualTo(4.157)
        assertThat(data.bms1.cells[9]).isEqualTo(4.160)
        assertThat(data.bms1.cells[10]).isEqualTo(4.097)
        assertThat(data.bms1.cells[11]).isEqualTo(4.148)
        assertThat(data.bms1.cells[12]).isEqualTo(4.157)
        assertThat(data.bms1.cells[13]).isEqualTo(4.177)
        assertThat(data.bms1.cells[14]).isEqualTo(0.0)
        assertThat(data.bms1.cells[15]).isEqualTo(0.0)

    }

    @Test
    fun `decode z10 bms2 sn data`() {
        // Arrange.
        val byteArray1 = "5AA522123E041034395945513138483151303432".hexToByteArray()
        val byteArray2 = "33160180258025EC13B000810100000000000001".hexToByteArray()
        val byteArray3 = "2569F8".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(data.bms2.serialNumber).isEqualTo("49YEQ18H1Q0423")
        assertThat(data.bms2.versionNumber).isEqualTo("1.1.6")
        assertThat(data.bms2.factoryCap).isEqualTo(9600)
        assertThat(data.bms2.actualCap).isEqualTo(9600)
        assertThat(data.bms2.fullCycles).isEqualTo(176)
        assertThat(data.bms2.chargeCount).isEqualTo(385)
        assertThat(data.bms2.mfgDateStr).isEqualTo("01.08.2018")
    }

    @Test
    fun `decode z10 bms2 status data`() {
        // Arrange.
        val byteArray1 = "5AA518123E043001029C2564000000A0162F2E00".hexToByteArray()
        val byteArray2 = "00000000009C259C25620044FB".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(data.bms2.status).isEqualTo(513)
        assertThat(data.bms2.remCap).isEqualTo(9628)
        assertThat(data.bms2.remPerc).isEqualTo(100)
        assertThat(data.bms2.current).isEqualTo(0.0)
        assertThat(data.bms2.voltage).isEqualTo(57.92)
        assertThat(data.bms2.temp1).isEqualTo(27)
        assertThat(data.bms2.temp2).isEqualTo(26)
        assertThat(data.bms2.balanceMap).isEqualTo(0)
        assertThat(data.bms2.health).isEqualTo(98)
    }

    @Test
    fun `decode z10 bms2 cells data`() {
        // Arrange.
        val byteArray1 = "5AA520123E04401B102C10221024102310211031".hexToByteArray()
        val byteArray2 = "1030102F10271026103510321035100000000021".hexToByteArray()
        val byteArray3 = "FC".hexToByteArray()
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(data.bms2.cells[0]).isEqualTo(4.123)
        assertThat(data.bms2.cells[1]).isEqualTo(4.140)
        assertThat(data.bms2.cells[2]).isEqualTo(4.130)
        assertThat(data.bms2.cells[3]).isEqualTo(4.132)
        assertThat(data.bms2.cells[4]).isEqualTo(4.131)
        assertThat(data.bms2.cells[5]).isEqualTo(4.129)
        assertThat(data.bms2.cells[6]).isEqualTo(4.145)
        assertThat(data.bms2.cells[7]).isEqualTo(4.144)
        assertThat(data.bms2.cells[8]).isEqualTo(4.143)
        assertThat(data.bms2.cells[9]).isEqualTo(4.135)
        assertThat(data.bms2.cells[10]).isEqualTo(4.134)
        assertThat(data.bms2.cells[11]).isEqualTo(4.149)
        assertThat(data.bms2.cells[12]).isEqualTo(4.146)
        assertThat(data.bms2.cells[13]).isEqualTo(4.149)
        assertThat(data.bms2.cells[14]).isEqualTo(0.0)
        assertThat(data.bms2.cells[15]).isEqualTo(0.0)
    }

    @Test
    fun `setDrl command`() {
        // Arrange.
        val expectedTrue  = "5aa5023e1403d30100d4fe".hexToByteArray()
        val expectedFalse = "5aa5023e1403d30000d5fe".hexToByteArray()

        // Act.
        adapter.setDrl(true)
        val actualTrue = adapter.settingCommand
        adapter.setDrl(false)
        val actualFalse = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actualTrue).isNotEqualTo(actualFalse)
        assertThat(actualTrue).isEqualTo(expectedTrue)
        assertThat(actualFalse).isEqualTo(expectedFalse)
    }

    @Test
    fun `setLightState command`() {
        // Arrange.
        val expectedTrue  = "5aa5023e1403d30400d1fe".hexToByteArray()
        val expectedFalse = "5aa5023e1403d30000d5fe".hexToByteArray()

        // Act.
        adapter.setLightState(true)
        val actualTrue = adapter.settingCommand
        adapter.setLightState(false)
        val actualFalse = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actualTrue).isNotEqualTo(actualFalse)
        assertThat(actualTrue).isEqualTo(expectedTrue)
        assertThat(actualFalse).isEqualTo(expectedFalse)
    }

    @Test
    fun `setTailLightState command`() {
        // Arrange.
        val expectedTrue  = "5aa5023e1403d30200d3fe".hexToByteArray()
        val expectedFalse = "5aa5023e1403d30000d5fe".hexToByteArray()

        // Act.
        adapter.setTailLightState(true)
        val actualTrue = adapter.settingCommand
        adapter.setTailLightState(false)
        val actualFalse = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actualTrue).isNotEqualTo(actualFalse)
        assertThat(actualTrue).isEqualTo(expectedTrue)
        assertThat(actualFalse).isEqualTo(expectedFalse)
    }

    @Test
    fun `setHandleButtonState command`() {
        // Arrange.
        val expectedTrue  = "5aa5023e1403d30000d5fe".hexToByteArray()
        val expectedFalse = "5aa5023e1403d30800cdfe".hexToByteArray()

        // Act.
        adapter.setHandleButtonState(true)
        val actualTrue = adapter.settingCommand
        adapter.setHandleButtonState(false)
        val actualFalse = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actualTrue).isNotEqualTo(actualFalse)
        assertThat(actualTrue).isEqualTo(expectedTrue)
        assertThat(actualFalse).isEqualTo(expectedFalse)
    }

    @Test
    fun `setBrakeAssist command`() {
        // Arrange.
        val expectedTrue  = "5aa5023e1403d30000d5fe".hexToByteArray()
        val expectedFalse = "5aa5023e1403d31000c5fe".hexToByteArray()

        // Act.
        adapter.setBrakeAssist(true)
        val actualTrue = adapter.settingCommand
        adapter.setBrakeAssist(false)
        val actualFalse = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actualTrue).isNotEqualTo(actualFalse)
        assertThat(actualTrue).isEqualTo(expectedTrue)
        assertThat(actualFalse).isEqualTo(expectedFalse)
    }

    @Test
    fun `setLedColor command`() {
        // Arrange.
        val expected1_0        = "5aa5043e1403c8f0000000eefd".hexToByteArray()
        val expected2_256      = "5aa5043e1403ca00000000dcfe".hexToByteArray()
        val expected3_30       = "5aa5043e1403ccf01e0000ccfd".hexToByteArray()
        val expected4_100500   = "5aa5043e1403ce00000000d8fe".hexToByteArray()
        val expected5_minus100 = "5aa5043e1403d0f09c00004afd".hexToByteArray()

        // Act.
        adapter.setLedColor(0, 1)
        val actual1_0 = adapter.settingCommand
        adapter.setLedColor(256, 2)
        val actual2_256 = adapter.settingCommand
        adapter.setLedColor(30, 3)
        val actual3_30 = adapter.settingCommand
        adapter.setLedColor(100500, 4)
        val actual4_100500 = adapter.settingCommand
        adapter.setLedColor(-100, 5)
        val actual5_minus100 = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual1_0).isEqualTo(expected1_0)
        assertThat(actual2_256).isEqualTo(expected2_256)
        assertThat(actual3_30).isEqualTo(expected3_30)
        assertThat(actual4_100500).isEqualTo(expected4_100500)
        assertThat(actual5_minus100).isEqualTo(expected5_minus100)
    }

    @Test
    fun `setAlarmEnabled command`() {
        // Arrange.
        val expected1_true  = "5aa5023e14037c01002bff".hexToByteArray()
        val expected1_false = "5aa5023e14037c00002cff".hexToByteArray()
        val expected2_true  = "5aa5023e14037c02002aff".hexToByteArray()
        val expected2_false = "5aa5023e14037c00002cff".hexToByteArray()
        val expected3_true  = "5aa5023e14037c040028ff".hexToByteArray()
        val expected3_false = "5aa5023e14037c00002cff".hexToByteArray()

        // Act.
        adapter.setAlarmEnabled(true, 1)
        val actual1_true = adapter.settingCommand
        adapter.setAlarmEnabled(false, 1)
        val actual1_false = adapter.settingCommand
        adapter.setAlarmEnabled(true, 2)
        val actual2_true = adapter.settingCommand
        adapter.setAlarmEnabled(false, 2)
        val actual2_false = adapter.settingCommand
        adapter.setAlarmEnabled(true, 3)
        val actual3_true = adapter.settingCommand
        adapter.setAlarmEnabled(false, 3)
        val actual3_false = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual1_true).isEqualTo(expected1_true)
        assertThat(actual1_false).isEqualTo(expected1_false)
        assertThat(actual2_true).isEqualTo(expected2_true)
        assertThat(actual2_false).isEqualTo(expected2_false)
        assertThat(actual3_true).isEqualTo(expected3_true)
        assertThat(actual3_false).isEqualTo(expected3_false)
    }

    @Test
    fun `setAlarmSpeed command`() {
        // Arrange.
        val expected0   = "5aa5023e14037de80340fe".hexToByteArray()
        val expected55  = "5aa5023e14037e7c1599fe".hexToByteArray()
        val expected100 = "5aa5023e14037f1027f2fe".hexToByteArray()

        // Act.
        adapter.setAlarmSpeed(10, 1)
        val actual0 = adapter.settingCommand
        adapter.setAlarmSpeed(55, 2)
        val actual55 = adapter.settingCommand
        adapter.setAlarmSpeed(100, 3)
        val actual100 = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual0).isEqualTo(expected0)
        assertThat(actual55).isEqualTo(expected55)
        assertThat(actual100).isEqualTo(expected100)
    }
}
