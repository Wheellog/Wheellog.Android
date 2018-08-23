package com.cooper.wheellog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.cooper.wheellog.utils.InMotionAdapter;
import com.cooper.wheellog.utils.SettingsUtil;
import timber.log.Timber;

public class ScanActivity extends AppCompatActivity {
    private DeviceListAdapter mDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler = new Handler();
    private ProgressBar pb;
    private TextView scanTitle;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ViewGroup nullParent = null;
        View convertView = getLayoutInflater().inflate(R.layout.activity_scan, nullParent);
        ListView lv = (ListView) convertView.findViewById(android.R.id.list);
        pb = (ProgressBar) convertView.findViewById(R.id.scanProgress);
        scanTitle = (TextView) convertView.findViewById(R.id.scan_title);
        lv.setOnItemClickListener(onItemClickListener);
        mDeviceListAdapter = new DeviceListAdapter(this);
        lv.setAdapter(mDeviceListAdapter);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert)
                .setView(convertView)
                .setCancelable(false)
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialogInterface, int keycode, KeyEvent keyEvent) {
                        if (keycode == KeyEvent.KEYCODE_BACK &&
                                keyEvent.getAction() == KeyEvent.ACTION_UP &&
                                !keyEvent.isCanceled()) {
                            if (mScanning)
                                scanLeDevice(false);
                            mHandler.removeCallbacksAndMessages(null);
                            dialogInterface.cancel();
                            finish();
                            return true;
                        }
                        return false;
                    }
                });
        alertDialog.show();

        if (!isLocationEnabled(this)) {
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(myIntent);
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        scanLeDevice(true);
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
            if (mScanning)
                scanLeDevice(false);
            mHandler.removeCallbacksAndMessages(null);
            final String deviceAddress = mDeviceListAdapter.getDevice(i).getAddress();
            final String deviceName = mDeviceListAdapter.getDevice(i).getName();
            Timber.i("Device selected = %s", deviceAddress);
            Timber.i("Device selected = %s", deviceName);
            Intent intent = new Intent();
            intent.putExtra("MAC", deviceAddress);
            intent.putExtra("NAME", deviceName);
            setResult(RESULT_OK, intent);
            //Ask for inmotion password
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle("Wheel Password ( InMotion only )");

            final EditText input = new EditText(view.getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String password = input.getText().toString();
                    SettingsUtil.setPasswordForWheel(view.getContext(), deviceAddress, password);
                    finish();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });
            builder.show();
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceListAdapter.addDevice(device);
                            mDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(false);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            pb.setVisibility(View.VISIBLE);
            scanTitle.setText(R.string.scanning);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            pb.setVisibility(View.GONE);
            scanTitle.setText(R.string.devices);
        }
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }
}
