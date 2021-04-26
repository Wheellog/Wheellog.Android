package com.cooper.wheellog

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.SomeUtil.Companion.playBeep
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.ConnectIQ.*
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import fi.iki.elonen.NanoHTTPD
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.*

class GarminConnectIQ : Service(), IQApplicationInfoListener, IQDeviceEventListener, IQApplicationEventListener, ConnectIQListener {
    private var keepAliveTimer: Timer? = null
    private var mSdkReady = false
    private var mConnectIQ: ConnectIQ = getInstance(this, IQConnectType.WIRELESS)
    private var mDevice: IQDevice? = null
    private var mMyApp: IQApp? = null
    private var mWebServer: GarminConnectIQWebServer? = null
    override fun onBind(intent: Intent): IBinder? {
        Timber.i("onBind")
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        super.onStartCommand(intent, flags, startId)
        instance = this

        // Setup Connect IQ
        mMyApp = IQApp(APP_ID)
        mConnectIQ.initialize(this, true, this)
        startForeground(Constants.MAIN_NOTIFICATION_ID, WheelLog.Notifications.notification)
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
        cancelRefreshTimer()
        try {
            mConnectIQ.unregisterAllForEvents()
            mConnectIQ.shutdown(this)
        } catch (e: InvalidStateException) {
            // This is usually because the SDK was already shut down
            // so no worries.
        }
        stopWebServer()
        unregisterWithDevice()
        stopForeground(false)
        instance = null
    }

    // General METHODS
    private fun populateDeviceList() {
        Timber.d( "populateDeviceList")
        try {
            val mDevices = mConnectIQ.knownDevices
            if (mDevices != null && mDevices.isNotEmpty()) {
                mDevice = mDevices[0]
                registerWithDevice()
            }
        } catch (e: InvalidStateException) {
            // This generally means you forgot to call initialize(), but since
            // we are in the callback for initialize(), this should never happen
        } catch (e: ServiceUnavailableException) {
            // This will happen if for some reason your app was not able to connect
            // to the ConnectIQ service running within Garmin Connect Mobile.  This
            // could be because Garmin Connect Mobile is not installed or needs to
            // be upgraded.
            Toast.makeText(this, R.string.garmin_connectiq_service_unavailable_message, Toast.LENGTH_LONG).show()
        }
    }

    private fun registerWithDevice() {
        Timber.d( "registerWithDevice")
        if (mDevice != null && mSdkReady) {
            // Register for device status updates
            try {
                mConnectIQ.registerForDeviceEvents(mDevice, this)
            } catch (e: InvalidStateException) {
                Timber.wtf("InvalidStateException:  We should not be here!")
            }

            // Register for application status updates
            try {
                mConnectIQ.getApplicationInfo(APP_ID, mDevice, this)
            } catch (e1: InvalidStateException) {
                Timber.d( "e1: ${e1.message}")
            } catch (e1: ServiceUnavailableException) {
                Timber.d( "e2: ${e1.message}")
            }

            // Register to receive messages from the device
            try {
                mConnectIQ.registerForAppEvents(mDevice, mMyApp, this)
            } catch (e: InvalidStateException) {
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun unregisterWithDevice() {
        Timber.d( "unregisterWithDevice")
        if (mDevice != null && mSdkReady) {
            // It is a good idea to unregister everything and shut things down to
            // release resources and prevent unwanted callbacks.
            try {
                mConnectIQ.unregisterForDeviceEvents(mDevice)
                if (mMyApp != null) {
                    mConnectIQ.unregisterForApplicationEvents(mDevice, mMyApp)
                }
            } catch (ignored: InvalidStateException) {
            }
        }
    }

    private fun cancelRefreshTimer() {
        if (keepAliveTimer != null) {
            keepAliveTimer!!.cancel()
            keepAliveTimer = null
        }
    }

    // IQApplicationInfoListener METHODS
    override fun onApplicationInfoReceived(app: IQApp) {
        Timber.d("onApplicationInfoReceived")
        Timber.d(app.toString())
    }

    override fun onApplicationNotInstalled(arg0: String) {
        Timber.d("onApplicationNotInstalled")

        // The WheelLog app is not installed on the device so we have
        // to let the user know to install it.
        cancelRefreshTimer() // no point in sending data...
        Toast.makeText(this, R.string.garmin_connectiq_missing_app_message, Toast.LENGTH_LONG).show()
        try {
            mConnectIQ.openStore(APP_ID)
        } catch (ignored: InvalidStateException) {
        } catch (ignored: ServiceUnavailableException) {
        }
    }

    // IQDeviceEventListener METHODS
    override fun onDeviceStatusChanged(device: IQDevice, status: IQDeviceStatus) {
        Timber.d("onDeviceStatusChanged")
        Timber.d("status is: ${status.name}")
        when (status.name) {
            "CONNECTED" -> {
                startWebServer()
            }
            "NOT_PAIRED", "NOT_CONNECTED", "UNKNOWN" -> {
                cancelRefreshTimer()
                stopWebServer()
            }
        }
    }

    // IQApplicationEventListener
    override fun onMessageReceived(device: IQDevice, app: IQApp, message: List<Any>, status: IQMessageStatus) {
        Timber.d("onMessageReceived")

        // We know from our widget that it will only ever send us strings, but in case
        // we get something else, we are simply going to do a toString() on each object in the
        // message list.
        var builder: StringBuilder? = StringBuilder()
        if (message.isNotEmpty()) {
            for (o in message) {
                if (o is HashMap<*, *>) {
                    try {
                        builder = null
                    } catch (ex: Exception) {
                        builder!!.append("MonkeyHash received:\n\n")
                        builder.append(o.toString())
                    }
                } else {
                    builder!!.append(o.toString())
                    builder.append("\r\n")
                }
            }
        } else {
            builder!!.append("Received an empty message from the ConnectIQ application")
        }
        if (builder != null) {
            Toast.makeText(applicationContext, builder.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    // ConnectIQListener METHODS
    override fun onInitializeError(errStatus: IQSdkErrorStatus) {
        Timber.d("sdk initialization error")
        mSdkReady = false
    }

    override fun onSdkReady() {
        Timber.d("sdk is ready")
        mSdkReady = true
        populateDeviceList()
    }

    override fun onSdkShutDown() {
        Timber.d("sdk shut down")
        mSdkReady = false
    }

    private fun startWebServer() {
        Timber.d("startWebServer")
        if (mWebServer != null) return
        try {
            mWebServer = GarminConnectIQWebServer(applicationContext)
            Timber.d("port is: ${mWebServer!!.listeningPort}")
            try {
                mConnectIQ.sendMessage(mDevice, mMyApp, mWebServer!!.listeningPort) { _: IQDevice?, _: IQApp?, status: IQMessageStatus ->
                    Timber.d("message status: ${status.name}")
                    if (status.name !== "SUCCESS") Toast.makeText(this@GarminConnectIQ, status.name, Toast.LENGTH_LONG).show()
                }
            } catch (e: InvalidStateException) {
                Timber.e("ConnectIQ is not in a valid state")
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show()
            } catch (e: ServiceUnavailableException) {
                Timber.e("ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?")
                Toast.makeText(this, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?", Toast.LENGTH_LONG).show()
            }
        } catch (ignored: IOException) {
        }
    }

    private fun stopWebServer() {
        Timber.d("stopWebServer")
        if (mWebServer != null) {
            mWebServer!!.stop()
            mWebServer = null
        }
    }

    companion object {
        const val APP_ID = "487e6172-972c-4f93-a4db-26fd689f935a"

        private var instance: GarminConnectIQ? = null
        val isInstanceCreated: Boolean
            get() = instance != null

        fun instance(): GarminConnectIQ? {
            return instance
        }
    }
}

internal class GarminConnectIQWebServer(context: Context) : NanoHTTPD("127.0.0.1", 0) {
    private var applicationContext: Context

    init {
        start(SOCKET_READ_TIMEOUT, false)
        applicationContext = context
    }

    private fun playHorn() {
        val hornMode = WheelLog.AppConfig.hornMode
        playBeep(applicationContext, hornMode == 1, false)
    }

    override fun serve(session: IHTTPSession): Response {
        return when (session.method) {
            Method.GET -> {
                val wheelData = WheelData.getInstance()
                when (session.uri) {
                    "/data?type=main" -> {
                        val message = JSONObject()
                        return try {
                            message.put("0", wheelData.speed)
                            message.put("1", WheelLog.AppConfig.useMph)
                            message.put("2", wheelData.batteryLevel)
                            message.put("3", wheelData.temperature)
                            message.put("4", if (WheelLog.AppConfig.useShortPwm) {
                                "${wheelData.calculatedPwm} / ${wheelData.maxPwm}"
                            } else {
                                wheelData.modeStr
                            })
                            newFixedLengthResponse(Response.Status.OK, "application/json", message.toString()) // Send data
                        } catch (e: JSONException) {
                            newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_PLAINTEXT, "Failed to get data")
                        }
                    }
                    "/data?type=details" -> {
                        return try {
                            val message = JSONObject()
                            message.put("0", WheelLog.AppConfig.useMph)
                            message.put("1", wheelData.averageRidingSpeedDouble)
                            message.put("2", wheelData.topSpeed)
                            message.put("3", wheelData.voltageDouble)
                            message.put("4", wheelData.batteryLevel)
                            message.put("5", wheelData.rideTimeString)
                            message.put("6", wheelData.distance)
                            newFixedLengthResponse(Response.Status.OK, "application/json", message.toString()) // Send data
                        } catch (e: JSONException) {
                            newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_PLAINTEXT, "Failed to get data")
                        }
                    }
                    "/data?type=alarms" -> {
                        val message = "${wheelData.alarm}"
                        newFixedLengthResponse(Response.Status.OK, "application/json", message) // Send data
                    }
                    else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404: File not found")
                }
            }
            Method.POST -> {
                when (session.uri) {
                    "/actions/triggerHorn" -> {
                        playHorn()
                        newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Executed!") // Send data
                    }
                    else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Error 404, action not found.")
                }
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Error 404, file not found.")
        }
    }
}