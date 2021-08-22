package com.cooper.wheellog.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.*
import timber.log.Timber
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class LoggingService: Service() {
    private lateinit var sdf: SimpleDateFormat
    private var mLocation: Location? = null
    private var mLastLocation: Location? = null
    private var mLocationDistance = 0.0
    private var mLocationManager: LocationManager? = null
    private var mLocationProvider = LocationManager.NETWORK_PROVIDER
    private var logLocationData = false
    private lateinit var fileUtil: FileUtil


    private val mBluetoothUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_BLUETOOTH_CONNECTION_STATE ->
                    if (mLocationManager != null && logLocationData) {
                        val connectionState = intent.getIntExtra(
                            Constants.INTENT_EXTRA_CONNECTION_STATE,
                            BleStateEnum.Disconnected.ordinal
                        )
                        if (connectionState == BleStateEnum.Connected.ordinal) {
                            if (PermissionsUtil.checkLocationPermission(context)) {
                                mLocationManager!!.requestLocationUpdates(
                                    mLocationProvider,
                                    250,
                                    0f,
                                    locationListener
                                )
                            } else {
                                showToast(R.string.logging_error_no_location_permission)
                            }

                        } else {
                            mLocationManager!!.removeUpdates(locationListener)
                        }
                    }
                Constants.ACTION_WHEEL_DATA_AVAILABLE -> updateFile()
            }
        }
    }

    private fun start(): Boolean {
        fileUtil = FileUtil(applicationContext)

        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE)
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE)
        applicationContext.registerReceiver(mBluetoothUpdateReceiver, intentFilter)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!PermissionsUtil.checkExternalFilePermission(applicationContext)) {
                showToast(R.string.logging_error_no_storage_permission)
                return false
            }
            if (!isExternalStorageReadable() || !isExternalStorageWritable()) {
                showToast(R.string.logging_error_storage_unavailable)
                return false
            }
        }

        logLocationData = WheelLog.AppConfig.logLocationData

        if (logLocationData && !PermissionsUtil.checkLocationPermission(applicationContext)) {
            showToast(R.string.logging_error_no_location_permission)
            logLocationData = false
        }

        sdf = SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.US)

        var writeToLastLog = false
        // TODO: fix me. WheelData не должна быть null
        val mac = WheelData.getInstance()?.mac ?: ""
        if (WheelLog.AppConfig.continueThisDayLog &&
            WheelLog.AppConfig.continueThisDayLogMacException != mac
        ) {
            val lastFileUtil = FileUtil.getLastLog(applicationContext)
            if (lastFileUtil != null &&
                lastFileUtil.file.path.contains(mac.replace(':', '_'))
            ) {
                fileUtil = lastFileUtil
                // parse prev log for filling wheeldata values
                val parser = ParserLogToWheelData()
                parser.parseFile(fileUtil)
                fileUtil.prepareStream()
                writeToLastLog = true
            }
        }

        if (!writeToLastLog) {
            val sdFormatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
            val filename = sdFormatter.format(Date()) + ".csv"
            if (!fileUtil.prepareFile(filename, mac)) {
                return false
            }
            WheelLog.AppConfig.continueThisDayLogMacException = ""
        }

        var locationHeaderString = ""
        if (logLocationData) {
            mLocationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (mLocationManager == null) {
                return false
            }
            val isGPSEnabled: Boolean = mLocationManager!!
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

            // Getting Network Provider status
            val isNetworkEnabled: Boolean = mLocationManager!!
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // Getting if the users wants to use GPS
            var useGPS = WheelLog.AppConfig.useGps
            if (!isGPSEnabled && !isNetworkEnabled) {
                logLocationData = false
                mLocationManager = null
                showToast(R.string.logging_error_all_location_providers_disabled)
            } else if (useGPS && !isGPSEnabled) {
                useGPS = false
                showToast(R.string.logging_error_gps_disabled)
            } else if (!useGPS && !isNetworkEnabled) {
                logLocationData = false
                mLocationManager = null
                showToast(R.string.logging_error_network_disabled)
            }
            if (logLocationData) {
                locationHeaderString =
                    ("${LogHeaderEnum.LATITUDE}," +
                            "${LogHeaderEnum.LONGITUDE}," +
                            "${LogHeaderEnum.GPS_SPEED}," +
                            "${LogHeaderEnum.GPS_ALT}," +
                            "${LogHeaderEnum.GPS_HEADING}," +
                            "${LogHeaderEnum.GPS_DISTANCE},").lowercase()
                mLocation = getLastBestLocation()
                mLocationProvider = LocationManager.NETWORK_PROVIDER
                if (useGPS) {
                    mLocationProvider = LocationManager.GPS_PROVIDER
                }
                // Acquire a reference to the system Location Manager
                mLocationManager?.requestLocationUpdates(
                    mLocationProvider,
                    250,
                    0f,
                    locationListener
                )
            }
        }

        if (!writeToLastLog) {
            fileUtil.writeLine(
                ("${LogHeaderEnum.DATE}," +
                        "${LogHeaderEnum.TIME}," +
                        locationHeaderString +
                        "${LogHeaderEnum.SPEED}," +
                        "${LogHeaderEnum.VOLTAGE}," +
                        "${LogHeaderEnum.PHASE_CURRENT}," +
                        "${LogHeaderEnum.CURRENT}," +
                        "${LogHeaderEnum.POWER}," +
                        "${LogHeaderEnum.TORQUE}," +
                        "${LogHeaderEnum.PWM}," +
                        "${LogHeaderEnum.BATTERY_LEVEL}," +
                        "${LogHeaderEnum.DISTANCE}," +
                        "${LogHeaderEnum.TOTALDISTANCE}," +
                        "${LogHeaderEnum.SYSTEM_TEMP}," +
                        "${LogHeaderEnum.TEMP2}," +
                        "${LogHeaderEnum.TILT}," +
                        "${LogHeaderEnum.ROLL}," +
                        "${LogHeaderEnum.MODE}," +
                        "${LogHeaderEnum.ALERT}").lowercase()
            )
        }

        val serviceIntent = Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED)
        serviceIntent.putExtra(
            Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION,
            fileUtil.absolutePath
        )
        serviceIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, true)
        applicationContext.sendBroadcast(serviceIntent)
        Timber.i("DataLogger Started")
        isStarted = true
        return true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("[log] onStartCommand called")
        startForeground(Constants.MAIN_NOTIFICATION_ID, WheelLog.Notifications.notification)
        if (!start()) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.i("[log] onDestroy called.")
        if (!isStarted) {
            return
        }

        isStarted = false
        try {
            applicationContext.unregisterReceiver(mBluetoothUpdateReceiver)
            if (mLocationManager != null && logLocationData) mLocationManager!!.removeUpdates(
                locationListener
            )
        } catch (ignored: Exception) {
        }

        if (logLocationData && mLastLocation != null) {
            WheelLog.AppConfig.lastLocationLaltitude = mLastLocation!!.latitude
            WheelLog.AppConfig.lastLocationLongitude = mLastLocation!!.longitude
        }

        var isBusy = false
        val path = fileUtil.absolutePath
        fileUtil.close()

        Timber.wtf("[log] Stopping...")

        // electro.club upload
        if (fileUtil.fileName != "" && WheelLog.AppConfig.autoUploadEc) {
            isBusy = true
            try {
                Timber.wtf("Uploading %s to electro.club", fileUtil.fileName)
                val data = fileUtil.readBytes()
                ElectroClub.instance.uploadTrack(
                    data,
                    fileUtil.fileName,
                    true
                ) { success: Boolean ->
                    if (!success) {
                        Timber.wtf("Upload failed...")
                    }
                    realDestroy(null)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Timber.wtf("Error upload log to electro.club: %s", e.toString())
                realDestroy(path)
            }
        }

        if (!isBusy) {
            realDestroy(path)
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    private fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    private fun updateFile() {
        if (!isStarted) {
            return
        }
        Timber.i("[log] updateFile called.")
        var locationDataString = ""
        if (logLocationData) {
            var longitude = ""
            var latitude = ""
            var gpsSpeed = ""
            var gpsAlt = ""
            var gpsBearing = ""
            if (mLocation != null) {
                longitude = mLocation!!.longitude.toString()
                latitude = mLocation!!.latitude.toString()
                gpsSpeed = (mLocation!!.speed * 3.6).toString()
                gpsAlt = mLocation!!.altitude.toString()
                gpsBearing = mLocation!!.bearing.toString()
                if (mLastLocation != null) {
                    mLocationDistance += mLastLocation!!.distanceTo(mLocation).toDouble()
                }
                mLastLocation = mLocation
            }
            locationDataString = String.format(
                Locale.US, "%s,%s,%s,%s,%s,%.0f,",
                latitude,
                longitude,
                gpsSpeed,
                gpsAlt,
                gpsBearing,
                mLocationDistance
            )
        }
        WheelData.getInstance()?.apply {
            fileUtil.writeLine(
                String.format(
                    Locale.US,
                    "%s,%s%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%d,%.2f,%.2f,%s,%s",
                    sdf.format(WheelData.getInstance().timeStamp),
                    locationDataString,
                    speedDouble,
                    voltageDouble,
                    phaseCurrentDouble,
                    currentDouble,
                    powerDouble,
                    torque,
                    calculatedPwm,
                    batteryLevel,
                    distance,
                    totalDistance,
                    temperature,
                    temperature2,
                    angle,
                    roll,
                    modeStr,
                    alert
                )
            )
        }
    }

    // Define a listener that responds to location updates
    private var locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Called when a new location is found by the network location provider.
            mLocation = location
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun realDestroy(path: String?) {
        val serviceIntent = Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED)
        if (path.isNullOrEmpty()) {
            serviceIntent.putExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION, path)
        }
        serviceIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, false)
        applicationContext.sendBroadcast(serviceIntent)
        Timber.wtf("[log] Stopped")
    }

    @SuppressLint("MissingPermission")
    private fun getLastBestLocation(): Location? {
        val locationGPS = mLocationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val locationNet = mLocationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val gpsLocationTime = locationGPS?.time ?: 0
        val netLocationTime = locationNet?.time ?: 0
        return if (0 < gpsLocationTime - netLocationTime) {
            locationGPS
        } else {
            locationNet
        }
    }

    private fun showToast(message_id: Int) {
        Toast.makeText(applicationContext, message_id, Toast.LENGTH_LONG).show()
    }

    companion object {
        @JvmStatic
        var isStarted: Boolean = false
            private set
    }
}