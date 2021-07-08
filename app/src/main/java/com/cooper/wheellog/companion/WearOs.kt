package com.cooper.wheellog.companion

import android.content.Context
import android.widget.Toast
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.SomeUtil
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class WearOs(var context: Context): MessageClient.OnMessageReceivedListener {
    private val dataItemPath = "/wheel_data"
    private val messagePath = "/messages"
    private var pingPong = false

    fun updateData() {
        val wd = WheelData.getInstance()
        val dataRequest = PutDataMapRequest.create(dataItemPath)
        dataRequest.dataMap.apply {
            putDouble("speed", wd.speedDouble)
            putInt("max_speed", wd.topSpeed)
            putDouble("voltage", wd.voltageDouble)
            putDouble("current", wd.currentDouble)
            putDouble("max_current", wd.maxCurrent)
            putDouble("pwm", wd.calculatedPwm)
            putDouble("max_pwm", wd.maxPwm)
            putInt("temperature", wd.temperature)
            putInt("max_temperature", wd.maxTemp)
            putDouble("max_power", wd.maxPower)
            putInt("battery",wd.batteryLevel)
            putInt("battery_lowest", wd.batteryLevel) // TODO
            putInt("battery", wd.batteryLevel)
            putString("main_unit",
                    if (WheelLog.AppConfig.useMph)
                        context.getString(R.string.mph)
                    else
                        context.getString(R.string.kmh))
            putBoolean("currentOnDial", WheelLog.AppConfig.currentOnDial)
            putInt("alarm", wd.alarm)
            putLong("timestamp", wd.lastLifeData)
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            putString("time_string", sdf.format(Date(wd.lastLifeData)))
        }
        val request = dataRequest.asPutDataRequest()
        request.setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
            .addOnFailureListener {
                Timber.e("[wear] %s", it.localizedMessage)
            }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == messagePath) {
            when (messageEvent.data.toString(Charsets.UTF_8)) {
                // TODO: localization
                "pong" -> {
                    Toast.makeText(context,"WearOs watch connected successfully!", Toast.LENGTH_LONG).show()
                    pingPong = true
                }
                "horn" -> SomeUtil.playBeep(context)
                "light" -> WheelData.getInstance().adapter?.switchFlashlight()
                else -> Timber.wtf("Unknown message from wear")
            }
        }
    }

    private fun sendMessage(message: String, path: String = messagePath) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener {
                it.forEach { node ->
                    if (node.isNearby) {
                        Wearable.getMessageClient(context)
                            .sendMessage(node.id, path, message.toByteArray(Charsets.UTF_8))
                    }
                }
            }
            .addOnFailureListener {
                Timber.wtf("[wear] exception %s", it.localizedMessage)
            }
    }

    init {
        addMessageListener()
        updateData()
        GlobalScope.launch {
            sendMessage("ping")
            delay(500)
            // if the wear application did not receive a response from the ping,
            // then an attempt to launch it
            if (!pingPong) {
                sendMessage("", "/start/wearos")
            }
        }
    }

    fun stop() {
        sendMessage("finish")
        removeMessageListener()
    }

    fun addMessageListener() {
        Wearable.getMessageClient(context).addListener(this)
    }

    fun removeMessageListener() {
        Wearable.getMessageClient(context).removeListener(this)
    }
}