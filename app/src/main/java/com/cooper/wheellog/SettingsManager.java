package com.cooper.wheellog;

import android.content.Context;
import android.content.SharedPreferences;


public class SettingsManager {

    private static final String key = "WheelLog";

    public static String getLastAddr(Context context) {
        SharedPreferences pref = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        if (pref.contains("last_mac")) {
            return pref.getString("last_addr", "");
        }
        return "";
    }

    public static void setLastAddr(Context context, String address) {
        SharedPreferences.Editor editor = context.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        editor.putString("last_mac", address);
        editor.apply();
    }
}
