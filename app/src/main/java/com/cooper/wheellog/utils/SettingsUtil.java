package com.cooper.wheellog.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import timber.log.Timber;


public class SettingsUtil {

    private static final String key = "WheelLog";


    public static String getLastAddress(Context context) {
        SharedPreferences pref = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        if (pref.contains("last_mac")) {
            return pref.getString("last_mac", "");
        }
        return "";
    }

    public static void setLastAddress(Context context, String address) {
        SharedPreferences.Editor editor = context.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        editor.putString("last_mac", address);
        editor.apply();
    }

    public static boolean isFirstRun(Context context) {
        SharedPreferences pref = context.getSharedPreferences(key, Context.MODE_PRIVATE);

        if (pref.contains("first_run"))
            return false;

        pref.edit().putBoolean("first_run", false).apply();
        return true;
    }

    public static boolean getBoolean(Context context, String preference) {
        return getSharedPreferences(context).getBoolean(preference, false);
    }

    public static boolean isAutoLogEnabled(Context context) {
        return getSharedPreferences(context).getBoolean("auto_log", false);
    }

    public static void setAutoLog(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean("auto_log", enabled).apply();
    }

    public static boolean isLogLocationEnabled(Context context) {
        return getSharedPreferences(context).getBoolean("log_location_data", false);
    }

    public static void setLogLocationEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean("log_location_data", enabled).apply();
    }

    public static boolean isUseGPSEnabled(Context context) {
        return getSharedPreferences(context).getBoolean("use_gps", false);
    }

    public static boolean isAutoUploadEnabled(Context context) {
        return getSharedPreferences(context).getBoolean("auto_upload", false);
    }

    public static void setAutoUploadEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean("auto_upload", enabled).apply();
    }
    
    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
