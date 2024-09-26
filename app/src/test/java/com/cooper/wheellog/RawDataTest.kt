package com.cooper.wheellog

import android.content.Context
import com.cooper.wheellog.utils.*
import com.cooper.wheellog.utils.Utils.Companion.hexToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat

class RawDataTest {
    private lateinit var data: WheelData
    private lateinit var config: AppConfig
    private val sdf = SimpleDateFormat("HH:mm:ss.SSS")

    @Before
    fun setUp() {
        mockkObject(WheelLog)
        every { WheelLog.appContext } returns mockkClass(Context::class, relaxed = true)
        config = mockkClass(AppConfig::class, relaxed = true)
        WheelLog.AppConfig = config
        data = spyk(WheelData())
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
        every { config.gotwayNegative } returns "1"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GW - decode with normal data`() {
        // Arrange.
        every { config.gotwayNegative } returns "1"
        mockkConstructor(android.os.Handler::class)
        every { anyConstructed<android.os.Handler>().postDelayed(any(), any()) } returns true
        val adapter = GotwayAdapter()
        data.wheelType = Constants.WHEEL_TYPE.GOTWAY
        val inputStream: InputStream = File("src/test/resources/rawDecodeTest.csv").inputStream()
        val startTime = sdf.parse("11:50:50.123")

        val dataList = mutableListOf<String>()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                val row = it.split(',')
                val time = sdf.parse(row[0])
                if (time != null && time > startTime) {
                    dataList.add(row[1])
                }
            }
        }

        // Act.
        dataList.forEach {
            val byteArray = it.hexToByteArray()
            adapter.decode(byteArray)
        }

        // Assert.
        assertThat(data.temperature).isEqualTo(24)
        assertThat(data.voltageDouble).isEqualTo(65.93)
        assertThat(data.phaseCurrentDouble).isEqualTo(1.4)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.totalDistance).isEqualTo(24786)
        assertThat(data.batteryLevel).isEqualTo(100)
    }

    @Test
    fun `Inmotion v5f - decode after connect`() {
        // Arrange.
        val adapter = InMotionAdapter()
        data.wheelType = Constants.WHEEL_TYPE.INMOTION
        val inputStream: InputStream = File("src/test/resources/RAW_inmotion_V5F.csv").inputStream()
        val startTime = sdf.parse("17:15:05.651")

        val dataList = mutableListOf<String>()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                val row = it.split(',')
                val time = sdf.parse(row[0])
                if (time != null && time > startTime) {
                    dataList.add(row[1])
                }
            }
        }

        // Act.
        var decodeSuccessCounter = 0
        dataList.forEach {
            val byteArray = it.hexToByteArray()
            if (adapter.decode(byteArray)) {
                decodeSuccessCounter++
            }
        }

        // Assert.
        assertThat(decodeSuccessCounter).isAtLeast((dataList.size * 0.15).toInt()) // more 15%
        assertThat(data.batteryLevel).isAnyOf(57, 44)
        assertThat(data.temperature).isEqualTo(28)
        assertThat(data.voltageDouble).isEqualTo(74.43)
        assertThat(data.angle).isLessThan(-0.04)
        assertThat(data.roll).isLessThan(-8)
        assertThat(data.speed).isEqualTo(0)
        assertThat(data.current).isEqualTo(0)
        assertThat(data.modeStr).isEqualTo("Drive")
    }

    @Test
    fun `Inmotion - alerts`() {
        // Arrange.
        val adapter = InMotionAdapter()
        data.wheelType = Constants.WHEEL_TYPE.INMOTION
        val inputStream: InputStream = File("src/test/resources/RAW_inmotion_alerts.csv").inputStream()

        val dataList = mutableListOf<String>()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                val row = it.split(',')
                dataList.add(row[1])
            }
        }

        // Act.
        var decodeSuccessCounter = 0
        dataList.forEach {
            val byteArray = it.hexToByteArray()
            if (adapter.decode(byteArray)) {
                decodeSuccessCounter++
            }
        }

        // Assert.
        //assertThat(data.alert).isEqualTo("")
    }

    @Test
    fun `Inmotion v8s - decode after connect`() {
        // Arrange.
        val adapter = InMotionAdapter()
        data.wheelType = Constants.WHEEL_TYPE.INMOTION
        val inputStream: InputStream = File("src/test/resources/RAW_inmotion_V8S.csv").inputStream()
        val startTime = sdf.parse("13:10:19.699")

        val dataList = mutableListOf<String>()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                val row = it.split(',')
                val time = sdf.parse(row[0])
                if (time != null && time > startTime) {
                    dataList.add(row[1])
                }
            }
        }

        // Act.
        dataList.forEach {
            val byteArray = it.hexToByteArray()
            adapter.decode(byteArray)
        }

        // Assert.
        assertThat(data.model).isEqualTo(InMotionAdapter.getModelString(InMotionAdapter.Model.V8S))
        assertThat(data.batteryLevel).isEqualTo(96)
        assertThat(data.temperature).isEqualTo(30)
        assertThat(data.temperature2).isEqualTo(-109) // Fix me
        assertThat(data.voltageDouble).isEqualTo(81.99)
        assertThat(data.angle).isLessThan(-0.07)
        assertThat(data.roll).isEqualTo(0)
        assertThat(data.speed).isEqualTo(0)
        assertThat(data.current).isEqualTo(8)
        assertThat(data.modeStr).isEqualTo("Drive")
    }
    /*
    @Test
    fun `Begode PWM`() {
        // Arrange.
        every { config.gotwayNegative } returns "1"
        mockkConstructor(android.os.Handler::class)
        every { anyConstructed<android.os.Handler>().postDelayed(any(), any()) } returns true
        val adapter = GotwayAdapter()
        data.wheelType = Constants.WHEEL_TYPE.GOTWAY
        val inputStream: InputStream = File("src/test/resources/RAW2.csv").inputStream()
        val startTime = sdf.parse("10:29:00.000")

        val dataList = mutableListOf<String>()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                val row = it.split(',')
                val time = sdf.parse(row[0])
                if (time != null && time > startTime) {
                    dataList.add(row[1])
                }
            }
        }

        // Act.
        dataList.forEach {
            val byteArray = it.hexToByteArray()
            adapter.decode(byteArray)
        }

        // Assert.
        assertThat(data.model).isEqualTo("bEGODE")


    }




    @Test
    fun `Inmotion v11y`() {
        // Arrange.
        val adapter = InmotionAdapterV2()
        data.wheelType = Constants.WHEEL_TYPE.INMOTION_V2
        val inputStream: InputStream = File("src/test/resources/RAW1.csv").inputStream()
        val startTime = sdf.parse("00:14:24.310")

        val dataList = mutableListOf<String>()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                val row = it.split(',')
                val time = sdf.parse(row[0])
                if (time != null && time > startTime) {
                    dataList.add(row[1])
                }
            }
        }

        // Act.
        dataList.forEach {
            val byteArray = it.hexToByteArray()
            adapter.decode(byteArray)
        }

        // Assert.
        assertThat(data.model).isEqualTo(InmotionAdapterV2.Model.V11Y.name)
        assertThat(data.batteryLevel).isEqualTo(96)

    }


    @Test
    fun `Inmotion v14`() {
        // Arrange.
        val adapter = InmotionAdapterV2()
        data.wheelType = Constants.WHEEL_TYPE.INMOTION_V2
        adapter.model = InmotionAdapterV2.Model.V14
        val inputStream: InputStream = File("src/test/resources/RAW.csv").inputStream()
        val startTime = sdf.parse("19:40:23.576")

        val dataList = mutableListOf<String>()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                val row = it.split(',')
                val time = sdf.parse(row[0])
                if (time != null && time > startTime) {
                    dataList.add(row[1])
                }
            }
        }

        // Act.
        dataList.forEach {
            val byteArray = it.hexToByteArray()
            adapter.decode(byteArray)
        }

        // Assert.
        assertThat(data.model).isEqualTo(InmotionAdapterV2.Model.V14.name)
        assertThat(data.batteryLevel).isEqualTo(96)

    }



    @Test
    fun `LynxBMS`() {
        // Arrange.
        val adapter = VeteranAdapter()
        data.wheelType = Constants.WHEEL_TYPE.VETERAN
        val inputStream: InputStream = File("src/test/resources/RAW_2024_09_11_16_39_31.csv").inputStream()
        val startTime = sdf.parse("09:51:02.214")

        val dataList = mutableListOf<String>()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                val row = it.split(',')
                val time = sdf.parse(row[0])
                if (time != null && time > startTime) {
                    dataList.add(row[1])
                }
            }
        }

        // Act.
        dataList.forEach {
            val byteArray = it.hexToByteArray()
            adapter.decode(byteArray)
        }

        // Assert.
        assertThat(data.version).isEqualTo("005.0.11")


    }

*/

}
