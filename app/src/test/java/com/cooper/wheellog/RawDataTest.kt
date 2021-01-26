package com.cooper.wheellog

import android.content.Context
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.GotwayAdapter
import com.cooper.wheellog.utils.Utils.Companion.hexToByteArray
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class RawDataTest {
    private lateinit var data: WheelData

    @Before
    fun setUp() {
        data = spyk(WheelData())
        every { data.bluetoothLeService.applicationContext } returns mockkClass(Context::class, relaxed = true)
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GW - decode with normal data`() {
        // Arrange.
        val adapter = GotwayAdapter()
        data.wheelType = Constants.WHEEL_TYPE.GOTWAY
        val inputStream: InputStream = File("src/test/resources/rawDecodeTest.csv").inputStream()
        val sdf = SimpleDateFormat("HH:mm:ss.SSS")
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
    }
}
