package com.cooper.wheellog

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.ParcelUuid
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config


@Config(sdk = [30, 33])
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    lateinit var controller: ActivityController<MainActivity>
    lateinit var activity: MainActivity

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .visible()
        activity = controller.get()
    }

    @Test
    fun `just launch`() {
        // Arrange.
        val shadowActivity = Shadows.shadowOf(activity)

        // Act.
        // Assert.
        assertThat(WheelData.getInstance()).isNotNull()
        assertThat(activity.mMenu.hasVisibleItems()).isEqualTo(true)
        assertThat(activity.pager.adapter).isNotNull()
        assertThat(activity.pager.adapter!!.itemCount).isEqualTo(4)
        assertThat(shadowActivity.nextStartedActivity.action).isEqualTo(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        assertThat(shadowActivity.nextStartedActivity.action).isEqualTo("android.content.pm.action.REQUEST_PERMISSIONS")
    }

    @Test
    fun `click on wheel menu`() {
        // Arrange.
        val shadowActivity = Shadows.shadowOf(activity)

        val bluetoothAdapter =
            (activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val shadowBle = Shadows.shadowOf(bluetoothAdapter)
        shadowBle.setState(BluetoothAdapter.STATE_ON)
        val MOCK_MAC_ADDRESS = "00:11:22:33:AA:BB"
        val device = bluetoothAdapter.getRemoteDevice(MOCK_MAC_ADDRESS)
        val uuids = arrayOf(
            ParcelUuid.fromString("00000000-1111-2222-3333-000000000011"),
            ParcelUuid.fromString("00000000-1111-2222-3333-0000000000aa")
        )

        Shadows.shadowOf(device).setUuids(uuids)
                

        WheelData.getInstance().bluetoothService = mockkClass(BluetoothService::class, relaxed = true)

        // Act.
        shadowActivity.clickMenuItem(R.id.miWheel)

        // Assert.
        verify (exactly = 1) { WheelData.getInstance().bluetoothService.toggleConnectToWheel() }
    }

    @Test
    fun `click on search menu with permission - launch ScanActivity`() {
        // Arrange.
        val shadowActivity = Shadows.shadowOf(activity).apply {
            grantPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
            )
            clearNextStartedActivities()
        }

        // Act.
        shadowActivity.clickMenuItem(R.id.miSearch)

        // Assert.
        val intent = shadowActivity.nextStartedActivity
        val shadowIntent = Shadows.shadowOf(intent)
        assertThat(ScanActivity::class.java).isEqualTo(shadowIntent.intentClass)
    }

    @Test
    fun `click on search menu with deny permission - launch ScanActivity - expect not launch`() {
        // Arrange.
        val shadowActivity = Shadows.shadowOf(activity).apply {
            grantPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            denyPermissions(Manifest.permission.BLUETOOTH)
            clearNextStartedActivities()
        }

        // Act.
        shadowActivity.clickMenuItem(R.id.miSearch)

        // Assert.
        assertThat(shadowActivity.nextStartedActivity).isNull()
    }

    @Test
    fun `click on settings menu - launch SettingsActivity`() {
        // Arrange.
        val shadowActivity = Shadows.shadowOf(activity)

        // Act.
        shadowActivity.clickMenuItem(R.id.miSettings)

        // Assert.
        val intent = shadowActivity.nextStartedActivity
        val shadowIntent = Shadows.shadowOf(intent)
        assertThat(SettingsActivity::class.java).isEqualTo(shadowIntent.intentClass)
    }

    // All the tests below will fail during the setup phase,
    // because the onDestroy method in the MainActivity kills the entire application.
    @Test
    @Ignore
    fun `full lifecycle`() {
        // Arrange.
        // Act.
        controller
            .pause()
            .resume()
            .destroy()

        // Assert.
        assertThat(activity.isDestroyed).isTrue()
    }
}