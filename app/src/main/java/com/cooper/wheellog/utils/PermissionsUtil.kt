package com.cooper.wheellog.utils

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

object PermissionsUtil {

    @JvmStatic
    fun checkExternalFilePermission(context: Context?): Boolean {
        val read_result = ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_EXTERNAL_STORAGE)
        val write_result = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return read_result == PackageManager.PERMISSION_GRANTED &&
            write_result == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun checkLocationPermission(context: Context?): Boolean {
        val result = ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

}