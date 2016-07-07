package com.cooper.wheellog;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int DEVICE_SCAN_REQUEST = 10;

    Menu mMenu;
    MenuItem miWheel;
    MenuItem miWatch;
    MenuItem miLogging;

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

            if (mBluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED &&
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
                Log.d(TAG, "Bluetooth connected");
                setConnectionState(BluetoothLeService.STATE_CONNECTED);
            } else if (Constants.ACTION_BLUETOOTH_DISCONNECTED.equals(action)) {
                Log.d(TAG, "Bluetooth disconnected");
                setConnectionState(BluetoothLeService.STATE_DISCONNECTED);
            } else if (Constants.ACTION_WHEEL_DATA_AVAILABLE.equals(action)) {
                updateScreen();
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
                if (mDeviceAddress != null && !mDeviceAddress.isEmpty())
                    SettingsManager.setLastAddr(getApplicationContext(), mDeviceAddress);

                String serial = Wheel.getInstance().getSerial();
                if (serial == null || serial.isEmpty()) {
                    byte[] data = new byte[20];
                    data[0] = (byte) -86;
                    data[1] = (byte) 85;
                    data[16] = (byte) -101;
                    data[17] = (byte) 20;
                    data[18] = (byte) 90;
                    data[19] = (byte) 90;
                    mBluetoothLeService.writeBluetoothGattCharacteristic(data);
                }
                break;
            case BluetoothLeService.STATE_CONNECTING:
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                break;
        }
        mConnectionState = connectionState;
        setMenuIconStates();
    }

    private void setMenuIconStates() {
        if (mMenu == null)
            return;

        switch (mConnectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                miWheel.setIcon(R.drawable.ic_action_wheel_orange);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                break;
            case BluetoothLeService.STATE_CONNECTING:
                miWheel.setIcon(R.drawable.anim_wheel_icon);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                ((AnimationDrawable) miWheel.getIcon()).start();
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                miWheel.setIcon(R.drawable.ic_action_wheel_white);
                miWheel.setTitle(R.string.connect_to_wheel);
                break;
        }

        if (PebbleConnectivity.isInstanceCreated()) {
            miWatch.setIcon(R.drawable.ic_action_watch_orange);
        } else {
            miWatch.setIcon(R.drawable.ic_action_watch_white);
        }

        if (DataLogger.isInstanceCreated()) {
            miLogging.setTitle(R.string.stop_data_service);
            miLogging.setIcon(R.drawable.ic_action_logging_orange);
        } else {
            miLogging.setTitle(R.string.start_data_service);
            miLogging.setIcon(R.drawable.ic_action_logging_white);
        }
    }

    private void updateScreen() {
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDeviceAddress = SettingsManager.getLastAddr(getApplicationContext());
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();

//        if (mDeviceAddress == null || mDeviceAddress.isEmpty())
//            buttonConnect.setEnabled(false);

        if (mBluetoothLeService != null &&
                mConnectionState != mBluetoothLeService.getConnectionState())
            setConnectionState(mBluetoothLeService.getConnectionState());

        registerReceiver(mBluetoothUpdateReceiver, BluetoothLeService.makeBluetoothUpdateIntentFilter());
        updateScreen();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setMenuIconStates();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenu = menu;
        miWheel = mMenu.findItem(R.id.miWheel);
        miWatch = mMenu.findItem(R.id.miWatch);
        miLogging = mMenu.findItem(R.id.miLogging);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.miSearch:
                Intent scanActivityIntent = new Intent(MainActivity.this, ScanActivity.class);
                startActivityForResult(scanActivityIntent, DEVICE_SCAN_REQUEST);
                return true;
            case R.id.miWheel:
                if (mConnectionState == BluetoothLeService.STATE_DISCONNECTED)
                    connectToWheel();
                else
                    disconnectFromWheel();
                return true;
            case R.id.miLogging:
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkExternalFilePermission())
                    requestExternalFilePermission();
                else
                {
                    Intent dataLoggerServiceIntent = new Intent(getApplicationContext(), DataLogger.class);
                    if (DataLogger.isInstanceCreated()) {
                        stopService(dataLoggerServiceIntent);
                        miLogging.setIcon(R.drawable.ic_action_logging_white);
                        miLogging.setTitle(R.string.start_data_service);
                    } else {
                        startService(dataLoggerServiceIntent);
                        miLogging.setIcon(R.drawable.ic_action_logging_orange);
                        miLogging.setTitle(R.string.stop_data_service);
                    }
                }
                return true;
            case R.id.miWatch:
                if (!PebbleConnectivity.isInstanceCreated())
                    startPebbleService();
                else
                    stopPebbleService();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        Wheel.getInstance().reset();
    }

    private void stopPebbleService() { startPebbleService(false);}
    private void startPebbleService() { startPebbleService(true);}
    private void startPebbleService(boolean start) {
        Intent pebbleServiceIntent = new Intent(getApplicationContext(), PebbleConnectivity.class);

        if (start) {
            startService(pebbleServiceIntent);
            miWatch.setIcon(R.drawable.ic_action_watch_orange);
            miWatch.setTitle(R.string.stop_pebble_service);
        } else {
            stopService(pebbleServiceIntent);
            miWatch.setIcon(R.drawable.ic_action_watch_white);
            miWatch.setTitle(R.string.start_pebble_service);
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
                return;
            }
            Log.d(TAG, "Bluetooth connecting");
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
//                buttonConnect.setEnabled(true);
            }
        }
    }
}