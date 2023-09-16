package com.cooper.wheellog.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

object PermissionsUtil {
    private val permissionsLocation = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.S)
    private val permissionsBle31 = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private val permissionsBleLegacy = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    )

    private val permissionsIO = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissionNotification = Manifest.permission.POST_NOTIFICATIONS

    private const val maxPermissionReq = 3
    private val permissionCounter = HashMap<String, Int>()
    val isMaxBleReq: Boolean
        get() = permissionCounter.any { p -> p.value > maxPermissionReq }

    /**
     * returns - all ble permissions is granted
     */
    fun checkBlePermissions(activity: Activity, requestCode: Int = 1): Boolean {
        val requestedPermission = permissionsLocation.toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestedPermission.addAll(permissionsBle31)
        } else {
            requestedPermission.addAll(permissionsBleLegacy)
        }
        return !reqPermissions(activity, requestedPermission, requestCode)
    }

    /**
     * returns - permission for notification is granted
     */
    fun checkNotificationsPermissions(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return !reqPermissions(activity, mutableListOf(permissionNotification), 77)
    }

    /**
     * returns - all permissions for IO is granted. Only for Android 9 or lower.
     */
    fun checkExternalFilePermission(activity: Activity, requestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return false
        }
        return !reqPermissions(activity, permissionsIO.toMutableList(), requestCode)
    }

    @JvmStatic
    fun checkExternalFilePermission(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun checkLocationPermission(context: Context): Boolean {
        var result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (result && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PermissionChecker.PERMISSION_GRANTED
        }
        return result
    }

    /**
     * return true if any permission in list is not granted
     */
    private fun reqPermissions(
        activity: Activity,
        permissions: MutableList<String>,
        requestCode: Int
    ): Boolean {
        permissions.removeAll {
            ActivityCompat.checkSelfPermission(activity.applicationContext, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        val result = permissions.any()
        permissions.removeAll { (permissionCounter[it] ?: 0) > maxPermissionReq }
        if (permissions.any()) {
            for (permission in permissions) {
                permissionCounter[permission] = (permissionCounter[permission] ?: 0) + 1
            }
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), requestCode)
        }
        return result
    }
}