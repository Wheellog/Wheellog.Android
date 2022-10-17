package com.cooper.wheellog

import android.Manifest
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController

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
        // Act.
        // Assert.
        assertThat(WheelData.getInstance()).isNotNull()
        assertThat(activity.mMenu.hasVisibleItems()).isEqualTo(true)
        assertThat(activity.pager.adapter).isNotNull()
        assertThat(activity.pager.adapter!!.itemCount).isEqualTo(4)
    }

    @Test
    fun `click on wheel menu`() {
        // Arrange.
        val shadowActivity = Shadows.shadowOf(activity)
        WheelData.getInstance().bluetoothService = mockkClass(BluetoothService::class, relaxed = true)

        // Act.
        shadowActivity.clickMenuItem(R.id.miWheel)

        // Assert.
        verify (exactly = 1) { WheelData.getInstance().bluetoothService.toggleConnectToWheel() }
    }

    @Test
    fun `click on search menu without permissions - request permission`() {
        // Arrange.
        val shadowActivity = Shadows.shadowOf(activity)

        // Act.
        shadowActivity.clickMenuItem(R.id.miSearch)

        // Assert.
        val intent = shadowActivity.nextStartedActivity
        assertThat("android.content.pm.action.REQUEST_PERMISSIONS").isEqualTo(intent.action)
    }

    @Test
    fun `click on search menu with permission - launch ScanActivity`() {
        // Arrange.
        val shadowActivity = Shadows.shadowOf(activity)
        shadowActivity.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        // Act.
        shadowActivity.clickMenuItem(R.id.miSearch)

        // Assert.
        val intent = shadowActivity.nextStartedActivity
        val shadowIntent = Shadows.shadowOf(intent)
        assertThat(ScanActivity::class.java).isEqualTo(shadowIntent.intentClass)
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