package com.cooper.wheellog

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.cooper.wheellog.utils.Alarms
import com.cooper.wheellog.utils.AudioUtil
import com.cooper.wheellog.utils.Constants
import androidx.core.math.MathUtils
import com.cooper.wheellog.utils.*
import com.cooper.wheellog.utils.SomeUtil.Companion.playBeep
import com.garmin.android.connectiq.ConnectIQ.*
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.*

class GarminConnectIQ : Service(), IQApplicationInfoListener, IQDeviceEventListener, IQApplicationEventListener, ConnectIQListener {
    private var keepAliveTimer: Timer? = null
    private var mSdkReady = false
    private var mConnectIQ = getInstance(this, IQConnectType.WIRELESS)
    private var mDevice: IQDevice? = null
    private var mApp: IQApp? = null
    private var mWebServer: GarminConnectIQWebServer? = null
    private var useBeta = WheelLog.AppConfig.useGarminBetaCompanion

    override fun onBind(intent: Intent): IBinder? {
        Timber.i("onBind")
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        super.onStartCommand(intent, flags, startId)
        instance = this

        // Setup Connect IQ
        mApp = if (useBeta) {
            IQApp(BETA_APP_ID)
        } else {
            IQApp(STABLE_APP_ID)
        }
        mConnectIQ.initialize(this, false, this)
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
        Timber.d("populateDeviceList")
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
        Timber.d("registerWithDevice")
        if (mDevice != null && mSdkReady) {
            // Register for device status updates
            try {
                mConnectIQ.registerForDeviceEvents(mDevice, this)
            } catch (e: InvalidStateException) {
                Timber.wtf("InvalidStateException:  We should not be here!")
            }

            // Register for application status updates
            try {
                if (useBeta) {
                    mConnectIQ.getApplicationInfo(BETA_APP_ID, mDevice, this)
                } else {
                    mConnectIQ.getApplicationInfo(STABLE_APP_ID, mDevice, this)
                }
            } catch (e1: InvalidStateException) {
                Timber.d("e1: ${e1.message}")
            } catch (e1: ServiceUnavailableException) {
                Timber.d("e2: ${e1.message}")
            }

            // Register to receive messages from the device
            try {
                mConnectIQ.registerForAppEvents(mDevice, mApp, this)
            } catch (e: InvalidStateException) {
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun unregisterWithDevice() {
        Timber.d("unregisterWithDevice")
        if (mDevice != null && mSdkReady) {
            // It is a good idea to unregister everything and shut things down to
            // release resources and prevent unwanted callbacks.
            try {
                mConnectIQ.unregisterForDeviceEvents(mDevice)
                if (mApp != null) {
                    mConnectIQ.unregisterForApplicationEvents(mDevice, mApp)
                }
            } catch (ignored: InvalidStateException) {

            }
        }
    }

    private fun cancelRefreshTimer() {
        keepAliveTimer?.cancel()
        keepAliveTimer = null
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
            mConnectIQ.openStore("35719a02-8a5d-46bc-b474-f26c54c4e045")
        } catch (ignored: InvalidStateException) {
        } catch (ignored: ServiceUnavailableException) {
        }
    }

    // IQDeviceEventListener METHODS
    override fun onDeviceStatusChanged(device: IQDevice, status: IQDeviceStatus) {
        Timber.d("onDeviceStatusChanged")
        Timber.d("status is: ${status.name}")
        if (status.name == "CONNECTED") {
            startWebServer()
        } else {
            cancelRefreshTimer()
            stopWebServer()
        }
    }

    // IQApplicationEventListener
    override fun onMessageReceived(device: IQDevice, app: IQApp, message: List<Any>, status: IQMessageStatus) {
        Timber.d("onMessageReceived")
        // This thing won't do anything, because every data transmit is done through a web server
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
                mConnectIQ.sendMessage(mDevice, mApp, mWebServer!!.listeningPort) { _: IQDevice?, _: IQApp?, status: IQMessageStatus ->
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
        } catch (e: IOException) {
            Timber.e(e, "IOException happened while starting ConnectIQ web server")
        }
    }

    private fun stopWebServer() {
        Timber.d("stopWebServer")
        mWebServer?.stop()
        mWebServer = null
    }

    companion object {
        const val STABLE_APP_ID = "487e6172-972c-4f93-a4db-26fd689f935a"
        const val BETA_APP_ID = "433c30dc-f316-4d11-a16e-de153d297705"

        private var instance: GarminConnectIQ? = null
        val isInstanceCreated: Boolean
            get() = instance != null
    }
}

internal class GarminConnectIQWebServer(context: Context) : NanoHTTPD("127.0.0.1", 0) {
    private var applicationContext: Context

    init {
        start(SOCKET_READ_TIMEOUT, false)
        applicationContext = context
    }

    private fun playHorn() {
        playBeep(WheelLog.AppConfig.hornMode == 1, false)
    }

    private val speedStr
        get() = run {
            val speed = if (!WheelLog.AppConfig.useMph) {
                WheelData.getInstance().speedDouble
            } else {
                MathsUtil.kmToMiles(WheelData.getInstance().speedDouble)
            }
            if (speed.toString().length > 3) {
                ((speed * 10).toInt().toFloat() / 10).toString()
            } else speed.toString()
        }

    private val topSpeed
        get() = if (!WheelLog.AppConfig.useMph) {
            WheelData.getInstance().topSpeed
        } else {
            MathsUtil.kmToMiles(WheelData.getInstance().topSpeed.toDouble()).toInt()
        }

    private val temperature
        get() = if (!WheelLog.AppConfig.useMph) {
            WheelData.getInstance().temperature
        } else {
            MathsUtil.celsiusToFahrenheit(WheelData.getInstance().temperature.toDouble()).toInt()
        }

    private val avgSpeed
        get() = if (!WheelLog.AppConfig.useMph) {
            WheelData.getInstance().averageSpeedDouble.toInt()
        } else {
            MathsUtil.kmToMiles(WheelData.getInstance().averageSpeedDouble).toInt()
        }

    private val avgRidingSpeed
        get() = if (!WheelLog.AppConfig.useMph) {
            WheelData.getInstance().averageRidingSpeedDouble.toInt()
        } else {
            MathsUtil.kmToMiles(WheelData.getInstance().averageRidingSpeedDouble).toInt()
        }

    override fun serve(session: IHTTPSession): Response {
        val wd = WheelData.getInstance()
        val ac = WheelLog.AppConfig
      
        return when (session.method) {
            Method.GET -> {
                when (session.uri) {
                    "/data/main" -> {
                        val message = JSONObject()
                        message.put("speed", speedStr)
                        message.put("topSpeed", ((topSpeed / 10).toFloat() / 10).toString())
                        message.put("speedLimit", ac.maxSpeed)
                        message.put("useMph", ac.useMph)
                        message.put("battery", wd.batteryLevel)
                        message.put("temp", temperature)
                        message.put("pwm", String.format("%02.0f", wd.calculatedPwm))
                        message.put("maxPwm", String.format("%02.0f", wd.maxPwm))
                        message.put("connectedToWheel", wd.isConnected)
                        message.put("wheelModel", wd.model)

                        return newFixedLengthResponse(Response.Status.OK, "application/json", message.toString()) // Send data
                    }
                    "/data/details" -> {
                        val message = JSONObject()
                        message.put("useMph", ac.useMph)
                        message.put("avgRidingSpeed", avgRidingSpeed)
                        message.put("avgSpeed", avgSpeed)
                        message.put("topSpeed", ((topSpeed / 10).toFloat() / 10).toString())
                        message.put("voltage", wd.voltageDouble.toString())
                        message.put("maxVoltage", wd.maxVoltageForWheel.toString())
                        message.put("battery", wd.batteryLevel)
                        message.put("ridingTime", wd.ridingTimeString)
                        message.put("distance", wd.distance)
                        message.put("pwm", String.format("%02.0f", wd.calculatedPwm))
                        message.put("maxPwm", String.format("%02.0f", wd.maxPwm))
                        message.put("torque", wd.torque)
                        message.put("power", wd.powerDouble)
                        message.put("maxPower", wd.maxPowerDouble)

                        message.put("connectedToWheel", wd.isConnected)

                        return newFixedLengthResponse(Response.Status.OK, "application/json", message.toString()) // Send data
                    }
                    "/data/alarms" -> {
                        val message = "${Alarms.alarm}"
                        newFixedLengthResponse(Response.Status.OK, "application/json", message) // Send data
                    }
                    else -> {
                        Timber.i("404 Wrong endpoint")
                        newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404: File not found")
                    }
                }
            }
            Method.POST -> {
                when (session.uri) {
                    "/actions/triggerHorn" -> {
                        playHorn()
                        newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Executed!") // Send data
                    }
                    "/actions/frontLight/enable" -> {
                        wd.updateLight(true)
                        newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Executed!") // Send data
                    }
                    "/actions/frontLight/disable" -> {
                        newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Executed!") // Send data
                    }
                    else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Error 404, action not found.")
                }
            }
            else -> {
                Timber.i("404 Wrong method")
                newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404: File not found")
            }
        }
    }
}