package com.cooper.wheellog.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog

object ThemeManager {

    private val original = HashMap<ThemeIconEnum, Int>()
    private val ajdm = HashMap<ThemeIconEnum, Int>()

    var theme = WheelLog.AppConfig.appTheme

    fun getTypeface(context: Context): Typeface {
        val fontResId = when(theme) {
            R.style.AJDMTheme -> R.font.ajdm
            else -> R.font.prime
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.resources.getFont(fontResId)
        else
            ResourcesCompat.getFont(context, fontResId)!!
    }

    init {
        // notification
        original[ThemeIconEnum.NotificationIcon] = R.drawable.ic_stat_wheel
        original[ThemeIconEnum.NotificationConnecting] = R.drawable.ic_action_wheel_light_orange
        original[ThemeIconEnum.NotificationConnected] = R.drawable.ic_action_wheel_orange
        original[ThemeIconEnum.NotificationDisconnected] = R.drawable.ic_action_wheel_grey
        original[ThemeIconEnum.NotificationLogOn] = R.drawable.ic_action_logging_orange
        original[ThemeIconEnum.NotificationLogOff] = R.drawable.ic_action_logging_grey
        original[ThemeIconEnum.NotificationWatchOn] = R.drawable.ic_action_watch_orange
        original[ThemeIconEnum.NotificationWatchOff] = R.drawable.ic_action_watch_grey
        original[ThemeIconEnum.NotificationHorn] = R.drawable.ic_horn_32_gray
        original[ThemeIconEnum.NotificationLight] = R.drawable.ic_sun_32_gray
        ajdm[ThemeIconEnum.NotificationIcon] = R.drawable.wheel_ajdm_w
        ajdm[ThemeIconEnum.NotificationConnecting] = R.drawable.ajdm_wheel_new_g
        ajdm[ThemeIconEnum.NotificationConnected] = R.drawable.ajdm_wheel_new
        ajdm[ThemeIconEnum.NotificationDisconnected] = R.drawable.ajdm_wheel_new_b
        ajdm[ThemeIconEnum.NotificationLogOn] = R.drawable.ajdm_log_new
        ajdm[ThemeIconEnum.NotificationLogOff] = R.drawable.ajdm_log_new_b
        ajdm[ThemeIconEnum.NotificationWatchOn] = R.drawable.ajdm_watch_new
        ajdm[ThemeIconEnum.NotificationWatchOff] = R.drawable.ajdm_watch_new_b
        ajdm[ThemeIconEnum.NotificationHorn] = R.drawable.horn_ajdm_b
        ajdm[ThemeIconEnum.NotificationLight] = R.drawable.light_ajdm_b
        // menu
        original[ThemeIconEnum.MenuWatchOn] = R.drawable.ic_action_watch_orange
        original[ThemeIconEnum.MenuWatchOff] = R.drawable.ic_action_watch_white
        original[ThemeIconEnum.MenuMiBandAlarm] = R.drawable.ic_mi_alarm
        original[ThemeIconEnum.MenuMiBandMin] = R.drawable.ic_mi_min
        original[ThemeIconEnum.MenuMiBandMed] = R.drawable.ic_mi_med
        original[ThemeIconEnum.MenuMiBandMax] = R.drawable.ic_mi_max
        original[ThemeIconEnum.MenuLogOn] = R.drawable.ic_action_logging_orange
        original[ThemeIconEnum.MenuLogOff] = R.drawable.ic_action_logging_white
        original[ThemeIconEnum.MenuWheelOn] = R.drawable.ic_action_wheel_orange
        original[ThemeIconEnum.MenuWheelSearch] = R.drawable.anim_wheel_icon
        original[ThemeIconEnum.MenuWheelOff] = R.drawable.ic_action_wheel_white
        original[ThemeIconEnum.MenuSettings] = R.drawable.ic_baseline_settings_24
        original[ThemeIconEnum.MenuBluetooth] = R.drawable.ic_action_bluetooth_searching_white
        ajdm[ThemeIconEnum.MenuWatchOn] = R.drawable.ajdm_watch_g
        ajdm[ThemeIconEnum.MenuWatchOff] = R.drawable.ajdm_watch_new
        ajdm[ThemeIconEnum.MenuMiBandAlarm] = R.drawable.ajdm_ic_mi_alarm
        ajdm[ThemeIconEnum.MenuMiBandMin] = R.drawable.ajdm_ic_mi_min
        ajdm[ThemeIconEnum.MenuMiBandMed] = R.drawable.ajdm_ic_mi_med
        ajdm[ThemeIconEnum.MenuMiBandMax] = R.drawable.ajdm_ic_mi_max
        ajdm[ThemeIconEnum.MenuLogOn] = R.drawable.ajdm_log_new_g
        ajdm[ThemeIconEnum.MenuLogOff] = R.drawable.ajdm_log_new
        ajdm[ThemeIconEnum.MenuWheelOn] = R.drawable.ajdm_wheel_new_g
        ajdm[ThemeIconEnum.MenuWheelSearch] = R.drawable.ajdm_anim_wheel_icon
        ajdm[ThemeIconEnum.MenuWheelOff] = R.drawable.ajdm_wheel_new
        ajdm[ThemeIconEnum.MenuSettings] = R.drawable.ajdm_sett3_new
        ajdm[ThemeIconEnum.MenuBluetooth] = R.drawable.ajdm_bt_new
        // settings page
        original[ThemeIconEnum.SettingsSpeedometer] = R.drawable.ic_speedometer_white_24dp
        original[ThemeIconEnum.SettingsLog] = R.drawable.ic_show_chart_white_24dp
        original[ThemeIconEnum.SettingsWatch] = R.drawable.ic_baseline_watch_24
        original[ThemeIconEnum.SettingsBug] = R.drawable.ic_baseline_bug_report_24
        original[ThemeIconEnum.SettingsAbout] = R.drawable.ic_baseline_info_24
        original[ThemeIconEnum.SettingsVibration] = R.drawable.ic_baseline_vibration_24
        original[ThemeIconEnum.SettingsWheel] = R.drawable.ic_wheel_white_24
        original[ThemeIconEnum.SettingsTrips] = R.drawable.ic_baseline_explore_24
        original[ThemeIconEnum.SettingsDonate] = R.drawable.ic_donate_24
        original[ThemeIconEnum.SettingsPowerOff] = R.drawable.ic_baseline_power_off_24
        original[ThemeIconEnum.SettingsCalibration] = R.drawable.ic_baseline_calibration_24
        original[ThemeIconEnum.SettingsLanguage] = R.drawable.ic_baseline_translate_24
        original[ThemeIconEnum.SettingsAutoLog] = R.drawable.ic_baseline_magic_log_24
        original[ThemeIconEnum.SettingsLocation] = R.drawable.ic_baseline_location_on_24
        original[ThemeIconEnum.SettingsBlocks] = R.drawable.ic_baseline_dashboard_customize_24
        original[ThemeIconEnum.SettingsNotification] = R.drawable.ic_notification
        original[ThemeIconEnum.SettingsPageEvents] = R.drawable.ic_baseline_event_note_24
        original[ThemeIconEnum.SettingsPageTrips] = R.drawable.ic_baseline_road_with_two_placeholders_24
        original[ThemeIconEnum.SettingsConnectionSound] = R.drawable.ic_baseline_music_note_24
        original[ThemeIconEnum.SettingsAutoMute] = R.drawable.ic_baseline_volume_off_24
        ajdm[ThemeIconEnum.SettingsSpeedometer] = R.drawable.ajdm_sett5_new
        ajdm[ThemeIconEnum.SettingsLog] = R.drawable.ajdm_log_new_set
        ajdm[ThemeIconEnum.SettingsWatch] = R.drawable.ajdm_watch_new
        ajdm[ThemeIconEnum.SettingsBug] = R.drawable.ajdm_flow
        ajdm[ThemeIconEnum.SettingsAbout] = R.drawable.ajdm_info
        ajdm[ThemeIconEnum.SettingsVibration] = R.drawable.ajdm_alarm
        ajdm[ThemeIconEnum.SettingsWheel] = R.drawable.ajdm_wheel_set
        ajdm[ThemeIconEnum.SettingsTrips] = R.drawable.ajdm_trip
        // trip page
        original[ThemeIconEnum.TripsPopupButton] = R.drawable.ic_baseline_settings_24
        original[ThemeIconEnum.TripsMap] = R.drawable.ic_baseline_map_24
        original[ThemeIconEnum.TripsUpload] = R.drawable.ic_baseline_cloud_upload_24
        original[ThemeIconEnum.TripsShare] = R.drawable.ic_baseline_share_24
        original[ThemeIconEnum.TripsDelete] = R.drawable.ic_baseline_delete_24
        original[ThemeIconEnum.TripsOpenEc] = R.drawable.ic_baseline_open_in_new_24
        ajdm[ThemeIconEnum.TripsUpload] = R.drawable.ajdm_cloud
        ajdm[ThemeIconEnum.TripsShare] = R.drawable.ajdm_share
    }

    @DrawableRes
    fun getId(icon: ThemeIconEnum): Int {
        return when(theme) {
            R.style.AJDMTheme -> ajdm[icon]
            else -> original[icon]
        } ?: R.drawable.transparent
    }

    fun changeAppIcon(pkg: Context) {
        // change main launcher icon
        val pm = pkg.packageManager
        val original = ComponentName(pkg, "com.cooper.wheellog.OriginalIconAlias")
        val ajdm = ComponentName(pkg, "com.cooper.wheellog.AjdmIconAlias")
        if (WheelLog.AppConfig.appTheme == R.style.AJDMTheme) {
            pm.setComponentEnabledSetting(
                    ajdm,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(
                    original,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
        } else {
            pm.setComponentEnabledSetting(
                    ajdm,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(
                    original,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP)
        }
    }
}