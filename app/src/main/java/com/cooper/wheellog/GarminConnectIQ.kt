package com.cooper.wheellog

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.cooper.wheellog.GarminConnectIQ
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.SomeUtil.Companion.playBeep
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.ConnectIQ.*
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import com.garmin.monkeybrains.serialization.MonkeyHash
import fi.iki.elonen.NanoHTTPD
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.*

class GarminConnectIQ : Service(), IQApplicationInfoListener, IQDeviceEventListener, IQApplicationEventListener, ConnectIQListener {
    enum class MessageType {
        EUC_DATA, PLAY_HORN, HTTP_READY
    }

    var lastSpeed = 0
    var lastBattery = 0
    var lastTemperature = 0
    var lastFanStatus = 0
    var lastRideTime = 0
    var lastDistance = 0
    var lastTopSpeed = 0
    var lastConnectionState = false
    var lastPower = 0
    private var keepAliveTimer: Timer? = null
    val handler = Handler() // to call toast from the TimerTask
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
        stopForeground(false)
        instance = null
    }

    // General METHODS
    private fun populateDeviceList() {
        Timber.d(TAG, "populateDeviceList")
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
        Timber.d(TAG, "registerWithDevice")
        if (mDevice != null && mSdkReady) {
            // Register for device status updates
            try {
                mConnectIQ.registerForDeviceEvents(mDevice, this)
            } catch (e: InvalidStateException) {
                Timber.tag(TAG).wtf("InvalidStateException:  We should not be here!")
            }

            // Register for application status updates
            try {
                mConnectIQ.getApplicationInfo(APP_ID, mDevice, this)
            } catch (e1: InvalidStateException) {
                Timber.d(TAG, "e1: ${e1.message}")
            } catch (e1: ServiceUnavailableException) {
                Timber.d(TAG, "e2: ${e1.message}")
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
        Timber.d(TAG, "unregisterWithDevice")
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

    private fun startRefreshTimer() {
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                handler.post { refreshData() }
            }
        }
        keepAliveTimer = Timer()
        keepAliveTimer!!.scheduleAtFixedRate(timerTask, 0, 1500) // 1.5cs
    }

    private fun refreshData() {
        if (WheelData.getInstance() == null) return
        try {
            val data = HashMap<Any, Any>()
            lastSpeed = WheelData.getInstance().speed / 10
            data[MESSAGE_KEY_SPEED] = lastSpeed
            lastBattery = WheelData.getInstance().batteryLevel
            data[MESSAGE_KEY_BATTERY] = lastBattery
            lastTemperature = WheelData.getInstance().temperature
            data[MESSAGE_KEY_TEMPERATURE] = lastTemperature
            lastFanStatus = WheelData.getInstance().fanStatus
            data[MESSAGE_KEY_FAN_STATE] = lastFanStatus
            lastConnectionState = WheelData.getInstance().isConnected
            data[MESSAGE_KEY_BT_STATE] = lastConnectionState
            data[MESSAGE_KEY_VIBE_ALERT] = false
            data[MESSAGE_KEY_USE_MPH] = WheelLog.AppConfig.useMph
            data[MESSAGE_KEY_MAX_SPEED] = WheelLog.AppConfig.maxSpeed
            lastRideTime = WheelData.getInstance().rideTime
            data[MESSAGE_KEY_RIDE_TIME] = lastRideTime
            lastDistance = WheelData.getInstance().distance
            data[MESSAGE_KEY_DISTANCE] = lastDistance / 100
            lastTopSpeed = WheelData.getInstance().topSpeed
            data[MESSAGE_KEY_TOP_SPEED] = lastTopSpeed / 10
            lastPower = WheelData.getInstance().powerDouble.toInt()
            data[MESSAGE_KEY_POWER] = lastPower
            val message = HashMap<Any, Any>()
            message[MESSAGE_KEY_MSG_TYPE] = MessageType.EUC_DATA.ordinal
            message[MESSAGE_KEY_MSG_DATA] = MonkeyHash(data)
            try {
                mConnectIQ.sendMessage(mDevice, mMyApp, message) { _: IQDevice?, _: IQApp?, status: IQMessageStatus ->
                    Timber.d(TAG, "message status: ${status.name}")
                    if (status.name !== "SUCCESS") Toast.makeText(this@GarminConnectIQ, status.name, Toast.LENGTH_LONG).show()
                }
            } catch (e: InvalidStateException) {
                Timber.e(TAG, "ConnectIQ is not in a valid state")
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show()
            } catch (e: ServiceUnavailableException) {
                Timber.e(TAG, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?")
                Toast.makeText(this, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?", Toast.LENGTH_LONG).show()
            }
        } catch (ex: Exception) {
            Timber.e(ex, "refreshData")
            Toast.makeText(this, "Error refreshing data", Toast.LENGTH_SHORT).show()
        }
    }

    // IQApplicationInfoListener METHODS
    override fun onApplicationInfoReceived(app: IQApp) {
        Timber.d(TAG, "onApplicationInfoReceived")
        Timber.d(TAG, app.toString())
    }

    override fun onApplicationNotInstalled(arg0: String) {
        Timber.d(TAG, "onApplicationNotInstalled")

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
        Timber.d(TAG, "onDeviceStatusChanged")
        Timber.d(TAG, "status is: ${status.name}")
        when (status.name) {
            "CONNECTED" -> {
                // Disabled the push method for now until a dev from garmin can shed some light on the
                // intermittent FAILURE_DURING_TRANSFER that we have seen. This is documented here:
                // https://forums.garmin.com/developer/connect-iq/f/legacy-bug-reports/5144/failure_during_transfer-issue-again-now-using-comm-sample
                if (!FEATURE_FLAG_NANOHTTPD) {
                    startRefreshTimer()
                }

                // As a workaround, start a nanohttpd server that will listen for data requests from the watch. This is
                // also documented on the link above and is apparently a good workaround for the meantime. In our implementation
                // we instanciate the httpd server on an ephemeral port and send a message to the watch to tell it on which port
                // it can request its data.
                if (FEATURE_FLAG_NANOHTTPD) {
                    startWebServer()
                }
            }
            "NOT_PAIRED", "NOT_CONNECTED", "UNKNOWN" -> {
                cancelRefreshTimer()
                stopWebServer()
            }
        }
    }

    // IQApplicationEventListener
    override fun onMessageReceived(device: IQDevice, app: IQApp, message: List<Any>, status: IQMessageStatus) {
        Timber.d(TAG, "onMessageReceived")

        // We know from our widget that it will only ever send us strings, but in case
        // we get something else, we are simply going to do a toString() on each object in the
        // message list.
        var builder: StringBuilder? = StringBuilder()
        if (message.isNotEmpty()) {
            for (o in message) {
                if (o is HashMap<*, *>) {
                    try {
                        val msgType = o[MESSAGE_KEY_MSG_TYPE] as Int
                        if (msgType == MessageType.PLAY_HORN.ordinal) {
                            playHorn()
                        }
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
        Timber.d(TAG, "sdk initialization error")
        mSdkReady = false
    }

    override fun onSdkReady() {
        Timber.d(TAG, "sdk is ready")
        mSdkReady = true
        populateDeviceList()
    }

    override fun onSdkShutDown() {
        Timber.d(TAG, "sdk shut down")
        mSdkReady = false
    }

    fun playHorn() {
        val horn_mode = WheelLog.AppConfig.hornMode
        playBeep(applicationContext, horn_mode == 1, false)
    }

    fun startWebServer() {
        Timber.d(TAG, "startWebServer")
        if (mWebServer != null) return
        try {
            mWebServer = GarminConnectIQWebServer()
            Timber.d(TAG, "port is: ${mWebServer!!.listeningPort}")
            val data = HashMap<Any, Any>()
            data[MESSAGE_KEY_HTTP_PORT] = mWebServer!!.listeningPort
            val message = HashMap<Any, Any>()
            message[MESSAGE_KEY_MSG_TYPE] = MessageType.HTTP_READY.ordinal
            message[MESSAGE_KEY_MSG_DATA] = MonkeyHash(data)
            try {
                mConnectIQ.sendMessage(mDevice, mMyApp, message) { device: IQDevice?, app: IQApp?, status: IQMessageStatus ->
                    Timber.d(TAG, "message status: ${status.name}")
                    if (status.name !== "SUCCESS") Toast.makeText(this@GarminConnectIQ, status.name, Toast.LENGTH_LONG).show()
                }
            } catch (e: InvalidStateException) {
                Timber.e(TAG, "ConnectIQ is not in a valid state")
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show()
            } catch (e: ServiceUnavailableException) {
                Timber.e(TAG, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?")
                Toast.makeText(this, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?", Toast.LENGTH_LONG).show()
            }
        } catch (ignored: IOException) {
        }
    }

    private fun stopWebServer() {
        Timber.d(TAG, "stopWebServer")
        if (mWebServer != null) {
            mWebServer!!.stop()
            mWebServer = null
        }
    }

    companion object {
        val TAG = GarminConnectIQ::class.java.simpleName
        const val APP_ID = "487e6172-972c-4f93-a4db-26fd689f935a"

        // This will require Garmin Connect V4.22
        // https://forums.garmin.com/developer/connect-iq/i/bug-reports/connect-version-4-20-broke-local-http-access
        const val FEATURE_FLAG_NANOHTTPD = true
        const val MESSAGE_KEY_MSG_TYPE = -2
        const val MESSAGE_KEY_MSG_DATA = -1
        const val MESSAGE_KEY_SPEED = 0
        const val MESSAGE_KEY_BATTERY = 1
        const val MESSAGE_KEY_TEMPERATURE = 2
        const val MESSAGE_KEY_FAN_STATE = 3
        const val MESSAGE_KEY_BT_STATE = 4
        const val MESSAGE_KEY_VIBE_ALERT = 5
        const val MESSAGE_KEY_USE_MPH = 6
        const val MESSAGE_KEY_MAX_SPEED = 7
        const val MESSAGE_KEY_RIDE_TIME = 8
        const val MESSAGE_KEY_DISTANCE = 9
        const val MESSAGE_KEY_TOP_SPEED = 10
        const val MESSAGE_KEY_POWER = 12
        const val MESSAGE_KEY_ALARM1_SPEED = 13
        const val MESSAGE_KEY_ALARM2_SPEED = 14
        const val MESSAGE_KEY_ALARM3_SPEED = 15
        const val MESSAGE_KEY_HTTP_PORT = 99
        private var instance: GarminConnectIQ? = null
        val isInstanceCreated: Boolean
            get() = instance != null

        fun instance(): GarminConnectIQ? {
            return instance
        }
    }
}

internal class GarminConnectIQWebServer : NanoHTTPD("127.0.0.1", 0) {
    override fun serve(session: IHTTPSession): Response {
        return if (session.method == Method.GET && session.uri == "/data") {
            handleData()
        } else newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Error 404, file not found.")
    }

    private fun handleData(): Response {
        Timber.d("GarminConnectIQWebSe...")
        val data = JSONObject()
        return try {
            data.put("" + GarminConnectIQ.MESSAGE_KEY_SPEED, WheelData.getInstance().speed / 10) // Convert to km/h
            data.put("" + GarminConnectIQ.MESSAGE_KEY_BATTERY, WheelData.getInstance().batteryLevel)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_TEMPERATURE, WheelData.getInstance().temperature)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_FAN_STATE, WheelData.getInstance().fanStatus)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_BT_STATE, WheelData.getInstance().isConnected)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_VIBE_ALERT, false)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_USE_MPH, WheelLog.AppConfig.useMph)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_MAX_SPEED, WheelLog.AppConfig.maxSpeed)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_RIDE_TIME, WheelData.getInstance().rideTime)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_DISTANCE, WheelData.getInstance().distance / 100)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_TOP_SPEED, WheelData.getInstance().topSpeed / 10)
            data.put("" + GarminConnectIQ.MESSAGE_KEY_POWER, WheelData.getInstance().powerDouble.toInt())
            if (WheelData.getInstance().wheelType == Constants.WHEEL_TYPE.KINGSONG) {
                data.put("" + GarminConnectIQ.MESSAGE_KEY_ALARM1_SPEED, WheelLog.AppConfig.wheelKsAlarm1)
                data.put("" + GarminConnectIQ.MESSAGE_KEY_ALARM2_SPEED, WheelLog.AppConfig.wheelKsAlarm2)
                data.put("" + GarminConnectIQ.MESSAGE_KEY_ALARM3_SPEED, WheelLog.AppConfig.wheelKsAlarm3)
            }
            val message = JSONObject()
            message.put("" + GarminConnectIQ.MESSAGE_KEY_MSG_TYPE, GarminConnectIQ.MessageType.EUC_DATA.ordinal)
            message.put("" + GarminConnectIQ.MESSAGE_KEY_MSG_DATA, data)
            newFixedLengthResponse(Response.Status.OK, "application/json", message.toString())
        } catch (e: JSONException) {
            newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, "text/plain", "")
        }
    }

    init {
        start(SOCKET_READ_TIMEOUT, false)
    }
}