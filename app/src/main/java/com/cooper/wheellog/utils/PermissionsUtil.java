package com.cooper.wheellog.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

public class PermissionsUtil {

    public static boolean checkExternalFilePermission(Context context){
        int read_result = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        int write_result = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return read_result == PackageManager.PERMISSION_GRANTED &&
                write_result == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkLocationPermission(Context context){
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }
}
