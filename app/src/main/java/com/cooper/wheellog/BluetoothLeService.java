package com.cooper.wheellog;

import android.app.Notification;
import android.app.NotificationManager;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
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
    
    private final static String TAG = "BluetoothLeService";
    private static final boolean DEBUG = false;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    private static final int notification_id = 1;

    private boolean disconnectRequested = false;
    private boolean autoConnect = false;


    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Constants.ACTION_BLUETOOTH_CONNECTED.equals(action)) {
                mConnectionState = STATE_CONNECTED;
                ((WheelLog) getApplicationContext()).setConnectionState(true);
                updateNotification(R.string.wheel_connected);
            } else if (Constants.ACTION_BLUETOOTH_DISCONNECTED.equals(action)) {
                mConnectionState = STATE_DISCONNECTED;
                ((WheelLog) getApplicationContext()).setConnectionState(false);
                updateNotification(R.string.wheel_disconnected);
            } else if (Constants.ACTION_BLUETOOTH_CONNECTING.equals(action)) {
                ((WheelLog) getApplicationContext()).setConnectionState(false);
                mConnectionState = STATE_CONNECTING;
                if (intent.hasExtra(Constants.INTENT_EXTRA_BLE_AUTO_CONNECT))
                    updateNotification(R.string.wheel_searching);
                else
                    updateNotification(R.string.wheel_connecting);

            } else if (Constants.ACTION_REQUEST_KINGSONG_NAME_DATA.equals(action)) {

                byte[] data = new byte[20];
                data[0] = (byte) -86;
                data[1] = (byte) 85;
                data[16] = (byte) -101;
                data[17] = (byte) 20;
                data[18] = (byte) 90;
                data[19] = (byte) 90;
                writeBluetoothGattCharacteristic(data);
            } else if (Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA.equals(action)) {

                byte[] data = new byte[20];
                data[0] = (byte) -86;
                data[1] = (byte) 85;
                data[16] = (byte) 99;
                data[17] = (byte) 20;
                data[18] = (byte) 90;
                data[19] = (byte) 90;
                writeBluetoothGattCharacteristic(data);
            } else if (Constants.ACTION_REQUEST_KINGSONG_HORN.equals(action)) {

                byte[] data = new byte[20];
                data[0] = (byte) -86;
                data[1] = (byte) 85;
                data[16] = (byte) -120;
                data[17] = (byte) 20;
                data[18] = (byte) 90;
                data[19] = (byte) 90;
                writeBluetoothGattCharacteristic(data);
            }
        }
    };

    public static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTING);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTED);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA);
        intentFilter.addAction(Constants.ACTION_REQUEST_KINGSONG_NAME_DATA);
        intentFilter.addAction(Constants.ACTION_REQUEST_KINGSONG_HORN);
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_STARTED);
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_STARTED);
        intentFilter.addAction(Constants.ACTION_WHEEL_TYPE_DEFINED);
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
                LOGI("Disconnected from GATT server.");
                if (!disconnectRequested && !autoConnect &&
                        mBluetoothGatt != null && mBluetoothGatt.getDevice() != null) {
                    autoConnect = true;
                    mBluetoothGatt = mBluetoothGatt.getDevice().connectGatt(BluetoothLeService.this, autoConnect, mGattCallback);
                    updateNotification(R.string.wheel_searching);
                    broadcastUpdate(Constants.ACTION_BLUETOOTH_CONNECTING, Constants.INTENT_EXTRA_BLE_AUTO_CONNECT);
                } else {
                    mConnectionState = STATE_DISCONNECTED;
                    broadcastUpdate(Constants.ACTION_BLUETOOTH_DISCONNECTED);
                }
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

                BluetoothGattService targetService;
                targetService = gatt.getService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID));
                if (targetService != null) {
                    ((WheelLog) getApplicationContext()).setWheelType(Constants.WHEEL_TYPE_KINGSONG);
                    broadcastUpdate(Constants.ACTION_WHEEL_TYPE_DEFINED);
                } else {
                    LOGI("targetService == null");
                    disconnect();
                    return;
                }

                BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.KINGSONG_NOTITY_CHARACTER_UUID));
                if (notifyCharacteristic == null) {
                    LOGI("not found target notify notifyCharacteristic");
                    disconnect();
                    return;
                }
                LOGI("found target notify character");
                gatt.setCharacteristicNotification(notifyCharacteristic, true);
                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.KINGSONG_DESCRIPTER_UUID));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(Constants.ACTION_BLUETOOTH_CONNECTED);
                return;
            }
            LOGI("onServicesDiscovered called, status == BluetoothGatt.GATT_FAILURE");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            LOGI("onCharacteristicChanged called " + characteristic.getUuid().toString());

            if (((WheelLog) getApplicationContext()).getWheelType() == Constants.WHEEL_TYPE_KINGSONG) {
                if (characteristic.getUuid().toString().equals(Constants.KINGSONG_READ_CHARACTER_UUID)) {
                    byte[] value = characteristic.getValue();
                    ((WheelLog) getApplicationContext()).decodeResponse(value);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            LOGI("onDescriptorWrite " + String.valueOf(status));
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String extra) {
        final Intent intent = new Intent(action);
        intent.putExtra(extra, true);
        sendBroadcast(intent);
    }

    private Notification buildNotification(int text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_wheel)
                .setContentTitle(getString(text))
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(int text) {
        Notification notification = buildNotification(text);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notification_id, notification);
    }

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

        Notification notification = buildNotification(R.string.wheel_disconnected);

        registerReceiver(messageReceiver, makeIntentFilter());
        startForeground(notification_id, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothGatt != null &&
                mConnectionState != STATE_DISCONNECTED)
            mBluetoothGatt.disconnect();
        close();
        unregisterReceiver(messageReceiver);
        stopForeground(true);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
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
        disconnectRequested = false;
        autoConnect = false;

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
                broadcastUpdate(Constants.ACTION_BLUETOOTH_CONNECTING);
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
        broadcastUpdate(Constants.ACTION_BLUETOOTH_CONNECTING);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        disconnectRequested = true;
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (mConnectionState != STATE_CONNECTED)
            mConnectionState = STATE_DISCONNECTED;
        mBluetoothGatt.disconnect();
        broadcastUpdate(Constants.ACTION_BLUETOOTH_DISCONNECTED);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
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
        BluetoothGattService service = this.mBluetoothGatt.getService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID));
        if (service == null) {
            LOGI("writeBluetoothGattCharacteristic service == null");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(Constants.KINGSONG_READ_CHARACTER_UUID));
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
