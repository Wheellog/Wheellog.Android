package com.cooper.wheellog;

import android.app.Application;
import timber.log.Timber;
import com.cooper.wheellog.FileLoggingTree;


public class WheelLog extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Timber.plant(new FileLoggingTree(getApplicationContext()));
        //Timber.plant(new Timber.DebugTree());
        if (BuildConfig.DEBUG) {
            //Timber.plant(new Timber.DebugTree());
        }
    }

}
