package com.cooper.wheellog.utils

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.Utils.Companion.hexToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test


class BleConnectorTest {
    private lateinit var data: WheelData
    private lateinit var connector: BleConnector
    private lateinit var context: Context
    private lateinit var bleManager: BluetoothManager

    @Before
    fun setUp() {
        data = spyk(WheelData())
        context = mockk(relaxed = true)

        mockkStatic(Toast::class, BleConnector::class, WheelData::class)
        mockkConstructor(Intent::class)

        WheelLog.AppConfig = mockk(relaxed = true)
        bleManager = mockk(relaxed = true)
        every { bleManager.adapter.isEnabled } returns true
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bleManager
        every { anyConstructed<Intent>().putExtra(any(), any<Int>()) } returns Intent()
        every { WheelData.getInstance() } returns data

        connector = spyk(BleConnector(context))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `toggleConnectToWheel when Disconnected`() {
        // Arrange.
        // Act.
        connector.toggleConnectToWheel()

        // Assert.
        verify { connector.connect() }
        assertThat(connector.connectionState).isEqualTo(BleStateEnum.Disconnected)
    }

    @Test
    fun `toggleConnectToWheel when not Disconnected`() {
        // Arrange.
        every { connector.connectionState } returns BleStateEnum.Connected

        // Act.
        connector.toggleConnectToWheel()

        // Assert.
        verify { connector.disconnect() }
    }

    @Test
    fun `connect without device address - expected Disconnected`() {
        // Arrange.
        // Act.
        val coldConnect = connector.connect()

        // Assert.
        assertThat(connector.connectionState).isEqualTo(BleStateEnum.Disconnected)
        assertThat(coldConnect).isFalse()
    }

    @Test
    fun `connect with device address - expected Connecting`() {
        // Arrange.
        connector.deviceAddress = "123"

        // Act.
        val coldConnect = connector.connect()

        // Assert.
        assertThat(connector.connectionState).isEqualTo(BleStateEnum.Connecting)
        assertThat(coldConnect).isTrue()
        verify { connector.connectionState }
    }

    @Test
    fun `writeBluetoothGattCharacteristic`() {
        // Arrange.
        every { data.wheelType } returns Constants.WHEEL_TYPE.KINGSONG
        val gatt: BluetoothGatt = mockk(relaxed = true)
        connector.mBluetoothGatt = gatt
        every { gatt.writeCharacteristic(any()) } returns true

        // Act.
        val result = connector.writeBluetoothGattCharacteristic(byteArrayOf(0x00))

        // Assert.
        assertThat(result).isTrue()
    }
}