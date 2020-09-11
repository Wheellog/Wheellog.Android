package com.cooper.wheellog.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.cooper.wheellog.R;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

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

    public static boolean getBoolean(Context context, String id, String preference) {
        return getSharedPreferences(context).getBoolean(preference+'_'+id, false);
    }

    public static boolean isAutoLogEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.auto_log), false);
    }


    public static void setAutoLog(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.auto_log), enabled).apply();
    }

    public static void setConnectionSound(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.connection_sound), enabled).apply();
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

    public static boolean isUsePipMode(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.use_pip_mode), false);
    }

    public static boolean isUseENG(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.use_eng), false);
    }

    public static int getMaxSpeed(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.max_speed), 30);
    }
    public static int getMaxSpeed(Context context, String id) {
        return getSharedPreferences(context).getInt(context.getString(R.string.max_speed)+'_'+id, 30);
    }

    public static int getHornMode(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.horn_mode), "0"));
    }
    public static int getHornMode(Context context, String id) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.horn_mode)+"_"+id, "0"));
    }
    public static boolean getGarminConnectIQEnable(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.garmin_connectiq_enable), false);
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

    public static void setAdvDataForWheel(Context context, String id, String advData) {
        getSharedPreferences(context).edit().putString("wheel_adv_data_"+id, advData).apply();
    }

    public static String getAdvDataForWheel(Context context, String id) {
        return getSharedPreferences(context).getString("wheel_adv_data_"+id, "");
    }

    public static boolean savePreferencesTo(Context context, String suffix) {
        if (suffix == null || suffix.equals("")) {
            return false;
        }
        SharedPreferences from = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences to = getDestinationPrefs(context, suffix);
        return sync(from, to);
    }

    public static boolean restorePreferencesFrom(Context context, String suffix) {
        if (suffix == null || suffix.equals("")) {
            return false;
        }
        SharedPreferences from = getDestinationPrefs(context, suffix);
        SharedPreferences to = PreferenceManager.getDefaultSharedPreferences(context);
        return sync(from, to);
    }

    private static SharedPreferences getDestinationPrefs(@NotNull Context context, String suffix) {
        return context.getSharedPreferences(
                context.getPackageName() + "_preferences_" + suffix,
                Context.MODE_PRIVATE);
    }

    private static boolean sync(SharedPreferences from, SharedPreferences to) {
        if (from == null || to == null) {
            return false;
        }
        Map<String, ?> prefs = from.getAll();
        if (prefs.isEmpty()) {
            return false;
        }
        SharedPreferences.Editor editor = to.edit();
        for (Map.Entry<String, ?> p : prefs.entrySet()) {
            String key = p.getKey();
            Object value = p.getValue();
            if (value instanceof String) editor.putString(key, (String) value);
            else if (value instanceof Set<?>) editor.putStringSet(key, (Set<String>) value);
            else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Float) editor.putFloat(key, (Float) value);
            else if (value instanceof Integer) editor.putInt(key, (Integer) value);
            else if (value instanceof Long) editor.putLong(key, (Long) value);
            else Timber.i("Unexpected Preferences type " + value);
        }
        editor.apply();
        return true;
    }
}
