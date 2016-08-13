package com.cooper.wheellog.utils;

import android.content.Context;
import android.content.SharedPreferences;


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
}
