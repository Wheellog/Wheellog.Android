package com.cooper.wheellog.map

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class StatsAppearanceTest {
    private fun contexts(): List<Context> {
        val base: Context = ApplicationProvider.getApplicationContext()
        return listOf(R.style.OriginalTheme, R.style.AJDMTheme, R.style.ContrastTheme, R.style.InfernoTheme,
            R.style.PorcelainTheme, R.style.PolarTheme, R.style.JadeTheme, R.style.SteelTheme, R.style.HeatherTheme).flatMap { style ->
            listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES).map { night ->
                val configuration = Configuration(base.resources.configuration).apply { uiMode = night }
                ContextThemeWrapper(base.createConfigurationContext(configuration), style)
            }
        }
    }

    @Test fun `chart and marker labels contrast with their actual surfaces`() {
        for (context in contexts()) {
            assertTrue(ColorUtils.calculateContrast(context.statsTextColor(), context.statsBackgroundColor()) >= 4.5)
            val marker = marker(context)
            marker.refreshContent(Entry(10f, 58.92f), Highlight(10f, 58.92f, 0))
            for (id in listOf(R.id.textView_Title, R.id.textView_Data)) {
                val text = marker.findViewById<TextView>(id)
                assertTrue(ColorUtils.calculateContrast(text.currentTextColor, actualBackground(text)) >= 4.5)
            }
        }
    }

    private fun actualBackground(view: View): Int {
        val color = when (val background = view.background) {
            is ColorDrawable -> background.color
            is GradientDrawable -> background.color?.defaultColor
            else -> null
        }
        if (color != null && Color.alpha(color) == 255) return color
        return actualBackground(view.parent as View)
    }

    @Test fun `split series show one popup row per metric and find the selected later session`() {
        val context = contexts().first()
        val morning = LineDataSet(listOf(Entry(10f, 20f)), "PWM")
        val lunch = LineDataSet(listOf(Entry(50f, 82.29f)), "PWM")
        val marker = ChartMarkerView(context, object : ValueFormatter() {}, listOf(morning, lunch))
        marker.refreshContent(Entry(50f, 82.29f), Highlight(50f, 82.29f, 1))
        val text = marker.findViewById<TextView>(R.id.textView_Data).text.toString()
        assertEquals(1, Regex("PWM").findAll(text).count())
        assertTrue(text.contains("82"))
        assertFalse(text.contains("20"))
        marker.refreshContent(Entry(30f, 0f), Highlight(30f, 0f, 0))
        assertTrue(marker.findViewById<TextView>(R.id.textView_Data).text.toString().contains("PWM: —"))
    }

    @Test fun `all seven metric lines are visible in both built in themes`() {
        for (context in contexts()) for (metric in listOf(R.color.stats_battery, R.color.stats_speed,
            R.color.stats_temp, R.color.stats_pwm, R.color.stats_voltage, R.color.stats_power, R.color.stats_current)) {
            assertTrue(ColorUtils.calculateContrast(context.statSeriesColor(metric), context.statsBackgroundColor()) >= 3.0)
        }
    }

    @Test fun `actual chart axes legend and empty message are neutral and readable in all themes`() {
        for (context in contexts()) {
            val chart = LineChart(context)
            styleStatsChart(chart, object : ValueFormatter() { override fun getFormattedValue(value: Float) = "12:34:56" })
            val foregrounds = listOf(chart.axisLeft.textColor, chart.axisRight.textColor,
                chart.xAxis.textColor, chart.legend.textColor,
                chart.getPaint(com.github.mikephil.charting.charts.Chart.PAINT_INFO).color)
            foregrounds.forEach { color ->
                assertEquals(Color.red(color), Color.green(color))
                assertEquals(Color.green(color), Color.blue(color))
                assertTrue(ColorUtils.calculateContrast(color, context.statsBackgroundColor()) >= 4.5)
            }
            assertTrue(chart.xAxis.labelCount <= 4)
            assertTrue(chart.xAxis.isAvoidFirstLastClippingEnabled)
            assertTrue(chart.xAxis.textSize >= 13f)
        }
    }

    @Test fun `missing readings are not displayed as zero and labels remain literal`() {
        val marker = marker(contexts().first())
        marker.refreshContent(Entry(10f, 0f), Highlight(10f, 0f, 0))
        val text = marker.findViewById<TextView>(R.id.textView_Data).text.toString()
        assertTrue(text.contains("PWM <test>: 0"))
        assertTrue(text.contains("Voltage: —"))
        assertEquals("12:57:51", marker.findViewById<TextView>(R.id.textView_Title).text.toString())
    }

    @Test fun `marker fits within the chart at both horizontal edges`() {
        val context = contexts().first()
        val chart = LineChart(context).apply { layout(0, 0, 320, 400) }
        val marker = marker(context).apply { chartView = chart }
        marker.refreshContent(Entry(10f, 0f), Highlight(10f, 0f, 0))
        for (x in listOf(0f, 320f)) {
            val offset = marker.getOffsetForDrawingAtPoint(x, 200f)
            assertTrue(x + offset.x >= 0f)
            assertTrue(x + offset.x + marker.width <= chart.width)
        }
    }

    private fun marker(context: Context): ChartMarkerView {
        val pwm = LineDataSet(listOf(Entry(10f, 0f)), "PWM <test>").apply { color = Color.GRAY }
        val voltage = LineDataSet(listOf(Entry(5f, 12f)), "Voltage")
        return ChartMarkerView(context, object : ValueFormatter() {
            override fun getFormattedValue(value: Float) = "12:57:51"
        }, listOf(pwm, voltage))
    }
}
