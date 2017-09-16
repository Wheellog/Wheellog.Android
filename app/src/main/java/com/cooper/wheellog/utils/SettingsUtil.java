package com.cooper.wheellog.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.cooper.wheellog.R;


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
	
	public static void setUserDistance(Context context, String id, long distance) {
        SharedPreferences.Editor editor = context.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        editor.putLong("user_distance_"+id, distance);
        editor.apply();
    }
	
	public static long getUserDistance(Context context, String id) {
        SharedPreferences pref = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        if (pref.contains("user_distance_"+id)) {
            return pref.getLong("user_distance_"+id, 0);
        }
        return 0;
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
        return getSharedPreferences(context).getBoolean(context.getString(R.string.auto_log), false);
    }

    public static void setAutoLog(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.auto_log), enabled).apply();
    }

    public static boolean isLogLocationEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.log_location_data), false);
    }

    public static void setLogLocationEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.log_location_data), enabled).apply();
    }

    public static boolean isUseGPSEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.use_gps), false);
    }

    public static boolean isAutoUploadEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.auto_upload), false);
    }

    public static void setAutoUploadEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.auto_upload), enabled).apply();
    }
    
    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isUseMPH(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.use_mph), false);
    }

    public static int getMaxSpeed(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.max_speed), 30);
    }

    public static int getHornMode(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.horn_mode), "0"));
    }

    //Inmotion Specific, but can be the same for other wheels

    public static boolean hasPasswordForWheel(Context context, String id) {
        return getSharedPreferences(context).contains("wheel_password_"+id);
    }

    public static String getPasswordForWheel(Context context, String id) {
        return getSharedPreferences(context).getString("wheel_password_"+id, "000000");
    }

    public static void setPasswordForWheel(Context context, String id, String password) {
        while (password.length() < 6) {
            password = "0" + password;
        }
        getSharedPreferences(context).edit().putString("wheel_password_"+id, password).apply();
    }
}
