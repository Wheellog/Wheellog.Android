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
import kotlin.math.abs
import kotlin.math.round

class KingsongAdapterTest {

    private lateinit var adapter: KingsongAdapter
    private lateinit var data: WheelData
    private var header = byteArrayOf(0x55, 0xAA.toByte())
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
        adapter = KingsongAdapter()
        data = spyk(WheelData())
        data.wheelType = Constants.WHEEL_TYPE.KINGSONG
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
    fun `decode Live data`() {
        // Arrange.
        val voltage = 6000.toShort()
        val speed = 111.toShort()
        val temperature = 12345.toShort()
        val distance = 1234567890
        val type = 169.toByte() // Live data
        val byteArray = header +
                MathsUtil.getBytes(voltage) +
                MathsUtil.getBytes(speed) +
                MathsUtil.getBytes(distance) +
                byteArrayOf(10, 11) +
                MathsUtil.getBytes(temperature) +
                byteArrayOf(14, 15, 16, type, 0, 0)

        // Act.
        val result = adapter.decode(MathsUtil.reverseEvery2(byteArray))

        // Assert.
        assertThat(result).isTrue()
        assertThat(data.voltageDouble).isEqualTo(voltage / 100.0)
        val speedInKm = round(speed / 10.0).toInt()
        assertThat(abs(data.speed)).isEqualTo(speedInKm)
        assertThat(data.temperature).isEqualTo(temperature / 100)
        assertThat(data.temperature2).isEqualTo(0)
        assertThat(data.totalDistanceDouble).isEqualTo(distance / 1000.0)
        assertThat(data.batteryLevel).isEqualTo(62)
    }

    @Test
    fun `decode Distance - Time - Fan Data`() {
        // Arrange.
        val topSpeed = 30000.toShort()
        val distance = 1234567890
        val type = 185.toByte() // Distance|Time|Fan Data
        val fanStatus = 321.toByte()
        val byteArray = header +
                MathsUtil.getBytes(distance) +
                byteArrayOf(6, 7) +
                MathsUtil.getBytes(topSpeed) +
                byteArrayOf(10, 11, 12, fanStatus, 14, 15, 16, type, 0, 0)

        // Act.
        val result = adapter.decode(MathsUtil.reverseEvery2(byteArray))

        // Assert.
        assertThat(result).isFalse()
        assertThat(data.wheelDistanceDouble).isEqualTo(distance / 1000.0)
        assertThat(data.topSpeedDouble).isEqualTo(topSpeed / 100.0)
        assertThat(data.fanStatus).isEqualTo(fanStatus)
    }

    @Test
    fun `decode Name and Model data`() {
        // Arrange.
        val type = 187.toByte() // Name and Type data
        val name = "Super-Wheel12"
        val model = name.split("-")[0]
        val byteArray = header +
                MathsUtil.reverseEvery2(byteArrayOf(2) + name.toByteArray(Charsets.UTF_8)) +
                byteArrayOf(16) +
                byteArrayOf(type, 0, 0)

        // Act.
        val result = adapter.decode(MathsUtil.reverseEvery2(byteArray))

        // Assert.
        assertThat(result).isFalse()
        assertThat(data.name).isEqualTo(name)
        assertThat(data.model).isEqualTo(model)
    }

    @Test
    fun `decode Serial number`() {
        // Arrange.
        val type = 179.toByte() // Name and Type data
        val serial = "King1234567890123"
        val serialBytes = serial.toByteArray(Charsets.UTF_8)
        val byteArray = MathsUtil.reverseEvery2(header) +
                serialBytes.copyOfRange(0, 14) +
                type +
                serialBytes.copyOfRange(14, serialBytes.size)

        // Act.
        val result = adapter.decode(byteArray)

        // Assert.
        assertThat(result).isFalse()
        assertThat(data.serial.trimEnd('\u0000')).isEqualTo(serial)
    }

    @Test
    fun `decode max speed and alerts`() {
        // Arrange.
        val type = 181.toByte() // Name and Type data
        val byteArray = MathsUtil.reverseEvery2(header) +
                byteArrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, type, 17, 18, 19)

        // Act.
        val result = adapter.decode(byteArray)

        // Assert.
        assertThat(result).isTrue()
    }

    @Test
    fun `decode real data 1`() {
        // Arrange.
        val byteArray1 = "aa554b532d5331382d30323035000000bb1484fd".hexToByteArray() //model name
        val byteArray2 = "aa556919030200009f36d700140500e0a9145a5a".hexToByteArray() //life data
        val byteArray3 = "aa550000090017011502140100004006b9145a5a".hexToByteArray() // dist/fan/time
        val byteArray4 = "aa55000000000000000000000000400cf5145a5a".hexToByteArray() // cpu load
        val byteArray5 = "aa55850c010000000000000016000000f6145a5a".hexToByteArray() // output

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)
        val result3 = adapter.decode(byteArray3)
        val result4 = adapter.decode(byteArray4)
        val result5 = adapter.decode(byteArray5)

        // Assert.
        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
        assertThat(result3).isFalse()
        assertThat(result4).isFalse()
        assertThat(result5).isFalse()
        // 1st data
        assertThat(data.name).isEqualTo("KS-S18-0205")
        assertThat(data.model).isEqualTo("KS-S18")
        assertThat(data.version).isEqualTo("2.05")
        //2nd data
        assertThat(data.speedDouble).isEqualTo(5.15)
        assertThat(data.temperature).isEqualTo(13)
        assertThat(data.voltageDouble).isEqualTo(65.05)
        assertThat(data.currentDouble).isEqualTo(2.15)
        assertThat(data.totalDistance).isEqualTo(13983)
        assertThat(data.batteryLevel).isEqualTo(12)
        assertThat(data.modeStr).isEqualTo("0")

        //3rd data
        assertThat(data.temperature2).isEqualTo(16)
        assertThat(data.fanStatus).isEqualTo(0)
        assertThat(data.chargingStatus).isEqualTo(0)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.009)
        //4th data
        assertThat(data.cpuLoad).isEqualTo(64)
        assertThat(data.output).isEqualTo(12)

        //5th data
        assertThat(data.speedLimit).isEqualTo(32.05) //limit speed
    }

    @Test
    fun `update pedals mode`() {
        // Arrange.

        // Act.
        adapter.updatePedalsMode(0)
        adapter.updatePedalsMode(1)
        adapter.updatePedalsMode(2)

        // Assert.
        verify(atLeast = 3) { data.bluetoothCmd(any()) }
    }
}
