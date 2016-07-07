package com.cooper.wheellog;

import java.util.UUID;

public class Constants {

    public static final String ACTION_BLUETOOTH_CONNECTED = "com.cooper.wheellog.bluetoothConnected";
    public static final String ACTION_BLUETOOTH_DISCONNECTED = "com.cooper.wheellog.bluetoothDisconnected";
    public static final String ACTION_BLUETOOTH_DATA_AVAILABLE = "com.cooper.wheellog.bluetoothDataAvailable";
    public static final String ACTION_WHEEL_DATA_AVAILABLE = "com.cooper.wheellog.wheelDataAvailable";
    public static final String ACTION_REQUEST_SERIAL_DATA = "com.cooper.wheellog.requestSerialData";
//    public static final String ACTION_PEBBLE_SERVICE_STARTED = "com.cooper.wheellog.pebbleServiceStarted";
//    public static final String ACTION_LOGGING_SERVICE_STARTED = "com.cooper.wheellog.loggingServiceStarted";

    public static final UUID PEBBLE_APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d");
    public static final int PEBBLE_KEY_LAUNCH_APP = 10007;
    public static final int PEBBLE_KEY_PLAY_HORN = 10008;

    public static final String LAUNCHED_FROM_PEBBLE = "launched_from_pebble";

    public static final int REQUEST_SERIAL_DATA = 10;
}