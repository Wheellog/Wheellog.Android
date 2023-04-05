package com.cooper.wheellog.utils

import java.util.*

object Constants {
    const val ACTION_BLUETOOTH_CONNECTION_STATE = "com.cooper.wheellog.bluetoothConnectionState"
    const val ACTION_WHEEL_TYPE_CHANGED = "com.cooper.wheellog.wheelTypeChanged"
    const val ACTION_WHEEL_DATA_AVAILABLE = "com.cooper.wheellog.wheelDataAvailable"
    const val ACTION_WHEEL_NEWS_AVAILABLE = "com.cooper.wheellog.wheelNews"
    const val ACTION_PEBBLE_SERVICE_TOGGLED = "com.cooper.wheellog.pebbleServiceToggled"
    const val ACTION_LOGGING_SERVICE_TOGGLED = "com.cooper.wheellog.loggingServiceToggled"
    const val ACTION_PREFERENCE_RESET = "com.cooper.wheellog.preferenceReset"
    const val ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED = "com.cooper.wheellog.pebblePreferenceChanged"
    const val ACTION_ALARM_TRIGGERED = "com.cooper.wheellog.alarmTriggered"
    const val ACTION_PEBBLE_APP_READY = "com.cooper.wheellog.pebbleAppReady"
    const val ACTION_PEBBLE_APP_SCREEN = "com.cooper.wheellog.pebbleAppScreen"
    const val ACTION_WHEEL_TYPE_RECOGNIZED = "com.cooper.wheellog.wheelTypeRecognized"
    const val ACTION_WHEEL_MODEL_CHANGED = "com.cooper.wheellog.wheelModelChanged"

    /**
     * The wheel has been successfully connected and all the necessary data for operation has already been received
     */
    const val ACTION_WHEEL_IS_READY = "com.cooper.wheellog.wheelIsReady"
    const val NOTIFICATION_BUTTON_CONNECTION = "com.cooper.wheellog.notificationConnectionButton"
    const val NOTIFICATION_BUTTON_LOGGING = "com.cooper.wheellog.notificationLoggingButton"
    const val NOTIFICATION_BUTTON_WATCH = "com.cooper.wheellog.notificationWatchButton"
    const val NOTIFICATION_BUTTON_BEEP = "com.cooper.wheellog.notificationBeepButton"
    const val NOTIFICATION_BUTTON_LIGHT = "com.cooper.wheellog.notificationLightButton"
    const val NOTIFICATION_BUTTON_MIBAND = "com.cooper.wheellog.notificationMiBandButton"
    const val NOTIFICATION_CHANNEL_ID_NOTIFICATION = "com.cooper.wheellog.Channel_Notification"
    const val notificationChannelName = "Notify"
    const val notificationChannelDescription = "Default Notify"
    @JvmField
    val KINGSONG_DESCRIPTER_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    @JvmField
    val KINGSONG_READ_CHARACTER_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    @JvmField
    val KINGSONG_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    @JvmField
    val GOTWAY_READ_CHARACTER_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    @JvmField
    val GOTWAY_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    @JvmField
    val INMOTION_DESCRIPTER_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    @JvmField
    val INMOTION_READ_CHARACTER_UUID = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb")
    @JvmField
    val INMOTION_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    val INMOTION_WRITE_CHARACTER_UUID = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb")
    val INMOTION_WRITE_SERVICE_UUID = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb")
    @JvmField
    val NINEBOT_Z_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val NINEBOT_Z_WRITE_CHARACTER_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    @JvmField
    val NINEBOT_Z_READ_CHARACTER_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    @JvmField
    val NINEBOT_Z_DESCRIPTER_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    @JvmField
    val INMOTION_V2_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val INMOTION_V2_WRITE_CHARACTER_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    @JvmField
    val INMOTION_V2_READ_CHARACTER_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    @JvmField
    val INMOTION_V2_DESCRIPTER_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    @JvmField
    val NINEBOT_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    val NINEBOT_WRITE_CHARACTER_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    @JvmField
    val NINEBOT_READ_CHARACTER_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    @JvmField
    val NINEBOT_DESCRIPTER_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    @JvmField
    val PEBBLE_APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d")
    const val PEBBLE_KEY_READY = 11
    const val PEBBLE_KEY_LAUNCH_APP = 10012
    const val PEBBLE_KEY_PLAY_HORN = 10013
    const val PEBBLE_KEY_DISPLAYED_SCREEN = 10014
    const val PEBBLE_APP_VERSION = 104
    const val INTENT_EXTRA_LAUNCHED_FROM_PEBBLE = "launched_from_pebble"
    const val INTENT_EXTRA_PEBBLE_APP_VERSION = "pebble_app_version"
    const val INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN = "pebble_displayed_Screen"
    const val INTENT_EXTRA_BLE_AUTO_CONNECT = "ble_auto_connect"
    const val INTENT_EXTRA_LOGGING_FILE_LOCATION = "logging_file_location"
    const val INTENT_EXTRA_IS_RUNNING = "is_running"
    const val INTENT_EXTRA_GRAPH_UPDATE_AVAILABLE = "graph_update_available"
    const val INTENT_EXTRA_CONNECTION_STATE = "connection_state"
    const val INTENT_EXTRA_WHEEL_SEARCH = "wheel_search"
    const val INTENT_EXTRA_DIRECT_SEARCH_FAILED = "direct_search_failed"
    const val INTENT_EXTRA_ALARM_TYPE = "alarm_type"
    const val INTENT_EXTRA_ALARM_VALUE = "alarm_value"
    const val INTENT_EXTRA_NEWS = "wheel_news"
    const val MAX_CELL_VOLTAGE = 4.2
    const val MAIN_NOTIFICATION_ID = 423411
    const val LOG_FOLDER_NAME = "WheelLog Logs"

    enum class WHEEL_TYPE {
        Unknown, KINGSONG, GOTWAY, NINEBOT, NINEBOT_Z, INMOTION, INMOTION_V2, VETERAN, GOTWAY_VIRTUAL
    }

    enum class PEBBLE_APP_SCREEN(val value: Int) {
        GUI(0),
        DETAILS(1);
    }

    enum class ALARM_TYPE(val value: Int) {
        SPEED1(1),
        SPEED2(2),
        SPEED3(3),
        CURRENT(4),
        TEMPERATURE(5),
        PWM(6),
        BATTERY(7);
    }
}