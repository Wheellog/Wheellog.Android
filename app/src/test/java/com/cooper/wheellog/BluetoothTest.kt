package com.cooper.wheellog

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.os.ParcelUuid
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.welie.blessed.ConnectionState
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@Config(sdk = [30, 33])
@RunWith(RobolectricTestRunner::class)
class BluetoothTest {

    private val mockMacAddress = "00:11:22:33:AA:BB"
    private val appContext: Application by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private val shadowApp: ShadowApplication by lazy {
        shadowOf(appContext)
    }
    private val activity: MainActivity by lazy {
        Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .visible()
            .get()
    }
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }
    private val service = spyk(BluetoothService())

    @Before
    fun setUp() {
        val localBinder = mockkClass(BluetoothService.LocalBinder::class, relaxed = true)
        every { localBinder.getService() } returns service
        justRun { service.sendBroadcast(any()) }
        shadowApp.setComponentNameAndServiceForBindService(
            ComponentName(appContext.packageName, BluetoothService::class.java.name), localBinder
        )
    }

    @Test
    fun `fill service after launch`() {
        // Arrange.
        shadowApp.grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        shadowOf(bluetoothAdapter).setState(BluetoothAdapter.STATE_ON)

        // Act.
        activity

        // Assert.
        assertThat(WheelData.getInstance().bluetoothService).isNotNull()
    }

    @Test
    fun `fast connect`() {
        // Arrange.
        shadowApp.grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        shadowOf(bluetoothAdapter).setState(BluetoothAdapter.STATE_ON)
        val device = bluetoothAdapter.getRemoteDevice(mockMacAddress)
        val uuids = arrayOf(
            ParcelUuid.fromString("00000000-1111-2222-3333-000000000011"),
            ParcelUuid.fromString("00000000-1111-2222-3333-0000000000aa")
        )
        shadowOf(device).setUuids(uuids)
        service.wheelAddress = mockMacAddress

        // Act.
        activity
        service.toggleConnectToWheel()

        // Assert.
        verify(atLeast = 1) { service.sendBroadcast(any()) }
        assertThat(service.isWheelSearch).isEqualTo(true)
        assertThat(service.connectionState).isEqualTo(ConnectionState.DISCONNECTED)
    }
}