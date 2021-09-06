package com.cooper.wheellog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableRecyclerView
import com.cooper.wheellog.databinding.ActivityWearBinding
import com.cooper.wheellog.utils.CommonUtils.Companion.messagePath
import com.cooper.wheellog.utils.CommonUtils.Companion.sendMessage
import com.cooper.wheellog.utils.CommonUtils.Companion.vibrate
import com.google.android.gms.wearable.*
import com.wheellog.shared.WearPage
import com.wheellog.shared.WearPages
import java.lang.IllegalArgumentException
import java.util.*


class WearActivity : FragmentActivity(),
        MessageClient.OnMessageReceivedListener,
        DataClient.OnDataChangedListener {

    private val dataItemPath = "/wheel_data"
    private lateinit var mMainRecyclerAdapter: MainRecyclerAdapter
    private var wd = WearData()
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        vibrate(this, longArrayOf(0, 100))
        // for test
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                wd.temperature = (Math.random() * 100).toInt()
                wd.speed = (Math.random() * 50)
                wd.maxSpeed = 60
                wd.distance = (Math.random() * 5000).toLong()
                mMainRecyclerAdapter.updateScreen()
            }
        }, 1000, 200)
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
        mMainRecyclerAdapter = MainRecyclerAdapter(WearPages.allOf(WearPage::class.java), wd)
        val binding = ActivityWearBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.apply {
            // Aligns the first and last items on the list vertically centered on the screen.
            isEdgeItemsCenteringEnabled = true
            isVerticalScrollBarEnabled = true

            // Improves performance because we know changes in content do not change the layout size of
            // the RecyclerView.
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(this@WearActivity)
            adapter = mMainRecyclerAdapter
            keepScreenOn = true
//            addOnScrollListener(object: RecyclerView.OnScrollListener() {
//                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                    super.onScrolled(recyclerView, dx, dy)
//                    val offset = recyclerView.computeVerticalScrollOffset()
//                    if (offset % recyclerView.height == 0) {
//                        mMainRecyclerAdapter.position = offset / recyclerView.height
//                    }
//                }
//            })
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
                "ping" -> {
                    sendMessage(this, "pong")
                    showAToast("connected!")
                    vibrate(this, longArrayOf(0, 100))
                }
                "finish" -> finish()
                else -> showAToast("Unknown message: " + messageEvent.data.toString(Charsets.UTF_8))
            }
        }
    }

    private fun applyData(map: DataMap) {
        try {
            wd.apply {
                speed = map.getDouble("speed")
                maxSpeed = map.getInt("max_speed")
                voltage = map.getDouble("voltage")
                current = map.getDouble("current")
                maxCurrent = map.getDouble("max_current")
                pwm = map.getDouble("pwm")
                maxPwm = map.getDouble("max_pwm")
                temperature = map.getInt("temperature")
                maxTemperature = map.getInt("max_temperature")
                maxPower = map.getDouble("max_power")
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
                val pagesString = map.getString("pages")
                if (pagesString != null) {
                    try {
                        val newPages = WearPage.deserialize(pagesString)
                        val newSize = newPages.size
                        val oldSize = mMainRecyclerAdapter.pages.size
                        if (newSize > 0 && newPages != mMainRecyclerAdapter.pages) {
                            mMainRecyclerAdapter.apply {
                                pages = newPages
                                when {
                                    newSize > oldSize ->
                                        notifyItemRangeRemoved(
                                            oldSize, newSize - oldSize
                                        )
                                    newSize < oldSize ->
                                        notifyItemRangeInserted(
                                            oldSize, oldSize - newSize
                                        )
                                    else -> notifyItemRangeChanged(1, newSize - 1)
                                }
                            }
                        }
                    } catch (ignored: IllegalArgumentException) {
                    }
                }
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

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            when(event.type) {
                DataEvent.TYPE_CHANGED -> {
                    val dataItem = event.dataItem
                    val path = dataItem.uri.path
                    if (path == dataItemPath) {
                        applyData(DataMapItem.fromDataItem(dataItem).dataMap)
                    }
                }
            }
        }
    }
}


