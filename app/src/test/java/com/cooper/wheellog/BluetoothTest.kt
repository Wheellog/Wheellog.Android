package com.cooper.wheellog

import android.bluetooth.*
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_SECONDARY
import android.content.Context
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import androidx.test.core.app.ApplicationProvider
import android.os.Build
import org.robolectric.annotation.Config
import com.google.common.truth.Truth.assertThat
import android.os.ParcelUuid
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@RunWith(RobolectricTestRunner::class)
class BluetoothTest {
    private lateinit var bleService: BluetoothLeService

    private lateinit var device: BluetoothDevice
    private lateinit var context: Context
    private val deviceMac1 = "00:11:22:AA:BB:CC"

    private lateinit var data: WheelData

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        device = adapter.getRemoteDevice(deviceMac1)
        bleService = spyk(BluetoothLeService())
        every { bleService.applicationContext } returns context
        justRun { bleService.sendBroadcast(any()) }
        data = spyk(WheelData())
        every { data.bluetoothLeService.applicationContext } returns context
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
        data.bluetoothLeService = bleService
        WheelLog.AppConfig = mockkClass(AppConfig::class, relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `ble initialize`() {
        // Arrange.
        // Act.
        val result = bleService.initialize()

        // Assert.
        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `ble connecting`() {
        // Arrange.
        bleService.initialize()
        bleService.setDeviceAddress(deviceMac1)

        // Act.
        val result = bleService.connect()

        // Assert.
        assertThat(result).isEqualTo(true)
        assertThat(bleService.connectionState).isEqualTo(BluetoothLeService.STATE_CONNECTING)
        verify(exactly = 1) { bleService.sendBroadcast(any()) }
    }

    @Test
    fun `ble connect`() {
        // Arrange.
        bleService.initialize()
        bleService.setDeviceAddress(deviceMac1)

        // Act.
        bleService.connect()
        val gatt = getPrivateField<BluetoothGatt>("mBluetoothGatt")
        val gattCallback = getPrivateField<BluetoothGattCallback>("mGattCallback")
        gattCallback.onConnectionStateChange(gatt, 4, BluetoothLeService.STATE_CONNECTED)

        // Assert.
        assertThat(bleService.connectionState).isEqualTo(BluetoothLeService.STATE_CONNECTING)
        verify(exactly = 1) { bleService.sendBroadcast(any()) }
    }

    private fun <T> getPrivateField(name: String): T {
        BluetoothLeService::class.memberProperties.find { it.name == name }!!.apply {
            isAccessible = true
            return get(bleService) as T
        }
    }
}