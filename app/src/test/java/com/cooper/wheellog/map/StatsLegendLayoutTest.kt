package com.cooper.wheellog.map

import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Utils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [29])
class StatsLegendLayoutTest {
    private val languages = listOf(
        listOf("PWM", "Напряжение", "Мощность", "Ток"),
        listOf("PWM", "Voltage", "Power", "Current")
    )

    @Test fun `split daily sessions measure the actual legend labels on first display`() {
        for (labels in languages) for (width in listOf(320, 590, 800)) {
            val chart = chart(width)
            assertTrue(bindStatsChartData(chart, series(labels, 3)))
            assertLegend(chart, labels)
        }
    }

    @Test fun `returning to statistics and changing session counts does not require rotation`() {
        for (labels in languages) {
            val chart = chart(320)
            for (sessions in listOf(1, 4, 2, 6)) {
                assertTrue(bindStatsChartData(chart, series(labels, sessions)))
                assertLegend(chart, labels)
            }
            resize(chart, 800)
            assertLegend(chart, labels)
            resize(chart, 320)
            assertLegend(chart, labels)
            assertTrue(bindStatsChartData(chart, series(labels, 5)))
            assertLegend(chart, labels)
        }
    }

    @Test fun `recreated tab and empty then restored chart keep legend geometry consistent`() {
        for (labels in languages) repeat(2) {
            val chart = chart(320)
            assertTrue(bindStatsChartData(chart, series(labels, 3)))
            assertLegend(chart, labels)
            assertFalse(bindStatsChartData(chart, emptyList()))
            assertTrue(bindStatsChartData(chart, series(labels.take(2), 2)))
            assertLegend(chart, labels.take(2))
            assertTrue(bindStatsChartData(chart, series(labels, 4)))
            assertLegend(chart, labels)
        }
    }

    private fun chart(width: Int): LineChart {
        val base: Context = ApplicationProvider.getApplicationContext()
        return LineChart(ContextThemeWrapper(base, R.style.InfernoTheme)).apply {
            styleStatsChart(this, object : ValueFormatter() {})
            resize(this, width)
        }
    }

    private fun resize(chart: LineChart, width: Int) {
        chart.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
        chart.layout(0, 0, width, 400)
    }

    private fun series(labels: List<String>, sessions: Int): List<LineDataSet> =
        labels.flatMapIndexed { index, label ->
            (0 until sessions).map { session ->
                LineDataSet(listOf(Entry(session * 100f, index + 1f),
                    Entry(session * 100f + 10f, index + 2f)), label).apply {
                    color = Color.GREEN
                    axisDependency = if (index == 2) YAxis.AxisDependency.RIGHT else YAxis.AxisDependency.LEFT
                }
            }
        }

    private fun assertLegend(chart: LineChart, labels: List<String>) {
        assertEquals(labels, chart.legend.entries.map { it.label })
        val sizes = chart.legend.calculatedLabelSizes
        assertEquals("Cached label sizes must describe the deduplicated legend", labels.size, sizes.size)
        labels.forEachIndexed { index, label ->
            assertEquals(label, Utils.calcTextWidth(chart.legendRenderer.labelPaint, label).toFloat(),
                sizes[index].width, 1f)
        }
        val canvas = LabelCanvas()
        chart.legendRenderer.renderLegend(canvas)
        val drawn = canvas.labels.filter { it.text in labels }
        assertEquals(labels, drawn.map { it.text })
        for (label in drawn) {
            assertTrue("${label.text} starts outside chart", label.left >= 0f)
            assertTrue("${label.text} ends outside chart", label.right <= chart.width + 1f)
        }
        for ((index, first) in drawn.withIndex()) for (second in drawn.drop(index + 1)) {
            if (kotlin.math.abs(first.baseline - second.baseline) < 1f) {
                assertTrue("${first.text} overlaps ${second.text}", first.right < second.left)
            }
        }
    }

    private data class Label(val text: String, val left: Float, val right: Float, val baseline: Float)

    private class LabelCanvas : Canvas() {
        val labels = mutableListOf<Label>()
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            labels.add(Label(text, x, x + paint.measureText(text), y))
        }
    }
}
