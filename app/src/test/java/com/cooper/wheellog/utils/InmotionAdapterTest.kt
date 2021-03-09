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

class InmotionAdapterTest {

    private var adapter: InMotionAdapter = InMotionAdapter()
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
    fun `decode with v5f full data`() {
        // Arrange.
        val byteArray1 = "AAAA1401A5550F7C000000B4720020FE0001001B".hexToByteArray()
        val byteArray2 = "0076BA5C28711200000000000000000100000000".hexToByteArray()
        val byteArray3 = "000000FA010301FA0103010402020100000000C2".hexToByteArray()
        val byteArray4 = "040001C2040001900302010000000000000000A8".hexToByteArray()
        val byteArray5 = "6100000010000000000000000000000000000000".hexToByteArray()
        val byteArray6 = "0000000100000000000000000000000000000000".hexToByteArray()
        val byteArray7 = "0000000200000500000000000000000000000004".hexToByteArray()
        val byteArray8 = "020301E35555".hexToByteArray()

        val byteArray11 = "AAAA1301A5550F60000000B4720020FE000100FF".hexToByteArray()
        val byteArray12 = "3F00003A18DEFF5D01000029F0FFFF29F0FFFFEC".hexToByteArray()
        val byteArray13 = "FFFFFF15200000000000001A1A00000000000000".hexToByteArray()
        val byteArray14 = "0000001CE3130000000000000026061A03D20721".hexToByteArray()
        val byteArray15 = "0000006F0100006F010000F7010000420C00002B".hexToByteArray()
        val byteArray16 = "110000070000000000000000000000265555".hexToByteArray()


        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)
        val result8 = adapter.decode(byteArray8)

        val result11 = adapter.decode(byteArray11)
        val result12 = adapter.decode(byteArray12)
        val result13 = adapter.decode(byteArray13)
        val result14 = adapter.decode(byteArray14)
        val result15 = adapter.decode(byteArray15)
        val result16 = adapter.decode(byteArray16)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isFalse()
        assertThat(result8).isTrue()
        assertThat(data.serial).isEqualTo("1271285CBA76001B")
        assertThat(data.model).isEqualTo("Inmotion V5F")
        assertThat(data.version).isEqualTo("1.3.506")
        assertThat(data.wheelLight).isFalse()
        assertThat(data.wheelLed).isFalse()
        assertThat(data.wheelHandleButton).isFalse() //incorrect processing v5f
        assertThat(data.wheelMaxSpeed).isEqualTo(25)
        assertThat(data.speakerVolume).isEqualTo(0) //wrong in processing of V5f! should be 100
        assertThat(data.pedalsPosition).isEqualTo(0)

        assertThat(result11).isFalse()
        assertThat(result12).isFalse()
        assertThat(result13).isFalse()
        assertThat(result14).isFalse()
        assertThat(result15).isFalse()
        assertThat(result16).isTrue()
        assertThat(data.speedDouble).isEqualTo(3.82)
        assertThat(data.temperature).isEqualTo(26)
        assertThat(data.temperature2).isEqualTo(0)
        assertThat(data.voltageDouble).isEqualTo(82.13)
        assertThat(data.currentDouble).isEqualTo(-0.2)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.totalDistance).isEqualTo(1303324)
        assertThat(data.batteryLevel).isEqualTo(97)
        assertThat(data.angle).isEqualTo(0.2499847412109375)
        assertThat(data.roll).isEqualTo(5.588888888888889)
    }

    @Test
    fun `decode with v8f full data`() {
        // Arrange.
        val byteArray1 = "AAAA1401A5550F8500000000000000FE0201000E".hexToByteArray()
        val byteArray2 = "009BBD5E4A601400000000000000000000000000".hexToByteArray()
        val byteArray3 = "0000001500020200000000070003020000000026".hexToByteArray()
        val byteArray4 = "0301010000000000000A000000000073000000C8".hexToByteArray()
        val byteArray5 = "AF00002510000000100000000000000000000000".hexToByteArray()
        val byteArray6 = "0000000100000000000000000000000000000000".hexToByteArray()
        val byteArray7 = "0000000600000800000000000000000000000000".hexToByteArray()
        val byteArray8 = "000000801027000001010A00DC5555".hexToByteArray()

        val byteArray11 = "AAAA1301A5550F9500000000000000FE0201008F".hexToByteArray()
        val byteArray12 = "020000000000000000000054FAFFFF54FAFFFFFB".hexToByteArray()
        val byteArray13 = "FFFFFFBE200000000000001B1B24240000000000".hexToByteArray()
        val byteArray14 = "000000AF5400000100000000302B140605E00722".hexToByteArray()
        val byteArray15 = "00000023000000C50000005D020000D900000006".hexToByteArray()
        val byteArray16 = "000000000000000000000000000000004000081B".hexToByteArray()
        val byteArray17 = "0000F221000033060000000000000B0000006216".hexToByteArray()
        val byteArray18 = "0000F42A0000030000000E000000110106000000".hexToByteArray()
        val byteArray19 = "000000000000C500765555".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)
        val result8 = adapter.decode(byteArray8)

        val result11 = adapter.decode(byteArray11)
        val result12 = adapter.decode(byteArray12)
        val result13 = adapter.decode(byteArray13)
        val result14 = adapter.decode(byteArray14)
        val result15 = adapter.decode(byteArray15)
        val result16 = adapter.decode(byteArray16)
        val result17 = adapter.decode(byteArray17)
        val result18 = adapter.decode(byteArray18)
        val result19 = adapter.decode(byteArray19)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isFalse()
        assertThat(result8).isTrue()
        assertThat(data.serial).isEqualTo("14604A5EBD9B000E")
        assertThat(data.model).isEqualTo("Inmotion V8F")
        assertThat(data.version).isEqualTo("2.2.21")
        assertThat(data.wheelLight).isFalse()
        assertThat(data.wheelLed).isTrue()
        assertThat(data.wheelHandleButton).isFalse()
        assertThat(data.wheelMaxSpeed).isEqualTo(45)
        assertThat(data.speakerVolume).isEqualTo(100)
        assertThat(data.pedalsPosition).isEqualTo(0)
        assertThat(data.pedalHardness).isEqualTo(100)
        assertThat(data.rideMode).isFalse()

        assertThat(result11).isFalse()
        assertThat(result12).isFalse()
        assertThat(result13).isFalse()
        assertThat(result14).isFalse()
        assertThat(result15).isFalse()
        assertThat(result16).isFalse()
        assertThat(result17).isFalse()
        assertThat(result18).isFalse()
        assertThat(result19).isTrue()
        assertThat(data.speedDouble).isEqualTo(1.37)
        assertThat(data.temperature).isEqualTo(27)
        assertThat(data.temperature2).isEqualTo(36)
        assertThat(data.voltageDouble).isEqualTo(83.82)
        assertThat(data.currentDouble).isEqualTo(-0.05)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.001)
        assertThat(data.totalDistance).isEqualTo(21679)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(0.0099945068359375)
        assertThat(data.roll).isEqualTo(6.722222222222222)
    }

    @Test
    fun `decode with v8f full data 2`() {
        // Arrange.
        val byteArray1 = "AAAA1401A5550F8500000000000000FE0201000E".hexToByteArray()
        val byteArray2 = "009BBD5E4A601400000000000000000000000000".hexToByteArray()
        val byteArray3 = "0000001500020200000000070003020000000026".hexToByteArray()
        val byteArray4 = "0301010000000000000A000000000073000000C8".hexToByteArray()
        val byteArray5 = "AF00002510000000100000000000000000000000".hexToByteArray()
        val byteArray6 = "0000000100000000000000000000000000000000".hexToByteArray()
        val byteArray7 = "0000000600000800000000000000000000000000".hexToByteArray()
        val byteArray8 = "000000801027000001010A00DC5555".hexToByteArray()

        val byteArray11 = "AAAA1301A5550F9500000000000000FE0201007A".hexToByteArray()
        val byteArray12 = "14000000000000000000003CFDFFFF3CFDFFFFF6".hexToByteArray()
        val byteArray13 = "FFFFFFA7200000400100001C1C2424F8FFFFFFE7".hexToByteArray()
        val byteArray14 = "FFFFFFB75400000900000000042C140605E00722".hexToByteArray()
        val byteArray15 = "000000E301000023010000AC0500000302000056".hexToByteArray()
        val byteArray16 = "0000004C0000000000000000000000004000081C".hexToByteArray()
        val byteArray17 = "0000F221000033060000BF020000070100006F16".hexToByteArray()
        val byteArray18 = "0000032B0000100000001D000000380256004C00".hexToByteArray()
        val byteArray19 = "F8FFE7FFE7FF2301465555".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)
        val result8 = adapter.decode(byteArray8)

        val result11 = adapter.decode(byteArray11)
        val result12 = adapter.decode(byteArray12)
        val result13 = adapter.decode(byteArray13)
        val result14 = adapter.decode(byteArray14)
        val result15 = adapter.decode(byteArray15)
        val result16 = adapter.decode(byteArray16)
        val result17 = adapter.decode(byteArray17)
        val result18 = adapter.decode(byteArray18)
        val result19 = adapter.decode(byteArray19)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isFalse()
        assertThat(result8).isTrue()
        assertThat(data.serial).isEqualTo("14604A5EBD9B000E")
        assertThat(data.model).isEqualTo("Inmotion V8F")
        assertThat(data.version).isEqualTo("2.2.21")
        assertThat(data.wheelLight).isFalse()
        assertThat(data.wheelLed).isTrue()
        assertThat(data.wheelHandleButton).isFalse()
        assertThat(data.wheelMaxSpeed).isEqualTo(45)
        assertThat(data.speakerVolume).isEqualTo(100)
        assertThat(data.pedalsPosition).isEqualTo(0)
        assertThat(data.pedalHardness).isEqualTo(100)
        assertThat(data.rideMode).isFalse()

        assertThat(result11).isFalse()
        assertThat(result12).isFalse()
        assertThat(result13).isFalse()
        assertThat(result14).isFalse()
        assertThat(result15).isFalse()
        assertThat(result16).isFalse()
        assertThat(result17).isFalse()
        assertThat(result18).isFalse()
        assertThat(result19).isTrue()
        assertThat(data.speedDouble).isEqualTo(0.66)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.temperature2).isEqualTo(36)
        assertThat(data.voltageDouble).isEqualTo(83.59)
        assertThat(data.currentDouble).isEqualTo(-0.1)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.009)
        assertThat(data.totalDistance).isEqualTo(21687)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.angle).isEqualTo(0.079986572265625)
        assertThat(data.roll).isEqualTo(16.133333333333333)
    }

    @Test
    fun `light commands`() {
        // Arrange.
        val expectedOn = "aaaa0d01a5550f010000000000000008050000805555".hexToByteArray()
        val expectedOff = "aaaa0d01a5550f0000000000000000080500007f5555".hexToByteArray()

        // Act.
        adapter.setLightState(true)
        val actualOn = adapter.settingCommand
        adapter.setLightState(false)
        val actualOff = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actualOn).isEqualTo(expectedOn)
        assertThat(actualOff).isEqualTo(expectedOff)
    }

    @Test
    fun `led command`() {
        // Arrange.
        val expectedOn = "aaaa1601a5550fb20000000f00000008050000495555".hexToByteArray()
        val expectedOff = "aaaa1601a5550fb200000010000000080500004a5555".hexToByteArray()

        // Act.
        adapter.setLedState(true)
        val actualOn = adapter.settingCommand
        adapter.setLedState(false)
        val actualOff = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actualOn).isEqualTo(expectedOn)
        assertThat(actualOff).isEqualTo(expectedOff)
    }

    @Test
    fun `handle button state command`() {
        // Arrange.
        val expectedOn = "aaaa2E01a5550f000000000000000008050000a05555".hexToByteArray()
        val expectedOff = "aaaa2E01a5550f010000000000000008050000a15555".hexToByteArray()

        // Act.
        adapter.setHandleButtonState(true)
        val actualOn = adapter.settingCommand
        adapter.setHandleButtonState(false)
        val actualOff = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actualOn).isEqualTo(expectedOn)
        assertThat(actualOff).isEqualTo(expectedOff)
    }

    @Test
    fun `max speed command`() {
        // Arrange.
        val expected00 = "aaaa1501a5550f010000000000000008050000885555".hexToByteArray()
        val expected10 = "aaaa1501a5550f010000001027000008050000bf5555".hexToByteArray()
        val expected20 = "aaaa1501a5550f01000000204e000008050000f65555".hexToByteArray()
        val expected25 = "aaaa1501a5550f01000000a861000008050000915555".hexToByteArray()
        val expected35 = "aaaa1501a5550f01000000b888000008050000c85555".hexToByteArray()

        // Act.
        adapter.setMaxSpeedState(0)
        val actual00 = adapter.settingCommand
        adapter.setMaxSpeedState(10)
        val actual10 = adapter.settingCommand
        adapter.setMaxSpeedState(20)
        val actual20 = adapter.settingCommand
        adapter.setMaxSpeedState(25)
        val actual25 = adapter.settingCommand
        adapter.setMaxSpeedState(35)
        val actual35 = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual00).isEqualTo(expected00)
        assertThat(actual10).isEqualTo(expected10)
        assertThat(actual20).isEqualTo(expected20)
        assertThat(actual25).isEqualTo(expected25)
        assertThat(actual35).isEqualTo(expected35)
    }

    @Test
    fun `tilt horizon command`() {
        // Arrange.
        val expected0 = "aaaa1501a5550f000000000000000008050000875555".hexToByteArray()
        val expected80 = "aaaa1501a5550f0000000000000800080500008f5555".hexToByteArray()
        val expectedMinus80 = "aaaa1501a5550f000000000000f8ff080500007e5555".hexToByteArray()

        // Act.
        adapter.setTiltHorizon(0)
        val actual0 = adapter.settingCommand
        adapter.setTiltHorizon(80)
        val actual80 = adapter.settingCommand
        adapter.setTiltHorizon(-80)
        val actualMinus80 = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual0).isEqualTo(expected0)
        assertThat(actual80).isEqualTo(expected80)
        assertThat(actualMinus80).isEqualTo(expectedMinus80)
    }

    @Test
    fun `speaker volume command`() {
        // Arrange.
        val expected0 = "aaaa0a06a5550f000000000000000008050000815555".hexToByteArray()
        val expected55 = "aaaa0a06a5550f7c1500000000000008050000125555".hexToByteArray()
        val expected100 = "aaaa0a06a5550f102700000000000008050000b85555".hexToByteArray()

        // Act.
        adapter.setSpeakerVolumeState(0)
        val actual0 = adapter.settingCommand
        adapter.setSpeakerVolumeState(55)
        val actual55 = adapter.settingCommand
        adapter.setSpeakerVolumeState(100)
        val actual100 = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual0).isEqualTo(expected0)
        assertThat(actual55).isEqualTo(expected55)
        assertThat(actual100).isEqualTo(expected100)
    }

    @Test
    fun `pedal hardness command`() {
        // Arrange.
        val expected4 = "AAAA1501A5550F060000000004000008050000915555".hexToByteArray()
        val expected46 = "AAAA1501A5550F060000004009000008050000D65555".hexToByteArray()
        val expected100 = "AAAA1501A5550F0600000000100000080500009D5555".hexToByteArray()

        // Act.
        adapter.setPedalHardness(4)
        val actual4 = adapter.settingCommand
        adapter.setPedalHardness(46)
        val actual46 = adapter.settingCommand
        adapter.setPedalHardness(100)
        val actual100 = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual4).isEqualTo(expected4)
        assertThat(actual46).isEqualTo(expected46)
        assertThat(actual100).isEqualTo(expected100)
    }


    @Test
    fun `ride mode command`() {
        // Arrange.
        val expected_comfort = "aaaa1501a5550f0a0000000000000008050000915555".hexToByteArray()
        val expected_classic = "aaaa1501a5550f0a0000000100000008050000925555".hexToByteArray()

        // Act.
        adapter.setRideMode(true)
        val actual_classic = adapter.settingCommand
        adapter.setRideMode(false)
        val actual_comfort = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual_classic).isEqualTo(expected_classic)
        assertThat(actual_comfort).isEqualTo(expected_comfort)
    }

    @Test
    fun `power off command`() {
        // Arrange.
        val expected = "aaaa1601a5550fb200000005000000080500003f5555".hexToByteArray()

        // Act.
        adapter.powerOff()
        val actual = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `wheel calibration command`() {
        // Arrange.
        val expected = "aaaa1901a5550f3254769800000000080500001f5555".hexToByteArray()

        // Act.
        adapter.wheelCalibration()
        val actual = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `wheel beep command`() {
        // Arrange.
        val expected = "aaaa1601a5550fb200000011000000080500004b5555".hexToByteArray()

        // Act.
        adapter.wheelBeep()
        val actual = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `play sound command`() {
        // Arrange.
        val expected = "AAAA0906A5550F020000000000000008050000825555".hexToByteArray()

        // Act.
        adapter.wheelSound(2)
        val actual = adapter.settingCommand

        // Assert.
        assertThat(adapter.settingCommandReady).isEqualTo(true)
        assertThat(actual).isEqualTo(expected)
    }

}
