package com.cooper.wheellog;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.FileUtil;
import com.cooper.wheellog.utils.NotificationUtil;
import com.cooper.wheellog.utils.PermissionsUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class LoggingService extends Service
{
    private static LoggingService instance = null;
    SimpleDateFormat sdf;
    private Location mLocation;
    private Location mLastLocation;
    private double mLocationDistance;
    private LocationManager mLocationManager;
    private String mLocationProvider = LocationManager.NETWORK_PROVIDER;
    private boolean logLocationData = false;
    private FileUtil fileUtil;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    @SuppressWarnings("MissingPermission")
    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                    if (mLocationManager != null && logLocationData) {
                        int connectionState = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BluetoothLeService.STATE_DISCONNECTED);
                        if (connectionState == BluetoothLeService.STATE_CONNECTED) {
                            mLocationManager.requestLocationUpdates(mLocationProvider, 250, 0, locationListener);
                        } else {
                            mLocationManager.removeUpdates(locationListener);
                        }
                    }
                    break;
                case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                    if (intent.hasExtra(Constants.INTENT_EXTRA_DATA_TO_LOGS)) {
                        updateFile();
                    }
                    break;
            }
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
        fileUtil = new FileUtil(getApplicationContext());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        registerReceiver(mBluetoothUpdateReceiver, intentFilter);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!PermissionsUtil.checkExternalFilePermission(this)) {
                showToast(R.string.logging_error_no_storage_permission);
                stopSelf();
                return START_STICKY;
            }

            if (!isExternalStorageReadable() || !isExternalStorageWritable()) {
                showToast(R.string.logging_error_storage_unavailable);
                stopSelf();
                return START_STICKY;
            }
        }

        logLocationData = WheelLog.AppConfig.getLogLocationData();

        if (logLocationData && !PermissionsUtil.checkLocationPermission(this)) {
            showToast(R.string.logging_error_no_location_permission);
            logLocationData = false;
        }

        sdf = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.US);

        SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);

        String filename = sdFormatter.format(new Date()) + ".csv";

        if (!fileUtil.prepareFile(filename, WheelData.getInstance().getMac())) {
            stopSelf();
            return START_STICKY;
        }

        if (logLocationData) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Getting GPS Provider status
            assert mLocationManager != null;
            boolean isGPSEnabled = mLocationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Getting Network Provider status
            boolean isNetworkEnabled = mLocationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // Getting if the users wants to use GPS
            boolean useGPS = WheelLog.AppConfig.getUseGps();

            if (!isGPSEnabled && !isNetworkEnabled) {
                logLocationData = false;
                mLocationManager = null;
                showToast(R.string.logging_error_all_location_providers_disabled);
            } else if (useGPS && !isGPSEnabled) {
                useGPS = false;
                showToast(R.string.logging_error_gps_disabled);
            } else if (!useGPS && !isNetworkEnabled) {
                logLocationData = false;
                mLocationManager = null;
                showToast(R.string.logging_error_network_disabled);
            }

            if (logLocationData) {
                fileUtil.writeLine("date,time,latitude,longitude,gps_speed,gps_alt,gps_heading,gps_distance,speed,voltage,phase_current,current,power,torque,pwm,battery_level,distance,totaldistance,system_temp,temp2,tilt,roll,mode,alert");
                mLocation = getLastBestLocation();
                mLocationProvider = LocationManager.NETWORK_PROVIDER;
                if (useGPS)
                    mLocationProvider = LocationManager.GPS_PROVIDER;
                // Acquire a reference to the system Location Manager
                mLocationManager.requestLocationUpdates(mLocationProvider, 250, 0, locationListener);
            } else
                fileUtil.writeLine("date,time,speed,voltage,phase_current,current,power,torque,pwm,battery_level,distance,totaldistance,system_temp,temp2,tilt,roll,mode,alert");
        }
        else {
            fileUtil.writeLine("date,time,speed,voltage,phase_current,current,power,torque,pwm,battery_level,distance,totaldistance,system_temp,temp2,tilt,roll,mode,alert");
        }

        Intent serviceIntent = new Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
        serviceIntent.putExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION, fileUtil.getAbsolutePath());
        serviceIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, true);
        sendBroadcast(serviceIntent);
        startForeground(Constants.MAIN_NOTIFICATION_ID, NotificationUtil.getNotification());
        Timber.i("DataLogger Started");

        return START_STICKY;
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onDestroy() {
        String path = "";

        if (fileUtil != null) {
            path = fileUtil.getAbsolutePath();
            fileUtil.close();
        }

        if (!isNullOrEmpty(path)) {
            Intent serviceIntent = new Intent(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
            serviceIntent.putExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION, path);
            serviceIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
            sendBroadcast(serviceIntent);

            // electro.club upload
            if (WheelLog.AppConfig.getAutoUploadEc()
                    && ElectroClub.getInstance().getUserToken() != null) {
                try {
                    byte[] data = fileUtil.readBytes();
                    String[] tokens = path.split("[\\\\|/]");
                    String filename = tokens[tokens.length - 1];
                    ElectroClub.getInstance().uploadTrack(data, filename, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        instance = null;
        unregisterReceiver(mBluetoothUpdateReceiver);
        if (mLocationManager != null && logLocationData)
            mLocationManager.removeUpdates(locationListener);
        stopSelf();
        Timber.i("DataLogger Stopped");
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
        if (logLocationData) {
            String longitude = "";
            String latitude = "";
            String gpsSpeed = "";
            String gpsAlt = "";
            String gpsBearing = "";
            if (mLocation != null) {
                longitude = String.valueOf(mLocation.getLongitude());
                latitude = String.valueOf(mLocation.getLatitude());
                gpsSpeed = String.valueOf(mLocation.getSpeed()*3.6);
                gpsAlt = String.valueOf(mLocation.getAltitude());
                gpsBearing = String.valueOf(mLocation.getBearing());
                if (mLastLocation != null)
                    mLocationDistance += mLastLocation.distanceTo(mLocation);

                mLastLocation = mLocation;
            }
            fileUtil.writeLine(String.format(Locale.US, "%s,%s,%s,%s,%s,%s,%.0f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%d,%.2f,%.2f,%s,%s",
                            sdf.format(WheelData.getInstance().getTimeStamp()),
                            latitude,
                            longitude,
                            gpsSpeed,
                            gpsAlt,
                            gpsBearing,
                            mLocationDistance,
                            WheelData.getInstance().getSpeedDouble(),
                            WheelData.getInstance().getVoltageDouble(),
                            WheelData.getInstance().getPhaseCurrentDouble(),
                            WheelData.getInstance().getCurrentDouble(),
                            WheelData.getInstance().getPowerDouble(),
                            WheelData.getInstance().getTorque(),
                            WheelData.getInstance().getCalculatedPwm(),
                            WheelData.getInstance().getBatteryLevel(),
                            WheelData.getInstance().getDistance(),
							WheelData.getInstance().getTotalDistance(),
                            WheelData.getInstance().getTemperature(),
							WheelData.getInstance().getTemperature2(),
							WheelData.getInstance().getAngle(),
							WheelData.getInstance().getRoll(),
							WheelData.getInstance().getModeStr(),
							WheelData.getInstance().getAlert()
                    ));
        } else {
            fileUtil.writeLine(String.format(Locale.US, "%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%d,%.2f,%.2f,%s,%s",
                            sdf.format(WheelData.getInstance().getTimeStamp()),
                            WheelData.getInstance().getSpeedDouble(),
                            WheelData.getInstance().getVoltageDouble(),
                            WheelData.getInstance().getPhaseCurrentDouble(),
                            WheelData.getInstance().getCurrentDouble(),
                            WheelData.getInstance().getPowerDouble(),
                            WheelData.getInstance().getTorque(),
                            WheelData.getInstance().getCalculatedPwm(),
                            WheelData.getInstance().getBatteryLevel(),
                            WheelData.getInstance().getDistance(),
							WheelData.getInstance().getTotalDistance(),
                            WheelData.getInstance().getTemperature(),
							WheelData.getInstance().getTemperature2(),
							WheelData.getInstance().getAngle(),
							WheelData.getInstance().getRoll(),
							WheelData.getInstance().getModeStr(),
							WheelData.getInstance().getAlert()
                    ));
        }
    }

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            mLocation = location;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    };

    @SuppressWarnings("MissingPermission")
    private Location getLastBestLocation() {

        Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if ( 0 < GPSLocationTime - NetLocationTime ) {
            return locationGPS;
        }
        else {
            return locationNet;
        }
    }

    private void showToast(int message_id) {
        for (int i = 0; i <= 3; i++)
            Toast.makeText(this, message_id, Toast.LENGTH_LONG).show();
    }
}