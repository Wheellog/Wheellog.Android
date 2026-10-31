package com.cooper.wheellog.map

import android.app.Application
import android.graphics.Color
import com.cooper.wheellog.data.NativeCsv
import com.github.mikephil.charting.components.YAxis
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class StatsSeriesTest {
    private fun read(vararg rows: String) = NativeCsv.read(
        ("date,time,speed,gps_speed\n" + rows.joinToString("\n") { "2026-10-26,$it" }).byteInputStream())

    @Test fun `wheel series have no fabricated line across the unrecorded morning break`() {
        val ticks = read("08:00:00.000,10,10", "08:00:01.000,0,0",
            "12:28:00.000,20,20", "12:28:01.000,0,0", "12:28:02.000,0,0")
        val series = statsSeries(ticks, "Speed", Color.GREEN, YAxis.AxisDependency.LEFT) { it.speed }
        assertEquals(listOf(2, 3), series.map { it.entryCount })
        assertEquals(5, series.sumOf { it.entryCount }) // actual stationary packets remain
        assertEquals(0f, series.last().getEntryForIndex(2).y)
        assertTrue(series.all { it.label == "Speed" })
        assertTrue(series.all { it.getEntryForIndex(it.entryCount - 1).x - it.getEntryForIndex(0).x <= 20f })
    }

    @Test fun `mixed GPS and wheel series split at source changes and large gaps`() {
        val ticks = read("08:00:00.000,,15", "08:00:10.000,,20",
            "12:28:00.000,18,19", "12:28:01.000,0,0", "12:28:02.000,,1")
        val series = statsSeries(ticks, "Speed", Color.GREEN, YAxis.AxisDependency.LEFT, false) { it.speed }
        assertEquals(listOf(2, 2, 1), series.map { it.entryCount })
        assertTrue(series.last().isDrawCirclesEnabled)
        assertEquals(18f, series[1].getEntryForIndex(0).y)
    }

    @Test fun `missing optional readings do not bridge into an old sample`() {
        val ticks = read("08:00:00.000,10,10", "08:00:03.000,,11", "08:00:06.000,20,20")
        val series = statsSeries(ticks, "Power", Color.YELLOW, YAxis.AxisDependency.RIGHT) {
            it.speed.takeIf { _ -> it.wheelDataAvailable }
        }
        assertEquals(listOf(1, 1), series.map { it.entryCount })
        assertTrue(series.all { it.axisDependency == YAxis.AxisDependency.RIGHT })
    }
}
