package com.cooper.wheellog.utils


import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.BluetoothLeService
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.Utils.Companion.hexToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.math.abs


class GotwayVirtualAdapterTest {

    private var adapter: GotwayVirtualAdapter = GotwayVirtualAdapter()
    private lateinit var data: WheelData
    private lateinit var mBluetoothLeService: BluetoothLeService

    @Before
    fun setUp() {
        data = spyk(WheelData())
        mBluetoothLeService = spyk(BluetoothLeService())
        data.wheelType = Constants.WHEEL_TYPE.GOTWAY_VIRTUAL
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `switch to gotway and decode`() {
        // Arrange.
        val byteArray1 = "55AA19C1000000000000008CF0000001FFF80018".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)

        // Assert.
        assertThat(data.wheelType).isEqualTo(Constants.WHEEL_TYPE.GOTWAY)
        assertThat(result1).isTrue()
        assertThat(abs(data.speed)).isEqualTo(0)
        assertThat(data.temperature).isEqualTo(24)
        assertThat(data.voltageDouble).isEqualTo(65.93)
        assertThat(data.phaseCurrentDouble).isEqualTo(1.4)
        assertThat(data.wheelDistanceDouble).isEqualTo(0.0)
        assertThat(data.batteryLevel).isEqualTo(100)
    }

    @Test
    fun `switch to veteran`() {
        // Arrange.
        val byteArray1 = "DC5A5C20238A0112121A00004D450005064611F2".hexToByteArray()
        val byteArray2 = "0E1000000AF00AF0041B000300000000".hexToByteArray()

        // Act.
        val result1 = adapter.decode(byteArray1)
        val result2 = adapter.decode(byteArray2)

        // Assert.
        assertThat(data.wheelType).isEqualTo(Constants.WHEEL_TYPE.VETERAN)
    }


}
