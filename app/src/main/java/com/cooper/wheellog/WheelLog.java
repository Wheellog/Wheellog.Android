package com.cooper.wheellog;

import android.app.Application;
import timber.log.Timber;
import com.cooper.wheellog.FileLoggingTree;
import android.content.Context;
import android.content.res.Configuration;




public class WheelLog extends Application {
    public static LocaleManager localeManager;
    @Override
    public void onCreate() {
        super.onCreate();
        //Timber.plant(new FileLoggingTree(getApplicationContext()));
        //Timber.plant(new Timber.DebugTree());
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        localeManager = new LocaleManager(base);
        super.attachBaseContext(localeManager.setLocale(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleManager.setLocale(this);
    }

}
