package com.cooper.wheellog

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.preference.PreferenceManager
import com.cooper.wheellog.utils.MiBandEnum
import com.cooper.wheellog.utils.NotificationUtil
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.VolumeKeyController
import com.wheellog.shared.Constants
import com.wheellog.shared.WearPage
import com.wheellog.shared.WearPages
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
// import com.yandex.metrica.YandexMetrica
import timber.log.Timber

class AppConfig(var context: Context): KoinComponent {
    private val notifications: NotificationUtil by inject()
    private val volumeKeyController: VolumeKeyController by inject()
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var specificPrefix: String = ""
    private val separator = ";"
    private val wd by lazy { WheelData.getInstance() }

    init {
        // Clear all preferences if they are incompatible
        val version = getValue("versionSettings", -1)
        val currentVer = 1
        if (version < currentVer && sharedPreferences.edit()?.clear()?.commit() == true) {
            setValue("versionSettings", currentVer)
            PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
        }
    }

    //region -=[ general settings ]=-    
    //region application
    var useEng: Boolean
        get() = getValue(R.string.use_eng, false)
        set(value) = setValue(R.string.use_eng, value)

    var appThemeInt: Int
        get() = getValue(R.string.app_theme, ThemeEnum.Original.value.toString()).toInt()
        set(value) = setValue(R.string.app_theme, value.toString())

    val appTheme: Int
        get() {
            val stringVal = getValue(R.string.app_theme, ThemeEnum.Original.value.toString())
            return when (ThemeEnum.fromInt(stringVal.toInt())) {
                ThemeEnum.AJDM -> R.style.AJDMTheme
                else -> R.style.OriginalTheme
            }
        }

    var dayNightThemeMode: Int
        get() = getValue(R.string.day_night_theme, MODE_NIGHT_UNSPECIFIED.toString()).toInt()
        set(value) = setValue(R.string.day_night_theme, value.toString())

    var useBetterPercents: Boolean
        get() = getValue(R.string.use_better_percents, false)
        set(value) = setValue(R.string.use_better_percents, value)

    var customPercents: Boolean
        get() = getValue(R.string.custom_percents, false)
        set(value) = setValue(R.string.custom_percents, value)

    var cellVoltageTiltback: Int
        get() = getSpecific(R.string.cell_voltage_tiltback, 330)
        set(value) = setSpecific(R.string.cell_voltage_tiltback, value)

    var useMph: Boolean
        get() = getValue(R.string.use_mph, false)
        set(value) = setValue(R.string.use_mph, value)

    var useFahrenheit: Boolean
        get() = getValue(R.string.use_fahrenheit, false)
        set(value) = setValue(R.string.use_fahrenheit, value)

   private var viewBlocksString: String?
        get() = getValue(R.string.view_blocks_string, null)
        set(value) = setValue(R.string.view_blocks_string, value)

   var viewBlocks: Array<String>
        get() = this.viewBlocksString?.split(separator)?.toTypedArray()
            ?: arrayOf(
                context.getString(R.string.pwm),
                context.getString(R.string.max_pwm),
                context.getString(R.string.voltage),
                context.getString(R.string.average_riding_speed),
                context.getString(R.string.riding_time),
                context.getString(R.string.top_speed),
                context.getString(R.string.distance),
                context.getString(R.string.total))
        set(value) { this.viewBlocksString = value.joinToString(separator) }

    var usePipMode: Boolean
        get() = getValue(R.string.use_pip_mode, true)
        set(value) = setValue(R.string.use_pip_mode, value)

    var pipBlock: String
        get() = getValue(R.string.pip_block, "")
        set(value) = setValue(R.string.pip_block, value)

    private var notificationButtonsString: String?
        get() = getValue(R.string.notification_buttons, null)
        set(value) {
            setValue(R.string.notification_buttons, value)
            notifications.update()
        }

    var notificationButtons: Array<String>
        get() = this.notificationButtonsString?.split(separator)?.toTypedArray()
            ?: arrayOf(
                context.getString(R.string.icon_connection),
                context.getString(R.string.icon_logging),
                context.getString(R.string.icon_watch))
        set(value) { this.notificationButtonsString = value.joinToString(separator) }

    var maxSpeed: Int
        get() = getValue(R.string.max_speed, 50)
        set(value) = setValue(R.string.max_speed, value)

    var valueOnDial: String
        get() = getValue(R.string.value_on_dial, "0")
        set(value) = setValue(R.string.value_on_dial, value)

    var pageGraph: Boolean
        get() = getValue(R.string.show_page_graph, true)
        set(value) = setValue(R.string.show_page_graph, value)

    var pageEvents: Boolean
        get() = getValue(R.string.show_page_events, false)
        set(value) = setValue(R.string.show_page_events, value)

    var pageTrips: Boolean
        get() = getValue(R.string.show_page_trips, true)
        set(value) = setValue(R.string.show_page_trips, value)

    var connectionSound: Boolean
        get() = getValue(R.string.connection_sound, false)
        set(value) = setValue(R.string.connection_sound, value)

    var noConnectionSound: Int
        get() = getValue(R.string.no_connection_sound, 5)
        set(value) = setValue(R.string.no_connection_sound, value)

    var useStopMusic: Boolean
        get() = getValue(R.string.use_stop_music, false)
        set(value) = setValue(R.string.use_stop_music, value)

    var showUnknownDevices: Boolean
        get() = getValue(R.string.show_unknown_devices, false)
        set(value) = setValue(R.string.show_unknown_devices, value)

    var useBeepOnSingleTap: Boolean
        get() = getValue(R.string.beep_on_single_tap, false)
        set(value) = setValue(R.string.beep_on_single_tap, value)

    var useBeepOnVolumeUp: Boolean
        get() = getValue(R.string.beep_on_volume_up, false)
        set(value) {
            setValue(R.string.beep_on_volume_up, value)
            volumeKeyController.setActive(wd.isConnected && value)
        }

    var beepByWheel: Boolean
        get() = getValue(R.string.beep_by_wheel, false)
        set(value) = setValue(R.string.beep_by_wheel, value)

    var useCustomBeep: Boolean
        get() = getValue(R.string.custom_beep, false)
        set(value) = setValue(R.string.custom_beep, value)

    var beepFile: Uri
        get() = Uri.parse(getValue(R.string.beep_file, ""))
        set(value) = setValue(R.string.beep_file, value.toString())

    var customBeepTimeLimit: Float
        get() = getValue("custom_beep_time_limit", 2.0f)
        set(value) = setValue("custom_beep_time_limit", value)

    var mibandMode: MiBandEnum
        get() = MiBandEnum.fromInt(getValue(R.string.miband_mode, MiBandEnum.Min.value))
        set(value) = setValue(R.string.miband_mode, value.value)

    var useReconnect: Boolean
        get() = getValue(R.string.use_reconnect, false)
        set(value) {
            setValue(R.string.use_reconnect, value)
            if (value)
                wd.bluetoothService?.startReconnectTimer()
            else
                wd.bluetoothService?.stopReconnectTimer()
        }

    var detectBatteryOptimization: Boolean
        get() = getValue(R.string.use_detect_battery_optimization, true)
        set(value) = setValue(R.string.use_detect_battery_optimization, value)

    var privatePolicyAccepted: Boolean
        get() = getValue(R.string.private_policy_accepted, false)
        set(value) = setValue(R.string.private_policy_accepted, value)

    var yandexMetricaAccepted: Boolean
        get() = getValue(R.string.yandex_metriсa_accepted, false)
        set(value) {
            setValue(R.string.yandex_metriсa_accepted, value)
//            YandexMetrica.setStatisticsSending(
//                context,
//                WheelLog.AppConfig.yandexMetricaAccepted
//            )
        }
    //endregion

    //region logs
    var autoLog: Boolean
        get() = getValue(R.string.auto_log, false)
        set(value) {
            setValue(R.string.auto_log, value)
        }

    var autoWatch: Boolean
        get() = getValue(R.string.auto_watch, false)
        set(value) = setValue(R.string.auto_watch, value)

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

    var ecGarage: String?
        get() = getSpecific(R.string.ec_garage, null)
        set(value) = setSpecific(R.string.ec_garage, value)

    var enableRawData: Boolean
        get() = getValue(R.string.use_raw_data, false)
        set(value) = setValue(R.string.use_raw_data, value)

    var startAutoLoggingWhenIsMovingMore: Float
        get() = getValue(R.string.auto_log_when_moving_more, 7f)
        set(value) = setValue(R.string.auto_log_when_moving_more, value)

    var continueThisDayLog: Boolean
        get() = getValue(R.string.continue_this_day_log, false)
        set(value) = setValue(R.string.continue_this_day_log, value)

    var continueThisDayLogMacException: String
        get() = getValue(R.string.continue_this_day_log_exception, "")
        set(value) = setValue(R.string.continue_this_day_log_exception, value)
    //endregion    
    
    //region watch
    var hornMode: Int
        get() = getValue(R.string.horn_mode, 0)
        set(value) = setValue(R.string.horn_mode, value)

    var garminConnectIqEnable: Boolean
        get() = getValue(R.string.garmin_connectiq_enable, false)
        set(value) = setValue(R.string.garmin_connectiq_enable, value)

    var useGarminBetaCompanion: Boolean
        get() = getValue(R.string.garmin_connectiq_use_beta, false)
        set(value) = setValue(R.string.garmin_connectiq_use_beta, value)

    var mainMenuButtons: Array<String>
        get() = getValue<String?>("main_menu_buttons", null)?.split(separator)?.toTypedArray()
            ?: arrayOf("watch")
        set(value) = setValue("main_menu_buttons", value.joinToString(separator))

    var showClock: Boolean
        get() = getValue("show_clock", true)
        set(value) = setValue("show_clock", value)

    var mibandFixRs: Boolean
        get() = getValue(R.string.miband_fixrs_enable, false)
        set(value) {
            setValue(R.string.miband_fixrs_enable, value)
            notifications.updateKostilTimer()
        }

    var wearOsPages: WearPages
        get() = WearPage.deserialize(
            getValue(Constants.wearPages,
                WearPage.serialize(WearPage.Main and WearPage.Voltage)))
        set(value) = setValue(Constants.wearPages, WearPage.serialize(value))
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

    var swapSpeedPwm
        get() = getValue(R.string.swap_speed_pwm, false)
        set(value) = setValue(R.string.swap_speed_pwm, value)

    var colorPwmStart: Int
        get() = getValue(R.string.color_pwm_start, 78)
        set(value) = setValue(R.string.color_pwm_start, value)

    var colorPwmEnd: Int
        get() = getValue(R.string.color_pwm_end, 92)
        set(value) = setValue(R.string.color_pwm_end, value)
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

    var useWheelBeepForAlarm: Boolean
        get() = getSpecific(R.string.use_wheel_beep_for_alarm, false)
        set(value) = setSpecific(R.string.use_wheel_beep_for_alarm, value)

    var pwmBasedAlarms: Boolean
        get() = getSpecific(R.string.altered_alarms, true)
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

    var rotationIsSet
        get() = getSpecific(R.string.rotation_set, false)
        set(value) = setSpecific(R.string.rotation_set, value)

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

    var alarmBattery
        get() = getSpecific(R.string.alarm_battery, 0)
        set(value) = setSpecific(R.string.alarm_battery, value)

    var alarmWheel: Boolean
        get() = getSpecific(R.string.alarm_wheel, false)
        set(value) = setSpecific(R.string.alarm_wheel, value)
    //endregion
    
    //region inmotion
    var ledEnabled: Boolean
        get() = getSpecific(R.string.led_enabled, false)
        set(value) = setSpecific(R.string.led_enabled, value)

    var drlEnabled: Boolean
        get() = getSpecific(R.string.drl_enabled, false)
        set(value) = setSpecific(R.string.drl_enabled, value)

    var taillightEnabled: Boolean
        get() = getSpecific(R.string.taillight_enabled, false)
        set(value) = setSpecific(R.string.taillight_enabled, value)

    var handleButtonDisabled: Boolean
        get() = getSpecific(R.string.handle_button_disabled, false)
        set(value) = setSpecific(R.string.handle_button_disabled, value)

    var speakerVolume: Int
        get() = getSpecific(R.string.speaker_volume, 0)
        set(value) = setSpecific(R.string.speaker_volume, value)

    var beeperVolume: Int
        get() = getSpecific(R.string.beeper_volume, 0)
        set(value) = setSpecific(R.string.beeper_volume, value)

    var pedalsAdjustment: Int
        get() = getSpecific(R.string.pedals_adjustment, 0)
        set(value) = setSpecific(R.string.pedals_adjustment, value)

    var pedalSensivity: Int
        get() = getSpecific(R.string.pedal_sensivity, 100)
        set(value) = setSpecific(R.string.pedal_sensivity, value)

    var rideMode: Boolean
        get() = getSpecific(R.string.ride_mode, false)
        set(value) = setSpecific(R.string.ride_mode, value)

    var lockMode: Boolean
        get() = getSpecific(R.string.lock_mode, false)
        set(value) = setSpecific(R.string.lock_mode, value)

    var transportMode: Boolean
        get() = getSpecific(R.string.transport_mode, false)
        set(value) = setSpecific(R.string.transport_mode, value)

    var goHomeMode: Boolean
        get() = getSpecific(R.string.go_home_mode, false)
        set(value) = setSpecific(R.string.go_home_mode, value)

    var fancierMode: Boolean
        get() = getSpecific(R.string.fancier_mode, false)
        set(value) = setSpecific(R.string.fancier_mode, value)

    var speakerMute: Boolean
        get() = getSpecific(R.string.speaker_mute, false)
        set(value) = setSpecific(R.string.speaker_mute, value)

    var fanQuietEnabled: Boolean
        get() = getSpecific(R.string.fan_quiet_enable, false)
        set(value) = setSpecific(R.string.fan_quiet_enable, value)

    var fanEnabled: Boolean
        get() = getSpecific(R.string.fan_enabled, false)
        set(value) = setSpecific(R.string.fan_enabled, value)

    var lightBrightness: Int
        get() = getSpecific(R.string.light_brightness, 0)
        set(value) = setSpecific(R.string.light_brightness, value)

    //endregion

    //region ninebotZ
    var wheelAlarm1Enabled: Boolean
        get() = getSpecific(R.string.wheel_alarm1_enabled, false)
        set(value) = setSpecific(R.string.wheel_alarm1_enabled, value)

    var wheelAlarm2Enabled: Boolean
        get() = getSpecific(R.string.wheel_alarm2_enabled, false)
        set(value) = setSpecific(R.string.wheel_alarm2_enabled, value)

    var wheelAlarm3Enabled: Boolean
        get() = getSpecific(R.string.wheel_alarm3_enabled, false)
        set(value) = setSpecific(R.string.wheel_alarm3_enabled, value)

    var wheelAlarm1Speed: Int
        get() = getSpecific(R.string.wheel_alarm1, 0)
        set(value) = setSpecific(R.string.wheel_alarm1, value)

    var wheelAlarm2Speed: Int
        get() = getSpecific(R.string.wheel_alarm2, 0)
        set(value) = setSpecific(R.string.wheel_alarm2, value)

    var wheelAlarm3Speed: Int
        get() = getSpecific(R.string.wheel_alarm3, 0)
        set(value) = setSpecific(R.string.wheel_alarm3, value)

    var wheelLimitedModeEnabled: Boolean
        get() = getSpecific(R.string.wheel_limited_mode_enabled, false)
        set(value) = setSpecific(R.string.wheel_limited_mode_enabled, value)

    var wheelLimitedModeSpeed: Int
        get() = getSpecific(R.string.wheel_limited_speed, 10)
        set(value) = setSpecific(R.string.wheel_limited_speed, value)

    var ledColor1: Int
        get() = getSpecific(R.string.nb_led_color1, 0)
        set(value) = setSpecific(R.string.nb_led_color1, value)

    var ledColor2: Int
        get() = getSpecific(R.string.nb_led_color2, 0)
        set(value) = setSpecific(R.string.nb_led_color2, value)

    var ledColor3: Int
        get() = getSpecific(R.string.nb_led_color3, 0)
        set(value) = setSpecific(R.string.nb_led_color3, value)

    var ledColor4: Int
        get() = getSpecific(R.string.nb_led_color4, 0)
        set(value) = setSpecific(R.string.nb_led_color4, value)

    var brakeAssistantEnabled: Boolean
        get() = getSpecific(R.string.brake_assistant_enabled, false)
        set(value) = setSpecific(R.string.brake_assistant_enabled, value)

    //end region

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

    var rollAngle: String // ListPreference only works with string parameters and writes them as string
        get() = getSpecific(R.string.roll_angle, "0")
        set(value) = setSpecific(R.string.roll_angle, value)

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

    var gwInMiles: Boolean
        get() = getSpecific(R.string.gw_in_miles, false)
        set(value) = setSpecific(R.string.gw_in_miles, value)

    var autoVoltage: Boolean
        get() = getSpecific(R.string.auto_voltage, true)
        set(value) = setSpecific(R.string.auto_voltage, value)

    var gotwayVoltage: String // ListPreference only works with string parameters
        get() = getSpecific(R.string.gotway_voltage, "1")
        set(value) = setSpecific(R.string.gotway_voltage, value)

    var gotwayNegative: String // ListPreference only works with string parameter
        get() = getSpecific(R.string.gotway_negative, "0")
        set(value) = setSpecific(R.string.gotway_negative, value)

    var hwPwm: Boolean
        get() = getSpecific(R.string.hw_pwm, false)
        set(value) = setSpecific(R.string.hw_pwm, value)

    var connectBeep: Boolean
        get() = getSpecific(R.string.connect_beep, true)
        set(value) = setSpecific(R.string.connect_beep, value)
    //endregion

    //region Alexovik
    var IsAlexovikFW: Boolean
        get() = getSpecific(R.string.is_alexovik_fw, false)
        set(value) = setSpecific(R.string.is_alexovik_fw, value)

    var extremeMode: Boolean
        get() = getSpecific(R.string.extreme_mode, false)
        set(value) = setSpecific(R.string.extreme_mode, value)

    var brakingCurrent: Int
        get() = getSpecific(R.string.braking_current, 80)
        set(value) = setSpecific(R.string.braking_current, value)

    var rotationControl: Boolean
        get() = getSpecific(R.string.rotation_control, false)
        set(value) = setSpecific(R.string.rotation_control, value)

    var rotationAngle: Int
        get() = getSpecific(R.string.rotation_angle, 350)
        set(value) = setSpecific(R.string.rotation_angle, value)

    var advancedSettings: Boolean
        get() = getSpecific(R.string.advanced_settings, false)
        set(value) = setSpecific(R.string.advanced_settings, value)

    var proportionalFactor: Int
        get() = getSpecific(R.string.proportional_factor, 20)
        set(value) = setSpecific(R.string.proportional_factor, value)

    var integralFactor: Int
        get() = getSpecific(R.string.integral_factor, 20)
        set(value) = setSpecific(R.string.integral_factor, value)

    var differentialFactor: Int
        get() = getSpecific(R.string.differential_factor, 40)
        set(value) = setSpecific(R.string.differential_factor, value)

    var dynamicCompensation: Int
        get() = getSpecific(R.string.dynamic_compensation, 0)
        set(value) = setSpecific(R.string.dynamic_compensation, value)

    var dynamicCompensationFilter: Int
        get() = getSpecific(R.string.dynamic_compensation_filter, 0)
        set(value) = setSpecific(R.string.dynamic_compensation_filter, value)

    var accelerationCompensation: Int
        get() = getSpecific(R.string.acceleration_compensation, 0)
        set(value) = setSpecific(R.string.acceleration_compensation, value)

    var proportionalCurrentFactorQ: Int
        get() = getSpecific(R.string.proportional_current_factor_q, 30)
        set(value) = setSpecific(R.string.proportional_current_factor_q, value)

    var integralCurrentFactorQ: Int
        get() = getSpecific(R.string.integral_current_factor_q, 50)
        set(value) = setSpecific(R.string.integral_current_factor_q, value)

    var proportionalCurrentFactorD: Int
        get() = getSpecific(R.string.proportional_current_factor_d, 20)
        set(value) = setSpecific(R.string.proportional_current_factor_d, value)

    var integralCurrentFactorD: Int
        get() = getSpecific(R.string.integral_current_factor_d, 40)
        set(value) = setSpecific(R.string.integral_current_factor_d, value)
    //endregion

    var lightEnabled: Boolean
        get() = getSpecific(R.string.light_enabled, false)
        set(value) = setSpecific(R.string.light_enabled, value)

    var profileName: String
        get() = getSpecific(R.string.profile_name, "")
        set(value) = setSpecific(R.string.profile_name, value)

    var batteryCapacity: Int
        get() = getSpecific(R.string.battery_capacity, 0)
        set(value) = setSpecific(R.string.battery_capacity, value)

    var chargingPower: Int
        get() = getSpecific(R.string.charging_power, 0)
        set(value) = setSpecific(R.string.charging_power, value)
    //endregion

    //region -=[ custom settings ]=-
    var passwordForWheel: String
        get() = getValue("wheel_password_$specificPrefix", "")
        set(value) {
            var password = value
            while (password.length < 6) {
                password = "0$password"
            }
            setValue("wheel_password_$specificPrefix", password)
        }

    var advDataForWheel: String
        get() = getValue("wheel_adv_data_$specificPrefix", "")
        set(value) = setValue("wheel_adv_data_$specificPrefix", value)

    var userDistance: Long
        get() = getValue("user_distance_$specificPrefix", 0L)
        set(value) = setValue("user_distance_$specificPrefix", value)
    //endregion

    var lastLocationLaltitude: Double
        get() = getValue("lastLocationLaltitude", 0.0)
        set(value) = setValue("lastLocationLaltitude", value)

    var lastLocationLongitude: Double
        get() = getValue("lastLocationLongitude", 0.0)
        set(value) = setValue("lastLocationLongitude", value)

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
            is Double -> sharedPreferences.edit().putFloat(key, value.toFloat()).apply()
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
                is Double -> sharedPreferences.getFloat(key, defaultValue.toFloat()).toDouble() as T
                is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
                is Long -> sharedPreferences.getLong(key, defaultValue) as T
                else -> defaultValue
            }
        } catch (ex: ClassCastException) {
            defaultValue
        }
    }
}