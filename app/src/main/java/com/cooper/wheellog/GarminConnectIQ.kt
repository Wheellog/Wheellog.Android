package com.cooper.wheellog

import android.app.Service
import android.content.Intent
import android.os.Handler
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
import com.garmin.monkeybrains.serialization.MonkeyHash
import timber.log.Timber
import java.util.*

open class GarminConnectIQ : Service(), IQApplicationInfoListener, IQDeviceEventListener, IQApplicationEventListener, ConnectIQListener {
    enum class MessageType {
        EUC_DATA, PLAY_HORN
    }

    private var lastSpeed = 0
    private var lastBattery = 0
    private var lastTemperature = 0
    private var lastFanStatus = 0
    private var lastRideTime = 0
    private var lastDistance = 0
    private var lastTopSpeed = 0
    private var lastConnectionState = false
    private var lastPower = 0
    private var keepAliveTimer: Timer? = null
    val handler = Handler() // to call toast from the TimerTask
    private var mSdkReady = false
    private var mConnectIQ: ConnectIQ? = null
    private var mDevices: List<IQDevice>? = null
    private var mDevice: IQDevice? = null
    private var mMyApp: IQApp? = null
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
        mConnectIQ = getInstance()
        mConnectIQ?.initialize(this, true, this)
        startForeground(Constants.MAIN_NOTIFICATION_ID, WheelLog.Notifications.notification)
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
        cancelRefreshTimer()
        try {
            mConnectIQ!!.unregisterAllForEvents()
            mConnectIQ!!.shutdown(this)
        } catch (e: InvalidStateException) {
            // This is usually because the SDK was already shut down
            // so no worries.
        }
        stopForeground(false)
        instance = null
    }

    // General METHODS
    private fun populateDeviceList() {
        Timber.d(TAG, "populateDeviceList")
        try {
            mDevices = mConnectIQ!!.knownDevices
            if (mDevices != null && mDevices!!.isNotEmpty()) {
                mDevice = mDevices!![0]
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
                mConnectIQ!!.registerForDeviceEvents(mDevice, this)
            } catch (e: InvalidStateException) {
                Timber.wtf(TAG, "InvalidStateException:  We should not be here!")
            }

            // Register for application status updates
            try {
                mConnectIQ!!.getApplicationInfo(APP_ID, mDevice, this)
            } catch (e1: InvalidStateException) {
                Timber.d(TAG, "e1: %s", e1.message)
            } catch (e1: ServiceUnavailableException) {
                Timber.d(TAG, "e2: %s", e1.message)
            }

            // Register to receive messages from the device
            try {
                mConnectIQ!!.registerForAppEvents(mDevice, mMyApp, this)
            } catch (e: InvalidStateException) {
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show()
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
        keepAliveTimer!!.scheduleAtFixedRate(timerTask, 0, 200) // 1.5cs
    }

    private fun refreshData() {
        if (WheelData.getInstance() == null) return
        try {
            val data = HashMap<Any, Any>()
            lastSpeed = WheelData.getInstance().speed
            data[messageKey_currentSpeed] = lastSpeed
            lastBattery = WheelData.getInstance().batteryLevel
            data[MESSAGE_KEY_BATTERY] = lastBattery
            lastTemperature = WheelData.getInstance().temperature
            data[messageKey_temperature] = lastTemperature
            lastFanStatus = WheelData.getInstance().fanStatus
            data[MESSAGE_KEY_FAN_STATE] = lastFanStatus
            lastConnectionState = WheelData.getInstance().isConnected
            data[messageKey_bluetooth] = lastConnectionState
            data[MESSAGE_KEY_VIBE_ALERT] = false
            data[MESSAGE_KEY_USE_MPH] = WheelLog.AppConfig.useMph
            data[MESSAGE_KEY_MAX_SPEED] = WheelLog.AppConfig.maxSpeed
            lastRideTime = WheelData.getInstance().rideTime
            data[messageKey_rideTime] = lastRideTime
            lastDistance = WheelData.getInstance().distance
            data[MESSAGE_KEY_DISTANCE] = lastDistance / 100
            lastTopSpeed = WheelData.getInstance().topSpeed
            data[messageKey_topSpeed] = lastTopSpeed / 10
            lastPower = WheelData.getInstance().powerDouble.toInt()
            data[messageKey_power] = lastPower
            val message = HashMap<Any, Any>()
            message[messageKey_messageType] = MessageType.EUC_DATA.ordinal
            message[messageKey_messageData] = MonkeyHash(data)
            try {
                mConnectIQ!!.sendMessage(mDevice, mMyApp, message) { _, _, status ->
                    Timber.d(TAG, "message status: %s", status.name)
                    if (status.name !== "SUCCESS") Toast.makeText(this@GarminConnectIQ, status.name, Toast.LENGTH_LONG).show()
                }
            } catch (e: InvalidStateException) {
                Timber.e("ConnectIQ is not in a valid state")
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show()
            } catch (e: ServiceUnavailableException) {
                Timber.e("ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?")
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
            mConnectIQ!!.openStore(APP_ID)
        } catch (e: InvalidStateException) {
        } catch (e: ServiceUnavailableException) {
        }
    }

    // IQDeviceEventListener METHODS
    override fun onDeviceStatusChanged(device: IQDevice, status: IQDeviceStatus) {
        Timber.d(TAG, "onDeviceStatusChanged")
        Timber.d("status is: %s", status.name)
        when (status.name) {
            "CONNECTED" -> {
                startRefreshTimer()
            }
            "NOT_PAIRED", "NOT_CONNECTED", "UNKNOWN" -> {
                cancelRefreshTimer()
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
                        val msgType = o[messageKey_messageType] as Int
                        if (msgType == MessageType.PLAY_HORN.ordinal) {
                            playHorn()
                        }
                        builder = null
                    } catch (ex: Exception) {
                        builder?.append("MonkeyHash received:\n\n")
                        builder?.append(o.toString())
                    }
                } else {
                    builder?.append(o.toString())
                    builder?.append("\r\n")
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

    private fun playHorn() {
        val hornMode = WheelLog.AppConfig.hornMode
        playBeep(applicationContext, hornMode == 1, false)
    }

    companion object {
        val TAG = GarminConnectIQ::class.java.simpleName
        const val APP_ID = "487e6172-972c-4f93-a4db-26fd689f935a"

        const val messageKey_messageType = -2
        const val messageKey_messageData = -1
        const val messageKey_currentSpeed = 0
        const val messageKey_batteryPercentage = 1
        const val messageKey_batteryVoltage = 2
        const val messageKey_temperature = 3
        const val messageKey_bluetooth = 4
        const val messageKey_power = 5
        const val messageKey_bottomSubtitle = 6
        const val messageKey_averageSpeed = 7
        const val messageKey_topSpeed = 8
        const val messageKey_rideDistance = 9
        const val messageKey_rideTime = 10
        const val messageKey_pwm = 11
        const val messageKey_maxPwm = 12
        const val messageKey_alarmType = 13
        const val messageKey_maxDial = 14
        const val messageKey_useMph = 15
        

        private var instance: GarminConnectIQ? = null

        fun instance(): GarminConnectIQ? {
            return instance
        }
        fun isInstanceCreated(): Boolean {
            return instance != null
        }
    }
}