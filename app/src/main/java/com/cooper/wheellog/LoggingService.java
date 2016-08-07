package com.cooper.wheellog;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;


import com.cooper.wheellog.Utils.Constants;
import com.cooper.wheellog.Utils.FileUtil;

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
    private WheelLog wheelLog;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if (Constants.ACTION_WHEEL_DATA_AVAILABLE.equals(action))
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

            wheelLog = (WheelLog) getApplicationContext();
            Timber.d("DataLogger Started");
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
        Timber.d("DataLogger stopped");
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

    private void updateFile(){
        FileUtil.writeLine(filename,
                String.format(Locale.US, "%s,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%d,%d",
                sdf.format(new Date()),
                wheelLog.getSpeedDouble(),
                wheelLog.getVoltageDouble(),
                wheelLog.getCurrentDouble(),
                wheelLog.getPowerDouble(),
                wheelLog.getBatteryLevel(),
                wheelLog.getDistanceDouble(),
                wheelLog.getTemperature(),
                wheelLog.getFanStatus()
                ));
    }
}