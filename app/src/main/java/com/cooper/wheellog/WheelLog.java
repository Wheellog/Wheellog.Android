package com.cooper.wheellog;

import android.app.Application;
import timber.log.Timber;

public class WheelLog extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

}
