package com.cooper.wheellog.map

import com.cooper.wheellog.data.LogTick
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet

/** Separate datasets prevent the renderer from inventing a line through missing hours. */
internal fun statsSeries(ticks: List<LogTick>, label: String, color: Int, axis: YAxis.AxisDependency,
                         wheelOnly: Boolean = true, value: (LogTick) -> Double?): List<LineDataSet> {
    val groups = mutableListOf<MutableList<Entry>>()
    var previous: LogTick? = null
    for (tick in ticks) {
        val reading = value(tick)?.takeIf { it.isFinite() } ?: continue
        val before = previous
        val limit = if (wheelOnly || before?.wheelDataAvailable == true && tick.wheelDataAvailable) 5_000 else 30_000
        if (before == null || tick.timestampMillis - before.timestampMillis !in 0..limit ||
            !wheelOnly && before.wheelDataAvailable != tick.wheelDataAvailable) groups.add(mutableListOf())
        groups.last().add(Entry(tick.timePlusDayOfWeek, reading.toFloat()))
        previous = tick
    }
    return groups.map { entries -> LineDataSet(entries, label).apply {
        this.color = color
        axisDependency = axis
        lineWidth = 2f
        setDrawCircles(entries.size == 1)
        setCircleColor(color)
        setDrawCircleHole(false)
        circleRadius = 2f
    } }
}
