package com.cooper.wheellog

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.google.android.gms.wearable.*

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
    }

    private fun setupViews() {
        val pages = arrayListOf(R.layout.recycler_row_main)
        mMainRecyclerAdapter = MainRecyclerAdapter(pages, wd)

        val mWearableRecyclerView = findViewById<WearableRecyclerView>(R.id.recycler_view)
        // Aligns the first and last items on the list vertically centered on the screen.
        mWearableRecyclerView.isEdgeItemsCenteringEnabled = true

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mWearableRecyclerView.setHasFixedSize(true)

        val mLayoutManager = LinearLayoutManager(this)
        mWearableRecyclerView.layoutManager = mLayoutManager
        mWearableRecyclerView.adapter = mMainRecyclerAdapter
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
                                maxPower = map.getDouble("max_power")
                                battery = map.getInt("battery")
                                mainUnit = map.getString("main_unit", "kmh")
                                timeStamp = map.getLong("timestamp")
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


