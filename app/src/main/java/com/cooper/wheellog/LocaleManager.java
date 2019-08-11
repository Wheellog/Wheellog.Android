package com.cooper.wheellog;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
//import android.preference.PreferenceManager;
import com.cooper.wheellog.utils.SettingsUtil;

import java.util.Locale;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.N;


public class LocaleManager {

    public static final  String LANGUAGE_ENGLISH   = "en";


    public LocaleManager(Context context) {
        //prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Context setLocale(Context c) {
        Context a;
        a = c;
        if (SettingsUtil.isUseENG(c)) {
            a = updateResources(c, LANGUAGE_ENGLISH);
        }
        return a;
//        return updateResources(c, LANGUAGE_ENGLISH);
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (isAtLeastVersion(JELLY_BEAN_MR1)) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        return context;
    }

    public static Locale getLocale(Resources res) {
        Configuration config = res.getConfiguration();
        return isAtLeastVersion(N) ? config.getLocales().get(0) : config.locale;
    }

    private static boolean isAtLeastVersion(int version) {
        return Build.VERSION.SDK_INT >= version;
    }
}