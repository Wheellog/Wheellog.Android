package com.cooper.wheellog.utils;

import java.util.UUID;

public class Constants {

    public static final String ACTION_BLUETOOTH_CONNECTION_STATE = "com.cooper.wheellog.bluetoothConnectionState";
    public static final String ACTION_WHEEL_DATA_AVAILABLE = "com.cooper.wheelTimber.wheelDataAvailable";
    public static final String ACTION_REQUEST_KINGSONG_SERIAL_DATA = "com.cooper.wheellog.requestSerialData";
    public static final String ACTION_REQUEST_KINGSONG_NAME_DATA = "com.cooper.wheellog.requestNameData";
    public static final String ACTION_REQUEST_KINGSONG_HORN = "com.cooper.wheellog.requestHorn";
    public static final String ACTION_PEBBLE_SERVICE_TOGGLED = "com.cooper.wheellog.pebbleServiceToggled";
    public static final String ACTION_LOGGING_SERVICE_TOGGLED = "com.cooper.wheellog.loggingServiceToggled";
    public static final String ACTION_REQUEST_CONNECTION_TOGGLE = "com.cooper.wheellog.requestConnectionToggle";
    public static final String ACTION_PREFERENCE_CHANGED = "com.cooper.wheellog.preferenceChanged";

    public static final String NOTIFICATION_BUTTON_CONNECTION = "com.cooper.wheellog.notificationConnectionButton";
    public static final String NOTIFICATION_BUTTON_LOGGING = "com.cooper.wheellog.notificationLoggingButton";
    public static final String NOTIFICATION_BUTTON_WATCH = "com.cooper.wheellog.notificationWatchButton";

    public static final String KINGSONG_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String KINGSONG_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String KINGSONG_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";

    public static final String GOTWAY_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String GOTWAY_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";

    public static final UUID PEBBLE_APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d");
    public static final int PEBBLE_KEY_LAUNCH_APP = 10008;
    public static final int PEBBLE_KEY_PLAY_HORN = 10009;
    public static final int PEBBLE_KEY_PLAY_HORN_MP3 = 10010;

    public static final String INTENT_EXTRA_LAUNCHED_FROM_PEBBLE = "launched_from_pebble";
    public static final String INTENT_EXTRA_BLE_AUTO_CONNECT = "ble_auto_connect";
    public static final String INTENT_EXTRA_LOGGING_FILE_LOCATION = "logging_file_location";
    public static final String INTENT_EXTRA_IS_RUNNING = "is_running";
    public static final String INTENT_EXTRA_GRAPH_UPDATE_AVILABLE = "graph_update_available";
    public static final String INTENT_EXTRA_CONNECTION_STATE = "connection_state";

    public enum WHEEL_TYPE {
        Unknown,
        KINGSONG,
        GOTWAY,
        NINEBOT
    }

    public static final int NOTIFICATION_ID = 1;
}