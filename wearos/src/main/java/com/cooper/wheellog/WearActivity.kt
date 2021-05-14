package com.cooper.wheellog

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.google.android.gms.wearable.*
import java.util.*

class WearActivity : FragmentActivity(),
        MessageClient.OnMessageReceivedListener,
        DataClient.OnDataChangedListener {

    private val dataItemPath = "/wheel_data"
    private val messagePath = "/messages"
    private lateinit var mMainRecyclerAdapter: MainRecyclerAdapter
    private var wd = WearData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear)
        setupViews()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupViews() {
        val pages = arrayListOf(0) // TODO add pages
        mMainRecyclerAdapter = MainRecyclerAdapter(pages, wd)

        findViewById<WearableRecyclerView>(R.id.recycler_view).apply {
            // Aligns the first and last items on the list vertically centered on the screen.
            isEdgeItemsCenteringEnabled = true

            // Improves performance because we know changes in content do not change the layout size of
            // the RecyclerView.
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(this@WearActivity)
            adapter = mMainRecyclerAdapter
            keepScreenOn = true
        }
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

    fun horn() {
        sendMessage("horn")
    }

    private fun sendMessage(message: String) {
        Wearable.getNodeClient(applicationContext).connectedNodes
            .addOnSuccessListener {
                it.forEach { node ->
                    if (node.isNearby) {
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(node.id, messagePath, message.toByteArray(Charsets.UTF_8))
                    }
                }
            }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == messagePath) {
            when (messageEvent.data.toString(Charsets.UTF_8)) {
                // TODO: Localization
                "ping" -> {
                    sendMessage("pong")
                    Toast.makeText(
                        applicationContext,
                        "connected!", Toast.LENGTH_LONG
                    ).show()
                }
                else -> Toast.makeText(
                    applicationContext,
                    "Unknown message: " + messageEvent.data.toString(Charsets.UTF_8), Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            when(event.type) {
                DataEvent.TYPE_CHANGED -> {
                    val dataItem = event.dataItem
                    val path = dataItem.uri.path
                    if (path == dataItemPath) {
                        try {
                            val map = DataMapItem.fromDataItem(dataItem).dataMap
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
                                alarm = map.getBoolean("alarm")
                                timeStamp = map.getLong("timestamp")
                                timeString = map.getString("time_string", "waiting...")
                            }
                            mMainRecyclerAdapter.updateScreen()
                        } catch (ex: Exception) {
                        }
                    }
                }
            }
        }
    }
}


