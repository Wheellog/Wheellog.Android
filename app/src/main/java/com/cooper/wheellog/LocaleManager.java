package com.cooper.wheellog;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.N;

import org.koin.java.KoinJavaComponent;

public class LocaleManager {
    private final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
    public static final  String LANGUAGE_ENGLISH = "en";

    public static Context setLocale(Context c) {
        Context a;
        a = c;
        final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
        if (appConfig.getUseEng()) {
            a = updateResources(c, LANGUAGE_ENGLISH);
        }
        return a;
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