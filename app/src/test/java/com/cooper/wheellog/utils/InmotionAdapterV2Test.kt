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

class InmotionAdapterV2Test {

    private var adapter: InmotionAdapterV2 = InmotionAdapterV2()
    private lateinit var data: WheelData

    @Before
    fun setUp() {
        mockkObject(WheelLog)
        every { WheelLog.appContext } returns mockkClass(Context::class, relaxed = true)
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
        assertThat(data.version).isEqualTo("Main:1.1.64 Drv:3.4.8 BLE:1.1.13")


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


    @Test
    fun `decode with v11 escape data`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V11)
        val byteArray1 = "aaaa1431843020a5a50068025207870080009400882c5fc4b000d7001000f4ff2b037c1564190000d9d9492b00000000000000000000a5a5".hexToByteArray() // wheel type
        // Act.
        val result1 = adapter.decode(byteArray1)
        // Assert.
        assertThat(result1).isTrue()
        assertThat(data.speedDouble).isEqualTo(6.16)
        assertThat(data.temperature).isEqualTo(20)
        assertThat(data.temperature2).isEqualTo(39)
        assertThat(data.imuTemp).isEqualTo(41)
        assertThat(data.cpuTemp).isEqualTo(41)
        assertThat(data.motorPower).isEqualTo(128.0)
        assertThat(data.currentLimit).isEqualTo(65.00)
        assertThat(data.speedLimit).isEqualTo(55.00)
        assertThat(data.torque).isEqualTo(18.74)
        assertThat(data.voltageDouble).isEqualTo(82.40)
        assertThat(data.currentDouble).isEqualTo(1.65)
        assertThat(data.wheelDistanceDouble).isEqualTo(1.48)
        assertThat(data.batteryLevel).isEqualTo(95)
        assertThat(data.powerDouble).isEqualTo(135.0)
        assertThat(data.angle).isEqualTo(0.16)
        assertThat(data.roll).isEqualTo(8.11)
    }

    @Test
    fun `decode v11 new fw with PWM`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V11)
        adapter.setProto(1)
        val byteArray1 = "aaaa143384411f8e03a5a506e90bd80242021600122a5acbb000cc002a0000000bfd7c1564190000d4d1ff09490a0000000000000000000010".hexToByteArray() // wheel type
        // Act.
        val result1 = adapter.decode(byteArray1)
        // Assert.
        assertThat(result1).isTrue()
        assertThat(data.speedDouble).isEqualTo(17.01)
        assertThat(data.temperature).isEqualTo(27)
        assertThat(data.temperature2).isEqualTo(28)
        assertThat(data.imuTemp).isEqualTo(33)
        assertThat(data.cpuTemp).isEqualTo(36)
        assertThat(data.motorPower).isEqualTo(578.0)
        assertThat(data.currentLimit).isEqualTo(65.00)
        assertThat(data.speedLimit).isEqualTo(55.00)
        assertThat(data.torque).isEqualTo(30.49)
        assertThat(data.voltageDouble).isEqualTo(80.01)
        assertThat(data.currentDouble).isEqualTo(9.1)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.22)
        assertThat(data.batteryLevel).isEqualTo(90)
        assertThat(data.powerDouble).isEqualTo(728.0)
        assertThat(data.angle).isEqualTo(0.42)
        assertThat(data.roll).isEqualTo(-7.57)
    }

    @Test
    fun `decode with v11 escape data2`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V11)
        adapter.setProto(1)
        val byteArray1 = "aaaa143184a5aa1e8100640b1301650059001504a0234cc0b000ce00180000007c007c1564190000d1d3492b00000000000000000000a5a5".hexToByteArray() // wheel type
        // Act.
        val result1 = adapter.decode(byteArray1)
        // Assert.
        assertThat(result1).isTrue()
        assertThat(data.speedDouble).isEqualTo(29.16)
        assertThat(data.temperature).isEqualTo(16)
        assertThat(data.temperature2).isEqualTo(30)
        assertThat(data.imuTemp).isEqualTo(35)
        assertThat(data.cpuTemp).isEqualTo(33)
        assertThat(data.motorPower).isEqualTo(89.0)
        assertThat(data.currentLimit).isEqualTo(65.00)
        assertThat(data.speedLimit).isEqualTo(55.00)
        assertThat(data.torque).isEqualTo(2.75)
        assertThat(data.voltageDouble).isEqualTo(78.50)
        assertThat(data.currentDouble).isEqualTo(1.29)
        assertThat(data.wheelDistanceDouble).isEqualTo(10.45)
        assertThat(data.batteryLevel).isEqualTo(76)
        assertThat(data.powerDouble).isEqualTo(101.0)
        assertThat(data.angle).isEqualTo(0.24)
        assertThat(data.roll).isEqualTo(1.24)
    }

    @Test
    fun `decode with v11 v1_4_0`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V11)
        adapter.setProto(2)
        val byteArray1 = "aaaa1445842d1d10000000efff070000000000000000002b0300000000000000008a149612e02e8813641900000000cbb000cccad1000028000000000049140000000000000000000021".hexToByteArray() // wheel type
        // Act.
        val result1 = adapter.decode(byteArray1)
        // Assert.
        assertThat(result1).isTrue()
        assertThat(data.speedDouble).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(27)
        assertThat(data.temperature2).isEqualTo(28)
        assertThat(data.imuTemp).isEqualTo(33)
        assertThat(data.cpuTemp).isEqualTo(26)
        assertThat(data.motorPower).isEqualTo(0)
        assertThat(data.currentLimit).isEqualTo(65.00)
        assertThat(data.speedLimit).isEqualTo(50.00)
        assertThat(data.torque).isEqualTo(-0.17)
        assertThat(data.voltageDouble).isEqualTo(74.69)
        assertThat(data.currentDouble).isEqualTo(0.16)
        assertThat(data.wheelDistanceDouble).isEqualTo(0)
        assertThat(data.batteryLevel).isEqualTo(53)
        assertThat(data.powerDouble).isEqualTo(0)
        assertThat(data.angle).isEqualTo(0)
        assertThat(data.roll).isEqualTo(0)
    }

    @Test
    fun `decode version with v11 v1_4_0`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V11)
        val byteArray1 = "aaaa111d820622000003040300070221000004011a000602230d00010107000001b9".hexToByteArray() // wheel type
        // Act.
        val result1 = adapter.decode(byteArray1)
        // Assert.
        assertThat(result1).isFalse()
        assertThat(data.version).isEqualTo("Main:1.4.0 Drv:4.3.0 BLE:1.1.13")
    }

    @Test
    fun `decode version with v12`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V11)
        val byteArray1 = "aaaa111d820622790002042000060221040005017d000602233700010203000402bb".hexToByteArray() // wheel type
        // Act.
        val result1 = adapter.decode(byteArray1)
        // Assert.
        assertThat(result1).isFalse()
        assertThat(data.version).isEqualTo("Main:1.5.4 Drv:4.2.121 BLE:2.1.55")

    }

    @Test
    fun `decode with v12 full data`() {
        // Arrange.
        val byteArray1 = "aaaa110882010207010103009c".hexToByteArray() // wheel type
        val byteArray2 = "aaaa11178202413033313135353133303030393733300000000000fb".hexToByteArray() // s/n
        val byteArray3 = "aaaa111d820622700002042000060221180004017d000602232400010203000402bc".hexToByteArray() //versions
        val byteArray5 = "aaaa142b900001082608000000c1b55622330000000000cdceb0ce0000000000000000000000000000000008000000ce".hexToByteArray() // probably statistics
        val byteArray6 = "aaaa1419916350000074471800d1140400c68e00007d350200b0ce000039".hexToByteArray() // totals
        val byteArray7 = "aaaa144384cd26090000000e00040000000000000000000000eafb000062009d2450463b1b581b000000000000cdce00ced1d0b03d2828000000004900000000000000000000008c".hexToByteArray()
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isTrue()
        assertThat(data.serial).isEqualTo("A031155130009730")
        assertThat(data.model).isEqualTo("Inmotion V12")
        assertThat(data.version).isEqualTo("Main:1.4.24 Drv:4.2.112 BLE:2.1.36")


        assertThat(data.speedDouble).isEqualTo(0.0)
        assertThat(data.temperature).isEqualTo(29)
        assertThat(data.temperature2).isEqualTo(30)
        assertThat(data.imuTemp).isEqualTo(32)
        assertThat(data.cpuTemp).isEqualTo(33)
        assertThat(data.motorPower).isEqualTo(0)
        assertThat(data.currentLimit).isEqualTo(70.00)
        assertThat(data.speedLimit).isEqualTo(69.71)
        assertThat(data.torque).isEqualTo(0.14)
        assertThat(data.voltageDouble).isEqualTo(99.33)
        assertThat(data.currentDouble).isEqualTo(0.09)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.totalDistance).isEqualTo(205790)
        assertThat(data.batteryLevel).isEqualTo(1) // old FW issue
        assertThat(data.powerDouble).isEqualTo(0.0)
        assertThat(data.angle).isEqualTo(0.0)
        assertThat(data.roll).isEqualTo(-10.46)
    }

    @Test
    fun `decode with v12 full data 2`() {
        // Arrange.
        val byteArray1 = "aaaa110882010207010103009c".hexToByteArray() // wheel type
        val byteArray2 = "aaaa11178202413033313135353133303030393733300000000000fb".hexToByteArray() // s/n
        val byteArray3 = "aaaa111d820622700002042000060221180004017d000602232400010203000402bc".hexToByteArray() //versions
        val byteArray5 = "aaaa142b900001082608000000c1b55622330000000000cdceb0ce0000000000000000000000000000000008000000ce".hexToByteArray() // probably statistics
        val byteArray6 = "aaaa1419916350000074471800d1140400c68e00007d350200b0ce000039".hexToByteArray() // totals
        val byteArray7 = "aaaa144384ae24600479135909c61536085a0b00003f000000eb003700a5aa21b61f50463b1b581b000000000000ddd900dfe5e4b0f9646400000000490800000000000000000000dd".hexToByteArray()
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isTrue()
        assertThat(data.serial).isEqualTo("A031155130009730")
        assertThat(data.model).isEqualTo("Inmotion V12")
        assertThat(data.version).isEqualTo("Main:1.4.24 Drv:4.2.112 BLE:2.1.36")


        assertThat(data.speedDouble).isEqualTo(49.85)
        assertThat(data.temperature).isEqualTo(45)
        assertThat(data.temperature2).isEqualTo(41)
        assertThat(data.imuTemp).isEqualTo(52)
        assertThat(data.cpuTemp).isEqualTo(53)
        assertThat(data.motorPower).isEqualTo(2906.0)
        assertThat(data.currentLimit).isEqualTo(70.00)
        assertThat(data.speedLimit).isEqualTo(69.71)
        assertThat(data.torque).isEqualTo(23.93)
        assertThat(data.voltageDouble).isEqualTo(93.90)
        assertThat(data.currentDouble).isEqualTo(11.20)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.55)
        assertThat(data.totalDistance).isEqualTo(205790)
        assertThat(data.batteryLevel).isEqualTo(86)
        assertThat(data.powerDouble).isEqualTo(2102.0)
        assertThat(data.angle).isEqualTo(0.63)
        assertThat(data.roll).isEqualTo(2.35)
    }

    @Test
    fun `decode with v12 data 3`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V12)
        val byteArray1 = "aaaa14438415273500930496014b0535003a0000008d000000fdfe010010271c255046581b581b000000000000ceca00cfd1d0b08d646400000000490000000000000000000000bc".hexToByteArray() // wheel type
        // Act.
        val result1 = adapter.decode(byteArray1)
        // Assert.
        assertThat(result1).isTrue()
        assertThat(data.speedDouble).isEqualTo(11.71)
        assertThat(data.temperature).isEqualTo(30)
        assertThat(data.temperature2).isEqualTo(26)
        assertThat(data.imuTemp).isEqualTo(32)
        assertThat(data.cpuTemp).isEqualTo(33)
        assertThat(data.motorPower).isEqualTo(58.0)
        assertThat(data.currentLimit).isEqualTo(70.00)
        assertThat(data.speedLimit).isEqualTo(70.00)
        assertThat(data.torque).isEqualTo(4.06)
        assertThat(data.voltageDouble).isEqualTo(100.05)
        assertThat(data.currentDouble).isEqualTo(0.53)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.01)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.powerDouble).isEqualTo(53.0)
        assertThat(data.angle).isEqualTo(1.41)
        assertThat(data.roll).isEqualTo(-2.59)
    }


    @Test
    fun `decode with v12 data 4`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V12)
        val byteArray1 = "aaaa1443842627090000000000060000000000000000000000b3fd000010271c255046581b581b000000000000ceca00ced0cfb048282800000000490000000000000000000000ef".hexToByteArray() // wheel type
        // Act.
        val result1 = adapter.decode(byteArray1)
        // Assert.
        assertThat(result1).isTrue()
        assertThat(data.speedDouble).isEqualTo(0.0)
        assertThat(data.temperature).isEqualTo(30)
        assertThat(data.temperature2).isEqualTo(26)
        assertThat(data.imuTemp).isEqualTo(31)
        assertThat(data.cpuTemp).isEqualTo(32)
        assertThat(data.motorPower).isEqualTo(0.0)
        assertThat(data.currentLimit).isEqualTo(70.0)
        assertThat(data.speedLimit).isEqualTo(70.0)
        assertThat(data.torque).isEqualTo(0.0)
        assertThat(data.voltageDouble).isEqualTo(100.22)
        assertThat(data.currentDouble).isEqualTo(0.09)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.batteryLevel).isEqualTo(100)
        assertThat(data.powerDouble).isEqualTo(0.0)
        assertThat(data.angle).isEqualTo(0.0)
        assertThat(data.roll).isEqualTo(-5.89)
    }



    @Test
    fun `decode with v13 full data 1`() {
        // Arrange.
        val byteArray1 = "aaaa1108820102080101010091".hexToByteArray() // wheel type
        val byteArray2 = "aaaa111782024130333131364231383030303130343600000000008a".hexToByteArray() // s/n
        val byteArray3 = "aaaa112f8206223a000005030008022115000002cf000802230a0002020000050224070001010200010125070001010200010172".hexToByteArray() //versions
        val byteArray5 = "aaaa142b9000010126010000004390a7d5010251000701cdcec9d000000000080000000000000004000000070000006c".hexToByteArray() // probably statistics
        val byteArray6 = "aaaa1419915e010000b7660000500900008c0600002d8b0000c9d000007e".hexToByteArray() // totals
        val byteArray7 = "aaaa145984092f3807000036003735000025130f27b108111d4203b00664fee703050000000000f225e225204e28233421401f401f204e401f709400000000cdccc9d1b0d10000b0286400000000004910000000000000001800000000b3".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isTrue()
        assertThat(data.serial).isEqualTo("A03116B180001046")
        assertThat(data.model).isEqualTo("Inmotion V13")
        assertThat(data.version).isEqualTo("Main:2.0.21 Drv:5.0.58 BLE:2.2.10")


        assertThat(data.speedDouble).isEqualTo(136.23)
        assertThat(data.temperature).isEqualTo(29)
        assertThat(data.temperature2).isEqualTo(28)
        assertThat(data.imuTemp).isEqualTo(33)
        assertThat(data.cpuTemp).isEqualTo(0)
        assertThat(data.motorPower).isEqualTo(1712.0)
        assertThat(data.currentLimit).isEqualTo(80.00)
        assertThat(data.speedLimit).isEqualTo(90.00)
        assertThat(data.torque).isEqualTo(74.41)
        assertThat(data.voltageDouble).isEqualTo(120.41)
        assertThat(data.currentDouble).isEqualTo(18.48)
        assertThat(data.wheelDistanceDouble).isEqualTo(4.901)
        assertThat(data.totalDistance).isEqualTo(3500)
        assertThat(data.batteryLevel).isEqualTo(97)
        assertThat(data.powerDouble).isEqualTo(2225.0)
        assertThat(data.angle).isEqualTo(0.54)
        assertThat(data.roll).isEqualTo(-4.12)
    }

    @Test
    fun `decode with v14 full data 1`() {
        // Arrange.
        val byteArray1 = "aaaa1108820102090201010093".hexToByteArray() // wheel type
        val byteArray2 = "aaaa1117820241303332313743304230303131323245000000000084".hexToByteArray() // s/n
        val byteArray3 = "aaaa11418206223c00060503000802212800000301000902230100000208000201240200000501000204260200000501000204250200000501000204270200000501000204eb".hexToByteArray() //versions
        val byteArray5 = "aaaa142b9000011d261d00000044c5895e2c08ac049205d0d1cbd0510000001e0f0000fc010000070100003401000051".hexToByteArray() // probably statistics
        val byteArray6 = "aaaa1419911d9c000059293800d01106007134010097110600cbd051001c".hexToByteArray() // totals
        val byteArray7 = "aaaa1459847c334000000000002c0800009900430866004f002700efff6400bfff5e0000000000a5aa26a4261027581b581b401f401f401f401fb88800000000cdcfcad0b0d00000b0cc640000000000491000000000000000000000000064".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isTrue()
        assertThat(data.serial).isEqualTo("A03217C0B001122E")
        assertThat(data.model).isEqualTo("Inmotion V14 50S")
        assertThat(data.version).isEqualTo("Main:3.0.40 Drv:5.6.60 BLE:2.0.1")


        assertThat(data.speedDouble).isEqualTo(20.92)
        assertThat(data.temperature).isEqualTo(29)
        assertThat(data.temperature2).isEqualTo(31)
        assertThat(data.imuTemp).isEqualTo(32)
        assertThat(data.cpuTemp).isEqualTo(0)
        assertThat(data.motorPower).isEqualTo(79.0)
        assertThat(data.currentLimit).isEqualTo(80.00)
        assertThat(data.speedLimit).isEqualTo(70.00)
        assertThat(data.torque).isEqualTo(1.53)
        assertThat(data.voltageDouble).isEqualTo(131.80)
        assertThat(data.currentDouble).isEqualTo(0.64)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.94)
        assertThat(data.totalDistance).isEqualTo(399650)
        assertThat(data.batteryLevel).isEqualTo(99)
        assertThat(data.powerDouble).isEqualTo(102.0)
        assertThat(data.angle).isEqualTo(0.39)
        assertThat(data.roll).isEqualTo(-0.17)
    }

    @Test
    fun `decode with v11y full data 1`() {
        // Arrange.
        val byteArray1 = "aaaa110882010206020101009c".hexToByteArray() // wheel type
        val byteArray2 = "aaaa1117820241303332313831304430303130303139000000000083".hexToByteArray() // s/n
        val byteArray3 = "aaaa112f8206220800030603000802213400050201000902230300030108000201240d00010101000101250d00010101000101ac".hexToByteArray() //versions
        val byteArray5 = "aaaa1428a0200410100e401f401f0000006464323232000000005802000a28645a280000144001040100250d92".hexToByteArray() // probably statistics
        val byteArray6 = "aaaa142b9000011f261f0000004456569ac5024c005400ccc5d0cb030000003e000000000000002000000073000000f5".hexToByteArray() // totals
        val byteArray7 = "aaaa141991c82e0000266708008d62000091e400005e720300d0cb03009e".hexToByteArray()
        val byteArray8 = "aaaa145984941e11000000000087000000090104020000000000006502000000000300000000004b20451fe02e0410100e401f401fa816a816c05d00000000ccc5cecdb0cd0000b0c36400000000004900000000000000000000000000fe".hexToByteArray()
        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result5 = adapter.decode(byteArray5)
        val result6 = adapter.decode(byteArray6)
        val result7 = adapter.decode(byteArray7)
        val result8 = adapter.decode(byteArray8)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isFalse()
        assertThat(result3).isFalse()
        assertThat(result5).isFalse()
        assertThat(result6).isFalse()
        assertThat(result7).isFalse()
        assertThat(result8).isTrue()
        assertThat(data.serial).isEqualTo("A0321810D0010019")
        assertThat(data.model).isEqualTo("Inmotion V11y")
        assertThat(data.version).isEqualTo("Main:2.5.52 Drv:6.3.8 BLE:1.3.3")


        assertThat(data.speedDouble).isEqualTo(1.35)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.temperature2).isEqualTo(21)
        assertThat(data.imuTemp).isEqualTo(29)
        assertThat(data.cpuTemp).isEqualTo(0)
        assertThat(data.motorPower).isEqualTo(0.0)
        assertThat(data.currentLimit).isEqualTo(58.00)
        assertThat(data.speedLimit).isEqualTo(41.00)
        assertThat(data.torque).isEqualTo(2.65)
        assertThat(data.voltageDouble).isEqualTo(78.28)
        assertThat(data.currentDouble).isEqualTo(0.17)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.03)
        assertThat(data.totalDistance).isEqualTo(119760)
        assertThat(data.batteryLevel).isEqualTo(81)
        assertThat(data.powerDouble).isEqualTo(0.0)
        assertThat(data.angle).isEqualTo(0.0)
        assertThat(data.roll).isEqualTo(6.13)
    }

/*
    @Test
    fun `Inmotion v12 - decode long trip`() {
        // Arrange.
        adapter.setModel(InmotionAdapterV2.Model.V11)
        val inputStream: InputStream = File("src/test/resources/RAW_2021_11_29_09_14_06.csv").inputStream()
        //val startTime = sdf.parse("09:30:10.000")
        val startTime = sdf.parse("09:00:00.000")
        val stopTime = sdf.parse("10:20:00.000")
        var decodeSuccessCounter = 0
        inputStream.bufferedReader().useLines { lines ->
            run lin@ {
                lines.forEach {
                    val row = it.split(',')
                    val time = sdf.parse(row[0])

                    if ((time != null) && (time > startTime)) {
                        if ((decodeSuccessCounter % 1000) == 0) System.out.println(row[0])
                        val byteArray = row[1].hexToByteArray()
                        decodeSuccessCounter++
                        if (adapter.decode(byteArray)) {

                        }
                        if (time > stopTime) return@lin
                    }
                }
            }
        }

        // Act.

        // Assert.
        //assertThat(decodeSuccessCounter).isAtLeast((dataList.size * 0.15).toInt()) // more 15%
        assertThat(data.batteryLevel).isAnyOf(57, 44)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.voltageDouble).isEqualTo(74.43)
        assertThat(data.angle).isLessThan(-0.04)
        assertThat(data.roll).isLessThan(-8)
        assertThat(data.speed).isEqualTo(0)
        assertThat(data.current).isEqualTo(0)
        assertThat(data.modeStr).isEqualTo("Drive")
    }
*/
}
