package com.cooper.wheellog;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int DEVICE_SCAN_REQUEST = 10;

    Button buttonPebbleService;
    Button buttonScan;
    Button buttonConnect;

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

    Handler mHandler = new Handler();
    private BluetoothLeService mBluetoothLeService;
    String mDeviceAddress;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private boolean doubleBackToExitPressedOnce = false;

    private final String TAG = "MainActivity";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            if (mBluetoothLeService.isConnected()) {
                mConnectionState = BluetoothLeService.STATE_CONNECTED;
                buttonConnect.setText("DISCONNECT");
            }
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
            if (Constants.ACTION_BLUETOOTH_CONNECTED.equals(action)) { // && mConnectionState != BluetoothLeService.STATE_CONNECTED) {
                Log.d(TAG, "Bluetooth connected");
                mConnectionState = BluetoothLeService.STATE_CONNECTED;
                buttonConnect.setText("DISCONNECT");
                SettingsManager.setLastAddr(getApplicationContext(), mDeviceAddress);
                buttonPebbleService.setEnabled(true);
                byte[] data = new byte[20];
                data[0] = (byte) -86;
                data[1] = (byte) 85;
                data[16] = (byte) -101;
                data[17] = (byte) 20;
                data[18] = (byte) 90;
                data[19] = (byte) 90;
                mBluetoothLeService.writeBluetoothGattCharacteristic(data);
            } else if (Constants.ACTION_BLUETOOTH_DISCONNECTED.equals(action)) {
                mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
                buttonConnect.setText("CONNECT");
            } else if (Constants.ACTION_WHEEL_DATA_AVAILABLE.equals(action)) {
                textViewSpeed.setText(String.format("%s KPH", Wheel.getInstance().getSpeedDouble()));
                textViewVoltage.setText(String.format("%sV", Wheel.getInstance().getVoltageDouble()));
                textViewTemperature.setText(String.format("%d°C", Wheel.getInstance().getTemperature()));
                textViewCurrent.setText(String.format("%sW", Wheel.getInstance().getCurrentDouble()));
                textViewBattery.setText(String.format("%d%%", Wheel.getInstance().getBatteryLevel()));
                textViewFanStatus.setText(Wheel.getInstance().getFanStatus() == 0 ? "Off" : "On");
                textViewMaxSpeed.setText(String.format("%s KPH", Wheel.getInstance().getMaxSpeedDouble()));
                textViewCurrentDistance.setText(String.format("%s KM", Wheel.getInstance().getCurrentDistanceDouble()));
                textViewTotalDistance.setText(String.format("%s KM", Wheel.getInstance().getTotalDistanceDouble()));
                textViewVersion.setText(String.format("%d", Wheel.getInstance().getVersion()));
                textViewName.setText(Wheel.getInstance().getName());
                textViewType.setText(Wheel.getInstance().getType());
                textViewSerial.setText(Wheel.getInstance().getSerial());
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mDeviceAddress = SettingsManager.getLastAddr(getApplicationContext());

        buttonPebbleService = (Button) findViewById(R.id.buttonPebbleService);
        buttonScan = (Button) findViewById(R.id.buttonBluetoothScan);
        buttonConnect = (Button) findViewById(R.id.buttonBluetoothConnect);

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

        buttonScan.setOnClickListener(clickListener);
        buttonConnect.setOnClickListener(clickListener);
        buttonPebbleService.setOnClickListener(clickListener);

        if (PebbleConnectivity.isInstanceCreated())
            buttonPebbleService.setText("STOP");
        else
            buttonPebbleService.setText("START");

        if (!mDeviceAddress.isEmpty())
            buttonConnect.setEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mBluetoothUpdateReceiver, BluetoothLeService.makeBluetoothUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBluetoothUpdateReceiver);
        mHandler.removeCallbacksAndMessages(null);
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

        Intent bluetoothServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        stopService(bluetoothServiceIntent);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view)
        {
            switch (view.getId()) {
                case R.id.buttonPebbleService:
                    buttonPebbleService.setText("STARTED");
                    Intent intent1 = new Intent(getApplicationContext(), PebbleConnectivity.class);
                    startService(intent1);
                    break;
                case R.id.buttonBluetoothScan:
                    Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                    startActivityForResult(intent, DEVICE_SCAN_REQUEST);
                    break;
                case R.id.buttonBluetoothConnect:
                    if (mConnectionState == BluetoothLeService.STATE_DISCONNECTED)
                        mBluetoothLeService.connect(mDeviceAddress);
                    else {
                        mBluetoothLeService.disconnect();
                    }
            }
        }
    };

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
