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
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cooper.wheellog.utils.SettingsUtil;

import java.util.Arrays;

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
            final String advData = mDeviceListAdapter.getAdvData(i);
            Timber.i("Device selected MAC = %s", deviceAddress);
            Timber.i("Device selected Name = %s", deviceName);
            Timber.i("Device selected Data = %s", advData);
            Intent intent = new Intent();
            intent.putExtra("MAC", deviceAddress);
            intent.putExtra("NAME", deviceName);
            intent.putExtra("ADV", advData);
            SettingsUtil.setAdvDataForWheel(view.getContext(), deviceAddress, advData);
            setResult(RESULT_OK, intent);
            //Ask for inmotion password
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(R.string.wheel_pass_imotion);

            final EditText input = new EditText(view.getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String password = input.getText().toString();
                    SettingsUtil.setPasswordForWheel(view.getContext(), deviceAddress, password);
                    finish();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
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
                    final String manufacturerData = findManufacturerData(scanRecord); // 4e421300000000ec
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceListAdapter.addDevice(device, manufacturerData);
                            mDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };


    public String findManufacturerData(byte[] scanRecord) {
        int index = 0;
        String result = "";
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            //Done once we run out of records
            if (length == 0) break;

            int type = scanRecord[index];
            //Done if our record isn't a valid type
            if (type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index+1, index+length);

            //Advance
            index += length;
            if (type == -1) {
                result = ByteArrayToString(data);
            }
        }
        Timber.i("Found data: " + result);
        return result;
    }

    public static String ByteArrayToString(byte[] ba)
    {
        String hexString = "";
        for(int i = 0; i < ba.length; i++){
            String thisByte = "".format("%02x", ba[i]);
            hexString += thisByte;
        }
        return hexString;
    }


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
