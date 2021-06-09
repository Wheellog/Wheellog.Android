package com.cooper.wheellog;

import android.app.Application;
import timber.log.Timber;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;

import com.cooper.wheellog.utils.NotificationUtil;
import com.cooper.wheellog.utils.ThemeManager;
import com.cooper.wheellog.utils.VolumeKeyController;

import org.jetbrains.annotations.NotNull;

public class WheelLog extends Application {
    private static WheelLog me;
    public static AppConfig AppConfig;
    public static NotificationUtil Notifications;
    public static VolumeKeyController VolumeKeyController;
    public static ThemeManager ThemeManager;

    @Override
    public void onCreate() {
        super.onCreate();
        me = this;
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            Timber.plant(new FileLoggingTree(getApplicationContext()));
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        AppConfig = new AppConfig(base);
        base = LocaleManager.setLocale(base);
        Notifications = new NotificationUtil(base);
        VolumeKeyController = new VolumeKeyController(base);
        ThemeManager = new ThemeManager();
        super.attachBaseContext(base);
    }

    public static ContentResolver cResolver() {
        return me.getContentResolver();
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleManager.setLocale(this);
    }

    @Override
    public void onTerminate() {
        VolumeKeyController.destroy();
        super.onTerminate();
    }
}