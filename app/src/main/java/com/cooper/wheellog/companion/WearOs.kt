package com.cooper.wheellog.companion

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.SomeUtil
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.wheellog.shared.Constants
import com.wheellog.shared.serialize
import kotlinx.coroutines.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class WearOs(var context: Context): MessageClient.OnMessageReceivedListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private var pingPong = false

    fun updatePages() {
        if (!pingPong) {
            // pingPong - means that WearOS is successfully connected.
            return
        }
        val dataRequest = PutDataMapRequest.create(Constants.wearOsPagesItemPath)
        dataRequest.dataMap.apply {
            putString("pages", WheelLog.AppConfig.wearOsPages.serialize())
            putLong("time", System.currentTimeMillis())
        }
        val request = dataRequest.asPutDataRequest()
        request.setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
            .addOnFailureListener {
                Timber.e("[wear] %s", it.localizedMessage)
            }
    }

    fun updateData() {
        if (!pingPong) {
            // pingPong - means that WearOS is successfully connected.
            return
        }
        val wd = WheelData.getInstance()
        val dataRequest = PutDataMapRequest.create(Constants.wearOsDataItemPath)
        dataRequest.dataMap.apply {
            putDouble("speed", wd.speedDouble)
            putDouble("max_speed", wd.topSpeed.toDouble())
            putDouble("voltage", wd.voltageDouble)
            putDouble("current", wd.currentDouble)
            putDouble("max_current", wd.maxCurrentDouble)
            putDouble("power", wd.powerDouble)
            putDouble("max_power", wd.maxPowerDouble)
            putDouble("pwm", wd.calculatedPwm)
            putDouble("max_pwm", wd.maxPwm)
            putInt("temperature", wd.temperature)
            putInt("max_temperature", wd.maxTemp)
            putInt("battery",wd.batteryLevel)
            putInt("battery_lowest", wd.batteryLowestLevel)
            putDouble("distance", wd.distanceDouble)
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
        if (messageEvent.path == Constants.wearOsDataMessagePath) {
            when (messageEvent.data.toString(Charsets.UTF_8)) {
                // TODO: localization
                Constants.wearOsPong  -> {
                    Toast.makeText(context,"WearOs watch connected successfully!", Toast.LENGTH_LONG).show()
                    pingPong = true
                    updatePages()
                }
                Constants.wearOsHorn -> SomeUtil.playBeep(context)
                Constants.wearOsLight -> WheelData.getInstance().adapter?.switchFlashlight()
                else -> Timber.wtf("Unknown message from wear")
            }
        }
    }

    private fun sendMessage(message: String, path: String = Constants.wearOsDataMessagePath) {
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
                Timber.e("[wear] exception %s", it.localizedMessage)
            }
    }

    init {
        addMessageListener()
        updateData()
        GlobalScope.launch {
            sendMessage( Constants.wearOsPing)
            delay(500)
            // if the wear application did not receive a response from the ping,
            // then an attempt to launch it
            if (!pingPong) {
                sendMessage("", Constants.wearOsStartPath)
            }
        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun stop() {
        sendMessage(Constants.wearOsFinish)
        removeMessageListener()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun addMessageListener() {
        Wearable.getMessageClient(context).addListener(this)
    }

    private fun removeMessageListener() {
        Wearable.getMessageClient(context).removeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "wearos_pages") {
            updatePages()
        }
    }
}