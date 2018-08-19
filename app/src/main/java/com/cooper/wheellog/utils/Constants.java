package com.cooper.wheellog.utils;

import java.util.UUID;

public class Constants {

    public static final String ACTION_BLUETOOTH_CONNECTION_STATE = "com.cooper.wheellog.bluetoothConnectionState";
    public static final String ACTION_WHEEL_DATA_AVAILABLE = "com.cooper.wheelTimber.wheelDataAvailable";
	public static final String ACTION_WHEEL_SETTING_CHANGED = "com.cooper.wheelTimber.wheelSettingChanged";
    public static final String ACTION_REQUEST_KINGSONG_SERIAL_DATA = "com.cooper.wheellog.requestSerialData";
    public static final String ACTION_REQUEST_KINGSONG_NAME_DATA = "com.cooper.wheellog.requestNameData";
    public static final String ACTION_REQUEST_KINGSONG_HORN = "com.cooper.wheellog.requestHorn";
    public static final String ACTION_PEBBLE_SERVICE_TOGGLED = "com.cooper.wheellog.pebbleServiceToggled";
    public static final String ACTION_LOGGING_SERVICE_TOGGLED = "com.cooper.wheellog.loggingServiceToggled";
    public static final String ACTION_REQUEST_CONNECTION_TOGGLE = "com.cooper.wheellog.requestConnectionToggle";
    public static final String ACTION_PREFERENCE_CHANGED = "com.cooper.wheellog.preferenceChanged";
    public static final String ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED = "com.cooper.wheellog.pebblePreferenceChanged";
    public static final String ACTION_ALARM_TRIGGERED = "com.cooper.wheellog.alarmTriggered";
    public static final String ACTION_PEBBLE_APP_READY = "com.cooper.wheellog.pebbleAppReady";
    public static final String ACTION_PEBBLE_APP_SCREEN = "com.cooper.wheellog.pebbleAppScreen";
	public static final String ACTION_WHEEL_TYPE_RECOGNIZED = "com.cooper.wheellog.wheelTypeRecognized";

    static final String NOTIFICATION_BUTTON_CONNECTION = "com.cooper.wheellog.notificationConnectionButton";
    static final String NOTIFICATION_BUTTON_LOGGING = "com.cooper.wheellog.notificationLoggingButton";
    static final String NOTIFICATION_BUTTON_WATCH = "com.cooper.wheellog.notificationWatchButton";

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
	public static final String INTENT_EXTRA_WHEEL_LIGHT = "wheel_light";
	public static final String INTENT_EXTRA_WHEEL_LED = "wheel_led";
	public static final String INTENT_EXTRA_WHEEL_BUTTON = "wheel_button";
	public static final String INTENT_EXTRA_WHEEL_MAX_SPEED= "wheel_max_speed";
	public static final String INTENT_EXTRA_WHEEL_SPEAKER_VOLUME = "wheel_speaker_volume";
	public static final String INTENT_EXTRA_WHEEL_REFRESH = "wheel_refresh";
	public static final String INTENT_EXTRA_WHEEL_PEDALS_ADJUSTMENT = "pedals_adjustment";
	public static final String INTENT_EXTRA_WHEEL_TYPE = "wheel_type";

    public static final String PREFERENCES_FRAGMENT_TAG = "tagPrefs";

    public enum WHEEL_TYPE {
        Unknown,
        KINGSONG,
        GOTWAY,
        NINEBOT,
        NINEBOT_Z,
        INMOTION;
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
        SPEED(0),
        CURRENT(1),
		TEMPERATURE(2);
		

        private final int value;

        ALARM_TYPE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    public static final int MAIN_NOTIFICATION_ID = 10;

    public static final String LOG_FOLDER_NAME = "WheelLog Logs";
}