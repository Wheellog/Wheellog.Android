package com.cooper.wheellog

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.FileUtil
import com.cooper.wheellog.utils.NotificationUtil
import com.cooper.wheellog.utils.ParserLogToWheelData
import com.cooper.wheellog.utils.PermissionsUtil.checkExternalFilePermission
import com.cooper.wheellog.utils.PermissionsUtil.checkLocationPermission
import com.welie.blessed.ConnectionState
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoggingService : Service() {
    private val appConfig: AppConfig by inject()
    private val notifications: NotificationUtil by inject()
    private var sdf: SimpleDateFormat? = null
    private var mLocation: Location? = null
    private var mLastLocation: Location? = null
    private var mLocationDistance = 0.0
    private val mLocationManager: LocationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private var mLocationProvider = LocationManager.NETWORK_PROVIDER
    private var logLocationData = false
    private lateinit var fileUtil: FileUtil
    private var ioState = CoroutineScope(Dispatchers.IO + Job())

    fun updateConnectionState(connectionState: ConnectionState) {
        if (logLocationData) {
            if (connectionState == ConnectionState.CONNECTED) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                mLocationManager.requestLocationUpdates(
                    mLocationProvider,
                    250,
                    0f,
                    locationListener
                )
            } else {
                mLocationManager.removeUpdates(locationListener)
            }
        }
    }

    private val mBinder: IBinder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        if (WheelData.getInstance() == null) {
            stopSelf()
            return null
        }
        instance = this
        fileUtil = FileUtil(applicationContext)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!checkExternalFilePermission(this)) {
                showToast(R.string.logging_error_no_storage_permission)
                stopSelf()
                return mBinder
            }
            if (!isExternalStorageReadable || !isExternalStorageWritable) {
                showToast(R.string.logging_error_storage_unavailable)
                stopSelf()
                return mBinder
            }
        }
        logLocationData = appConfig.logLocationData
        if (logLocationData && !checkLocationPermission(this)) {
            showToast(R.string.logging_error_no_location_permission)
            logLocationData = false
        }
        sdf = SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.US)
        var writeToLastLog = false
        val mac = WheelData.getInstance().mac
        if (appConfig.continueThisDayLog &&
            appConfig.continueThisDayLogMacException != mac
        ) {
            val lastFileUtil = FileUtil.getLastLog(applicationContext)
            if (lastFileUtil?.file?.path?.contains(mac.replace(':', '_')) == true
            ) {
                fileUtil = lastFileUtil
                // parse prev log for filling wheeldata values
                val parser = ParserLogToWheelData()
                parser.parseFile(fileUtil)
                fileUtil.prepareStream()
                writeToLastLog = true
                // reset trip duration for recalculation in trip list
                val dao = ElectroClub.instance.dao
                if (dao != null) {
                    ioState.launch {
                        dao.getTripByFileName(fileUtil.file!!.name)?.apply {
                            duration = 0
                            dao.update(this)
                        }
                    }
                }
            }
        }
        if (!writeToLastLog) {
            val sdFormatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
            val filename = sdFormatter.format(Date()) + ".csv"
            if (!fileUtil.prepareFile(filename, WheelData.getInstance().mac)) {
                stopSelf()
                return mBinder
            }
            appConfig.continueThisDayLogMacException = ""
        }
        var locationHeaderString = ""
        if (logLocationData) {
            val isGPSEnabled = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

            // Getting Network Provider status
            val isNetworkEnabled = mLocationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // Getting if the users wants to use GPS
            var useGPS = appConfig.useGps
            if (!isGPSEnabled && !isNetworkEnabled) {
                logLocationData = false
                showToast(R.string.logging_error_all_location_providers_disabled)
            } else if (useGPS && !isGPSEnabled) {
                useGPS = false
                showToast(R.string.logging_error_gps_disabled)
            } else if (!useGPS && !isNetworkEnabled) {
                logLocationData = false
                showToast(R.string.logging_error_network_disabled)
            }
            if (logLocationData) {
                locationHeaderString =
                    "latitude,longitude,gps_speed,gps_alt,gps_heading,gps_distance,"
                mLocation = lastBestLocation
                mLocationProvider = LocationManager.NETWORK_PROVIDER
                if (useGPS) {
                    mLocationProvider = LocationManager.GPS_PROVIDER
                }
                // Acquire a reference to the system Location Manager
                mLocationManager.requestLocationUpdates(
                    mLocationProvider,
                    250,
                    0f,
                    locationListener
                )
            }
        }
        if (!writeToLastLog) {
            fileUtil.writeLine("date,time," + locationHeaderString + "speed,voltage,phase_current,current,power,torque,pwm,battery_level,distance,totaldistance,system_temp,temp2,tilt,roll,mode,alert")
        }
        val serviceIntent = Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED)
        serviceIntent.putExtra(
            Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION,
            fileUtil.absolutePath
        )
        serviceIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, true)
        sendBroadcast(serviceIntent)
        Timber.i("DataLogger Started")

        return mBinder
    }

    private fun isNullOrEmpty(s: String?): Boolean {
        return s == null || s.trim { it <= ' ' }.isEmpty()
    }

    override fun onDestroy() {
        var isBusy = false
        if (logLocationData && mLastLocation != null) {
            appConfig.lastLocationLaltitude = mLastLocation!!.latitude
            appConfig.lastLocationLongitude = mLastLocation!!.longitude
        }
        val path = fileUtil.absolutePath
        fileUtil.close()
        Timber.wtf("DataLogger Stopping...")
        notifications.setCustomTitle("Uploading tack...")

        // electro.club upload
        if (fileUtil.fileName != "" && appConfig.autoUploadEc) {
            isBusy = true
            try {
                Timber.wtf("Uploading %s to electro.club", fileUtil.fileName)
                val data = fileUtil.readBytes() ?: return
                ElectroClub.instance.uploadTrack(
                    data,
                    fileUtil.fileName,
                    true
                ) { success: Boolean? ->
                    if (!success!!) {
                        Timber.wtf("Upload failed...")
                        notifications.setCustomTitle("Upload failed.")
                    }
                    reallyDestroy(null)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Timber.wtf("Error upload log to electro.club: %s", e.toString())
                reallyDestroy(path)
            }
        }
        if (!isBusy) {
            reallyDestroy(path)
        }
    }

    private fun reallyDestroy(path: String?) {
        val serviceIntent = Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED)
        if (!isNullOrEmpty(path)) {
            serviceIntent.putExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION, path)
        }
        serviceIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, false)
        sendBroadcast(serviceIntent)
        try {
            if (logLocationData) {
                mLocationManager.removeUpdates(locationListener)
            }
        } catch (ignored: Exception) {
        }
        instance = null
        Timber.wtf("DataLogger Stopped")
    }

    private val isExternalStorageWritable: Boolean
        /* Checks if external storage is available for read and write */
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }
    private val isExternalStorageReadable: Boolean
        /* Checks if external storage is available to at least read */
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    fun updateFile() {
        var LocationDataString = ""
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
                if (mLastLocation != null) mLocationDistance += mLastLocation!!.distanceTo(mLocation!!)
                    .toDouble()
                mLastLocation = mLocation
            }
            LocationDataString = String.format(
                Locale.US, "%s,%s,%s,%s,%s,%.0f,",
                latitude,
                longitude,
                gpsSpeed,
                gpsAlt,
                gpsBearing,
                mLocationDistance
            )
        }
        val wd = WheelData.getInstance()
        fileUtil.writeLine(
            String.format(
                Locale.US, "%s,%s%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%d,%.2f,%.2f,%s,%s",
                sdf!!.format(WheelData.getInstance().timeStamp),
                LocationDataString,
                wd.speedDouble,
                wd.voltageDouble,
                wd.phaseCurrentDouble,
                wd.currentDouble,
                wd.powerDouble,
                wd.torque,
                wd.calculatedPwm,
                wd.batteryLevel,
                wd.distance,
                wd.totalDistance,
                wd.temperature,
                wd.temperature2,
                wd.angle,
                wd.roll,
                wd.modeStr,
                wd.alert
            )
        )
    }

    // Define a listener that responds to location updates
    private var locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            mLocation = location
        }

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}

        @Deprecated("Need for old API level 28 and lower.")
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }

    private val lastBestLocation: Location?
        @SuppressLint("MissingPermission") get() {
            val locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val locationNet =
                mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            var GPSLocationTime: Long = 0
            var NetLocationTime: Long = 0
            if (locationGPS != null) {
                GPSLocationTime = locationGPS.time
            }
            if (locationNet != null) {
                NetLocationTime = locationNet.time
            }
            return if (GPSLocationTime - NetLocationTime > 0) {
                locationGPS
            } else {
                locationNet
            }
        }

    private fun showToast(messageId: Int) {
        for (i in 0..3) Toast.makeText(this, messageId, Toast.LENGTH_LONG).show()
    }

    inner class LocalBinder : Binder() {
        fun getService(): LoggingService {
            return this@LoggingService
        }
    }

    companion object {
        private var instance: LoggingService? = null
        fun isInstanceCreated(): Boolean {
            return instance != null
        }
    }
}