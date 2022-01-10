package com.cooper.wheellog.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment: Fragment() {
    private val timeFormatter = SimpleDateFormat("HH:mm:ss ", Locale.US)
    private lateinit var chart: LineChart
    private val viewModel: MapViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.chart_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chart = view.findViewById(R.id.chart)
        chart.apply {
            setDrawGridBackground(false)
            description.isEnabled = true
            setHardwareAccelerationEnabled(true)
            legend.textColor = getColorEx(android.R.color.white)
            setNoDataText(resources.getString(R.string.no_chart_data))
            setNoDataTextColor(getColorEx(android.R.color.white))
            axisLeft.apply {
                setDrawGridLines(false)
                textColor = view.getColorEx(android.R.color.white)
                axisMinimum = 0f
            }
            axisRight.apply {
                setDrawGridLines(false)
                textColor = view.getColorEx(android.R.color.white)
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = view.getColorEx(android.R.color.white)
                valueFormatter = chartAxisValueFormatter

            }
        }

        viewModel.selectedItem.observe(viewLifecycleOwner, { tripData ->
            val lineData = LineData()
            tripData.stats?.forEach {
                lineData.addDataSet(it)
            }
            chart.data = lineData
            chart.marker = ChartMarkerView(requireContext(), chart.xAxis.valueFormatter, tripData)
            chart.invalidate()
        })
    }

    private var chartAxisValueFormatter: ValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return timeFormatter.format(Date((value).toLong() * 100))
        }
    }
}