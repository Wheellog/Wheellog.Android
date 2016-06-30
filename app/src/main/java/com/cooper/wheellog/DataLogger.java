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
    private static final boolean DEBUG = true;
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
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.UK);
        registerReceiver(mBluetoothUpdateReceiver, BluetoothLeService.makeBluetoothUpdateIntentFilter());

        File root = android.os.Environment.getExternalStorageDirectory();
        //LOGI("\nExternal file system root: "+root);

        checkExternalMedia();

        File dir = new File (root.getAbsolutePath() + "/Download");
        dir.mkdirs();
        file = new File(dir, "wheelLog.txt");
        fileExists = file.exists();

        Log.d(TAG, "DataLogger Started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        unregisterReceiver(mBluetoothUpdateReceiver);
        Log.d(TAG, "DataLogger stopped");
    }

    private void checkExternalMedia(){
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWritable;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWritable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWritable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWritable = false;
        }
        LOGI("\n\nExternal Media: readable="
                +mExternalStorageAvailable+" writable="+mExternalStorageWritable);
    }

    private void writeToSDFile(){
        try {
            FileOutputStream f = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(f);

            if (!fileExists)
            {
                fileExists = true;
                pw.println("date,speed,voltage,current,distance,fan_status");
            }

            pw.println(String.format("%s,%f,%f,%f,%f,%d",
                    sdf.format(new Date()),
                    Wheel.getInstance().getSpeedDouble(),
                    Wheel.getInstance().getVoltageDouble(),
                    Wheel.getInstance().getCurrentDouble(),
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