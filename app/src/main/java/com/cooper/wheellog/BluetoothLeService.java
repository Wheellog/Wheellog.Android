package com.cooper.wheellog;

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
import android.widget.Toast;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.NotificationUtil;
import com.cooper.wheellog.utils.SettingsUtil;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private Date mDisconnectTime;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private boolean disconnectRequested = false;
    private boolean autoConnect = false;
    private NotificationUtil mNotificationHandler;

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                    int connectionState = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, STATE_DISCONNECTED);
                    switch (connectionState) {
                        case STATE_CONNECTED:
                            mConnectionState = STATE_CONNECTED;
                            if (!LoggingService.isInstanceCreated() && SettingsUtil.isAutoLogEnabled(BluetoothLeService.this))
                                startService(new Intent(getApplicationContext(), LoggingService.class));
                            break;
                        case STATE_DISCONNECTED:
                            mConnectionState = STATE_DISCONNECTED;
                            break;
                        case STATE_CONNECTING:
                            mConnectionState = STATE_CONNECTING;
                            break;
                    }
                    break;
                case Constants.ACTION_REQUEST_KINGSONG_NAME_DATA:
                    if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.KINGSONG) {
                        byte[] data = new byte[20];
                        data[0] = (byte) -86;
                        data[1] = (byte) 85;
                        data[16] = (byte) -101;
                        data[17] = (byte) 20;
                        data[18] = (byte) 90;
                        data[19] = (byte) 90;
                        writeBluetoothGattCharacteristic(data);
                    }
                    break;
                case Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA:
                    if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.KINGSONG) {
                        byte[] data = new byte[20];
                        data[0] = (byte) -86;
                        data[1] = (byte) 85;
                        data[16] = (byte) 99;
                        data[17] = (byte) 20;
                        data[18] = (byte) 90;
                        data[19] = (byte) 90;
                        writeBluetoothGattCharacteristic(data);
                    }
                    break;
                case Constants.ACTION_REQUEST_KINGSONG_HORN:
                    if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.KINGSONG) {
                        byte[] data = new byte[20];
                        data[0] = (byte) -86;
                        data[1] = (byte) 85;
                        data[16] = (byte) -120;
                        data[17] = (byte) 20;
                        data[18] = (byte) 90;
                        data[19] = (byte) 90;
                        writeBluetoothGattCharacteristic(data);
                    }
                    break;
                case Constants.ACTION_REQUEST_CONNECTION_TOGGLE:
                    if (mConnectionState == STATE_DISCONNECTED)
                        connect();
                    else {
                        disconnect();
                        close();
                    }
                    break;
            }
        }
    };

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA);
        intentFilter.addAction(Constants.ACTION_REQUEST_KINGSONG_NAME_DATA);
        intentFilter.addAction(Constants.ACTION_REQUEST_KINGSONG_HORN);
        intentFilter.addAction(Constants.ACTION_REQUEST_CONNECTION_TOGGLE);
        return intentFilter;
    }

    public int getConnectionState() {
        return mConnectionState;
    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Timber.i("Connected to GATT server.");
                mDisconnectTime = null;
                // Attempts to discover services after successful connection.
                Timber.i("Attempting to start service discovery:%b",
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Timber.i("Disconnected from GATT server.");
                if (mConnectionState == STATE_CONNECTED)
                    mDisconnectTime = Calendar.getInstance().getTime();
                if (!disconnectRequested &&
                        mBluetoothGatt != null && mBluetoothGatt.getDevice() != null) {
                    Timber.i("Trying to reconnect");
                    if (!autoConnect) {
                        autoConnect = true;
                        mBluetoothGatt.close();
                        mBluetoothGatt = mBluetoothGatt.getDevice().connectGatt(BluetoothLeService.this, autoConnect, mGattCallback);
                        broadcastConnectionUpdate(STATE_CONNECTING, true);
                    } else
                        broadcastConnectionUpdate(STATE_CONNECTING, true);
                } else {
                    Timber.i("Disconnected");
                    mConnectionState = STATE_DISCONNECTED;
                    broadcastConnectionUpdate(STATE_DISCONNECTED);
                }
            }
            else
                Toast.makeText(BluetoothLeService.this, "Unknown Connection State\rState = " + String.valueOf(newState), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Timber.i("onServicesDiscovered called");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.i("onServicesDiscovered called, status == BluetoothGatt.GATT_SUCCESS");
                boolean recognisedWheel = WheelData.getInstance().detectWheel(BluetoothLeService.this);
                if (recognisedWheel) {
                    mConnectionState = STATE_CONNECTED;
                    broadcastConnectionUpdate(mConnectionState);
                } else
                    disconnect();
                return;
            }
            Timber.i("onServicesDiscovered called, status == BluetoothGatt.GATT_FAILURE");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Timber.v("onCharacteristicRead called %s", characteristic.getUuid().toString());
            readData(characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Timber.v("onCharacteristicChanged called %s", characteristic.getUuid().toString());
            readData(characteristic, BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Timber.v("onDescriptorWrite %d", status);
        }
    };

    private void readData(BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.KINGSONG) {
                if (characteristic.getUuid().toString().equals(Constants.KINGSONG_READ_CHARACTER_UUID)) {
                    byte[] value = characteristic.getValue();
                    WheelData.getInstance().decodeResponse(value);
                }
            }

            if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.GOTWAY) {
                byte[] value = characteristic.getValue();
                WheelData.getInstance().decodeResponse(value);
            }
        }
    }

    private void broadcastConnectionUpdate(int connectionState) {
        broadcastConnectionUpdate(connectionState, false);
    }

    private void broadcastConnectionUpdate(int connectionState, boolean auto_connect) {
        WheelData.getInstance().setConnected(connectionState == STATE_CONNECTED);
        final Intent intent = new Intent(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intent.putExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, connectionState);
        if (auto_connect)
            intent.putExtra(Constants.INTENT_EXTRA_BLE_AUTO_CONNECT, true);
        sendBroadcast(intent);
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
        mNotificationHandler = new NotificationUtil(this);
        registerReceiver(messageReceiver, makeIntentFilter());
        startForeground(Constants.MAIN_NOTIFICATION_ID, mNotificationHandler.buildNotification());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothGatt != null &&
                mConnectionState != STATE_DISCONNECTED)
            mBluetoothGatt.disconnect();
        close();
        mNotificationHandler.unregisterReceiver();
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
                Timber.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }
        if (mBluetoothAdapter == null)
            mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Timber.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public void setDeviceAddress(String address) {
        if (address != null && !address.isEmpty())
            mBluetoothDeviceAddress = address;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */

    public boolean connect() { //final String address) {
        disconnectRequested = false;
        autoConnect = false;
        mDisconnectTime = null;

        if (mBluetoothAdapter == null || mBluetoothDeviceAddress == null || mBluetoothDeviceAddress.isEmpty()) {
            Timber.w("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothGatt != null && mBluetoothGatt.getDevice().getAddress().equals(mBluetoothDeviceAddress)) {
            Timber.i("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                broadcastConnectionUpdate(mConnectionState);
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress);
        if (device == null) {
            Timber.w("Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, autoConnect, mGattCallback);
        Timber.i("Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
        broadcastConnectionUpdate(mConnectionState);
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
            Timber.w("BluetoothAdapter not initialized");
            return;
        }
        if (mConnectionState != STATE_CONNECTED)
            mConnectionState = STATE_DISCONNECTED;
        mBluetoothGatt.disconnect();
        broadcastConnectionUpdate(STATE_DISCONNECTED);
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
//            Timber.w(TAG, "BluetoothAdapter not initialized");
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
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.w("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }


    public boolean writeBluetoothGattCharacteristic(byte[] cmd) {
        if (this.mBluetoothGatt == null) {
            return false;
        }

        switch (WheelData.getInstance().getWheelType()) {
            case KINGSONG:
                BluetoothGattService ks_service = this.mBluetoothGatt.getService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID));
                if (ks_service == null) {
                    Timber.v("writeBluetoothGattCharacteristic service == null");
                    return false;
                }
                BluetoothGattCharacteristic ks_characteristic = ks_service.getCharacteristic(UUID.fromString(Constants.KINGSONG_READ_CHARACTER_UUID));
                if (ks_characteristic == null) {
                    Timber.v("writeBluetoothGattCharacteristic characteristic == null");
                    return false;
                }
                ks_characteristic.setValue(cmd);
                Timber.v("writeBluetoothGattCharacteristic writeType = %d", ks_characteristic.getWriteType());
                ks_characteristic.setWriteType(1);
                return this.mBluetoothGatt.writeCharacteristic(ks_characteristic);
            case GOTWAY:
                BluetoothGattService gw_service = this.mBluetoothGatt.getService(UUID.fromString(Constants.GOTWAY_SERVICE_UUID));
                if (gw_service == null) {
                    Timber.v("writeBluetoothGattCharacteristic service == null");
                    return false;
                }
                BluetoothGattCharacteristic characteristic = gw_service.getCharacteristic(UUID.fromString(Constants.GOTWAY_READ_CHARACTER_UUID));
                if (characteristic == null) {
                    Timber.v("writeBluetoothGattCharacteristic characteristic == null");
                    return false;
                }
                characteristic.setValue(cmd);
                Timber.v("writeBluetoothGattCharacteristic writeType = %d", characteristic.getWriteType());
                return this.mBluetoothGatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    public void writeBluetoothGattDescriptor(BluetoothGattDescriptor descriptor) {
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public Date getDisconnectTime() {
        return mDisconnectTime;
    }
    
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public BluetoothGattService getGattService(UUID service_id) {
        return mBluetoothGatt.getService(service_id);
    }
}
