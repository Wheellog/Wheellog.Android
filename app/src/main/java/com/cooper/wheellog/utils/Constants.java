package com.cooper.wheellog.utils;

import java.util.UUID;

public class Constants {

    public static final String ACTION_BLUETOOTH_CONNECTION_STATE = "com.jastrebov.wldmod.bluetoothConnectionState";
    public static final String ACTION_WHEEL_DATA_AVAILABLE = "com.jastrebov.wheelTimber.wheelDataAvailable";
	public static final String ACTION_WHEEL_SETTING_CHANGED = "com.jastrebov.wheelTimber.wheelSettingChanged";
    public static final String ACTION_REQUEST_KINGSONG_SERIAL_DATA = "com.jastrebov.wldmod.requestSerialData";
    public static final String ACTION_REQUEST_KINGSONG_NAME_DATA = "com.jastrebov.wldmod.requestNameData";
    public static final String ACTION_REQUEST_KINGSONG_HORN = "com.jastrebov.wldmod.requestHorn";
    public static final String ACTION_PEBBLE_SERVICE_TOGGLED = "com.jastrebov.wldmod.pebbleServiceToggled";
    public static final String ACTION_LOGGING_SERVICE_TOGGLED = "com.jastrebov.wldmod.loggingServiceToggled";
    public static final String ACTION_REQUEST_CONNECTION_TOGGLE = "com.jastrebov.wldmod.requestConnectionToggle";
    public static final String ACTION_REQUEST_RECONNECT = "com.jastrebov.wldmod.requestReconnectToggle";
    public static final String ACTION_PREFERENCE_CHANGED = "com.jastrebov.wldmod.preferenceChanged";
    public static final String ACTION_PREFERENCE_RESET = "com.jastrebov.wldmod.preferenceReset";
    public static final String ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED = "com.jastrebov.wldmod.pebblePreferenceChanged";
    public static final String ACTION_ALARM_TRIGGERED = "com.jastrebov.wldmod.alarmTriggered";
    public static final String ACTION_PEBBLE_APP_READY = "com.jastrebov.wldmod.pebbleAppReady";
    public static final String ACTION_PEBBLE_APP_SCREEN = "com.jastrebov.wldmod.pebbleAppScreen";
	public static final String ACTION_WHEEL_TYPE_RECOGNIZED = "com.jastrebov.wldmod.wheelTypeRecognized";
    public static final String ACTION_REQUEST_SWMIBAND = "com.jastrebov.wldmod.request_swmiband";
    public static final String ACTION_WHEEL_TYPE_CHANGED = "com.jastrebov.wldmod.wheelTypeChanged";

    static final String NOTIFICATION_BUTTON_CONNECTION = "com.cooper.wheellog.notificationConnectionButton";
    static final String NOTIFICATION_BUTTON_LOGGING = "com.cooper.wheellog.notificationLoggingButton";
    static final String NOTIFICATION_BUTTON_WATCH = "com.cooper.wheellog.notificationWatchButton";

    public static final String NOTIFICATION_CHANNEL_ID_NOTIFICATION = "com.cooper.wheellog.Channel_Notification";

    public static final String KINGSONG_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String KINGSONG_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String KINGSONG_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";

    public static final String GOTWAY_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String GOTWAY_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";

    public static final String INMOTION_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String INMOTION_READ_CHARACTER_UUID = "0000ffe4-0000-1000-8000-00805f9b34fb";
    public static final String INMOTION_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String INMOTION_WRITE_CHARACTER_UUID = "0000ffe9-0000-1000-8000-00805f9b34fb";
    public static final String INMOTION_WRITE_SERVICE_UUID = "0000ffe5-0000-1000-8000-00805f9b34fb";

    public static final String NINEBOT_Z_SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NINEBOT_Z_WRITE_CHARACTER_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NINEBOT_Z_READ_CHARACTER_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NINEBOT_Z_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String INMOTION_V2_SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String INMOTION_V2_WRITE_CHARACTER_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String INMOTION_V2_READ_CHARACTER_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String INMOTION_V2_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String NINEBOT_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String NINEBOT_WRITE_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String NINEBOT_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String NINEBOT_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";


    public static final UUID PEBBLE_APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d");
    public static final int PEBBLE_KEY_READY = 11;
    public static final int PEBBLE_KEY_LAUNCH_APP = 10012;
    public static final int PEBBLE_KEY_PLAY_HORN = 10013;
    public static final int PEBBLE_KEY_DISPLAYED_SCREEN = 10014;
    public static final int PEBBLE_APP_VERSION = 104;

    public static final String INTENT_EXTRA_LAUNCHED_FROM_PEBBLE = "launched_from_pebble";
    public static final String INTENT_EXTRA_PEBBLE_APP_VERSION = "pebble_app_version";
    public static final String INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN = "pebble_displayed_Screen";
    public static final String INTENT_EXTRA_BLE_AUTO_CONNECT = "ble_auto_connect";
    public static final String INTENT_EXTRA_LOGGING_FILE_LOCATION = "logging_file_location";
    public static final String INTENT_EXTRA_IS_RUNNING = "is_running";
    public static final String INTENT_EXTRA_GRAPH_UPDATE_AVILABLE = "graph_update_available";
    public static final String INTENT_EXTRA_CONNECTION_STATE = "connection_state";
    public static final String INTENT_EXTRA_ALARM_TYPE = "alarm_type";
    public static final String INTENT_EXTRA_WHEEL_SETTINGS = "wheel_settings";
    public static final String INTENT_EXTRA_DATA_TO_LOGS = "wheel_data_to_logs";
    public static final String INTENT_EXTRA_SETTINGS_KEY = "settings_key";
    public static final String PREFERENCES_FRAGMENT_TAG = "tagPrefs";

    public static final double MAX_CELL_VOLTAGE = 4.2;

    public enum WHEEL_TYPE {
        Unknown,
        KINGSONG,
        GOTWAY,
        NINEBOT,
        NINEBOT_Z,
        INMOTION,
        INMOTION_V2,
        VETERAN,
        GOTWAY_VIRTUAL
    }

    public enum PEBBLE_APP_SCREEN {
        GUI(0),
        DETAILS(1);

        private final int value;

        PEBBLE_APP_SCREEN(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    public enum ALARM_TYPE {
        //SPEED(0),
        CURRENT(4),
        TEMPERATURE(5),
        SPEED1(1),
        SPEED2(2),
        SPEED3(3);
		

        private final int value;

        ALARM_TYPE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    public static final int MAIN_NOTIFICATION_ID = 423411;

    public static final String LOG_FOLDER_NAME = "WheelLog AJDM Logs";
}
