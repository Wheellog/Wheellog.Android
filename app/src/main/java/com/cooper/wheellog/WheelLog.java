package com.cooper.wheellog;

import android.app.Application;
import timber.log.Timber;
import android.content.Context;
import android.content.res.Configuration;

import com.cooper.wheellog.utils.NotificationUtil;
import com.cooper.wheellog.utils.VolumeKeyController;

public class WheelLog extends Application {
    public static LocaleManager localeManager;
    public static AppConfig AppConfig;
    public static NotificationUtil Notifications;
    public static VolumeKeyController VolumeKeyController;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            Timber.plant(new FileLoggingTree(getApplicationContext()));
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        AppConfig = new AppConfig(base);
        localeManager = new LocaleManager(base);
        Notifications = new NotificationUtil(base);
        VolumeKeyController = new VolumeKeyController(base);
        super.attachBaseContext(localeManager.setLocale(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleManager.setLocale(this);
    }

    @Override
    public void onTerminate() {
        VolumeKeyController.destroy();
        super.onTerminate();
    }
}