package com.cooper.wheellog.track

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.*
import com.cooper.wheellog.map.*
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class TrackStatsReadabilityTest {
    private lateinit var base: Context
    private lateinit var config: AppConfig
    @Before fun setup() {
        base = ApplicationProvider.getApplicationContext()
        config = AppConfig(base)
        startKoin { modules(module { single { config } }) }
    }
    @After fun cleanup() { ThemeManager.theme = R.style.OriginalTheme; stopKoin() }
    @Test fun `all nine themes have readable chart labels series and actual marker surfaces day and night`() {
        for (theme in ThemeEnum.entries) for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            config.appThemeInt = theme.value
            ThemeManager.theme = config.appTheme
            val configuration = Configuration(base.resources.configuration).apply { uiMode = night }
            val context = ContextThemeWrapper(base.createConfigurationContext(configuration), config.appTheme)
            val chartBackground = context.statsBackgroundColor()
            assertTrue("axis text ${theme.name}", ColorUtils.calculateContrast(context.statsTextColor(), chartBackground) >= 4.5)
            for (metric in listOf(R.color.stats_battery, R.color.stats_speed, R.color.stats_temp, R.color.stats_pwm, R.color.stats_voltage, R.color.stats_power, R.color.stats_current)) {
                assertTrue("series ${theme.name}: $metric", ColorUtils.calculateContrast(context.statSeriesColor(metric), chartBackground) >= 3.0)
            }
            val sample = LineDataSet(listOf(Entry(10f, 58.92f)), "PWM").apply { color = context.statSeriesColor(R.color.stats_pwm) }
            val absent = LineDataSet(listOf(Entry(5f, 12f)), "Voltage")
            val marker = ChartMarkerView(context, object : ValueFormatter() { override fun getFormattedValue(value: Float) = "12:57:51" }, listOf(sample, absent))
            marker.refreshContent(Entry(10f, 58.92f), Highlight(10f, 58.92f, 0))
            val surface = (marker.background as GradientDrawable).color!!.defaultColor
            val title = marker.findViewById<TextView>(R.id.textView_Title)
            val data = marker.findViewById<TextView>(R.id.textView_Data)
            assertTrue("marker ${theme.name}", ColorUtils.calculateContrast(data.currentTextColor, surface) >= 4.5)
            assertEquals(data.currentTextColor, title.currentTextColor)
            assertTrue(data.text.toString().contains("58"))
            assertTrue(data.text.toString().contains("Voltage: —"))
            assertEquals("12:57:51", title.text.toString())
            assertTrue("marker fits mobile", marker.measuredWidth <= context.resources.displayMetrics.widthPixels)
        }
    }
    @Test fun `theme ids and selected styles persist for all palettes`() {
        val styles = listOf(R.style.OriginalTheme, R.style.AJDMTheme, R.style.ContrastTheme, R.style.InfernoTheme,
            R.style.PorcelainTheme, R.style.PolarTheme, R.style.JadeTheme, R.style.SteelTheme, R.style.HeatherTheme)
        assertEquals(9, ThemeEnum.entries.size)
        styles.forEachIndexed { id, style ->
            config.appThemeInt = id
            assertEquals(style, AppConfig(base).appTheme)
        }
    }
    @Test fun `AJDM launcher shortcut stays enabled when switching all themes`() {
        val alias = android.content.ComponentName(base, "com.cooper.wheellog.AjdmIconAlias")
        val original = android.content.ComponentName(base, "com.cooper.wheellog.OriginalIconAlias")
        val pm = base.packageManager
        pm.setComponentEnabledSetting(alias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(original, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        for (theme in ThemeEnum.entries) {
            config.appThemeInt = theme.value; ThemeManager.theme = config.appTheme
            ThemeManager.changeAppIcon(base)
            assertEquals(android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, pm.getComponentEnabledSetting(alias))
        }
    }
}
