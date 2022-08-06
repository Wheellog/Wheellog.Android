package com.cooper.wheellog.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment: Fragment(R.layout.chart_fragment) {

    private val timeFormatter = SimpleDateFormat("HH:mm:ss ", Locale.US)

    private lateinit var chart1: LineChart
    private lateinit var chart2: LineChart

    private val viewModel: MapViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chart1 = view.findViewById(R.id.chart1)
        chart2 = view.findViewById(R.id.chart2)
        initChart(chart1)
        initChart(chart2)

        viewModel.selectedItem.observe(viewLifecycleOwner) { tripData ->
            if (tripData != null) {
                tripDataReceived(chart1, tripData.stats1)
                tripDataReceived(chart2, tripData.stats2)
            }
        }
    }

    private val chartAxisValueFormatter: ValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return timeFormatter.format(Date((value).toLong() * 100))
        }
    }

    private fun initChart(chart: LineChart) {
        chart.apply {
            setDrawGridBackground(false)
            description.isEnabled = true
            setHardwareAccelerationEnabled(true)
            legend.textColor = getColorEx(android.R.color.white)
            setNoDataText(resources.getString(R.string.no_chart_data))
            setNoDataTextColor(getColorEx(android.R.color.white))
            axisLeft.apply {
                setDrawGridLines(false)
                textColor = chart.getColorEx(android.R.color.white)
                axisMinimum = 0f
            }
            axisRight.apply {
                setDrawGridLines(false)
                textColor = chart.getColorEx(android.R.color.white)
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = chart.getColorEx(android.R.color.white)
                valueFormatter = chartAxisValueFormatter
            }
        }
    }

    private fun tripDataReceived(chart: LineChart, stats: List<LineDataSet>) {
        chart.apply {
            data = LineData().apply {
                stats.forEach {
                    addDataSet(it)
                }
            }
            marker = ChartMarkerView(requireContext(), chart.xAxis.valueFormatter, stats)
            invalidate()
        }
    }
}