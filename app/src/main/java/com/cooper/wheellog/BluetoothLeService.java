package com.cooper.wheellog;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private static final String DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";
//    private static final String NOTITY_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final String READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final boolean autoConnect = true;
    private static final boolean DEBUG = true;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private Handler mHandler = new Handler();

    public static IntentFilter makeBluetoothUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTED);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_REQUEST_SERIAL_DATA);
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_STARTED);
        return intentFilter;
    }

    public int getConnectionState() {
        return mConnectionState;
    }
    
    private void LOGI(final String msg) {
        if (DEBUG)
            Log.i(TAG, msg);
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                LOGI("Connected to GATT server.");
                // Attempts to discover services after successful connection.
                LOGI("Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Wheel.getInstance().setConnectionState(false);
                LOGI("Disconnected from GATT server.");
                broadcastUpdate(Constants.ACTION_BLUETOOTH_DISCONNECTED);
            }
            else
                Toast.makeText(BluetoothLeService.this, "Unknown Connection State\rState = " + String.valueOf(newState), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            LOGI("onServicesDiscovered called");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LOGI("onServicesDiscovered called, status == BluetoothGatt.GATT_SUCCESS");
                BluetoothGattService targetService = gatt.getService(UUID.fromString(BluetoothLeService.SERVICE_UUID));
                if (targetService == null) {
                    LOGI("targetService == null");
                    return;
                }
                BluetoothGattCharacteristic characteristic = targetService.getCharacteristic(UUID.fromString(BluetoothLeService.READ_CHARACTER_UUID));
                BluetoothGattCharacteristic writeCharacteristic = targetService.getCharacteristic(UUID.fromString(BluetoothLeService.READ_CHARACTER_UUID));
                if (writeCharacteristic == null) {
                    LOGI("not found target notify writecharacter");
                    return;
                }
                LOGI("writeCharacteristic write =" + writeCharacteristic.getWriteType());
                if (characteristic == null) {
                    LOGI("not found target notify character");
                    return;
                }
                LOGI("writeCharacteristic write =" + writeCharacteristic.getWriteType());
                LOGI("found target notify character");
                gatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(BluetoothLeService.DESCRIPTER_UUID));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                mConnectionState = BluetoothLeService.STATE_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                return;
            }
            LOGI("onServicesDiscovered called, status == BluetoothGatt.GATT_FAILURE");
        }

//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt,
//                                         BluetoothGattCharacteristic characteristic,
//                                         int status) {
//            super.onCharacteristicRead(gatt, characteristic, status);
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(Constants.ACTION_BLUETOOTH_DATA_AVAILABLE, characteristic);
//            }
//        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            LOGI("onCharacteristicChanged called " + characteristic.getUuid().toString());
            if (characteristic.getUuid().toString().equals(BluetoothLeService.READ_CHARACTER_UUID)) {
                byte[] value = characteristic.getValue();
//                StringBuilder stringBuilder = new StringBuilder(20);
//                for (int i = 0; i < value.length; i += 1) {
//                    Object[] objArr = new Object[1];
//                    objArr[0] = Byte.valueOf(value[i]);
//                    stringBuilder.append(String.format("%02X ", objArr));
//                }
//                LOGI("received data = " + stringBuilder);
                int result = Wheel.getInstance().decodeResponse(value);
                if (result == Constants.REQUEST_SERIAL_DATA)
                    sendBroadcast(new Intent(Constants.ACTION_REQUEST_SERIAL_DATA));

                Intent intent = new Intent(Constants.ACTION_WHEEL_DATA_AVAILABLE);
                sendBroadcast(intent);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            LOGI("onDescriptorWrite " + String.valueOf(status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Wheel.getInstance().setConnectionState(true);
                broadcastUpdate(Constants.ACTION_BLUETOOTH_CONNECTED);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

//    private void broadcastUpdate(final String action,
//                                 final BluetoothGattCharacteristic characteristic) {
//        final Intent intent = new Intent(action);
//        sendBroadcast(intent);
//    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(getString(R.string.notification_title))
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        if (mBluetoothAdapter == null)
            mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null || address.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, autoConnect, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (mConnectionState != STATE_CONNECTED) {
            mConnectionState = STATE_DISCONNECTED;
            broadcastUpdate(Constants.ACTION_BLUETOOTH_DISCONNECTED);
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        mHandler.removeCallbacksAndMessages(null);

        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

//    /**
//     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
//     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
//     * callback.
//     *
//     * @param characteristic The characteristic to read from.
//     */
//    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.readCharacteristic(characteristic);
//    }

//    /**
//     * Enables or disables notification on a give characteristic.
//     *
//     * @param characteristic Characteristic to act on.
//     * @param enabled If true, enable notification.  False otherwise.
//     */
//    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
//                                              boolean enabled) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
//    }


    public boolean writeBluetoothGattCharacteristic(byte[] cmd) {
        if (this.mBluetoothGatt == null) {
            return false;
        }
        BluetoothGattService service = this.mBluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
        if (service == null) {
            LOGI("writeBluetoothGattCharacteristic service == null");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(READ_CHARACTER_UUID));
        if (characteristic == null) {
            LOGI("writeBluetoothGattCharacteristic characteristic == null");
            return false;
        }
        characteristic.setValue(cmd);
        LOGI("writeBluetoothGattCharacteristic writeType = " + characteristic.getWriteType());
        characteristic.setWriteType(1);
        return this.mBluetoothGatt.writeCharacteristic(characteristic);
    }
    
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
//    public List<BluetoothGattService> getSupportedGattServices() {
//        if (mBluetoothGatt == null) return null;
//
//        return mBluetoothGatt.getServices();
//    }
}
