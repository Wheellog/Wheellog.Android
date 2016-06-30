package com.cooper.wheellog;

import java.util.UUID;

public class Constants {

    public static final String ACTION_BLUETOOTH_CONNECTED = "com.cooper.kingsongstats.bluetoothConnected";
    public static final String ACTION_BLUETOOTH_DISCONNECTED = "com.cooper.kingsongstats.bluetoothDisconnected";
    public static final String ACTION_BLUETOOTH_DATA_AVAILABLE = "com.cooper.kingsongstats.bluetoothDataAvailable";
    public static final String ACTION_WHEEL_DATA_AVAILABLE = "com.cooper.kingsongstats.wheelDataAvailable";
    public static final String ACTION_REQUEST_SERIAL_DATA = "com.cooper.kingsongstats.requestSerialData";

    public static final UUID PEBBLE_APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d");

    public static final String LAUNCHED_FROM_PEBBLE = "launched_from_pebble";

    public static final int REQUEST_SERIAL_DATA = 10;
}