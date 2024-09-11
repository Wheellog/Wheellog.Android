package com.cooper.wheellog

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.view.*
import android.widget.TextView
import androidx.gridlayout.widget.GridLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE
import com.cooper.wheellog.utils.FileUtil
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.cooper.wheellog.utils.StringUtil.inArray
import com.cooper.wheellog.utils.StringUtil.toTempString
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.views.TripAdapter
import com.cooper.wheellog.views.WheelView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.*
import java.util.*

class MainPageAdapter(private var pages: MutableList<Int>, val activity: MainActivity) : RecyclerView.Adapter<MainPageAdapter.ViewHolder>(), OnSharedPreferenceChangeListener {

    private var xAxisLabels = ArrayList<String>()

    var wheelView: WheelView? = null
    private var chart1: LineChart? = null
    var position: Int = -1
    private var pagesView = LinkedHashMap<Int, View?>()

    private var listOfTrips: RecyclerView? = null

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

    fun addPage(page: Int, index: Int = 0) {
        if (!pages.contains(page)) {
            if (index == 0 || index >= pages.size) {
                pages.add(page)
            } else {
                pages.add(index, page)
            }
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
                eventsTextView?.typeface = ThemeManager.getTypeface(view.context)
            }
            R.layout.main_view_trips -> {
                listOfTrips = view.findViewById(R.id.list_trips)
                // listOfTrips?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
                listOfTrips?.adapter = TripAdapter(activity, FileUtil.fillTrips(activity))
                // for Tests
                // val models = arrayListOf(TripModel("title", "desc", "asd"))
                // listOfTrips?.adapter = TripAdapter(activity, models)
            }
            R.layout.main_view_smart_bms -> {
                createSmartBmsPage()
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

                    var profileName = WheelLog.AppConfig.profileName
                    if (profileName.trim { it <= ' ' } == "") {
                        profileName = if (data.model == "") data.name else data.model
                    }
                    setWheelModel(profileName)
                }
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
                    // TODO: fix me
                    // ужасно-тормозной код по перерисовывнию графика.
                    // например каждая очистка вызывает перерисовку
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
                        if (WheelLog.AppConfig.useMph)
                            dataSetSpeed.addEntry(Entry(dataSetSpeed.entryCount.toFloat(), MathsUtil.kmToMiles(value)))
                        else
                            dataSetSpeed.addEntry(Entry(dataSetSpeed.entryCount.toFloat(), value))
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
                updateFieldForSmartBmsPage(R.string.bmsSn, data.bms1.serialNumber, data.bms2.serialNumber)
                updateFieldForSmartBmsPage(R.string.bmsFw, data.bms1.versionNumber, data.bms2.versionNumber)
                updateFieldForSmartBmsPage(R.string.bmsFactoryCap, String.format(Locale.US, "%d mAh", data.bms1.factoryCap), String.format(Locale.US, "%d mAh", data.bms2.factoryCap))
                updateFieldForSmartBmsPage(R.string.bmsActualCap, String.format(Locale.US, "%d mAh", data.bms1.actualCap), String.format(Locale.US, "%d mAh", data.bms2.actualCap))
                updateFieldForSmartBmsPage(R.string.bmsCycles, String.format(Locale.US, "%d", data.bms1.fullCycles), String.format(Locale.US, "%d", data.bms2.fullCycles))
                updateFieldForSmartBmsPage(R.string.bmsChrgCount, String.format(Locale.US, "%d", data.bms1.chargeCount), String.format(Locale.US, "%d", data.bms2.chargeCount))
                updateFieldForSmartBmsPage(R.string.bmsMfgDate, data.bms1.mfgDateStr, data.bms2.mfgDateStr)
                updateFieldForSmartBmsPage(R.string.bmsStatus, String.format(Locale.US, "%d", data.bms1.status), String.format(Locale.US, "%d", data.bms2.status))
                updateFieldForSmartBmsPage(R.string.bmsRemCap, String.format(Locale.US, "%d mAh", data.bms1.remCap), String.format(Locale.US, "%d mAh", data.bms2.remCap))
                updateFieldForSmartBmsPage(R.string.bmsRemPerc, String.format(Locale.US, "%d %%", data.bms1.remPerc), String.format(Locale.US, "%d %%", data.bms2.remPerc))
                updateFieldForSmartBmsPage(R.string.bmsCurrent, String.format(Locale.US, "%.2f A", data.bms1.current), String.format(Locale.US, "%.2f A", data.bms2.current))
                updateFieldForSmartBmsPage(R.string.bmsVoltage, String.format(Locale.US, "%.2f V", data.bms1.voltage), String.format(Locale.US, "%.2f V", data.bms2.voltage))
                updateFieldForSmartBmsPage(R.string.bmsTemp1, String.format(Locale.US, "%.1f°C", data.bms1.temp1), String.format(Locale.US, "%.1f°C", data.bms2.temp1))
                updateFieldForSmartBmsPage(R.string.bmsTemp2, String.format(Locale.US, "%.1f°C", data.bms1.temp2), String.format(Locale.US, "%.1f°C", data.bms2.temp2))
                updateFieldForSmartBmsPage(R.string.bmsTemp3, String.format(Locale.US, "%.1f°C", data.bms1.temp3), String.format(Locale.US, "%.1f°C", data.bms2.temp3))
                updateFieldForSmartBmsPage(R.string.bmsTemp4, String.format(Locale.US, "%.1f°C", data.bms1.temp4), String.format(Locale.US, "%.1f°C", data.bms2.temp4))
                updateFieldForSmartBmsPage(R.string.bmsTemp5, String.format(Locale.US, "%.1f°C", data.bms1.temp5), String.format(Locale.US, "%.1f°C", data.bms2.temp5))
                updateFieldForSmartBmsPage(R.string.bmsTemp6, String.format(Locale.US, "%.1f°C", data.bms1.temp6), String.format(Locale.US, "%.1f°C", data.bms2.temp6))
                updateFieldForSmartBmsPage(R.string.bmsTempMos, String.format(Locale.US, "%.1f°C", data.bms1.tempMos), String.format(Locale.US, "%.1f°C", data.bms2.tempMos))
                updateFieldForSmartBmsPage(R.string.bmsTempMosEnv, String.format(Locale.US, "%.1f°C", data.bms1.tempMosEnv), String.format(Locale.US, "%.1f°C", data.bms2.tempMosEnv))
                updateFieldForSmartBmsPage(R.string.bmsHealth, String.format(Locale.US, "%d %%", data.bms1.health), String.format(Locale.US, "%d %%", data.bms2.health))
                updateFieldForSmartBmsPage(R.string.bmsMaxCell, String.format(Locale.US, "%.3f V", data.bms1.maxCell), String.format(Locale.US, "%.3f V", data.bms2.maxCell))
                updateFieldForSmartBmsPage(R.string.bmsMinCell, String.format(Locale.US, "%.3f V", data.bms1.minCell), String.format(Locale.US, "%.3f V", data.bms2.minCell))
                updateFieldForSmartBmsPage(R.string.bmsCellDiff, String.format(Locale.US, "%.3f V", data.bms1.cellDiff), String.format(Locale.US, "%.3f V", data.bms2.cellDiff))
                var cells = ArrayList<Int>()
                cells.add(R.string.bmsCell1)
                cells.add(R.string.bmsCell2)
                cells.add(R.string.bmsCell3)
                cells.add(R.string.bmsCell4)
                cells.add(R.string.bmsCell5)
                cells.add(R.string.bmsCell6)
                cells.add(R.string.bmsCell7)
                cells.add(R.string.bmsCell8)
                cells.add(R.string.bmsCell9)
                cells.add(R.string.bmsCell10)
                cells.add(R.string.bmsCell11)
                cells.add(R.string.bmsCell12)
                cells.add(R.string.bmsCell13)
                cells.add(R.string.bmsCell14)
                cells.add(R.string.bmsCell15)
                cells.add(R.string.bmsCell16)
                cells.add(R.string.bmsCell17)
                cells.add(R.string.bmsCell18)
                cells.add(R.string.bmsCell19)
                cells.add(R.string.bmsCell20)
                cells.add(R.string.bmsCell21)
                cells.add(R.string.bmsCell22)
                cells.add(R.string.bmsCell23)
                cells.add(R.string.bmsCell24)
                cells.add(R.string.bmsCell25)
                cells.add(R.string.bmsCell26)
                cells.add(R.string.bmsCell27)
                cells.add(R.string.bmsCell28)
                cells.add(R.string.bmsCell29)
                cells.add(R.string.bmsCell30)
                cells.add(R.string.bmsCell31)
                cells.add(R.string.bmsCell32)
                cells.add(R.string.bmsCell33)
                cells.add(R.string.bmsCell34)
                cells.add(R.string.bmsCell35)
                cells.add(R.string.bmsCell36)
                var balanceMap1 = data.bms1.balanceMap
                var balanceMap2 = data.bms2.balanceMap
                var index = 0
                while (index < cells.size) {
                    var bal1 = if (balanceMap1 shr index and 0x01 == 1) "[B]" else ""
                    var bal2 = if (balanceMap2 shr index and 0x01 == 1) "[B]" else ""
                    updateFieldForSmartBmsPage(cells[index], String.format(Locale.US, "%.3f V %s", data.bms1.cells[index], bal1), String.format(Locale.US, "%.3f V %s", data.bms2.cells[index], bal2))
                    index += 1
                }
                updateSmartBmsPage()
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

    private var chartAxisValueFormatter: IndexAxisValueFormatter = object : IndexAxisValueFormatter () {
        override fun getFormattedValue(value: Float): String {
            return if (value < xAxisLabels.size) xAxisLabels[value.toInt()] else ""
        }

        // we don't draw numbers, so no decimal digits needed
        fun getDecimalDigits(): Int {
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
        val font = ThemeManager.getTypeface(activity)
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
                setupFieldForSecondPage(R.string.angle)
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
                setupFieldForSecondPage(R.string.model)
                setupFieldForSecondPage(R.string.version)
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

    //region SmartBMS page
    private val smartBms1PageValues = LinkedHashMap<Int, String>()
    private val smartBms2PageValues = LinkedHashMap<Int, String>()


    private fun setupFieldForSmartBmsPage(resId: Int) {
        smartBms1PageValues[resId] = ""
        smartBms2PageValues[resId] = ""
    }

    private fun updateFieldForSmartBmsPage(resId: Int, value1: String, value2: String) {
        if (smartBms1PageValues.containsKey(resId)) {
            smartBms1PageValues[resId] = value1
            smartBms2PageValues[resId] = value2
        }
    }

    private fun createSmartBmsPage() {
        val layout = pagesView[R.layout.main_view_smart_bms]?.findViewById<GridLayout>(R.id.page_smart_bms_grid) ?: return
        layout.removeAllViews()
        val font = ThemeManager.getTypeface(activity)
        val bat1Text = (activity.layoutInflater.inflate(
                R.layout.textview_smart_bms_battery_template, layout, false
        ) as TextView).apply {
            text = activity.getString(R.string.bmsBattery1Title)
            typeface = font
        }
        val bat2Text = (activity.layoutInflater.inflate(
                R.layout.textview_smart_bms_battery_template, layout, false
        ) as TextView).apply {
            text = activity.getString(R.string.bmsBattery2Title)
            typeface = font
        }
        layout.addView(bat1Text)
        layout.addView(bat2Text)

        var views1 = ArrayList<View>()
        var views2 = ArrayList<View>()
        for ((key1, value1) in smartBms1PageValues) {

            val headerText1 = (activity.layoutInflater.inflate(
                    R.layout.textview_smart_bms_title_template, layout, false
            ) as TextView).apply {
                text = activity.getString(key1)
                typeface = font
            }
            val valueText1 = (activity.layoutInflater.inflate(
                    R.layout.textview_smart_bms_value_template, layout, false
            ) as TextView).apply {
                text = value1
                typeface = font
            }
            views1.add(headerText1)
            views1.add(valueText1)
        }
        for ((key2, value2) in smartBms2PageValues) {
            val headerText2 = (activity.layoutInflater.inflate(
                    R.layout.textview_smart_bms_title_template, layout, false
            ) as TextView).apply {
                text = activity.getString(key2)
                typeface = font
            }
            val valueText2 = (activity.layoutInflater.inflate(
                    R.layout.textview_smart_bms_value_template, layout, false
            ) as TextView).apply {
                text = value2
                typeface = font
            }
            views2.add(headerText2)
            views2.add(valueText2)
        }
        var index = 0
        while (index < views1.size) {
            layout.addView(views1[index])
            layout.addView(views1[index+1])
            layout.addView(views2[index])
            layout.addView(views2[index+1])
            index += 2
        }
    }

    private fun updateSmartBmsPage() {
        val layout = pagesView[R.layout.main_view_smart_bms]?.findViewById<GridLayout>(R.id.page_smart_bms_grid) ?: return
        val count = layout.childCount
        if (smartBms1PageValues.size * 4 != count-2) {
            return
        }
        var index = 3
        for (value in smartBms1PageValues.values) {
            val valueText = layout.getChildAt(index) as TextView
            valueText.text = value
            index += 4
        }
        index = 5
        for (value in smartBms2PageValues.values) {
            val valueText = layout.getChildAt(index) as TextView
            valueText.text = value
            index += 4
        }
    }

    fun configureSmartBmsDisplay() {
        smartBms1PageValues.clear()
        smartBms2PageValues.clear()
        when (WheelData.getInstance().wheelType) {
            WHEEL_TYPE.KINGSONG -> {
                if (inArray(WheelData.getInstance().model, arrayOf("KS-S20", "KS-S22", "KS-S19", "KS-S16", "KS-S16P"))) {
                    addPage(R.layout.main_view_smart_bms, 2)
                    setupFieldForSmartBmsPage(R.string.bmsSn)
                    setupFieldForSmartBmsPage(R.string.bmsFw)
                    setupFieldForSmartBmsPage(R.string.bmsFactoryCap)
                    setupFieldForSmartBmsPage(R.string.bmsCycles)
                    //setupFieldForSmartBmsPage(R.string.bmsStatus) // not parsed yet
                    setupFieldForSmartBmsPage(R.string.bmsRemCap)
                    setupFieldForSmartBmsPage(R.string.bmsRemPerc)
                    setupFieldForSmartBmsPage(R.string.bmsCurrent)
                    setupFieldForSmartBmsPage(R.string.bmsVoltage)
                    setupFieldForSmartBmsPage(R.string.bmsTemp1)
                    setupFieldForSmartBmsPage(R.string.bmsTemp2)
                    setupFieldForSmartBmsPage(R.string.bmsTemp3)
                    setupFieldForSmartBmsPage(R.string.bmsTemp4)
                    if (inArray(WheelData.getInstance().model, arrayOf("KS-S20", "KS-S22"))) {
                        setupFieldForSmartBmsPage(R.string.bmsTemp5)
                        setupFieldForSmartBmsPage(R.string.bmsTemp6)
                    }
                    setupFieldForSmartBmsPage(R.string.bmsTempMos)
                    setupFieldForSmartBmsPage(R.string.bmsTempMosEnv)
                    setupFieldForSmartBmsPage(R.string.bmsMaxCell)
                    setupFieldForSmartBmsPage(R.string.bmsMinCell)
                    setupFieldForSmartBmsPage(R.string.bmsCellDiff)
                    setupFieldForSmartBmsPage(R.string.bmsCell1)
                    setupFieldForSmartBmsPage(R.string.bmsCell2)
                    setupFieldForSmartBmsPage(R.string.bmsCell3)
                    setupFieldForSmartBmsPage(R.string.bmsCell4)
                    setupFieldForSmartBmsPage(R.string.bmsCell5)
                    setupFieldForSmartBmsPage(R.string.bmsCell6)
                    setupFieldForSmartBmsPage(R.string.bmsCell7)
                    setupFieldForSmartBmsPage(R.string.bmsCell8)
                    setupFieldForSmartBmsPage(R.string.bmsCell9)
                    setupFieldForSmartBmsPage(R.string.bmsCell10)
                    setupFieldForSmartBmsPage(R.string.bmsCell11)
                    setupFieldForSmartBmsPage(R.string.bmsCell12)
                    setupFieldForSmartBmsPage(R.string.bmsCell13)
                    setupFieldForSmartBmsPage(R.string.bmsCell14)
                    setupFieldForSmartBmsPage(R.string.bmsCell15)
                    setupFieldForSmartBmsPage(R.string.bmsCell16)
                    setupFieldForSmartBmsPage(R.string.bmsCell17)
                    setupFieldForSmartBmsPage(R.string.bmsCell18)
                    setupFieldForSmartBmsPage(R.string.bmsCell19)
                    setupFieldForSmartBmsPage(R.string.bmsCell20)
                    if (inArray(WheelData.getInstance().model, arrayOf("KS-S20", "KS-S22", "KS-S19"))) {
                        setupFieldForSmartBmsPage(R.string.bmsCell21)
                        setupFieldForSmartBmsPage(R.string.bmsCell22)
                        setupFieldForSmartBmsPage(R.string.bmsCell23)
                        setupFieldForSmartBmsPage(R.string.bmsCell24)
                    }
                    if (inArray(WheelData.getInstance().model, arrayOf("KS-S20", "KS-S22"))) {
                        setupFieldForSmartBmsPage(R.string.bmsCell25)
                        setupFieldForSmartBmsPage(R.string.bmsCell26)
                        setupFieldForSmartBmsPage(R.string.bmsCell27)
                        setupFieldForSmartBmsPage(R.string.bmsCell28)
                        setupFieldForSmartBmsPage(R.string.bmsCell29)
                        setupFieldForSmartBmsPage(R.string.bmsCell30)
                    }
                } else {
                    removePage(R.layout.main_view_smart_bms)
                    return
                }
            }
            WHEEL_TYPE.VETERAN -> {
                if (inArray(WheelData.getInstance().model, arrayOf("Lynx", "Sherman L"))) {
                    addPage(R.layout.main_view_smart_bms, 2)
                    setupFieldForSmartBmsPage(R.string.bmsCurrent)
                    setupFieldForSmartBmsPage(R.string.bmsVoltage)
                    setupFieldForSmartBmsPage(R.string.bmsTemp1)
                    setupFieldForSmartBmsPage(R.string.bmsTemp2)
                    setupFieldForSmartBmsPage(R.string.bmsTemp3)
                    setupFieldForSmartBmsPage(R.string.bmsTemp4)
                    setupFieldForSmartBmsPage(R.string.bmsTemp5)
                    setupFieldForSmartBmsPage(R.string.bmsTemp6)
                    setupFieldForSmartBmsPage(R.string.bmsMaxCell)
                    setupFieldForSmartBmsPage(R.string.bmsMinCell)
                    setupFieldForSmartBmsPage(R.string.bmsCellDiff)
                    setupFieldForSmartBmsPage(R.string.bmsCell1)
                    setupFieldForSmartBmsPage(R.string.bmsCell2)
                    setupFieldForSmartBmsPage(R.string.bmsCell3)
                    setupFieldForSmartBmsPage(R.string.bmsCell4)
                    setupFieldForSmartBmsPage(R.string.bmsCell5)
                    setupFieldForSmartBmsPage(R.string.bmsCell6)
                    setupFieldForSmartBmsPage(R.string.bmsCell7)
                    setupFieldForSmartBmsPage(R.string.bmsCell8)
                    setupFieldForSmartBmsPage(R.string.bmsCell9)
                    setupFieldForSmartBmsPage(R.string.bmsCell10)
                    setupFieldForSmartBmsPage(R.string.bmsCell11)
                    setupFieldForSmartBmsPage(R.string.bmsCell12)
                    setupFieldForSmartBmsPage(R.string.bmsCell13)
                    setupFieldForSmartBmsPage(R.string.bmsCell14)
                    setupFieldForSmartBmsPage(R.string.bmsCell15)
                    setupFieldForSmartBmsPage(R.string.bmsCell16)
                    setupFieldForSmartBmsPage(R.string.bmsCell17)
                    setupFieldForSmartBmsPage(R.string.bmsCell18)
                    setupFieldForSmartBmsPage(R.string.bmsCell19)
                    setupFieldForSmartBmsPage(R.string.bmsCell20)
                    setupFieldForSmartBmsPage(R.string.bmsCell21)
                    setupFieldForSmartBmsPage(R.string.bmsCell22)
                    setupFieldForSmartBmsPage(R.string.bmsCell23)
                    setupFieldForSmartBmsPage(R.string.bmsCell24)
                    setupFieldForSmartBmsPage(R.string.bmsCell25)
                    setupFieldForSmartBmsPage(R.string.bmsCell26)
                    setupFieldForSmartBmsPage(R.string.bmsCell27)
                    setupFieldForSmartBmsPage(R.string.bmsCell28)
                    setupFieldForSmartBmsPage(R.string.bmsCell29)
                    setupFieldForSmartBmsPage(R.string.bmsCell30)
                    setupFieldForSmartBmsPage(R.string.bmsCell31)
                    setupFieldForSmartBmsPage(R.string.bmsCell32)
                    setupFieldForSmartBmsPage(R.string.bmsCell33)
                    setupFieldForSmartBmsPage(R.string.bmsCell34)
                    setupFieldForSmartBmsPage(R.string.bmsCell35)
                    setupFieldForSmartBmsPage(R.string.bmsCell36)
                } else {
                    removePage(R.layout.main_view_smart_bms)
                    return
                }
            }
            WHEEL_TYPE.NINEBOT_Z -> {
                if (WheelData.getInstance().protoVer == "") { //hide page for S2
                    addPage(R.layout.main_view_smart_bms, 2)
                    setupFieldForSmartBmsPage(R.string.bmsSn)
                    setupFieldForSmartBmsPage(R.string.bmsFw)
                    setupFieldForSmartBmsPage(R.string.bmsFactoryCap)
                    setupFieldForSmartBmsPage(R.string.bmsActualCap)
                    setupFieldForSmartBmsPage(R.string.bmsCycles)
                    setupFieldForSmartBmsPage(R.string.bmsChrgCount)
                    setupFieldForSmartBmsPage(R.string.bmsMfgDate)
                    setupFieldForSmartBmsPage(R.string.bmsStatus)
                    setupFieldForSmartBmsPage(R.string.bmsRemCap)
                    setupFieldForSmartBmsPage(R.string.bmsRemPerc)
                    setupFieldForSmartBmsPage(R.string.bmsCurrent)
                    setupFieldForSmartBmsPage(R.string.bmsVoltage)
                    setupFieldForSmartBmsPage(R.string.bmsTemp1)
                    setupFieldForSmartBmsPage(R.string.bmsTemp2)
                    setupFieldForSmartBmsPage(R.string.bmsHealth)
                    setupFieldForSmartBmsPage(R.string.bmsMaxCell)
                    setupFieldForSmartBmsPage(R.string.bmsMinCell)
                    setupFieldForSmartBmsPage(R.string.bmsCellDiff)
                    setupFieldForSmartBmsPage(R.string.bmsCell1)
                    setupFieldForSmartBmsPage(R.string.bmsCell2)
                    setupFieldForSmartBmsPage(R.string.bmsCell3)
                    setupFieldForSmartBmsPage(R.string.bmsCell4)
                    setupFieldForSmartBmsPage(R.string.bmsCell5)
                    setupFieldForSmartBmsPage(R.string.bmsCell6)
                    setupFieldForSmartBmsPage(R.string.bmsCell7)
                    setupFieldForSmartBmsPage(R.string.bmsCell8)
                    setupFieldForSmartBmsPage(R.string.bmsCell9)
                    setupFieldForSmartBmsPage(R.string.bmsCell10)
                    setupFieldForSmartBmsPage(R.string.bmsCell11)
                    setupFieldForSmartBmsPage(R.string.bmsCell12)
                    setupFieldForSmartBmsPage(R.string.bmsCell13)
                    setupFieldForSmartBmsPage(R.string.bmsCell14)
                    setupFieldForSmartBmsPage(R.string.bmsCell15)
                    setupFieldForSmartBmsPage(R.string.bmsCell16)
                } else {
                    removePage(R.layout.main_view_smart_bms)
                    return
                }
            }
            else -> {
                removePage(R.layout.main_view_smart_bms)
                return
            }
        }
        createSmartBmsPage()
    }
    //endregion

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (WheelLog.AppConfig.getResId(key)) {
            R.string.show_page_graph -> if (WheelLog.AppConfig.pageGraph) {
                addPage(R.layout.main_view_graph, 2)
            } else {
                removePage(R.layout.main_view_graph)
            }
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
