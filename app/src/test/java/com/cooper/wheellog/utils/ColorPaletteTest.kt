package com.cooper.wheellog.utils

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class ColorPaletteTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun `saved theme ids survive upgrade and invalid values fall back`() {
        val config = AppConfig(context)
        for ((value, style) in listOf(0 to R.style.OriginalTheme, 1 to R.style.AJDMTheme,
            2 to R.style.ContrastTheme, 3 to R.style.InfernoTheme)) {
            config.appThemeInt = value
            assertEquals(style, AppConfig(context).appTheme)
        }
        config.setValue(R.string.app_theme, "unknown")
        assertEquals(R.style.OriginalTheme, config.appTheme)
        config.appThemeInt = 999
        assertEquals(R.style.OriginalTheme, config.appTheme)
    }

    @Test fun `battery threshold persists and is clamped`() {
        val config = AppConfig(context)
        assertEquals(20, config.lowBatteryColorThreshold)
        assertEquals(R.string.low_battery_color_threshold, config.getResId(context.getString(R.string.low_battery_color_threshold)))
        config.lowBatteryColorThreshold = 15
        assertEquals(15, AppConfig(context).lowBatteryColorThreshold)
        config.lowBatteryColorThreshold = -1
        assertEquals(0, config.lowBatteryColorThreshold)
        config.lowBatteryColorThreshold = 101
        assertEquals(100, config.lowBatteryColorThreshold)
    }

    @Test fun `new schemes have the same exact alert red in light and dark mode`() {
        for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            val configuration = Configuration(context.resources.configuration).apply { uiMode = night }
            val themed = context.createConfigurationContext(configuration)
            assertEquals(Color.RED, themed.getColor(R.color.palette_signal_red))
            for ((style, foreground) in listOf(R.style.ContrastTheme to "#F4F4F5", R.style.InfernoTheme to "#FF3557")) {
                val wrapper = ContextThemeWrapper(themed, style)
                val value = TypedValue()
                assertTrue(wrapper.theme.resolveAttribute(R.attr.paletteText, value, true))
                assertEquals(Color.parseColor(foreground), value.data)
                val palette = ColorPalette.forTheme(style)!!
                assertEquals(Color.parseColor(foreground), themed.getColor(palette.resourceFor(R.color.wheelview_speed_text)))
                assertEquals(Color.parseColor("#209100"), themed.getColor(palette.resourceFor(R.color.wheelview_main_negative_dial)))
                assertEquals(Color.parseColor("#FFCC00"), themed.getColor(palette.resourceFor(R.color.wheelview_battery_low_dial)))
                assertEquals(Color.parseColor("#F4FDFF"), themed.getColor(palette.resourceFor(R.color.wheelview_battery_dial)))
                assertEquals(Color.parseColor("#69777B"), themed.getColor(palette.resourceFor(R.color.wheelview_versiontext)))
            }
        }
        assertNull(ColorPalette.forTheme(R.style.OriginalTheme))
        assertNull(ColorPalette.forTheme(R.style.AJDMTheme))
    }

    @Test fun `Russian theme names have no numbering`() {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale("ru")) }
        val russian = context.createConfigurationContext(config)
        assertEquals("Стандартная", russian.getString(ThemeEnum.Original.title))
        assertEquals("Контрастная", russian.getString(ThemeEnum.Contrast.title))
        assertEquals("Инферно", russian.getString(ThemeEnum.Inferno.title))
    }
}
