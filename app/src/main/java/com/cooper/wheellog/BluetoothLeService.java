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
import com.cooper.wheellog.utils.NotificationUtil;

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

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private boolean disconnectRequested = false;
    private boolean autoConnect = false;
    private NotificationUtil mNotificationHandler;

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Constants.ACTION_BLUETOOTH_CONNECTED.equals(action)) {
                mConnectionState = STATE_CONNECTED;
                WheelData.getInstance().setConnectionState(true);
            } else if (Constants.ACTION_BLUETOOTH_DISCONNECTED.equals(action)) {
                mConnectionState = STATE_DISCONNECTED;
                WheelData.getInstance().setConnectionState(false);
            } else if (Constants.ACTION_BLUETOOTH_CONNECTING.equals(action)) {
                WheelData.getInstance().setConnectionState(false);
                mConnectionState = STATE_CONNECTING;
            } else if (Constants.ACTION_REQUEST_KINGSONG_NAME_DATA.equals(action)) {
                if (WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE_KINGSONG) {
                    byte[] data = new byte[20];
                    data[0] = (byte) -86;
                    data[1] = (byte) 85;
                    data[16] = (byte) -101;
                    data[17] = (byte) 20;
                    data[18] = (byte) 90;
                    data[19] = (byte) 90;
                    writeBluetoothGattCharacteristic(data);
                }
            } else if (Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA.equals(action)) {
                if (WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE_KINGSONG) {
                    byte[] data = new byte[20];
                    data[0] = (byte) -86;
                    data[1] = (byte) 85;
                    data[16] = (byte) 99;
                    data[17] = (byte) 20;
                    data[18] = (byte) 90;
                    data[19] = (byte) 90;
                    writeBluetoothGattCharacteristic(data);
                }
            } else if (Constants.ACTION_REQUEST_KINGSONG_HORN.equals(action)) {
                if (WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE_KINGSONG) {
                    byte[] data = new byte[20];
                    data[0] = (byte) -86;
                    data[1] = (byte) 85;
                    data[16] = (byte) -120;
                    data[17] = (byte) 20;
                    data[18] = (byte) 90;
                    data[19] = (byte) 90;
                    writeBluetoothGattCharacteristic(data);
                }
            } else if (Constants.ACTION_REQUEST_CONNECTION_TOGGLE.equals(action)) {
                if (mConnectionState == STATE_DISCONNECTED)
                    connect();
                else {
                    disconnect();
                    close();
                }
            }
        }
    };

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTING);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTED);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_DISCONNECTED);
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
                // Attempts to discover services after successful connection.
                Timber.i("Attempting to start service discovery:%b",
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Timber.i("Disconnected from GATT server.");
                if (!disconnectRequested && !autoConnect &&
                        mBluetoothGatt != null && mBluetoothGatt.getDevice() != null) {
                    autoConnect = true;
                    mBluetoothGatt = mBluetoothGatt.getDevice().connectGatt(BluetoothLeService.this, autoConnect, mGattCallback);
//                    Notifications.updateNotification(BluetoothLeService.this, R.string.wheel_searching, mConnectionState);
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
            Timber.i("onServicesDiscovered called");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.i("onServicesDiscovered called, status == BluetoothGatt.GATT_SUCCESS");
                boolean recognisedWheel = WheelData.getInstance().detectWheel(gatt);
                if (recognisedWheel) {
                    mConnectionState = STATE_CONNECTED;
                    broadcastUpdate(Constants.ACTION_BLUETOOTH_CONNECTED);
                } else
                    disconnect();
                return;
            }
            Timber.i("onServicesDiscovered called, status == BluetoothGatt.GATT_FAILURE");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Timber.i("onCharacteristicRead called %s", characteristic.getUuid().toString());
            readData(characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Timber.i("onCharacteristicChanged called %s", characteristic.getUuid().toString());
            readData(characteristic, BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Timber.i("onDescriptorWrite %d", status);
        }
    };

    private void readData(BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE_KINGSONG) {
                if (characteristic.getUuid().toString().equals(Constants.KINGSONG_READ_CHARACTER_UUID)) {
                    byte[] value = characteristic.getValue();
                    WheelData.getInstance().decodeResponse(value);
                }
            }

            if (WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE_GOTWAY) {
                byte[] value = characteristic.getValue();
                WheelData.getInstance().decodeResponse(value);
            }
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String extra) {
        final Intent intent = new Intent(action);
        intent.putExtra(extra, true);
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
        startForeground(Constants.NOTIFICATION_ID, mNotificationHandler.buildNotification());

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

        if (mBluetoothAdapter == null || mBluetoothDeviceAddress == null || mBluetoothDeviceAddress.isEmpty()) {
            Timber.w("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
        if (mBluetoothGatt != null && mBluetoothGatt.getDevice().getAddress().equals(mBluetoothDeviceAddress)) {
            Timber.d("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                broadcastUpdate(Constants.ACTION_BLUETOOTH_CONNECTING);
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
        Timber.d("Trying to create a new connection.");
//        mBluetoothDeviceAddress = address;
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
            Timber.w("BluetoothAdapter not initialized");
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
//    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
//                                              boolean enabled) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Timber.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
//    }


    public boolean writeBluetoothGattCharacteristic(byte[] cmd) {
        if (this.mBluetoothGatt == null) {
            return false;
        }

        if (WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE_KINGSONG) {

            BluetoothGattService service = this.mBluetoothGatt.getService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID));
            if (service == null) {
                Timber.i("writeBluetoothGattCharacteristic service == null");
                return false;
            }
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(Constants.KINGSONG_READ_CHARACTER_UUID));
            if (characteristic == null) {
                Timber.i("writeBluetoothGattCharacteristic characteristic == null");
                return false;
            }
            characteristic.setValue(cmd);
            Timber.i("writeBluetoothGattCharacteristic writeType = %d", characteristic.getWriteType());
            characteristic.setWriteType(1);
            return this.mBluetoothGatt.writeCharacteristic(characteristic);
        }
        return false;
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
