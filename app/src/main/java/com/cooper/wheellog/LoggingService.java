package com.cooper.wheellog;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;


import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.FileUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class LoggingService extends Service
{
    private static LoggingService instance = null;
    SimpleDateFormat sdf;
    private String filename;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            updateFile();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        sdf = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.US);
        registerReceiver(mBluetoothUpdateReceiver, new IntentFilter(Constants.ACTION_WHEEL_DATA_AVAILABLE));

        if (isExternalStorageReadable() && isExternalStorageWritable()) {

            SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
            filename = sdFormatter.format(new Date()) + ".csv";
            File file = FileUtil.getFile(filename);
            if (file == null) {
                stopSelf();
                return START_STICKY;
            }

            FileUtil.writeLine(filename, "date,time,speed,voltage,current,power,battery_level,distance,temperature,fan_status");
            Intent serviceIntent = new Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
            serviceIntent.putExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION, file.getAbsolutePath());
            serviceIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, true);
            sendBroadcast(serviceIntent);

            Timber.i("DataLogger Started");
            return START_STICKY;
        }
        stopSelf();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Intent serviceIntent = new Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
        serviceIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
        sendBroadcast(serviceIntent);
        instance = null;
        unregisterReceiver(mBluetoothUpdateReceiver);
        Timber.i("DataLogger stopped");
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private void updateFile() {
        FileUtil.writeLine(filename,
                String.format(Locale.US, "%s,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%d,%d",
                sdf.format(new Date()),
                WheelData.getInstance().getSpeedDouble(),
                WheelData.getInstance().getVoltageDouble(),
                WheelData.getInstance().getCurrentDouble(),
                WheelData.getInstance().getPowerDouble(),
                WheelData.getInstance().getBatteryLevel(),
                WheelData.getInstance().getDistanceDouble(),
                WheelData.getInstance().getTemperature(),
                WheelData.getInstance().getFanStatus()
                ));
    }
}