package com.cooper.wheellog.utils

import android.app.Application
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class PaletteWheelIconTest {
    @Test fun `search blinks using independent frames and connected or stopped states are steady`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (palette in listOf(ColorPalette.Contrast, ColorPalette.Inferno)) {
            val search = PaletteWheelIcon.create(context, palette, connected = false, searching = true) as AnimationDrawable
            assertEquals(2, search.numberOfFrames)
            assertFalse(search.isOneShot)
            assertEquals(500, search.getDuration(0))
            assertEquals(500, search.getDuration(1))
            assertNotSame(search.getFrame(0), search.getFrame(1))
            assertNotSame(search.getFrame(0).constantState, search.getFrame(1).constantState)
            assertFalse(PaletteWheelIcon.create(context, palette, connected = true, searching = true) is AnimationDrawable)
            assertFalse(PaletteWheelIcon.create(context, palette, connected = false, searching = false) is AnimationDrawable)
        }
    }
}
