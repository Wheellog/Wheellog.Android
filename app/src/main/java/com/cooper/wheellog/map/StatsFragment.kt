package com.cooper.wheellog.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cooper.wheellog.R
import com.cooper.wheellog.databinding.ChartFragmentBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment: Fragment(), OnChartValueSelectedListener {

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val viewModel: MapViewModel by activityViewModels()
    private lateinit var binding: ChartFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ChartFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initChart(binding.chart1)
        initChart(binding.chart2)

        viewModel.selectedItem.observe(viewLifecycleOwner) { tripData ->
            if (tripData != null) {
                tripDataReceived(binding.chart1, tripData.stats1)
                tripDataReceived(binding.chart2, tripData.stats2)
                val end = (((tripData.endedAt ?: tripData.startedAt) - tripData.startedAt) / 100f).coerceAtLeast(1f)
                for (chart in listOf(binding.chart1, binding.chart2)) {
                    chart.xAxis.axisMinimum = 0f
                    chart.xAxis.axisMaximum = end
                    chart.invalidate()
                }
                binding.chart1.setOnChartValueSelectedListener(this)
                binding.chart2.setOnChartValueSelectedListener(this)
            }
        }
    }

    private val chartAxisValueFormatter: ValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return timeFormatter.format(Date((viewModel.selectedItem.value?.startedAt ?: 0L) + (value * 100).toLong()))
        }
    }

    private fun initChart(chart: LineChart) {
        styleStatsChart(chart, chartAxisValueFormatter)
    }

    private fun tripDataReceived(chart: LineChart, stats: List<LineDataSet>) {
        chart.apply {
            if (!bindStatsChartData(chart, stats)) return
            marker = ChartMarkerView(requireContext(), chart.xAxis.valueFormatter, stats).apply { chartView = chart }
            invalidate()
        }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if (h == null) return
        for (chart in listOf(binding.chart1, binding.chart2)) {
            val index = chart.data?.dataSets?.indexOfFirst { it.getEntriesForXValue(h.x).isNotEmpty() } ?: -1
            if (index >= 0) chart.highlightValues(arrayOf(Highlight(h.x, Float.NaN, index)))
            else chart.highlightValues(emptyArray())
        }
    }

    override fun onNothingSelected() {
        binding.chart1.highlightValues(arrayOf())
        binding.chart2.highlightValues(arrayOf())
    }
}

/** Set the final legend before setData synchronously measures it and reserves chart offsets. */
internal fun bindStatsChartData(chart: LineChart, stats: List<LineDataSet>): Boolean {
    val visibleSeries = stats.filter { it.entryCount > 0 }
    if (visibleSeries.isEmpty()) {
        chart.clear()
        return false
    }
    chart.legend.setCustom(visibleSeries.distinctBy { it.label }.map { series ->
        LegendEntry().apply {
            label = series.label
            formColor = series.color
            form = Legend.LegendForm.SQUARE
        }
    })
    chart.axisRight.isEnabled = visibleSeries.any { it.axisDependency == YAxis.AxisDependency.RIGHT }
    chart.data = LineData().apply { visibleSeries.forEach { addDataSet(it) } }
    return true
}

/** Shared with the chart appearance checks so they inspect the actual label configuration. */
internal fun styleStatsChart(chart: LineChart, timeFormatter: ValueFormatter) {
    chart.apply {
        setDrawGridBackground(false)
        description.isEnabled = false
        setHardwareAccelerationEnabled(true)
        setBackgroundColor(context.statsBackgroundColor())
        legend.textColor = context.statsTextColor()
        legend.textSize = 13f
        legend.isWordWrapEnabled = true
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 4f
        setNoDataText(context.getString(R.string.no_chart_data))
        setNoDataTextColor(context.statsTextColor())
        getPaint(com.github.mikephil.charting.charts.Chart.PAINT_INFO).textSize =
            android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        axisLeft.apply {
            setDrawGridLines(false)
            textColor = chart.context.statsTextColor()
            textSize = 13f
            axisMinimum = 0f
        }
        axisRight.apply {
            setDrawGridLines(false)
            textColor = chart.context.statsTextColor()
            textSize = 13f
        }
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = chart.context.statsTextColor()
            textSize = 13f
            valueFormatter = timeFormatter
            setLabelCount(4, true)
            setAvoidFirstLastClipping(true)
        }
    }
}
