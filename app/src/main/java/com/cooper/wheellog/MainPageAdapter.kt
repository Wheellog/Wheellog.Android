package com.cooper.wheellog

import android.app.Activity
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.utils.FileUtil
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SomeUtil.Companion.playBeep
import com.cooper.wheellog.utils.StringUtil.Companion.deleteFirstSentence
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
import java.text.SimpleDateFormat
import java.util.*


class MainPageAdapter(var pages: MutableList<Int>) : RecyclerView.Adapter<MainPageAdapter.ViewHolder>(), OnSharedPreferenceChangeListener {

    private var xAxis_labels = ArrayList<String>()

    var isInit = false
    lateinit var wheelView: WheelView
    lateinit var chart1: LineChart
    var position: Int = -1

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
            notifyItemRemoved(index)
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(viewType, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        when (pages[position]) {
            R.layout.main_view_main -> {
                wheelView = holder.itemView.findViewById(R.id.wheelView)
                wheelView.setOnTouchListener(object : OnTouchListener {
                    private val gestureDetector = GestureDetector(
                            context, object : SimpleOnGestureListener() {
                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            WheelData.getInstance().adapter?.switchFlashlight()
                            return super.onDoubleTap(e)
                        }

                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            if (WheelLog.AppConfig.useBeepOnSingleTap) {
                                playBeep(context)
                                return true
                            }
                            return super.onSingleTapConfirmed(e)
                        }
                    })

                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        gestureDetector.onTouchEvent(event)
                        return true
                    }
                })
                isInit = true
            }
            R.layout.main_view_graph -> {
                chart1 = holder.itemView.findViewById(R.id.chart)
                chart1.setDrawGridBackground(false)
                chart1.description.isEnabled = false
                chart1.setHardwareAccelerationEnabled(true)
                chart1.isHighlightPerTapEnabled = false
                chart1.isHighlightPerDragEnabled = false
                chart1.legend.textColor = context.resources.getColor(android.R.color.white)
                chart1.setNoDataText(context.resources.getString(R.string.no_chart_data))
                chart1.setNoDataTextColor(context.resources.getColor(android.R.color.white))

                val leftAxis: YAxis = chart1.axisLeft
                val rightAxis: YAxis = chart1.axisRight
                leftAxis.axisMinimum = 0f
                rightAxis.axisMinimum = 0f
                leftAxis.setDrawGridLines(false)
                rightAxis.setDrawGridLines(false)
                leftAxis.textColor = context.resources.getColor(android.R.color.white)
                rightAxis.textColor = context.resources.getColor(android.R.color.white)

                val xAxis: XAxis = chart1.xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.textColor = context.resources.getColor(android.R.color.white)
                xAxis.valueFormatter = chartAxisValueFormatter
            }
            R.layout.main_view_events -> {
                eventsTextView = holder.itemView.findViewById(R.id.events_textbox)
            }
            R.layout.main_view_trips -> {
                val listOfTrips = holder.itemView.findViewById<RecyclerView>(R.id.list_trips)
                val context = holder.itemView.context
                listOfTrips.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                listOfTrips.adapter = TripAdapter(context, FileUtil.fillTrips(context))
            }
            R.layout.main_view_smart_bms -> {
                tvBms1Sn = holder.itemView.findViewById(R.id.tvBms1Sn)
                tvBms2Sn = holder.itemView.findViewById(R.id.tvBms2Sn)
                tvBms1Fw = holder.itemView.findViewById(R.id.tvBms1Fw)
                tvBms2Fw = holder.itemView.findViewById(R.id.tvBms2Fw)
                tvBms1FactoryCap = holder.itemView.findViewById(R.id.tvBms1FactoryCap)
                tvBms2FactoryCap = holder.itemView.findViewById(R.id.tvBms2FactoryCap)
                tvBms1ActualCap = holder.itemView.findViewById(R.id.tvBms1ActualCap)
                tvBms2ActualCap = holder.itemView.findViewById(R.id.tvBms2ActualCap)
                tvBms1Cycles = holder.itemView.findViewById(R.id.tvBms1Cycles)
                tvBms2Cycles = holder.itemView.findViewById(R.id.tvBms2Cycles)
                tvBms1ChrgCount = holder.itemView.findViewById(R.id.tvBms1ChrgCount)
                tvBms2ChrgCount = holder.itemView.findViewById(R.id.tvBms2ChrgCount)
                tvBms1MfgDate = holder.itemView.findViewById(R.id.tvBms1MfgDate)
                tvBms2MfgDate = holder.itemView.findViewById(R.id.tvBms2MfgDate)
                tvBms1Status = holder.itemView.findViewById(R.id.tvBms1Status)
                tvBms2Status = holder.itemView.findViewById(R.id.tvBms2Status)
                tvBms1RemCap = holder.itemView.findViewById(R.id.tvBms1RemCap)
                tvBms2RemCap = holder.itemView.findViewById(R.id.tvBms2RemCap)
                tvBms1RemPerc = holder.itemView.findViewById(R.id.tvBms1RemPerc)
                tvBms2RemPerc = holder.itemView.findViewById(R.id.tvBms2RemPerc)
                tvBms1Current = holder.itemView.findViewById(R.id.tvBms1Current)
                tvBms2Current = holder.itemView.findViewById(R.id.tvBms2Current)
                tvBms1Voltage = holder.itemView.findViewById(R.id.tvBms1Voltage)
                tvBms2Voltage = holder.itemView.findViewById(R.id.tvBms2Voltage)
                tvBms1Temp1 = holder.itemView.findViewById(R.id.tvBms1Temp1)
                tvBms2Temp1 = holder.itemView.findViewById(R.id.tvBms2Temp1)
                tvBms1Temp2 = holder.itemView.findViewById(R.id.tvBms1Temp2)
                tvBms2Temp2 = holder.itemView.findViewById(R.id.tvBms2Temp2)
                tvBms1Health = holder.itemView.findViewById(R.id.tvBms1Health)
                tvBms2Health = holder.itemView.findViewById(R.id.tvBms2Health)
                tvBms1Cell1 = holder.itemView.findViewById(R.id.tvBms1Cell1)
                tvBms2Cell1 = holder.itemView.findViewById(R.id.tvBms2Cell1)
                tvBms1Cell2 = holder.itemView.findViewById(R.id.tvBms1Cell2)
                tvBms2Cell2 = holder.itemView.findViewById(R.id.tvBms2Cell2)
                tvBms1Cell3 = holder.itemView.findViewById(R.id.tvBms1Cell3)
                tvBms2Cell3 = holder.itemView.findViewById(R.id.tvBms2Cell3)
                tvBms1Cell4 = holder.itemView.findViewById(R.id.tvBms1Cell4)
                tvBms2Cell4 = holder.itemView.findViewById(R.id.tvBms2Cell4)
                tvBms1Cell5 = holder.itemView.findViewById(R.id.tvBms1Cell5)
                tvBms2Cell5 = holder.itemView.findViewById(R.id.tvBms2Cell5)
                tvBms1Cell6 = holder.itemView.findViewById(R.id.tvBms1Cell6)
                tvBms2Cell6 = holder.itemView.findViewById(R.id.tvBms2Cell6)
                tvBms1Cell7 = holder.itemView.findViewById(R.id.tvBms1Cell7)
                tvBms2Cell7 = holder.itemView.findViewById(R.id.tvBms2Cell7)
                tvBms1Cell8 = holder.itemView.findViewById(R.id.tvBms1Cell8)
                tvBms2Cell8 = holder.itemView.findViewById(R.id.tvBms2Cell8)
                tvBms1Cell9 = holder.itemView.findViewById(R.id.tvBms1Cell9)
                tvBms2Cell9 = holder.itemView.findViewById(R.id.tvBms2Cell9)
                tvBms1Cell10 = holder.itemView.findViewById(R.id.tvBms1Cell10)
                tvBms2Cell10 = holder.itemView.findViewById(R.id.tvBms2Cell10)
                tvBms1Cell11 = holder.itemView.findViewById(R.id.tvBms1Cell11)
                tvBms2Cell11 = holder.itemView.findViewById(R.id.tvBms2Cell11)
                tvBms1Cell12 = holder.itemView.findViewById(R.id.tvBms1Cell12)
                tvBms2Cell12 = holder.itemView.findViewById(R.id.tvBms2Cell12)
                tvBms1Cell13 = holder.itemView.findViewById(R.id.tvBms1Cell13)
                tvBms2Cell13 = holder.itemView.findViewById(R.id.tvBms2Cell13)
                tvBms1Cell14 = holder.itemView.findViewById(R.id.tvBms1Cell14)
                tvBms2Cell14 = holder.itemView.findViewById(R.id.tvBms2Cell14)
                tvTitleBms1Cell15 = holder.itemView.findViewById(R.id.tvTitleBms1Cell15)
                tvBms1Cell15 = holder.itemView.findViewById(R.id.tvBms1Cell15)
                tvTitleBms2Cell15 = holder.itemView.findViewById(R.id.tvTitleBms2Cell15)
                tvBms2Cell15 = holder.itemView.findViewById(R.id.tvBms2Cell15)
                tvTitleBms1Cell16 = holder.itemView.findViewById(R.id.tvTitleBms1Cell16)
                tvBms1Cell16 = holder.itemView.findViewById(R.id.tvBms1Cell16)
                tvTitleBms2Cell16 = holder.itemView.findViewById(R.id.tvTitleBms2Cell16)
                tvBms2Cell16 = holder.itemView.findViewById(R.id.tvBms2Cell16)
            }
        }
    }

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun getItemViewType(position: Int): Int {
        return pages[position]
    }

    private fun getActivity(): MainActivity? {
        var context = wheelView.context
        while (context is ContextWrapper) {
            if (context is MainActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    fun updateScreen(updateGraph: Boolean) {
        if (!isInit) {
            return
        }
        val data = WheelData.getInstance()
        when (pages[position]) {
            R.layout.main_view_main -> {
                data.bmsView = false
                wheelView.setSpeed(data.speed)
                wheelView.setBattery(data.batteryLevel)
                wheelView.setTemperature(data.temperature)
                wheelView.setRideTime(data.ridingTimeString)
                wheelView.setTopSpeed(data.topSpeedDouble)
                wheelView.setDistance(data.distanceDouble)
                wheelView.setTotalDistance(data.totalDistanceDouble)
                wheelView.setVoltage(data.voltageDouble)
                wheelView.setCurrent(data.currentDouble)
                wheelView.setAverageSpeed(data.averageRidingSpeedDouble)
                wheelView.setMaxPwm(data.maxPwm)
                wheelView.setMaxTemperature(data.maxTemp)
                wheelView.setPwm(data.calculatedPwm)
                wheelView.updateViewBlocksVisibility()
                wheelView.redrawTextBoxes()
                wheelView.invalidate()

                val profileName = WheelLog.AppConfig.profileName
                if (profileName.trim { it <= ' ' } == "") {
                    wheelView.setWheelModel(if (data.model == "") data.name else data.model)
                } else {
                    wheelView.setWheelModel(profileName)
                }
            }
            R.layout.main_view_params_list -> {
                getActivity()?.updateSecondScreen()
            }
            R.layout.main_view_graph -> {
                if (updateGraph) {
                    xAxis_labels = WheelData.getInstance().xAxis
                    if (xAxis_labels.size > 0) {
                        val dataSetSpeed: LineDataSet
                        val dataSetCurrent: LineDataSet
                        if (chart1.data == null) {
                            dataSetSpeed = LineDataSet(null, chart1.context.getString(R.string.speed_axis))
                            dataSetCurrent = LineDataSet(null, chart1.context.getString(R.string.current_axis))
                            dataSetSpeed.lineWidth = 2f
                            dataSetCurrent.lineWidth = 2f
                            dataSetSpeed.axisDependency = YAxis.AxisDependency.LEFT
                            dataSetCurrent.axisDependency = YAxis.AxisDependency.RIGHT
                            dataSetSpeed.mode = LineDataSet.Mode.CUBIC_BEZIER
                            dataSetCurrent.mode = LineDataSet.Mode.CUBIC_BEZIER
                            dataSetSpeed.color = chart1.context.resources.getColor(android.R.color.white)
                            dataSetCurrent.color = chart1.context.resources.getColor(R.color.accent)
                            dataSetSpeed.setDrawCircles(false)
                            dataSetCurrent.setDrawCircles(false)
                            dataSetSpeed.setDrawValues(false)
                            dataSetCurrent.setDrawValues(false)
                            val chart1_lineData = LineData()
                            chart1_lineData.addDataSet(dataSetCurrent)
                            chart1_lineData.addDataSet(dataSetSpeed)
                            chart1.data = chart1_lineData
                            chart1.findViewById<View>(R.id.leftAxisLabel).visibility = View.VISIBLE
                            chart1.findViewById<View>(R.id.rightAxisLabel).visibility = View.VISIBLE
                        } else {
                            dataSetSpeed = chart1.data.getDataSetByLabel(chart1.context.getString(R.string.speed_axis), true) as LineDataSet
                            dataSetCurrent = chart1.data.getDataSetByLabel(chart1.context.getString(R.string.current_axis), true) as LineDataSet
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
                        chart1.data.notifyDataChanged()
                        chart1.notifyDataSetChanged()
                        chart1.invalidate()
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

    // TODO - Tinder.plant and не то это место...
    private var eventsTextView: TextView? = null
    private var eventsCurrentCount = 0
    private var eventsMaxCount = 500
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun logEvent(message: String) {
        if (eventsTextView == null) {
            return
        }

        val formattedMessage = String.format("[%s] %s%n", timeFormatter.format(Date()), message)
        if (eventsCurrentCount < eventsMaxCount) {
            eventsTextView?.append(formattedMessage)
            eventsCurrentCount++
        } else {
            eventsTextView?.text = String.format("%s%s", deleteFirstSentence(eventsTextView!!.text), formattedMessage)
        }
    }

    private var chartAxisValueFormatter: IAxisValueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            return if (value < xAxis_labels.size) xAxis_labels.get(value.toInt()) else ""
        }

        // we don't draw numbers, so no decimal digits needed
        override fun getDecimalDigits(): Int {
            return 0
        }
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
            }
            R.string.view_blocks_string -> updateScreen(true)
        }
    }

    class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)
}
