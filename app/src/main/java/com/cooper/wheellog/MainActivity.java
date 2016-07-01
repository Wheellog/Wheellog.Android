package com.cooper.wheellog;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int DEVICE_SCAN_REQUEST = 10;

    Button buttonScan;
    Button buttonConnect;
    Button buttonPebbleService;
    Button buttonDataService;

    TextView textViewSpeed;
    TextView textViewTemperature;
    TextView textViewCurrent;
    TextView textViewVoltage;
    TextView textViewBattery;
    TextView textViewFanStatus;
    TextView textViewMaxSpeed;
    TextView textViewCurrentDistance;
    TextView textViewType;
    TextView textViewName;
    TextView textViewVersion;
    TextView textViewSerial;
    TextView textViewTotalDistance;
    TextView textViewRideTime;

    private BluetoothLeService mBluetoothLeService;
    String mDeviceAddress;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private boolean doubleBackToExitPressedOnce = false;
    private final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 10;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            if (mConnectionState != mBluetoothLeService.getConnectionState())
                setConnectionState(mBluetoothLeService.getConnectionState());

            if (mConnectionState == BluetoothLeService.STATE_DISCONNECTED &&
                    mDeviceAddress != null && !mDeviceAddress.isEmpty())
                connectToWheel();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Constants.ACTION_BLUETOOTH_CONNECTED.equals(action) && mConnectionState != BluetoothLeService.STATE_CONNECTED) {
                setConnectionState(BluetoothLeService.STATE_CONNECTED);
            } else if (Constants.ACTION_BLUETOOTH_DISCONNECTED.equals(action)) {
                setConnectionState(BluetoothLeService.STATE_DISCONNECTED);
            } else if (Constants.ACTION_WHEEL_DATA_AVAILABLE.equals(action)) {
                textViewSpeed.setText(String.format("%s KPH", Wheel.getInstance().getSpeedDouble()));
                textViewVoltage.setText(String.format("%sV", Wheel.getInstance().getVoltageDouble()));
                textViewTemperature.setText(String.format(Locale.US, "%d°C", Wheel.getInstance().getTemperature()));
                textViewCurrent.setText(String.format("%sW", Wheel.getInstance().getCurrentDouble()));
                textViewBattery.setText(String.format(Locale.US, "%d%%", Wheel.getInstance().getBatteryLevel()));
                textViewFanStatus.setText(Wheel.getInstance().getFanStatus() == 0 ? "Off" : "On");
                textViewMaxSpeed.setText(String.format("%s KPH", Wheel.getInstance().getMaxSpeedDouble()));
                textViewCurrentDistance.setText(String.format("%s KM", Wheel.getInstance().getCurrentDistanceDouble()));
                textViewTotalDistance.setText(String.format("%s KM", Wheel.getInstance().getTotalDistanceDouble()));
                textViewVersion.setText(String.format(Locale.US, "%d", Wheel.getInstance().getVersion()));
                textViewName.setText(Wheel.getInstance().getName());
                textViewType.setText(Wheel.getInstance().getType());
                textViewSerial.setText(Wheel.getInstance().getSerial());
                textViewRideTime.setText(Wheel.getInstance().getCurrentTimeString());
            } else if (Constants.ACTION_REQUEST_SERIAL_DATA.equals(action)) {
                byte[] data = new byte[20];
                data[0] = (byte) -86;
                data[1] = (byte) 85;
                data[16] = (byte) 99;
                data[17] = (byte) 20;
                data[18] = (byte) 90;
                data[19] = (byte) 90;
                mBluetoothLeService.writeBluetoothGattCharacteristic(data);
            }
        }
    };

    private void setConnectionState(int connectionState) {
        switch (connectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                Log.d(TAG, "Bluetooth connected");
                buttonConnect.setText(R.string.disconnect);
                buttonScan.setEnabled(false);
                SettingsManager.setLastAddr(getApplicationContext(), mDeviceAddress);

                byte[] data = new byte[20];
                data[0] = (byte) -86;
                data[1] = (byte) 85;
                data[16] = (byte) -101;
                data[17] = (byte) 20;
                data[18] = (byte) 90;
                data[19] = (byte) 90;
                mBluetoothLeService.writeBluetoothGattCharacteristic(data);
                break;
            case BluetoothLeService.STATE_CONNECTING:
                Log.d(TAG, "Bluetooth connecting");
                buttonScan.setEnabled(false);
                buttonConnect.setText(R.string.waiting_for_device);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                Log.d(TAG, "Bluetooth disconnected");
                buttonScan.setEnabled(true);
                buttonConnect.setText(R.string.connect);
                break;
        }
        mConnectionState = connectionState;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDeviceAddress = SettingsManager.getLastAddr(getApplicationContext());

        buttonScan = (Button) findViewById(R.id.buttonBluetoothScan);
        buttonConnect = (Button) findViewById(R.id.buttonBluetoothConnect);
        buttonPebbleService = (Button) findViewById(R.id.buttonPebbleService);
        buttonDataService = (Button) findViewById(R.id.buttonDataLoggingService);

        textViewSpeed = (TextView) findViewById(R.id.tvSpeed);
        textViewCurrent = (TextView) findViewById(R.id.tvCurrent);
        textViewTemperature = (TextView) findViewById(R.id.tvTemperature);
        textViewVoltage = (TextView) findViewById(R.id.tvVoltage);
        textViewBattery = (TextView) findViewById(R.id.tvBattery);
        textViewFanStatus = (TextView) findViewById(R.id.tvFanStatus);
        textViewMaxSpeed = (TextView) findViewById(R.id.tvMaxSpeed);
        textViewCurrentDistance = (TextView) findViewById(R.id.tvCurrentDistance);
        textViewTotalDistance = (TextView) findViewById(R.id.tvTotalDistance);
        textViewType = (TextView) findViewById(R.id.tvType);
        textViewName = (TextView) findViewById(R.id.tvName);
        textViewVersion = (TextView) findViewById(R.id.tvVersion);
        textViewSerial = (TextView) findViewById(R.id.tvSerial);
        textViewRideTime = (TextView) findViewById(R.id.tvRideTime);

        buttonScan.setOnClickListener(clickListener);
        buttonConnect.setOnClickListener(clickListener);
        buttonPebbleService.setOnClickListener(clickListener);
        buttonDataService.setOnClickListener(clickListener);

        if (PebbleConnectivity.isInstanceCreated())
            buttonPebbleService.setText(R.string.stop_pebble_service);
        else
            buttonPebbleService.setText(R.string.start_pebble_service);

        if (DataLogger.isInstanceCreated())
            buttonDataService.setText(R.string.stop_data_service);
        else
            buttonDataService.setText(R.string.start_data_service);

        if (mDeviceAddress == null || mDeviceAddress.isEmpty())
            buttonConnect.setEnabled(false);

//        Intent intent = getIntent();
//        boolean launchedFromPebble = intent.getBooleanExtra(Constants.LAUNCHED_FROM_PEBBLE, false);
//        if (launchedFromPebble)
//            startPebbleService();

        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBluetoothLeService != null &&
                mConnectionState != mBluetoothLeService.getConnectionState())
            setConnectionState(mBluetoothLeService.getConnectionState());
        registerReceiver(mBluetoothUpdateReceiver, BluetoothLeService.makeBluetoothUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBluetoothUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            close();
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    private void close() {
        if (PebbleConnectivity.isInstanceCreated()) {
            Intent PebbleServiceIntent = new Intent(getApplicationContext(), PebbleConnectivity.class);
            stopService(PebbleServiceIntent);
        }

        if (DataLogger.isInstanceCreated()) {
            Intent DataServiceIntent = new Intent(getApplicationContext(), DataLogger.class);
            stopService(DataServiceIntent);
        }

        Intent bluetoothServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        stopService(bluetoothServiceIntent);
    }

    private View.OnClickListener clickListener = new View.OnClickListener()
    {

        @Override
        public void onClick(View view)
        {
            switch (view.getId())
            {
                case R.id.buttonBluetoothScan:
                    Intent scanActivityIntent = new Intent(MainActivity.this, ScanActivity.class);
                    startActivityForResult(scanActivityIntent, DEVICE_SCAN_REQUEST);
                    break;
                case R.id.buttonBluetoothConnect:
                    if (mConnectionState == BluetoothLeService.STATE_DISCONNECTED)
                        connectToWheel();
                    else
                        disconnectFromWheel();
                    break;
                case R.id.buttonPebbleService:
                    if (!PebbleConnectivity.isInstanceCreated())
                        startPebbleService();
                    else
                        stopPebbleService();
                    break;
                case R.id.buttonDataLoggingService:

                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkExternalFilePermission())
                        requestExternalFilePermission();
                    else
                    {
                        Intent dataLoggerServiceIntent = new Intent(getApplicationContext(), DataLogger.class);
                        if (DataLogger.isInstanceCreated())
                        {
                            buttonDataService.setText(R.string.start_data_service);
                            stopService(dataLoggerServiceIntent);
                        }
                        else
                        {
                            buttonDataService.setText(R.string.stop_data_service);
                            startService(dataLoggerServiceIntent);
                        }
                        break;
                    }
                    break;
            }
        }
    };

    private void stopPebbleService() { startPebbleService(false);}
    private void startPebbleService() { startPebbleService(true);}
    private void startPebbleService(boolean start) {
        Intent pebbleServiceIntent = new Intent(getApplicationContext(), PebbleConnectivity.class);

        if (start) {
                buttonPebbleService.setText(R.string.stop_pebble_service);
                startService(pebbleServiceIntent);
        } else {
            buttonPebbleService.setText(R.string.start_pebble_service);
            stopService(pebbleServiceIntent);
        }
    }

    private void connectToWheel() { connectToWheel(true);}
    private void disconnectFromWheel() { connectToWheel(false);}
    private void connectToWheel(boolean connect) {

        if (connect) {
            setConnectionState(BluetoothLeService.STATE_CONNECTING);
            Boolean connecting = mBluetoothLeService.connect(mDeviceAddress);
            if (!connecting) {
                setConnectionState(BluetoothLeService.STATE_DISCONNECTED);
                Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
            }
        }
        else
         mBluetoothLeService.disconnect();
    }

    private boolean checkExternalFilePermission(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestExternalFilePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(this, "External write permission is required to write logs. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent dataLoggingIntent = new Intent(MainActivity.this, DataLogger.class);
                    startService(dataLoggingIntent);
                } else {
                    Toast.makeText(this, "External write permission is required to write logs. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEVICE_SCAN_REQUEST) {
            if (resultCode == RESULT_OK) {
                mDeviceAddress = data.getStringExtra("MAC");
                buttonConnect.setEnabled(true);
            }
        }
    }
}