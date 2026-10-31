package com.cooper.wheellog.voice

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class VoiceDistanceSettingsTest {
    private lateinit var context: Context
    private lateinit var prefs: VoiceSettings
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("voice_announcements_v1", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = VoiceSettings(context)
    }
    private fun flush() { shadowOf(Looper.getMainLooper()).idle() }

    @Test fun `new interval is five kilometres but an explicitly saved interval survives reopening`() {
        assertEquals(5000L, prefs.distanceReports("wheel").intervalMetres)
        assertEquals(5000L, DistanceReports().intervalMetres)
        prefs.setDistanceInterval("wheel", 2000)
        assertEquals(2000L, VoiceSettings(context).distanceReports("WHEEL").intervalMetres)
        assertEquals(5000L, prefs.distanceReports("another").intervalMetres)
    }

    @Test fun `all three switches publish both states without a wheel or telemetry`() {
        var displayed = DistanceReports()
        val stop = prefs.observeDistanceReports("wheel") { displayed = it }
        prefs.setTargetMetres("wheel", 11111000)
        prefs.setPeriodicDistance("wheel", true); flush()
        assertTrue(displayed.periodic)
        prefs.setAfterDistance("wheel", true, null); flush()
        assertTrue(displayed.after)
        assertNull(displayed.originMetres)
        prefs.setTargetDistance("wheel", true); flush()
        assertTrue(displayed.target)
        assertEquals(11111000L, displayed.targetMetres)
        prefs.setPeriodicDistance("wheel", false)
        prefs.setAfterDistance("wheel", false, null)
        prefs.setTargetDistance("wheel", false); flush()
        assertFalse(displayed.periodic || displayed.after || displayed.target)
        assertEquals(displayed, VoiceSettings(context).distanceReports("wheel"))
        stop()
    }

    @Test fun `service completion updates the open screen without another telemetry sample`() {
        prefs.setPeriodicDistance("wheel", true)
        prefs.setAfterDistance("wheel", true, 100000)
        prefs.setTargetMetres("wheel", 105000)
        prefs.setTargetDistance("wheel", true); flush()
        var displayed = DistanceReports()
        val stop = prefs.observeDistanceReports("wheel") { displayed = it }
        assertTrue(displayed.after && displayed.target)
        VoiceSettings(context).finishDistanceReports("wheel", after = true, target = true); flush()
        assertFalse(displayed.after || displayed.target)
        assertTrue(displayed.periodic)
        assertEquals(105000L, displayed.targetMetres)
        stop()
    }

    @Test fun `observation follows only the selected wheel and detaches when settings close`() {
        var updates = 0
        var displayed = DistanceReports()
        val stop = prefs.observeDistanceReports("a") { updates++; displayed = it }
        val initial = updates
        prefs.setPeriodicDistance("ab", true); flush()
        assertEquals(initial, updates)
        prefs.setPeriodicDistance("A", true); flush()
        assertTrue(displayed.periodic)
        assertTrue(updates > initial)
        stop()
        val closed = updates
        prefs.setPeriodicDistance("a", false); flush()
        assertEquals(closed, updates)
        assertTrue(displayed.periodic)
        assertFalse(prefs.distanceReports("a").periodic)
    }

    @Test fun `offline activation remains armed and its origin appears on the first fresh sample`() {
        var displayed = DistanceReports()
        val stop = prefs.observeDistanceReports("wheel") { displayed = it }
        prefs.setAfterDistance("wheel", true, null); flush()
        assertTrue(displayed.after)
        assertNull(displayed.originMetres)
        VoiceSettings(context).initializeDistance("wheel", 10414001, true); flush()
        assertTrue(displayed.after)
        assertEquals(10414001L, displayed.originMetres)
        assertEquals(10414001L, displayed.targetMetres)
        stop()
    }
}
