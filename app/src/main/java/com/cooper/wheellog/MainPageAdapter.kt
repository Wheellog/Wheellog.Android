package com.cooper.wheellog

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.view.*
import android.widget.TextView
import androidx.gridlayout.widget.GridLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE
import com.cooper.wheellog.utils.FileUtil
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx
import com.cooper.wheellog.utils.StringUtil.Companion.toTempString
import com.cooper.wheellog.views.TripAdapter
import com.cooper.wheellog.views.WheelView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.LinkedHashMap

class MainPageAdapter(private var pages: MutableList<Int>, val activity: MainActivity) : RecyclerView.Adapter<MainPageAdapter.ViewHolder>(), OnSharedPreferenceChangeListener {

    private var xAxisLabels = ArrayList<String>()

    var wheelView: WheelView? = null
    private var chart1: LineChart? = null
    var position: Int = -1
    private var pagesView = LinkedHashMap<Int, View?>()

    private var listOfTrips: RecyclerView? = null

    private var tvBms1Sn: TextView? = null
    private var tvBms2Sn: TextView? = null
    private var tvBms1Fw: TextView? = null
    private var tvBms2Fw: TextView? = null
    private var tvBms1FactoryCap: TextView? = null
    private var tvBms2FactoryCap: TextView? = null
    private var tvBms1ActualCap: TextView? = null
    private var tvBms2ActualCap: TextView? = null
    private var tvBms1Cycles: TextView? = null
    private var tvBms2Cycles: TextView? = null
    private var tvBms1ChrgCount: TextView? = null
    private var tvBms2ChrgCount: TextView? = null
    private var tvBms1MfgDate: TextView? = null
    private var tvBms2MfgDate: TextView? = null
    private var tvBms1Status: TextView? = null
    private var tvBms2Status: TextView? = null
    private var tvBms1RemCap: TextView? = null
    private var tvBms2RemCap: TextView? = null
    private var tvBms1RemPerc: TextView? = null
    private var tvBms2RemPerc: TextView? = null
    private var tvBms1Current: TextView? = null
    private var tvBms2Current: TextView? = null
    private var tvBms1Voltage: TextView? = null
    private var tvBms2Voltage: TextView? = null
    private var tvBms1Temp1: TextView? = null
    private var tvBms2Temp1: TextView? = null
    private var tvBms1Temp2: TextView? = null
    private var tvBms2Temp2: TextView? = null
    private var tvBms1Health: TextView? = null
    private var tvBms2Health: TextView? = null
    private var tvBms1Cell1: TextView? = null
    private var tvBms2Cell1: TextView? = null
    private var tvBms1Cell2: TextView? = null
    private var tvBms2Cell2: TextView? = null
    private var tvBms1Cell3: TextView? = null
    private var tvBms2Cell3: TextView? = null
    private var tvBms1Cell4: TextView? = null
    private var tvBms2Cell4: TextView? = null
    private var tvBms1Cell5: TextView? = null
    private var tvBms2Cell5: TextView? = null
    private var tvBms1Cell6: TextView? = null
    private var tvBms2Cell6: TextView? = null
    private var tvBms1Cell7: TextView? = null
    private var tvBms2Cell7: TextView? = null
    private var tvBms1Cell8: TextView? = null
    private var tvBms2Cell8: TextView? = null
    private var tvBms1Cell9: TextView? = null
    private var tvBms2Cell9: TextView? = null
    private var tvBms1Cell10: TextView? = null
    private var tvBms2Cell10: TextView? = null
    private var tvBms1Cell11: TextView? = null
    private var tvBms2Cell11: TextView? = null
    private var tvBms1Cell12: TextView? = null
    private var tvBms2Cell12: TextView? = null
    private var tvBms1Cell13: TextView? = null
    private var tvBms2Cell13: TextView? = null
    private var tvBms1Cell14: TextView? = null
    private var tvBms2Cell14: TextView? = null
    private var tvTitleBms1Cell15: TextView? = null
    private var tvBms1Cell15: TextView? = null
    private var tvTitleBms2Cell15: TextView? = null
    private var tvBms2Cell15: TextView? = null
    private var tvTitleBms1Cell16: TextView? = null
    private var tvBms1Cell16: TextView? = null
    private var tvTitleBms2Cell16: TextView? = null
    private var tvBms2Cell16: TextView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(recyclerView.context)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(recyclerView.context)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    fun addPage(page: Int) {
        if (!pages.contains(page)) {
            pages.add(page)
            pagesView[page] = null
            notifyItemInserted(page)
        }
    }

    fun removePage(page: Int) {
        if (pages.contains(page)) {
            if (page == R.layout.main_view_events) {
                eventsTextView = null
            }
            val index = pages.indexOf(page)
            pages.removeAt(index)
            pagesView.remove(page)
            notifyItemRemoved(index)
        }
    }

    fun updatePageOfTrips() {
        if (listOfTrips != null) {
            (listOfTrips!!.adapter as TripAdapter).updateTrips(FileUtil.fillTrips(activity))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(viewType, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        pagesView[pages[position]] = view
        when (pages[position]) {
            R.layout.main_view_main -> {
                wheelView = view.findViewById(R.id.wheelView)
            }
            R.layout.main_view_params_list -> {
                createSecondPage()
            }
            R.layout.main_view_graph -> {
                chart1 = view.findViewById(R.id.chart)
                chart1?.apply {
                    setDrawGridBackground(false)
                    description.isEnabled = false
                    setHardwareAccelerationEnabled(true)
                    isHighlightPerTapEnabled = false
                    isHighlightPerDragEnabled = false
                    legend.textColor = getColorEx(android.R.color.white)
                    setNoDataText(resources.getString(R.string.no_chart_data))
                    setNoDataTextColor(getColorEx(android.R.color.white))
                }

                val leftAxis: YAxis = chart1!!.axisLeft
                val rightAxis: YAxis = chart1!!.axisRight
                leftAxis.axisMinimum = 0f
                rightAxis.axisMinimum = 0f
                leftAxis.setDrawGridLines(false)
                rightAxis.setDrawGridLines(false)
                leftAxis.textColor = view.getColorEx(android.R.color.white)
                rightAxis.textColor = view.getColorEx(android.R.color.white)

                val xAxis: XAxis = chart1!!.xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.textColor = view.getColorEx(android.R.color.white)
                xAxis.valueFormatter = chartAxisValueFormatter
            }
            R.layout.main_view_events -> {
                eventsTextView = view.findViewById(R.id.events_textbox)
                eventsTextView?.text = logsCashe
                eventsTextView?.typeface = WheelLog.ThemeManager.getTypeface(view.context)
            }
            R.layout.main_view_trips -> {
                listOfTrips = view.findViewById(R.id.list_trips)
                listOfTrips?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
                listOfTrips?.adapter = TripAdapter(activity, FileUtil.fillTrips(activity))
            }
            R.layout.main_view_smart_bms -> {
                tvBms1Sn = view.findViewById(R.id.tvBms1Sn)
                tvBms2Sn = view.findViewById(R.id.tvBms2Sn)
                tvBms1Fw = view.findViewById(R.id.tvBms1Fw)
                tvBms2Fw = view.findViewById(R.id.tvBms2Fw)
                tvBms1FactoryCap = view.findViewById(R.id.tvBms1FactoryCap)
                tvBms2FactoryCap = view.findViewById(R.id.tvBms2FactoryCap)
                tvBms1ActualCap = view.findViewById(R.id.tvBms1ActualCap)
                tvBms2ActualCap = view.findViewById(R.id.tvBms2ActualCap)
                tvBms1Cycles = view.findViewById(R.id.tvBms1Cycles)
                tvBms2Cycles = view.findViewById(R.id.tvBms2Cycles)
                tvBms1ChrgCount = view.findViewById(R.id.tvBms1ChrgCount)
                tvBms2ChrgCount = view.findViewById(R.id.tvBms2ChrgCount)
                tvBms1MfgDate = view.findViewById(R.id.tvBms1MfgDate)
                tvBms2MfgDate = view.findViewById(R.id.tvBms2MfgDate)
                tvBms1Status = view.findViewById(R.id.tvBms1Status)
                tvBms2Status = view.findViewById(R.id.tvBms2Status)
                tvBms1RemCap = view.findViewById(R.id.tvBms1RemCap)
                tvBms2RemCap = view.findViewById(R.id.tvBms2RemCap)
                tvBms1RemPerc = view.findViewById(R.id.tvBms1RemPerc)
                tvBms2RemPerc = view.findViewById(R.id.tvBms2RemPerc)
                tvBms1Current = view.findViewById(R.id.tvBms1Current)
                tvBms2Current = view.findViewById(R.id.tvBms2Current)
                tvBms1Voltage = view.findViewById(R.id.tvBms1Voltage)
                tvBms2Voltage = view.findViewById(R.id.tvBms2Voltage)
                tvBms1Temp1 = view.findViewById(R.id.tvBms1Temp1)
                tvBms2Temp1 = view.findViewById(R.id.tvBms2Temp1)
                tvBms1Temp2 = view.findViewById(R.id.tvBms1Temp2)
                tvBms2Temp2 = view.findViewById(R.id.tvBms2Temp2)
                tvBms1Health = view.findViewById(R.id.tvBms1Health)
                tvBms2Health = view.findViewById(R.id.tvBms2Health)
                tvBms1Cell1 = view.findViewById(R.id.tvBms1Cell1)
                tvBms2Cell1 = view.findViewById(R.id.tvBms2Cell1)
                tvBms1Cell2 = view.findViewById(R.id.tvBms1Cell2)
                tvBms2Cell2 = view.findViewById(R.id.tvBms2Cell2)
                tvBms1Cell3 = view.findViewById(R.id.tvBms1Cell3)
                tvBms2Cell3 = view.findViewById(R.id.tvBms2Cell3)
                tvBms1Cell4 = view.findViewById(R.id.tvBms1Cell4)
                tvBms2Cell4 = view.findViewById(R.id.tvBms2Cell4)
                tvBms1Cell5 = view.findViewById(R.id.tvBms1Cell5)
                tvBms2Cell5 = view.findViewById(R.id.tvBms2Cell5)
                tvBms1Cell6 = view.findViewById(R.id.tvBms1Cell6)
                tvBms2Cell6 = view.findViewById(R.id.tvBms2Cell6)
                tvBms1Cell7 = view.findViewById(R.id.tvBms1Cell7)
                tvBms2Cell7 = view.findViewById(R.id.tvBms2Cell7)
                tvBms1Cell8 = view.findViewById(R.id.tvBms1Cell8)
                tvBms2Cell8 = view.findViewById(R.id.tvBms2Cell8)
                tvBms1Cell9 = view.findViewById(R.id.tvBms1Cell9)
                tvBms2Cell9 = view.findViewById(R.id.tvBms2Cell9)
                tvBms1Cell10 = view.findViewById(R.id.tvBms1Cell10)
                tvBms2Cell10 = view.findViewById(R.id.tvBms2Cell10)
                tvBms1Cell11 = view.findViewById(R.id.tvBms1Cell11)
                tvBms2Cell11 = view.findViewById(R.id.tvBms2Cell11)
                tvBms1Cell12 = view.findViewById(R.id.tvBms1Cell12)
                tvBms2Cell12 = view.findViewById(R.id.tvBms2Cell12)
                tvBms1Cell13 = view.findViewById(R.id.tvBms1Cell13)
                tvBms2Cell13 = view.findViewById(R.id.tvBms2Cell13)
                tvBms1Cell14 = view.findViewById(R.id.tvBms1Cell14)
                tvBms2Cell14 = view.findViewById(R.id.tvBms2Cell14)
                tvTitleBms1Cell15 = view.findViewById(R.id.tvTitleBms1Cell15)
                tvBms1Cell15 = view.findViewById(R.id.tvBms1Cell15)
                tvTitleBms2Cell15 = view.findViewById(R.id.tvTitleBms2Cell15)
                tvBms2Cell15 = view.findViewById(R.id.tvBms2Cell15)
                tvTitleBms1Cell16 = view.findViewById(R.id.tvTitleBms1Cell16)
                tvBms1Cell16 = view.findViewById(R.id.tvBms1Cell16)
                tvTitleBms2Cell16 = view.findViewById(R.id.tvTitleBms2Cell16)
                tvBms2Cell16 = view.findViewById(R.id.tvBms2Cell16)
            }
        }
    }

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun getItemViewType(position: Int): Int {
        return pages[position]
    }

    fun updateScreen(updateGraph: Boolean) {
        if (position == -1 || position >= pages.size) {
            return
        }
        val data = WheelData.getInstance()
        when (pages[position]) {
            R.layout.main_view_main -> {
                data.bmsView = false
                wheelView?.apply {
                    setSpeed(data.speed)
                    setBattery(data.batteryLevel)
                    setBatteryLowest(data.batteryLowestLevel)
                    setTemperature(data.temperature)
                    setRideTime(data.ridingTimeString)
                    setTopSpeed(data.topSpeedDouble)
                    setDistance(data.distanceDouble)
                    setTotalDistance(data.totalDistanceDouble)
                    setVoltage(data.voltageDouble)
                    setCurrent(data.currentDouble)
                    setAverageSpeed(data.averageRidingSpeedDouble)
                    setMaxPwm(data.maxPwm)
                    setMaxTemperature(data.maxTemp)
                    setPwm(data.calculatedPwm)
                    updateViewBlocksVisibility()
                    redrawTextBoxes()
                    invalidate()
                }

                var profileName = WheelLog.AppConfig.profileName
                if (profileName.trim { it <= ' ' } == "") {
                    profileName = if (data.model == "") data.name else data.model
                }
                wheelView?.setWheelModel(profileName)
            }
            R.layout.main_view_params_list -> {
                if (WheelLog.AppConfig.useMph) {
                    updateFieldForSecondPage(R.string.speed, String.format(Locale.US, "%.1f " + activity.getString(R.string.mph), MathsUtil.kmToMiles(WheelData.getInstance().speedDouble)))
                    updateFieldForSecondPage(R.string.top_speed, String.format(Locale.US, "%.1f " + activity.getString(R.string.mph), MathsUtil.kmToMiles(WheelData.getInstance().topSpeedDouble)))
                    updateFieldForSecondPage(R.string.average_speed, String.format(Locale.US, "%.1f " + activity.getString(R.string.mph), MathsUtil.kmToMiles(WheelData.getInstance().averageSpeedDouble)))
                    updateFieldForSecondPage(R.string.average_riding_speed, String.format(Locale.US, "%.1f " + activity.getString(R.string.mph), MathsUtil.kmToMiles(WheelData.getInstance().averageRidingSpeedDouble)))
                    updateFieldForSecondPage(R.string.dynamic_speed_limit, String.format(Locale.US, "%.1f " + activity.getString(R.string.mph), MathsUtil.kmToMiles(WheelData.getInstance().speedLimit)))
                    updateFieldForSecondPage(R.string.distance, String.format(Locale.US, "%.2f " + activity.getString(R.string.miles), MathsUtil.kmToMiles(WheelData.getInstance().distanceDouble)))
                    updateFieldForSecondPage(R.string.wheel_distance, String.format(Locale.US, "%.2f " + activity.getString(R.string.miles), MathsUtil.kmToMiles(WheelData.getInstance().wheelDistanceDouble)))
                    updateFieldForSecondPage(R.string.user_distance, String.format(Locale.US, "%.2f " + activity.getString(R.string.miles), MathsUtil.kmToMiles(WheelData.getInstance().userDistanceDouble)))
                    updateFieldForSecondPage(R.string.total_distance, String.format(Locale.US, "%.2f " + activity.getString(R.string.miles), MathsUtil.kmToMiles(WheelData.getInstance().totalDistanceDouble)))
                } else {
                    updateFieldForSecondPage(R.string.speed, String.format(Locale.US, "%.1f " + activity.getString(R.string.kmh), WheelData.getInstance().speedDouble))
                    updateFieldForSecondPage(R.string.top_speed, String.format(Locale.US, "%.1f " + activity.getString(R.string.kmh), WheelData.getInstance().topSpeedDouble))
                    updateFieldForSecondPage(R.string.average_speed, String.format(Locale.US, "%.1f " + activity.getString(R.string.kmh), WheelData.getInstance().averageSpeedDouble))
                    updateFieldForSecondPage(R.string.average_riding_speed, String.format(Locale.US, "%.1f " + activity.getString(R.string.kmh), WheelData.getInstance().averageRidingSpeedDouble))
                    updateFieldForSecondPage(R.string.dynamic_speed_limit, String.format(Locale.US, "%.1f " + activity.getString(R.string.kmh), WheelData.getInstance().speedLimit))
                    updateFieldForSecondPage(R.string.distance, String.format(Locale.US, "%.3f " + activity.getString(R.string.km), WheelData.getInstance().distanceDouble))
                    updateFieldForSecondPage(R.string.wheel_distance, String.format(Locale.US, "%.3f " + activity.getString(R.string.km), WheelData.getInstance().wheelDistanceDouble))
                    updateFieldForSecondPage(R.string.user_distance, String.format(Locale.US, "%.3f " + activity.getString(R.string.km), WheelData.getInstance().userDistanceDouble))
                    updateFieldForSecondPage(R.string.total_distance, String.format(Locale.US, "%.3f " + activity.getString(R.string.km), WheelData.getInstance().totalDistanceDouble))
                }
                updateFieldForSecondPage(R.string.voltage, String.format(Locale.US, "%.2f " + activity.getString(R.string.volt), WheelData.getInstance().voltageDouble))
                updateFieldForSecondPage(R.string.voltage_sag, String.format(Locale.US, "%.2f " + activity.getString(R.string.volt), WheelData.getInstance().voltageSagDouble))

                updateFieldForSecondPage(R.string.temperature, WheelData.getInstance().temperature.toTempString())
                updateFieldForSecondPage(R.string.temperature2, WheelData.getInstance().temperature2.toTempString())
                updateFieldForSecondPage(R.string.cpu_temp, WheelData.getInstance().cpuTemp.toTempString())
                updateFieldForSecondPage(R.string.imu_temp, WheelData.getInstance().imuTemp.toTempString())

                updateFieldForSecondPage(R.string.angle, String.format(Locale.US, "%.2f°", WheelData.getInstance().angle))
                updateFieldForSecondPage(R.string.roll, String.format(Locale.US, "%.2f°", WheelData.getInstance().roll))
                updateFieldForSecondPage(R.string.current, String.format(Locale.US, "%.2f " + activity.getString(R.string.amp), WheelData.getInstance().currentDouble))
                updateFieldForSecondPage(R.string.dynamic_current_limit, String.format(Locale.US, "%.2f " + activity.getString(R.string.amp), WheelData.getInstance().currentLimit))
                updateFieldForSecondPage(R.string.torque, String.format(Locale.US, "%.2f " + activity.getString(R.string.newton), WheelData.getInstance().torque))
                updateFieldForSecondPage(R.string.power, String.format(Locale.US, "%.2f " + activity.getString(R.string.watt), WheelData.getInstance().powerDouble))
                updateFieldForSecondPage(R.string.motor_power, String.format(Locale.US, "%.2f " + activity.getString(R.string.watt), WheelData.getInstance().motorPower))
                updateFieldForSecondPage(R.string.battery, String.format(Locale.US, "%d%%", WheelData.getInstance().batteryLevel))
                updateFieldForSecondPage(R.string.fan_status, if (WheelData.getInstance().fanStatus == 0) activity.getString(R.string.off) else activity.getString(R.string.on))
                updateFieldForSecondPage(R.string.charging_status, if (WheelData.getInstance().chargingStatus == 0) activity.getString(R.string.discharging) else activity.getString(R.string.charging))
                updateFieldForSecondPage(R.string.version, String.format(Locale.US, "%s", WheelData.getInstance().version))
                updateFieldForSecondPage(R.string.output, String.format(Locale.US, "%d%%", WheelData.getInstance().output))
                updateFieldForSecondPage(R.string.cpuload, String.format(Locale.US, "%d%%", WheelData.getInstance().cpuLoad))
                updateFieldForSecondPage(R.string.name, WheelData.getInstance().name)
                updateFieldForSecondPage(R.string.model, WheelData.getInstance().model)
                updateFieldForSecondPage(R.string.serial_number, WheelData.getInstance().serial)
                updateFieldForSecondPage(R.string.ride_time, WheelData.getInstance().rideTimeString)
                updateFieldForSecondPage(R.string.riding_time, WheelData.getInstance().ridingTimeString)
                updateFieldForSecondPage(R.string.mode, WheelData.getInstance().modeStr)
                updateFieldForSecondPage(R.string.charging, WheelData.getInstance().chargeTime)
                updateSecondPage()
            }
            R.layout.main_view_graph -> {
                if (!updateGraph || chart1 == null) {
                    return
                }
                xAxisLabels = WheelData.getInstance().xAxis
                if (xAxisLabels.size > 0) {
                    val dataSetSpeed: LineDataSet
                    val dataSetCurrent: LineDataSet
                    if (chart1!!.data == null) {
                        dataSetSpeed = LineDataSet(null, activity.getString(R.string.speed_axis))
                        dataSetCurrent = LineDataSet(null, activity.getString(R.string.current_axis))
                        dataSetSpeed.lineWidth = 2f
                        dataSetCurrent.lineWidth = 2f
                        dataSetSpeed.axisDependency = YAxis.AxisDependency.LEFT
                        dataSetCurrent.axisDependency = YAxis.AxisDependency.RIGHT
                        dataSetSpeed.mode = LineDataSet.Mode.CUBIC_BEZIER
                        dataSetCurrent.mode = LineDataSet.Mode.CUBIC_BEZIER
                        dataSetSpeed.color = chart1!!.getColorEx(android.R.color.white)
                        dataSetCurrent.color = chart1!!.getColorEx(R.color.accent)
                        dataSetSpeed.setDrawCircles(false)
                        dataSetCurrent.setDrawCircles(false)
                        dataSetSpeed.setDrawValues(false)
                        dataSetCurrent.setDrawValues(false)
                        val chart1LineData = LineData()
                        chart1LineData.addDataSet(dataSetCurrent)
                        chart1LineData.addDataSet(dataSetSpeed)
                        chart1!!.data = chart1LineData
                        pagesView[R.layout.main_view_graph]?.findViewById<View>(R.id.leftAxisLabel)?.visibility = View.VISIBLE
                        pagesView[R.layout.main_view_graph]?.findViewById<View>(R.id.leftAxisLabel)?.visibility = View.VISIBLE
                    } else {
                        dataSetSpeed = chart1!!.data.getDataSetByLabel(activity.getString(R.string.speed_axis), true) as LineDataSet
                        dataSetCurrent = chart1!!.data.getDataSetByLabel(activity.getString(R.string.current_axis), true) as LineDataSet
                    }
                    dataSetSpeed.clear()
                    dataSetCurrent.clear()
                    val currentAxis = ArrayList(WheelData.getInstance().currentAxis)
                    val speedAxis = ArrayList(WheelData.getInstance().speedAxis)
                    for (d in currentAxis) {
                        var value = 0f
                        if (d != null) value = d
                        dataSetCurrent.addEntry(Entry(dataSetCurrent.entryCount.toFloat(), value))
                    }
                    for (d in speedAxis) {
                        var value = 0f
                        if (d != null) value = d
                        if (WheelLog.AppConfig.useMph) dataSetSpeed.addEntry(Entry(dataSetSpeed.entryCount.toFloat(), MathsUtil.kmToMiles(value)))
                        else dataSetSpeed.addEntry(Entry(dataSetSpeed.entryCount.toFloat(), value))
                    }
                    dataSetCurrent.notifyDataSetChanged()
                    dataSetSpeed.notifyDataSetChanged()
                    chart1?.apply {
                        this.data.notifyDataChanged()
                        notifyDataSetChanged()
                        invalidate()
                    }
                }
            }
            R.layout.main_view_smart_bms -> {
                data.bmsView = true
                tvBms1Sn?.text = data.bms1.serialNumber
                tvBms1Fw?.text = data.bms1.versionNumber
                tvBms1FactoryCap?.text = String.format(Locale.US, "%d mAh", data.bms1.factoryCap)
                tvBms1ActualCap?.text = String.format(Locale.US, "%d mAh", data.bms1.actualCap)
                tvBms1Cycles?.text = String.format(Locale.US, "%d", data.bms1.fullCycles)
                tvBms1ChrgCount?.text = String.format(Locale.US, "%d", data.bms1.chargeCount)
                tvBms1MfgDate?.text = data.bms1.mfgDateStr
                tvBms1Status?.text = String.format(Locale.US, "%d", data.bms1.status)
                tvBms1RemCap?.text = String.format(Locale.US, "%d mAh", data.bms1.remCap)
                tvBms1RemPerc?.text = String.format(Locale.US, "%d %%", data.bms1.remPerc)
                tvBms1Current?.text = String.format(Locale.US, "%.2f A", data.bms1.current)
                tvBms1Voltage?.text = String.format(Locale.US, "%.2f V", data.bms1.voltage)
                tvBms1Temp1?.text = String.format(Locale.US, "%d°C", data.bms1.temp1)
                tvBms1Temp2?.text = String.format(Locale.US, "%d°C", data.bms1.temp2)
                tvBms1Health?.text = String.format(Locale.US, "%d %%", data.bms1.health)
                var balanceMap = data.bms1.balanceMap
                var bal = if (balanceMap and 0x01 == 1) "[B]" else ""
                tvBms1Cell1?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[0], bal)
                bal = if (balanceMap shr 1 and 0x01 == 1) "[B]" else ""
                tvBms1Cell2?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[1], bal)
                bal = if (balanceMap shr 2 and 0x01 == 1) "[B]" else ""
                tvBms1Cell3?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[2], bal)
                bal = if (balanceMap shr 3 and 0x01 == 1) "[B]" else ""
                tvBms1Cell4?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[3], bal)
                bal = if (balanceMap shr 4 and 0x01 == 1) "[B]" else ""
                tvBms1Cell5?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[4], bal)
                bal = if (balanceMap shr 5 and 0x01 == 1) "[B]" else ""
                tvBms1Cell6?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[5], bal)
                bal = if (balanceMap shr 6 and 0x01 == 1) "[B]" else ""
                tvBms1Cell7?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[6], bal)
                bal = if (balanceMap shr 7 and 0x01 == 1) "[B]" else ""
                tvBms1Cell8?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[7], bal)
                bal = if (balanceMap shr 8 and 0x01 == 1) "[B]" else ""
                tvBms1Cell9?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[8], bal)
                bal = if (balanceMap shr 9 and 0x01 == 1) "[B]" else ""
                tvBms1Cell10?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[9], bal)
                bal = if (balanceMap shr 10 and 0x01 == 1) "[B]" else ""
                tvBms1Cell11?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[10], bal)
                bal = if (balanceMap shr 11 and 0x01 == 1) "[B]" else ""
                tvBms1Cell12?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[11], bal)
                bal = if (balanceMap shr 12 and 0x01 == 1) "[B]" else ""
                tvBms1Cell13?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[12], bal)
                bal = if (balanceMap shr 13 and 0x01 == 1) "[B]" else ""
                tvBms1Cell14?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[13], bal)
                if (data.bms1.cells[14] == 0.0) {
                    tvBms1Cell15?.visibility = View.GONE
                    tvTitleBms1Cell15?.visibility = View.GONE
                } else {
                    tvBms1Cell15?.visibility = View.VISIBLE
                    tvTitleBms1Cell15?.visibility = View.VISIBLE
                }
                if (data.bms1.cells[15] == 0.0) {
                    tvBms1Cell16?.visibility = View.GONE
                    tvTitleBms1Cell16?.visibility = View.GONE
                } else {
                    tvBms1Cell16?.visibility = View.VISIBLE
                    tvTitleBms1Cell16?.visibility = View.VISIBLE
                }
                bal = if (balanceMap shr 14 and 0x01 == 1) "[B]" else ""
                tvBms1Cell15?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[14], bal)
                bal = if (balanceMap shr 15 and 0x01 == 1) "[B]" else ""
                tvBms1Cell16?.text = String.format(Locale.US, "%.3f V %s", data.bms1.cells[15], bal)
                tvBms2Sn?.text = data.bms2.serialNumber
                tvBms2Fw?.text = data.bms2.versionNumber
                tvBms2FactoryCap?.text = String.format(Locale.US, "%d mAh", data.bms2.factoryCap)
                tvBms2ActualCap?.text = String.format(Locale.US, "%d mAh", data.bms2.actualCap)
                tvBms2Cycles?.text = String.format(Locale.US, "%d", data.bms2.fullCycles)
                tvBms2ChrgCount?.text = String.format(Locale.US, "%d", data.bms2.chargeCount)
                tvBms2MfgDate?.text = data.bms2.mfgDateStr
                tvBms2Status?.text = String.format(Locale.US, "%d", data.bms2.status)
                tvBms2RemCap?.text = String.format(Locale.US, "%d mAh", data.bms2.remCap)
                tvBms2RemPerc?.text = String.format(Locale.US, "%d %%", data.bms2.remPerc)
                tvBms2Current?.text = String.format(Locale.US, "%.2f A", data.bms2.current)
                tvBms2Voltage?.text = String.format(Locale.US, "%.2f V", data.bms2.voltage)
                tvBms2Temp1?.text = String.format(Locale.US, "%d°C", data.bms2.temp1)
                tvBms2Temp2?.text = String.format(Locale.US, "%d°C", data.bms2.temp2)
                tvBms2Health?.text = String.format(Locale.US, "%d %%", data.bms2.health)
                balanceMap = data.bms2.balanceMap
                bal = if (balanceMap and 0x01 == 1) "[B]" else ""
                tvBms2Cell1?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[0], bal)
                bal = if (balanceMap shr 1 and 0x01 == 1) "[B]" else ""
                tvBms2Cell2?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[1], bal)
                bal = if (balanceMap shr 2 and 0x01 == 1) "[B]" else ""
                tvBms2Cell3?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[2], bal)
                bal = if (balanceMap shr 3 and 0x01 == 1) "[B]" else ""
                tvBms2Cell4?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[3], bal)
                bal = if (balanceMap shr 4 and 0x01 == 1) "[B]" else ""
                tvBms2Cell5?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[4], bal)
                bal = if (balanceMap shr 5 and 0x01 == 1) "[B]" else ""
                tvBms2Cell6?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[5], bal)
                bal = if (balanceMap shr 6 and 0x01 == 1) "[B]" else ""
                tvBms2Cell7?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[6], bal)
                bal = if (balanceMap shr 7 and 0x01 == 1) "[B]" else ""
                tvBms2Cell8?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[7], bal)
                bal = if (balanceMap shr 8 and 0x01 == 1) "[B]" else ""
                tvBms2Cell9?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[8], bal)
                bal = if (balanceMap shr 9 and 0x01 == 1) "[B]" else ""
                tvBms2Cell10?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[9], bal)
                bal = if (balanceMap shr 10 and 0x01 == 1) "[B]" else ""
                tvBms2Cell11?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[10], bal)
                bal = if (balanceMap shr 11 and 0x01 == 1) "[B]" else ""
                tvBms2Cell12?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[11], bal)
                bal = if (balanceMap shr 12 and 0x01 == 1) "[B]" else ""
                tvBms2Cell13?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[12], bal)
                bal = if (balanceMap shr 13 and 0x01 == 1) "[B]" else ""
                tvBms2Cell14?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[13], bal)
                if (data.bms2.cells[14] == 0.0) {
                    tvBms2Cell15?.visibility = View.GONE
                    tvTitleBms2Cell15?.visibility = View.GONE
                } else {
                    tvBms2Cell15?.visibility = View.VISIBLE
                    tvTitleBms2Cell15?.visibility = View.VISIBLE
                }
                if (data.bms2.cells[15] == 0.0) {
                    tvBms2Cell16?.visibility = View.GONE
                    tvTitleBms2Cell16?.visibility = View.GONE
                } else {
                    tvBms2Cell16?.visibility = View.VISIBLE
                    tvTitleBms2Cell16?.visibility = View.VISIBLE
                }
                bal = if (balanceMap shr 14 and 0x01 == 1) "[B]" else ""
                tvBms2Cell15?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[14], bal)
                bal = if (balanceMap shr 15 and 0x01 == 1) "[B]" else ""
                tvBms2Cell16?.text = String.format(Locale.US, "%.3f V %s", data.bms2.cells[15], bal)
            }
        }
    }

    private var eventsTextView: TextView? = null
    private var eventsCurrentCount = 0
    private var eventsMaxCount = 500
    private var logsCashe = StringBuffer()

    fun logEvent(message: String) {
        logsCashe.append(message)
        if (eventsCurrentCount > eventsMaxCount) {
            val indexOfNewLine = logsCashe.indexOfFirst { r -> r == '\n' }
            logsCashe.delete(0, indexOfNewLine)
        } else {
            eventsCurrentCount++
        }
        eventsTextView?.text = logsCashe
    }

    private var chartAxisValueFormatter: IAxisValueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            return if (value < xAxisLabels.size) xAxisLabels[value.toInt()] else ""
        }

        // we don't draw numbers, so no decimal digits needed
        override fun getDecimalDigits(): Int {
            return 0
        }
    }

    //region SecondPage
    private val secondPageValues = LinkedHashMap<Int, String>()

    private fun setupFieldForSecondPage(resId: Int) {
        secondPageValues[resId] = ""
    }

    private fun updateFieldForSecondPage(resId: Int, value: String) {
        if (secondPageValues.containsKey(resId)) {
            secondPageValues[resId] = value
        }
    }

    private fun createSecondPage() {
        val layout = pagesView[R.layout.main_view_params_list]?.findViewById<GridLayout>(R.id.page_two_grid) ?: return
        layout.removeAllViews()
        val font = WheelLog.ThemeManager.getTypeface(activity)
        for ((key, value) in secondPageValues) {
            val headerText = (activity.layoutInflater.inflate(
                R.layout.textview_title_template, layout, false
            ) as TextView).apply {
                text = activity.getString(key)
                typeface = font
            }
            val valueText = (activity.layoutInflater.inflate(
                R.layout.textview_value_template, layout, false
            ) as TextView).apply {
                text = value
                typeface = font
            }
            layout.addView(headerText)
            layout.addView(valueText)
        }
    }

    private fun updateSecondPage() {
        val layout = pagesView[R.layout.main_view_params_list]?.findViewById<GridLayout>(R.id.page_two_grid) ?: return
        val count = layout.childCount
        if (secondPageValues.size * 2 != count) {
            return
        }
        var index = 1
        for (value in secondPageValues.values) {
            val valueText = layout.getChildAt(index) as TextView
            valueText.text = value
            index += 2
        }
    }
    //endregion

    fun configureSecondDisplay() {
        secondPageValues.clear()
        when (WheelData.getInstance().wheelType) {
            WHEEL_TYPE.KINGSONG -> {
                setupFieldForSecondPage(R.string.speed)
                setupFieldForSecondPage(R.string.dynamic_speed_limit)
                setupFieldForSecondPage(R.string.top_speed)
                setupFieldForSecondPage(R.string.average_speed)
                setupFieldForSecondPage(R.string.average_riding_speed)
                setupFieldForSecondPage(R.string.battery)
                setupFieldForSecondPage(R.string.output)
                setupFieldForSecondPage(R.string.cpuload)
                setupFieldForSecondPage(R.string.temperature)
                setupFieldForSecondPage(R.string.temperature2)
                setupFieldForSecondPage(R.string.ride_time)
                setupFieldForSecondPage(R.string.riding_time)
                setupFieldForSecondPage(R.string.distance)
                setupFieldForSecondPage(R.string.wheel_distance)
                setupFieldForSecondPage(R.string.user_distance)
                setupFieldForSecondPage(R.string.total_distance)
                setupFieldForSecondPage(R.string.voltage)
                setupFieldForSecondPage(R.string.voltage_sag)
                setupFieldForSecondPage(R.string.current)
                setupFieldForSecondPage(R.string.power)
                setupFieldForSecondPage(R.string.fan_status)
                setupFieldForSecondPage(R.string.charging_status)
                setupFieldForSecondPage(R.string.charging)
                setupFieldForSecondPage(R.string.mode)
                setupFieldForSecondPage(R.string.name)
                setupFieldForSecondPage(R.string.model)
                setupFieldForSecondPage(R.string.version)
                setupFieldForSecondPage(R.string.serial_number)
            }
            WHEEL_TYPE.VETERAN -> {
                setupFieldForSecondPage(R.string.speed)
                setupFieldForSecondPage(R.string.top_speed)
                setupFieldForSecondPage(R.string.average_speed)
                setupFieldForSecondPage(R.string.average_riding_speed)
                setupFieldForSecondPage(R.string.battery)
                setupFieldForSecondPage(R.string.temperature)
                setupFieldForSecondPage(R.string.ride_time)
                setupFieldForSecondPage(R.string.riding_time)
                setupFieldForSecondPage(R.string.distance)
                setupFieldForSecondPage(R.string.wheel_distance)
                setupFieldForSecondPage(R.string.user_distance)
                setupFieldForSecondPage(R.string.total_distance)
                setupFieldForSecondPage(R.string.voltage)
                setupFieldForSecondPage(R.string.voltage_sag)
                setupFieldForSecondPage(R.string.current)
                setupFieldForSecondPage(R.string.power)
                setupFieldForSecondPage(R.string.charging_status)
                setupFieldForSecondPage(R.string.charging)
                setupFieldForSecondPage(R.string.model)
                setupFieldForSecondPage(R.string.version)
            }
            WHEEL_TYPE.GOTWAY -> {
                setupFieldForSecondPage(R.string.speed)
                setupFieldForSecondPage(R.string.top_speed)
                setupFieldForSecondPage(R.string.average_speed)
                setupFieldForSecondPage(R.string.average_riding_speed)
                setupFieldForSecondPage(R.string.battery)
                setupFieldForSecondPage(R.string.temperature)
                setupFieldForSecondPage(R.string.ride_time)
                setupFieldForSecondPage(R.string.riding_time)
                setupFieldForSecondPage(R.string.distance)
                setupFieldForSecondPage(R.string.wheel_distance)
                setupFieldForSecondPage(R.string.user_distance)
                setupFieldForSecondPage(R.string.total_distance)
                setupFieldForSecondPage(R.string.voltage)
                setupFieldForSecondPage(R.string.voltage_sag)
                setupFieldForSecondPage(R.string.current)
                setupFieldForSecondPage(R.string.power)
                setupFieldForSecondPage(R.string.charging_status)
                setupFieldForSecondPage(R.string.charging)
            }
            WHEEL_TYPE.INMOTION_V2 -> {
                setupFieldForSecondPage(R.string.speed)
                setupFieldForSecondPage(R.string.dynamic_speed_limit)
                setupFieldForSecondPage(R.string.torque)
                setupFieldForSecondPage(R.string.top_speed)
                setupFieldForSecondPage(R.string.average_speed)
                setupFieldForSecondPage(R.string.average_riding_speed)
                setupFieldForSecondPage(R.string.battery)
                setupFieldForSecondPage(R.string.temperature)
                setupFieldForSecondPage(R.string.temperature2)
                setupFieldForSecondPage(R.string.cpu_temp)
                setupFieldForSecondPage(R.string.imu_temp)
                setupFieldForSecondPage(R.string.angle)
                setupFieldForSecondPage(R.string.roll)
                setupFieldForSecondPage(R.string.ride_time)
                setupFieldForSecondPage(R.string.riding_time)
                setupFieldForSecondPage(R.string.distance)
                setupFieldForSecondPage(R.string.wheel_distance)
                setupFieldForSecondPage(R.string.user_distance)
                setupFieldForSecondPage(R.string.total_distance)
                setupFieldForSecondPage(R.string.voltage)
                setupFieldForSecondPage(R.string.voltage_sag)
                setupFieldForSecondPage(R.string.current)
                setupFieldForSecondPage(R.string.dynamic_current_limit)
                setupFieldForSecondPage(R.string.power)
                setupFieldForSecondPage(R.string.motor_power)
                setupFieldForSecondPage(R.string.mode)
                setupFieldForSecondPage(R.string.model)
                setupFieldForSecondPage(R.string.version)
                setupFieldForSecondPage(R.string.serial_number)
            }
            WHEEL_TYPE.INMOTION -> {
                setupFieldForSecondPage(R.string.speed)
                setupFieldForSecondPage(R.string.top_speed)
                setupFieldForSecondPage(R.string.average_speed)
                setupFieldForSecondPage(R.string.average_riding_speed)
                setupFieldForSecondPage(R.string.battery)
                setupFieldForSecondPage(R.string.temperature)
                setupFieldForSecondPage(R.string.temperature2)
                setupFieldForSecondPage(R.string.angle)
                setupFieldForSecondPage(R.string.roll)
                setupFieldForSecondPage(R.string.ride_time)
                setupFieldForSecondPage(R.string.riding_time)
                setupFieldForSecondPage(R.string.distance)
                setupFieldForSecondPage(R.string.wheel_distance)
                setupFieldForSecondPage(R.string.user_distance)
                setupFieldForSecondPage(R.string.total_distance)
                setupFieldForSecondPage(R.string.voltage)
                setupFieldForSecondPage(R.string.voltage_sag)
                setupFieldForSecondPage(R.string.current)
                setupFieldForSecondPage(R.string.power)
                setupFieldForSecondPage(R.string.mode)
                setupFieldForSecondPage(R.string.model)
                setupFieldForSecondPage(R.string.version)
                setupFieldForSecondPage(R.string.serial_number)
                setupFieldForSecondPage(R.string.charging_status)
                setupFieldForSecondPage(R.string.charging)
            }
            WHEEL_TYPE.NINEBOT_Z, WHEEL_TYPE.NINEBOT -> {
                setupFieldForSecondPage(R.string.speed)
                setupFieldForSecondPage(R.string.top_speed)
                setupFieldForSecondPage(R.string.average_speed)
                setupFieldForSecondPage(R.string.average_riding_speed)
                setupFieldForSecondPage(R.string.battery)
                setupFieldForSecondPage(R.string.temperature)
                setupFieldForSecondPage(R.string.ride_time)
                setupFieldForSecondPage(R.string.riding_time)
                setupFieldForSecondPage(R.string.distance)
                setupFieldForSecondPage(R.string.user_distance)
                setupFieldForSecondPage(R.string.total_distance)
                setupFieldForSecondPage(R.string.voltage)
                setupFieldForSecondPage(R.string.voltage_sag)
                setupFieldForSecondPage(R.string.current)
                setupFieldForSecondPage(R.string.power)
                setupFieldForSecondPage(R.string.model)
                setupFieldForSecondPage(R.string.version)
                setupFieldForSecondPage(R.string.serial_number)
            }
            else -> {}
        }
        createSecondPage()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (WheelLog.AppConfig.getResId(key)) {
            R.string.show_page_events -> if (WheelLog.AppConfig.pageEvents) {
                addPage(R.layout.main_view_events)
            } else {
                removePage(R.layout.main_view_events)
            }
            R.string.show_page_trips -> if (WheelLog.AppConfig.pageTrips) {
                addPage(R.layout.main_view_trips)
            } else {
                removePage(R.layout.main_view_trips)
                listOfTrips = null
            }
            R.string.view_blocks_string -> updateScreen(true)
            R.string.auto_upload_ec ->
                GlobalScope.launch {
                    delay(500)
                    MainScope().launch {
                        listOfTrips?.apply {
                            // redraw
                            val a = adapter as TripAdapter
                            if (a.uploadVisible != WheelLog.AppConfig.autoUploadEc) {
                                a.uploadVisible = WheelLog.AppConfig.autoUploadEc
                                val l = layoutManager
                                adapter = null
                                layoutManager = null
                                adapter = a
                                layoutManager = l
                                a.notifyDataSetChanged()
                            }
                        }
                    }
                }
        }
    }

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)
}
