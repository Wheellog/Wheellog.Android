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
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var isConnected = false
    private var sendPingJob: Job

    fun sendUpdateData() {
        if (!isConnected) {
            sendMessage(Constants.wearOsPingMessage)
            return
        }
        val wd = WheelData.getInstance()
        val dataRequest = PutDataMapRequest.create(Constants.wearOsDataItemPath)
        dataRequest.dataMap.apply {
            putDouble(Constants.wearOsSpeedData, wd.speedDouble)
            putDouble(Constants.wearOsMaxSpeedData, wd.topSpeed.toDouble())
            putDouble(Constants.wearOsVoltageData, wd.voltageDouble)
            putDouble(Constants.wearOsCurrentData, wd.currentDouble)
            putDouble(Constants.wearOsMaxCurrentData, wd.maxCurrentDouble)
            putDouble(Constants.wearOsPowerData, wd.powerDouble)
            putDouble(Constants.wearOsMaxPowerData, wd.maxPowerDouble)
            putDouble(Constants.wearOsPWMData, wd.calculatedPwm)
            putDouble(Constants.wearOsMaxPWMData, wd.maxPwm)
            putDouble(Constants.wearOsTemperatureData, wd.temperature)
            putDouble(Constants.wearOsMaxTemperatureData, wd.maxTemp)
            putInt(Constants.wearOsBatteryData,wd.batteryLevel)
            putInt(Constants.wearOsBatteryLowData, wd.batteryLowestLevel)
            putDouble(Constants.wearOsDistanceData, wd.distanceDouble)
            putString(Constants.wearOsUnitData,
                    if (WheelLog.AppConfig.useMph)
                        context.getString(R.string.mph)
                    else
                        context.getString(R.string.kmh))
            putBoolean(Constants.wearOsCurrentOnDialData, WheelLog.AppConfig.currentOnDial)
            putInt(Constants.wearOsAlarmData, wd.alarm)
            putLong(Constants.wearOsTimestampData, wd.lastLifeData)
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            putString(Constants.wearOsTimeStringData, sdf.format(Date(wd.lastLifeData)))
        }
        val request = dataRequest.asPutDataRequest()
        request.setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
            .addOnFailureListener {
                Timber.e("[wear] %s", it.localizedMessage)
            }
    }

    fun stop() {
        sendMessage(Constants.wearOsFinishMessage)
        removeMessageListener()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == Constants.wearOsDataMessagePath) {
            when (messageEvent.data.toString(Charsets.UTF_8)) {
                // TODO: localization
                Constants.wearOsPongMessage  -> {
                    Toast.makeText(context,"WearOs watch connected successfully!", Toast.LENGTH_LONG).show()
                    isConnected = true
                    sendUpdatePages()
                    sendPingJob.cancel()
                }
                Constants.wearOsHornMessage -> SomeUtil.playBeep(context)
                Constants.wearOsLightMessage -> WheelData.getInstance().adapter?.switchFlashlight()
                else -> Timber.wtf("Unknown message from wear")
            }
        }
    }

    init {
        addMessageListener()
        sendUpdateData()
        sendPingJob = backgroundScope.launch {
            sendMessage(Constants.wearOsPingMessage)
            delay(500)
            ensureActive()
            // if the wear application did not receive a response from the ping,
            // then an attempt to launch it
            if (!isConnected) {
                sendMessage("", Constants.wearOsStartPath)
            }
        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
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

    private fun sendUpdatePages() {
        if (!isConnected) {
            return
        }
        val dataRequest = PutDataMapRequest.create(Constants.wearOsPagesItemPath)
        dataRequest.dataMap.apply {
            putString(Constants.wearOsPagesData, WheelLog.AppConfig.wearOsPages.serialize())
            putLong(Constants.wearOsTimestampData, System.currentTimeMillis())
        }
        val request = dataRequest.asPutDataRequest()
        request.setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
            .addOnFailureListener {
                Timber.e("[wear] %s", it.localizedMessage)
            }
    }

    private fun addMessageListener() {
        Wearable.getMessageClient(context).addListener(this)
    }

    private fun removeMessageListener() {
        Wearable.getMessageClient(context).removeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Constants.wearPages) {
            sendUpdatePages()
        }
    }
}