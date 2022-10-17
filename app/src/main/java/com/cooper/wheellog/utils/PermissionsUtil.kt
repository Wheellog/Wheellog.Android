package com.cooper.wheellog.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

object PermissionsUtil {
    private val permissionsLocation = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @RequiresApi(Build.VERSION_CODES.S)
    private val permissionsBle31 = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissionNotification = Manifest.permission.POST_NOTIFICATIONS

    private const val maxPermissionReq = 3
    private val permissionCounter = HashMap<String, Int>()
    val isMaxBleReq: Boolean
        get() = permissionCounter.any { p -> p.value > maxPermissionReq }

    fun checkBlePermissions(activity: Activity, requestCode: Int = 1): Boolean {
        val requestedPermission = mutableListOf<String>()
        requestedPermission.addAll(permissionsLocation)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestedPermission.addAll(permissionsBle31)
        }
        requestedPermission.removeAll {
            ActivityCompat.checkSelfPermission(activity.applicationContext, it) == PackageManager.PERMISSION_GRANTED
        }
        if (requestedPermission.any()) {
            ActivityCompat.requestPermissions(activity, requestedPermission.toTypedArray(), requestCode)
            for (permission in requestedPermission) {
                permissionCounter[permission] = (permissionCounter[permission] ?: 0) + 1
            }
            return false
        }
        return true
    }

    fun checkNotificationsPermissions(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        val result = ActivityCompat.checkSelfPermission(activity.applicationContext, permissionNotification) == PackageManager.PERMISSION_GRANTED
        val requestCode = 77;
        if (!result) {
            ActivityCompat.requestPermissions(activity, arrayOf(permissionNotification), requestCode)
            return false
        }
        return true
    }

    @JvmStatic
    fun checkExternalFilePermission(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun checkLocationPermission(context: Context): Boolean {
        val result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }
}