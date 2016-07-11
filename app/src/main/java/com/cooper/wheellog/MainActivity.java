package com.cooper.wheellog;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Menu mMenu;
    MenuItem miSearch;
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

    private WheelLog wheelLog;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private boolean doubleBackToExitPressedOnce = false;
    private Snackbar snackbar;

    private final String TAG = "MainActivity";
    private static final int RESULT_PERMISSION_REQUEST_CODE = 10;
    private static final int RESULT_DEVICE_SCAN_REQUEST = 20;
    private static final int RESULT_REQUEST_ENABLE_BT = 30;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, getResources().getString(R.string.error_bluetooth_not_initialised));
                Toast.makeText(MainActivity.this, R.string.error_bluetooth_not_initialised, Toast.LENGTH_SHORT).show();
                finish();
            }

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
            if (Constants.ACTION_BLUETOOTH_CONNECTING.equals(action) && mConnectionState != BluetoothLeService.STATE_CONNECTING) {
                Log.d(TAG, "Bluetooth Connecting");
                setConnectionState(BluetoothLeService.STATE_CONNECTING);
            } else if (Constants.ACTION_BLUETOOTH_CONNECTED.equals(action) && mConnectionState != BluetoothLeService.STATE_CONNECTED) {
                Log.d(TAG, "Bluetooth connected");
                if (wheelLog.getName().isEmpty()) {
                    final Intent getNameIntent = new Intent(Constants.ACTION_REQUEST_NAME_DATA);
                    sendBroadcast(getNameIntent);
                } else if (wheelLog.getSerial().isEmpty()) {
                    final Intent getSerialIntent = new Intent(Constants.ACTION_REQUEST_SERIAL_DATA);
                    sendBroadcast(getSerialIntent);
                }
                setConnectionState(BluetoothLeService.STATE_CONNECTED);
            } else if (Constants.ACTION_BLUETOOTH_DISCONNECTED.equals(action)) {
                Log.d(TAG, "Bluetooth disconnected");
                setConnectionState(BluetoothLeService.STATE_DISCONNECTED);
            } else if (Constants.ACTION_WHEEL_DATA_AVAILABLE.equals(action)) {
                updateScreen();
            }
        }
    };

    private void setConnectionState(int connectionState) {

        switch (connectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                if (mDeviceAddress != null && !mDeviceAddress.isEmpty())
                    SettingsManager.setLastAddr(getApplicationContext(), mDeviceAddress);
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

        if (mDeviceAddress == null || mDeviceAddress.isEmpty()) {
            miWheel.setEnabled(false);
            miWheel.getIcon().setAlpha(64);
        }

            switch (mConnectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                miWheel.setIcon(R.drawable.ic_action_wheel_orange);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                break;
            case BluetoothLeService.STATE_CONNECTING:
                miWheel.setIcon(R.drawable.anim_wheel_icon);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                ((AnimationDrawable) miWheel.getIcon()).start();
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                miWheel.setIcon(R.drawable.ic_action_wheel_white);
                miWheel.setTitle(R.string.connect_to_wheel);
                miSearch.setEnabled(true);
                miSearch.getIcon().setAlpha(255);
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
        textViewSpeed.setText(String.format(Locale.US, "%.1f KPH", wheelLog.getSpeedDouble()));
        textViewVoltage.setText(String.format("%sV", wheelLog.getVoltageDouble()));
        textViewTemperature.setText(String.format(Locale.US, "%d°C", wheelLog.getTemperature()));
        textViewCurrent.setText(String.format("%sW", wheelLog.getCurrentDouble()));
        textViewBattery.setText(String.format(Locale.US, "%d%%", wheelLog.getBatteryLevel()));
        textViewFanStatus.setText(wheelLog.getFanStatus() == 0 ? "Off" : "On");
        textViewMaxSpeed.setText(String.format(Locale.US, "%.1f KPH", wheelLog.getMaxSpeedDouble()));
        textViewCurrentDistance.setText(String.format(Locale.US, "%.2f KM", wheelLog.getDistanceDouble()));
        textViewTotalDistance.setText(String.format(Locale.US, "%.2f KM", wheelLog.getTotalDistanceDouble()));
        textViewVersion.setText(String.format(Locale.US, "%d", wheelLog.getVersion()));
        textViewName.setText(wheelLog.getName());
        textViewType.setText(wheelLog.getType());
        textViewSerial.setText(wheelLog.getSerial());
        textViewRideTime.setText(wheelLog.getCurrentTimeString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDeviceAddress = SettingsManager.getLastAddr(getApplicationContext());
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        wheelLog = (WheelLog) getApplicationContext();

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

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        } else if (!mBluetoothAdapter.isEnabled()) {


            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, RESULT_REQUEST_ENABLE_BT);
            }
        } else {
            startBluetoothService();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

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

        if (PebbleConnectivity.isInstanceCreated()) {
            Intent PebbleServiceIntent = new Intent(getApplicationContext(), PebbleConnectivity.class);
            stopService(PebbleServiceIntent);
        }

        if (DataLogger.isInstanceCreated()) {
            Intent DataServiceIntent = new Intent(getApplicationContext(), DataLogger.class);
            stopService(DataServiceIntent);
        }

        if (mBluetoothLeService != null) {
            unbindService(mServiceConnection);
            mBluetoothLeService.close();
            mBluetoothLeService = null;
            Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
            stopService(gattServiceIntent);
        }
        wheelLog.reset();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenu = menu;
        miSearch = mMenu.findItem(R.id.miSearch);
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
                startActivityForResult(scanActivityIntent, RESULT_DEVICE_SCAN_REQUEST);
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
            finish();
            return;
        }

        doubleBackToExitPressedOnce = true;
        showSnackBar(R.string.back_to_exit);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    private void showSnackBar(int msg)
    {
        if (snackbar == null) {
            View mainView = findViewById(R.id.main_view);
            snackbar = Snackbar
                    .make(mainView, "", Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundResource(R.color.primary_dark);
            snackbar.setDuration(2000);
        }
        snackbar.setText(msg);
        snackbar.show();
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

    private void startBluetoothService() {
        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void connectToWheel() { connectToWheel(true);}
    private void disconnectFromWheel() { connectToWheel(false);}
    private void connectToWheel(boolean connect) {

        if (connect) {
            Boolean connecting = mBluetoothLeService.connect(mDeviceAddress);
            if (!connecting)
                showSnackBar(R.string.connection_failed);
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
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, RESULT_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case RESULT_PERMISSION_REQUEST_CODE:
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
        switch (requestCode) {
            case RESULT_DEVICE_SCAN_REQUEST:
                if (resultCode == RESULT_OK) {
                    mDeviceAddress = data.getStringExtra("MAC");
                    wheelLog.reset();
                    updateScreen();
                    connectToWheel();
                }
                break;
            case RESULT_REQUEST_ENABLE_BT:
                if (mBluetoothAdapter.isEnabled())
                    startBluetoothService();
                else {
                    Toast.makeText(this, R.string.bluetooth_required, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }
}