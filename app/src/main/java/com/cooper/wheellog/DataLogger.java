package com.cooper.wheellog;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataLogger extends Service
{
    private static final boolean DEBUG = false;
    private final static String TAG = DataLogger.class.getSimpleName();
    private static DataLogger instance = null;
    SimpleDateFormat sdf;
    private boolean fileExists;
    private File file;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private void LOGI(final String msg) {
        if (DEBUG)
            Log.i(TAG, msg);
    }

    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if (Constants.ACTION_WHEEL_DATA_AVAILABLE.equals(action))
                writeToSDFile();
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
        registerReceiver(mBluetoothUpdateReceiver, BluetoothLeService.makeBluetoothUpdateIntentFilter());

        if (isExternalStorageReadable() && isExternalStorageWritable()) {

            File dir = getDownloadsStorageDir();

            SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
            String fileName = sdFormatter.format(new Date()) + ".csv";

            file = new File(dir, fileName);
            fileExists = file.exists();

            Log.d(TAG, "DataLogger Started");
            return START_STICKY;
        }
        stopSelf();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        unregisterReceiver(mBluetoothUpdateReceiver);
        Log.d(TAG, "DataLogger stopped");
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

    public File getDownloadsStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "wheelLog");
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    private void writeToSDFile(){
        try {
            FileOutputStream f = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(f);

            if (!fileExists)
            {
                fileExists = true;
                pw.println("date,time,speed,voltage,current,battery_level,distance,temperature,fan_status");
            }

            pw.println(String.format(Locale.US, "%s,%f,%f,%f,%d,%f,%d,%d",
                    sdf.format(new Date()),
                    Wheel.getInstance().getSpeedDouble(),
                    Wheel.getInstance().getVoltageDouble(),
                    Wheel.getInstance().getCurrentDouble(),
                    Wheel.getInstance().getBatteryLevel(),
                    Wheel.getInstance().getCurrentDistanceDouble(),
                    Wheel.getInstance().getTemperature(),
                    Wheel.getInstance().getFanStatus()
                    ));
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGI("File written to "+file);
    }
}