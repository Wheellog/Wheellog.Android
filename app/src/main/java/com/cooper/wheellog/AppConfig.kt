package com.cooper.wheellog

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class AppConfig(var context: Context) {
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var specificPrefix: String = "default"

    init {
        // Clear all preferences if they are incompatible
        val version = getValue("versionSettings", -1)
        val currentVer = 1
        if (version < currentVer) {
            sharedPreferences.edit().clear().commit()
            setValue("versionSettings", currentVer)
            PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
        }
    }

    //region -=[ general settings ]=-    
    //region application
    var useEng: Boolean
        get() = getValue(R.string.use_eng, false)
        set(value) = setValue(R.string.use_eng, value)

    var useBetterPercents: Boolean
        get() = getValue(R.string.use_better_percents, false)
        set(value) = setValue(R.string.use_better_percents, value)

    var fixedPercents: Boolean
        get() = getValue(R.string.fixed_percents, false)
        set(value) = setValue(R.string.fixed_percents, value)

    var cellVoltageTiltback: Int
        get() = getValue(R.string.cell_voltage_tiltback, 330)
        set(value) = setValue(R.string.cell_voltage_tiltback, value)

    var useMph: Boolean
        get() = getValue(R.string.use_mph, false)
        set(value) = setValue(R.string.use_mph, value)

    var viewBlocksString: String?
        get() = getValue(R.string.view_blocks_string, null)
        set(value) = setValue(R.string.view_blocks_string, value)

    var maxSpeed: Int
        get() = getValue(R.string.max_speed, 30)
        set(value) = setValue(R.string.max_speed, value)

    var currentOnDial: Boolean
        get() = getValue(R.string.current_on_dial, false)
        set(value) = setValue(R.string.current_on_dial, value)

    var pageEvents: Boolean
        get() = getValue(R.string.show_page_events, false)
        set(value) = setValue(R.string.show_page_events, value)

    var connectionSound: Boolean
        get() = getValue(R.string.connection_sound, false)
        set(value) = setValue(R.string.connection_sound, value)

    var noConnectionSound: Int
        get() = getValue(R.string.no_connection_sound, 0)
        set(value) = setValue(R.string.no_connection_sound, value)

    var useStopMusic: Boolean
        get() = getValue(R.string.use_stop_music, false)
        set(value) = setValue(R.string.use_stop_music, value)

    var batteryCapacity: Int
        get() = getValue(R.string.battery_capacity, 0)
        set(value) = setValue(R.string.battery_capacity, value)

    var chargingPower: Int
        get() = getValue(R.string.charging_power, 0)
        set(value) = setValue(R.string.charging_power, value)

    var showUnknownDevices: Boolean
        get() = getValue(R.string.show_unknown_devices, false)
        set(value) = setValue(R.string.show_unknown_devices, value)

    var useBeepOnSingleTap: Boolean
        get() = getValue(R.string.beep_on_single_tap, false)
        set(value) = setValue(R.string.beep_on_single_tap, value)
    //endregion

    //region logs
    var autoLog: Boolean
        get() = getValue(R.string.auto_log, false)
        set(value) = setValue(R.string.auto_log, value)

    var autoUploadEc: Boolean
        get() = getValue(R.string.auto_upload_ec, false)
        set(value) = setValue(R.string.auto_upload_ec, value)

    var logLocationData: Boolean
        get() = getValue(R.string.log_location_data, false)
        set(value) = setValue(R.string.log_location_data, value)

    var ecUserId: String?
        get() = getValue(R.string.ec_user_id, null)
        set(value) = setValue(R.string.ec_user_id, value)

    var ecToken: String?
        get() = getValue(R.string.ec_token, null)
        set(value) = setValue(R.string.ec_token, value)
    //endregion    
    
    //region watch
    var hornMode: Int
        get() = getValue(R.string.horn_mode, 0)
        set(value) = setValue(R.string.horn_mode, value)

    var garminConnectIqEnable: Boolean
        get() = getValue(R.string.garmin_connectiq_enable, false)
        set(value) = setValue(R.string.garmin_connectiq_enable, value)
    //endregion

    var lastMac: String
        get() {
            specificPrefix = getValue(R.string.last_mac, "")
            return specificPrefix
        }
        set(value) {
            specificPrefix = value
            setValue(R.string.last_mac, value)
        }

    var useGps: Boolean
        get() = getValue(R.string.use_gps, false)
        set(value) = setValue(R.string.use_gps, value)

    var useShortPwm
        get() = getValue(R.string.use_short_pwm, false)
        set(value) = setValue(R.string.use_short_pwm, value)
    //endregion

    //region -=[ specific settings ]=-
    //region alarms
    var alarmsEnabled: Boolean
        get() = getSpecific(R.string.alarms_enabled, false)
        set(value) = setSpecific(R.string.alarms_enabled, value)

    var disablePhoneVibrate: Boolean
        get() = getSpecific(R.string.disable_phone_vibrate, false)
        set(value) = setSpecific(R.string.disable_phone_vibrate, value)

    var disablePhoneBeep: Boolean
        get() = getSpecific(R.string.disable_phone_beep, false)
        set(value) = setSpecific(R.string.disable_phone_beep, value)

    var alteredAlarms: Boolean
        get() = getSpecific(R.string.altered_alarms, false)
        set(value) = setSpecific(R.string.altered_alarms, value)

    var alarm1Speed: Int
        get() = getSpecific(R.string.alarm_1_speed, 29)
        set(value) = setSpecific(R.string.alarm_1_speed, value)

    var alarm1Battery: Int
        get() = getSpecific(R.string.alarm_1_battery, 100)
        set(value) = setSpecific(R.string.alarm_1_battery, value)

    var alarm2Speed: Int
        get() = getSpecific(R.string.alarm_2_speed, 0)
        set(value) = setSpecific(R.string.alarm_2_speed, value)

    var alarm2Battery: Int
        get() = getSpecific(R.string.alarm_2_battery, 0)
        set(value) = setSpecific(R.string.alarm_2_battery, value)

    var alarm3Speed: Int
        get() = getSpecific(R.string.alarm_3_speed, 0)
        set(value) = setSpecific(R.string.alarm_3_speed, value)

    var alarm3Battery: Int
        get() = getSpecific(R.string.alarm_3_battery, 0)
        set(value) = setSpecific(R.string.alarm_3_battery, value)

    var alarmTemperature
        get() = getSpecific(R.string.alarm_temperature, 0)
        set(value) = setSpecific(R.string.alarm_temperature, value)

    var rotationSpeed
        get() = getSpecific(R.string.rotation_speed, 500)
        set(value) = setSpecific(R.string.rotation_speed, value)

    var rotationVoltage
        get() = getSpecific(R.string.rotation_voltage, 840)
        set(value) = setSpecific(R.string.rotation_voltage, value)

    var powerFactor
        get() = getSpecific(R.string.power_factor, 90)
        set(value) = setSpecific(R.string.power_factor, value)

    var alarmFactor1
        get() = getSpecific(R.string.alarm_factor1, 80)
        set(value) = setSpecific(R.string.alarm_factor1, value)

    var alarmFactor2
        get() = getSpecific(R.string.alarm_factor2, 90)
        set(value) = setSpecific(R.string.alarm_factor2, value)

    var alarmFactor3
        get() = getSpecific(R.string.alarm_factor3, 95)
        set(value) = setSpecific(R.string.alarm_factor3, value)

    var warningSpeed
        get() = getSpecific(R.string.warning_speed, 0)
        set(value) = setSpecific(R.string.warning_speed, value)

    var warningPwm
        get() = getSpecific(R.string.warning_pwm, 0)
        set(value) = setSpecific(R.string.warning_pwm, value)

    var warningSpeedPeriod
        get() = getSpecific(R.string.warning_speed_period, 0)
        set(value) = setSpecific(R.string.warning_speed_period, value)

    var alarmCurrent
        get() = getSpecific(R.string.alarm_current, 0)
        set(value) = setSpecific(R.string.alarm_current, value)
    //endregion
    
    //region inmotion
    var ledEnabled: Boolean
        get() = getSpecific(R.string.led_enabled, false)
        set(value) = setSpecific(R.string.led_enabled, value)

    var handleButtonDisabled: Boolean
        get() = getSpecific(R.string.handle_button_disabled, false)
        set(value) = setSpecific(R.string.handle_button_disabled, value)

    var speakerVolume: Int
        get() = getSpecific(R.string.speaker_volume, 0)
        set(value) = setSpecific(R.string.speaker_volume, value)

    var pedalsAdjustment: Int
        get() = getSpecific(R.string.pedals_adjustment, 0)
        set(value) = setSpecific(R.string.pedals_adjustment, value)
    //endregion
    
    //region kingsong
    var lightMode: String // ListPreference only works with string parameters and writes them as string
        get() = getSpecific(R.string.light_mode, "0")
        set(value) = setSpecific(R.string.light_mode, value)

    var strobeMode: String // ListPreference only works with string parameters and writes them as string
        get() = getSpecific(R.string.strobe_mode, "0")
        set(value) = setSpecific(R.string.strobe_mode, value)

    var ledMode: String // ListPreference only works with string parameters and writes them as string
        get() = getSpecific(R.string.led_mode, "0")
        set(value) = setSpecific(R.string.led_mode, value)

    var pedalsMode: String // ListPreference only works with string parameters and writes them as string
        get() = getSpecific(R.string.pedals_mode, "0")
        set(value) = setSpecific(R.string.pedals_mode, value)

    var wheelMaxSpeed: Int
        get() = getSpecific(R.string.wheel_max_speed, 0)
        set(value) = setSpecific(R.string.wheel_max_speed, value)

    var wheelKsAlarm1: Int
        get() = getSpecific(R.string.wheel_ks_alarm1, 0)
        set(value) = setSpecific(R.string.wheel_ks_alarm1, value)

    var wheelKsAlarm2: Int
        get() = getSpecific(R.string.wheel_ks_alarm2, 0)
        set(value) = setSpecific(R.string.wheel_ks_alarm2, value)

    var wheelKsAlarm3: Int
        get() = getSpecific(R.string.wheel_ks_alarm3, 0)
        set(value) = setSpecific(R.string.wheel_ks_alarm3, value)

    var ks18LScaler: Boolean
        get() = getSpecific(R.string.ks18l_scaler, false)
        set(value) = setSpecific(R.string.ks18l_scaler, value)
    //endregion
    
    //region begode
    var alarmMode: String // ListPreference only works with string parameters
        get() = getSpecific(R.string.alarm_mode, "0")
        set(value) = setSpecific(R.string.alarm_mode, value)
    
    var useRatio: Boolean
        get() = getSpecific(R.string.use_ratio, false)
        set(value) = setSpecific(R.string.use_ratio, value)

    var gotwayVoltage: String // ListPreference only works with string parameters
        get() = getSpecific(R.string.gotway_voltage, "1")
        set(value) = setSpecific(R.string.gotway_voltage, value)

    var gotwayNegative: String // ListPreference only works with string parameter
        get() = getSpecific(R.string.gotway_negative, "0")
        set(value) = setSpecific(R.string.gotway_negative, value)

    var connectBeep: Boolean
        get() = getSpecific(R.string.connect_beep, true)
        set(value) = setSpecific(R.string.connect_beep, value)
    //endregion

    var lightEnabled: Boolean
        get() = getSpecific(R.string.light_enabled, false)
        set(value) = setSpecific(R.string.light_enabled, value)

    var profileName: String
        get() = getSpecific(R.string.profile_name, "")
        set(value) = setSpecific(R.string.profile_name, value)
    //endregion

    //region -=[ custom settings ]=-
    var passwordForWheel: String
        get() = getValue("wheel_password_" + specificPrefix, "")
        set(value) {
            var password = value
            while (password.length < 6) {
                password = "0$password"
            }
            setValue("wheel_password_" + specificPrefix, password)
        }

    var advDataForWheel: String
        get() = getValue("wheel_adv_data_" + specificPrefix, "")
        set(value) = setValue("wheel_adv_data_" + specificPrefix, value)

    var userDistance: Long
        get() = getValue("user_distance_" + specificPrefix, 0L)
        set(value) = setValue("user_distance_" + specificPrefix, value)
    //endregion

    fun getResId(resName: String?): Int {
        return if (resName == null || resName === "") {
            -1
        } else try {
            context.resources.getIdentifier(resName, "string", context.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun setSpecific(resId: Int, value: Any?) {
        setValue(specificPrefix + "_" + context.getString(resId), value)
    }

    fun setValue(resId: Int, value: Any?) {
        setValue(context.getString(resId), value)
    }

    fun setValue(key: String, value: Any?) {
        when (value) {
            is String? -> sharedPreferences.edit().putString(key, value).apply()
            is String -> sharedPreferences.edit().putString(key, value).apply()
            is Int -> sharedPreferences.edit().putInt(key, value).apply()
            is Float -> sharedPreferences.edit().putFloat(key, value).apply()
            is Boolean -> sharedPreferences.edit().putBoolean(key, value).apply()
            is Long -> sharedPreferences.edit().putLong(key, value).apply()
        }
    }

    private fun <T : Any?> getSpecific(resId: Int, defaultValue: T): T {
        return getValue(specificPrefix + "_" + context.getString(resId), defaultValue)
    }

    fun <T : Any?> getValue(resId: Int, defaultValue: T): T {
        return getValue(context.getString(resId), defaultValue)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any?> getValue(key: String, defaultValue: T): T {
        return try {
            when (defaultValue) {
                is String? -> sharedPreferences.getString(key, defaultValue) as T
                is String -> sharedPreferences.getString(key, defaultValue) as T
                is Int -> sharedPreferences.getInt(key, defaultValue) as T
                is Float -> sharedPreferences.getFloat(key, defaultValue) as T
                is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
                is Long -> sharedPreferences.getLong(key, defaultValue) as T
                else -> defaultValue
            }
        } catch (ex: ClassCastException) {
            defaultValue
        }
    }
}