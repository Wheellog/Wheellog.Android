package com.cooper.wheellog

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.databinding.ActivityWearBinding
import com.cooper.wheellog.utils.CommonUtils.Companion.messagePath
import com.cooper.wheellog.utils.CommonUtils.Companion.sendMessage
import com.cooper.wheellog.utils.CommonUtils.Companion.vibrate
import com.google.android.gms.wearable.*
import com.wheellog.shared.Constants
import com.wheellog.shared.WearPage
import com.wheellog.shared.WearPages
import java.lang.IllegalArgumentException
import java.util.*


class WearActivity : FragmentActivity(),
        MessageClient.OnMessageReceivedListener,
        DataClient.OnDataChangedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var mMainRecyclerAdapter: MainRecyclerAdapter
    private var wd = WearData()
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        vibrate(this, longArrayOf(0, 100))
        // for test
//        Timer().scheduleAtFixedRate(object : TimerTask() {
//            override fun run() {
//                wd.temperature = (Math.random() * 100).toInt()
//                wd.speed = (Math.random() * 50)
//                wd.maxSpeed = 60
//                wd.distance = (Math.random() * 5000).toLong()
//                mMainRecyclerAdapter.updateScreen()
//            }
//        }, 1000, 200)
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
        Wearable.getDataClient(this).removeListener(this)
    }

    private fun setupViews() {
        mMainRecyclerAdapter = MainRecyclerAdapter(WearPage.Main and WearPage.Voltage, wd)
        val binding = ActivityWearBinding.inflate(layoutInflater)
        setContentView(binding.root)
        recyclerView = binding.recyclerView.apply {
            // Aligns the first and last items on the list vertically centered on the screen.
            isEdgeItemsCenteringEnabled = true
            isVerticalScrollBarEnabled = true

            // Improves performance because we know changes in content do not change the layout size of
            // the RecyclerView.
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(this@WearActivity)
            adapter = mMainRecyclerAdapter
            keepScreenOn = true
        }
    }

    private fun showAToast(message: String?) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == messagePath) {
            when (messageEvent.data.toString(Charsets.UTF_8)) {
                // TODO: Localization
                Constants.wearOsPing -> {
                    sendMessage(this, Constants.wearOsPong)
                    showAToast("connected!")
                    vibrate(this, longArrayOf(0, 100))
                }
                Constants.wearOsFinish -> finish()
                else -> showAToast("Unknown message: " + messageEvent.data.toString(Charsets.UTF_8))
            }
        }
    }

    private fun applyData(map: DataMap) {
        try {
            wd.apply {
                speed.apply {
                    value = map.getDouble("speed")
                    max = map.getDouble("max_speed", max)
                }
                voltage.apply {
                    value = map.getDouble("voltage")
                }
                current.apply {
                    value = map.getDouble("current")
                    max = map.getDouble("max_current", max)
                }
                pwm.apply {
                    value = map.getDouble("pwm")
                    max = map.getDouble("max_pwm", max)
                }
                temperature.apply {
                    value = map.getDouble("temperature")
                    max = map.getDouble("max_temperature", max)
                }
                power.apply {
                    value = map.getDouble("power")
                    max = map.getDouble("max_power", max)
                }
                distance = map.getDouble("distance", 0.0)
                battery = map.getInt("battery")
                batteryLowest = map.getInt("battery_lowest")
                mainUnit = map.getString("main_unit", "kmh")
                currentOnDial = map.getBoolean("currentOnDial")
                val alarmInt = map.getInt("alarm")
                alarmSpeed = alarmInt and 1 == 1
                alarmCurrent = alarmInt and 2 == 1
                alarmTemp = alarmInt and 4 == 1
                timeStamp = map.getLong("timestamp")
                timeString = map.getString("time_string", "waiting...")
            }
            mMainRecyclerAdapter.updateScreen()
            if (wd.alarmTemp || wd.alarmSpeed || wd.alarmCurrent) {
                vibrate(this, longArrayOf(0, 500, 50, 300))
                // TODO: localization
                showAToast(
                    when {
                        wd.alarmTemp -> "temperature"
                        wd.alarmCurrent -> "current"
                        else -> "speed"
                    }
                )
            }
        } catch (ex: Exception) {
        }
    }

    private fun updatePages(map: DataMap) {
        val pagesString = map.getString("pages", "")
        if (pagesString == "") {
            val newPages = WearPage.deserialize(pagesString)
            val newAdapter = MainRecyclerAdapter(newPages, wd)
            recyclerView.swapAdapter(newAdapter, false)
            mMainRecyclerAdapter = newAdapter
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            when(event.type) {
                DataEvent.TYPE_CHANGED -> {
                    val dataItem = event.dataItem
                    when (dataItem.uri.path) {
                        Constants.wearOsDataItemPath ->
                            applyData(DataMapItem.fromDataItem(dataItem).dataMap)
                        Constants.wearOsPagesItemPath ->
                            updatePages(DataMapItem.fromDataItem(dataItem).dataMap)
                    }
                }
            }
        }
    }
}


