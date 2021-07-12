package com.cooper.wheellog.preferences

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.presentation.preferences.SeekBarPreference
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.InMotionAdapter
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.VeteranAdapter


class WheelSettings(context: Context, ps: PreferenceScreen) : BaseSettingsClass(context, ps) {
    private var mac: String = ""
    private var speedMultipier: Double = 1.0
    private var speedUnit: String = ""

    private fun ninebotZ() {
        Preference(context).apply {
            key = mac + getString(R.string.no_settings)
            isEnabled = false
            summary = getString(R.string.ninebotz_settings_title)
            ps.addPreference(this)
        }
    }

    private fun inmotion() {
        SwitchPreference(context).apply {
            key = mac + getString(R.string.light_enabled)
            title = getString(R.string.on_headlight_title)
            summary = getString(R.string.on_headlight_description)
            isChecked = WheelLog.AppConfig.lightEnabled
            ps.addPreference(this)
        }
        if (InMotionAdapter.getInstance().ledThere) {
            SwitchPreference(context).apply {
                key = mac + getString(R.string.led_enabled)
                title = getString(R.string.leds_settings_title)
                summary = getString(R.string.leds_settings_description)
                isChecked = WheelLog.AppConfig.ledEnabled
                ps.addPreference(this)
            }
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.handle_button_disabled)
            title = getString(R.string.disable_handle_button_title)
            summary = getString(R.string.disable_handle_button_description)
            isChecked = WheelLog.AppConfig.handleButtonDisabled
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.wheel_max_speed)
            title = getString(R.string.max_speed_title)
            summary = getString(R.string.tilt_back_description)
            min = 3
            max = InMotionAdapter.getInstance().maxSpeed
            unit = speedUnit
            multiplier = speedMultipier
            increment = 1
            setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.speaker_volume)
            title = getString(R.string.speaker_volume_title)
            summary = getString(R.string.speaker_volume_description)
            min = 0
            max = 100
            unit = "%"
            increment = 1
            setDefaultValue(WheelLog.AppConfig.speakerVolume)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.pedals_adjustment)
            title = getString(R.string.pedal_horizont_title)
            summary = getString(R.string.pedal_horizont_description)
            min = -80
            max = 80
            unit = "°"
            increment = 1
            decimalPlaces = 1
            setDefaultValue(WheelLog.AppConfig.pedalsAdjustment)
            ps.addPreference(this)
        }
        if (InMotionAdapter.getInstance().wheelModesWheel) {
            SwitchPreference(context).apply {
                key = mac + getString(R.string.ride_mode)
                title = getString(R.string.ride_mode_title)
                summary = getString(R.string.ride_mode_description)
                isChecked = WheelLog.AppConfig.rideMode
                ps.addPreference(this)
            }
            SeekBarPreference(context).apply {
                key = mac + getString(R.string.pedal_sensivity)
                title = getString(R.string.pedal_sensivity_title)
                summary = getString(R.string.pedal_sensivity_description)
                min = 4
                max = 100
                unit = "%"
                increment = 1
                setDefaultValue(WheelLog.AppConfig.pedalSensivity)
                ps.addPreference(this)
            }
        }

        if (WheelData.getInstance().speed < 1) {
            Preference(context).apply {
                setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_power_off_24))
                title = getString(R.string.power_off)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    AlertDialog.Builder(context)
                            .setTitle(getString(R.string.power_off))
                            .setMessage(getString(R.string.power_off_message))
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                WheelData.getInstance().powerOff()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_power_off_24))
                            .show()
                    true
                }
                ps.addPreference(this)
            }
            Preference(context).apply {
                setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_calibration_24))
                title = getString(R.string.wheel_calibration)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    AlertDialog.Builder(context)
                            .setTitle(getString(R.string.wheel_calibration))
                            .setMessage(getString(R.string.wheel_calibration_message_inmo))
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                WheelData.getInstance().wheelCalibration()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_calibration_24))
                            .show()
                    true
                }
                ps.addPreference(this)
            }
        }
    }

    private fun inmotionV2() {
        SwitchPreference(context).apply {
            key = mac + getString(R.string.light_enabled)
            title = getString(R.string.on_headlight_title)
            summary = getString(R.string.on_headlight_description)
            isChecked = WheelLog.AppConfig.lightEnabled
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.drl_enabled)
            title = getString(R.string.drl_settings_title)
            summary = getString(R.string.drl_settings_description)
            isChecked = WheelLog.AppConfig.drlEnabled
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.light_brightness)
            title = getString(R.string.light_brightness_title)
            summary = getString(R.string.light_brightness_description)
            min = 0
            max = 100
            unit = "%"
            increment = 1
            setDefaultValue(WheelLog.AppConfig.lightBrightness)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.fan_enabled)
            title = getString(R.string.fan_title)
            summary = getString(R.string.fan_description)
            isChecked = WheelLog.AppConfig.fanEnabled
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.fan_quiet_enable)
            title = getString(R.string.fan_quiet_title)
            summary = getString(R.string.fan_quiet_description)
            isChecked = WheelLog.AppConfig.fanQuietEnabled
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.handle_button_disabled)
            title = getString(R.string.disable_handle_button_title)
            summary = getString(R.string.disable_handle_button_description)
            isChecked = WheelLog.AppConfig.handleButtonDisabled
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.speaker_volume)
            title = getString(R.string.speaker_volume_title)
            summary = getString(R.string.speaker_volume_description)
            min = 0
            max = 100
            unit = "%"
            increment = 1
            setDefaultValue(WheelLog.AppConfig.speakerVolume)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.speaker_mute)
            title = getString(R.string.speaker_mute_title)
            summary = getString(R.string.speaker_mute_description)
            isChecked = WheelLog.AppConfig.speakerMute
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.wheel_max_speed)
            title = getString(R.string.max_speed_title)
            summary = getString(R.string.tilt_back_description)
            min = 3
            max = 60
            unit = speedUnit
            multiplier = speedMultipier
            increment = 1
            setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.pedals_adjustment)
            title = getString(R.string.pedal_horizont_title)
            summary = getString(R.string.pedal_horizont_description)
            min = -100
            max = 100
            unit = "°"
            increment = 1
            decimalPlaces = 1
            setDefaultValue(WheelLog.AppConfig.pedalsAdjustment)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.ride_mode)
            title = getString(R.string.ride_mode_title)
            summary = getString(R.string.ride_mode_description)
            isChecked = WheelLog.AppConfig.rideMode
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.pedal_sensivity)
            title = getString(R.string.pedal_sensivity_title)
            summary = getString(R.string.pedal_sensivity_description)
            min = 0
            max = 100
            unit = "%"
            increment = 1
            setDefaultValue(WheelLog.AppConfig.pedalSensivity)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.fancier_mode)
            title = getString(R.string.fancier_mode_title)
            summary = getString(R.string.fancier_mode_description)
            isChecked = WheelLog.AppConfig.fancierMode
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.go_home_mode)
            title = getString(R.string.go_home_mode_title)
            summary = getString(R.string.go_home_mode_description)
            isChecked = WheelLog.AppConfig.goHomeMode
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.transport_mode)
            title = getString(R.string.transport_mode_title)
            summary = getString(R.string.transport_mode_description)
            isChecked = WheelLog.AppConfig.transportMode
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.lock_mode)
            title = getString(R.string.lock_mode_title)
            summary = getString(R.string.lock_mode_description)
            isChecked = WheelLog.AppConfig.lockMode
            ps.addPreference(this)
        }


        if (WheelData.getInstance().speed < 1) {
            Preference(context).apply {
                setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_power_off_24))
                title = getString(R.string.power_off)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    AlertDialog.Builder(context)
                            .setTitle(getString(R.string.power_off))
                            .setMessage(getString(R.string.power_off_message))
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                WheelData.getInstance().powerOff()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_power_off_24))
                            .show()
                    true
                }
                ps.addPreference(this)
            }
            Preference(context).apply {
                setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_calibration_24))
                title = getString(R.string.wheel_calibration)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    AlertDialog.Builder(context)
                            .setTitle(getString(R.string.wheel_calibration))
                            .setMessage(getString(R.string.wheel_calibration_message_inmo))
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                WheelData.getInstance().wheelCalibration()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_calibration_24))
                            .show()
                    true
                }
                ps.addPreference(this)
            }
        }
    }

    private fun kingsong() {
        ListPreference(context).apply {
            key = mac + getString(R.string.light_mode)
            title = getString(R.string.light_mode_title)
            summary = getString(R.string.on_off_auto)
            setEntries(R.array.light_mode_ks)
            setEntryValues(R.array.light_mode_values)
            setDefaultValue(WheelLog.AppConfig.lightMode)
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.strobe_mode)
            title = getString(R.string.strobe_mode_title)
            summary = getString(R.string.on_off)
            setEntries(R.array.strobe_mode_ks)
            setEntryValues(R.array.strobe_mode_values)
            setDefaultValue(WheelLog.AppConfig.strobeMode)
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.led_mode)
            title = getString(R.string.led_mode_title)
            summary = getString(R.string.on_off)
            setEntries(R.array.led_mode)
            setEntryValues(R.array.led_mode_values)
            setDefaultValue(WheelLog.AppConfig.ledMode)
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.pedals_mode)
            title = getString(R.string.pedals_mode_title)
            summary = getString(R.string.soft_medium_hard)
            setEntries(R.array.pedals_mode)
            setEntryValues(R.array.pedals_mode_values)
            setDefaultValue(WheelLog.AppConfig.pedalsMode)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.wheel_max_speed)
            title = getString(R.string.max_speed_title)
            summary = getString(R.string.tilt_back_description)
            increment = 1
            max = 50
            min = 0
            unit = speedUnit
            multiplier = speedMultipier
            setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.wheel_ks_alarm3)
            title = getString(R.string.alert3_title)
            summary = getString(R.string.alarm3_description)
            increment = 1
            max = 50
            min = 0
            unit = speedUnit
            multiplier = speedMultipier
            setDefaultValue(WheelLog.AppConfig.wheelKsAlarm3)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.wheel_ks_alarm2)
            title = getString(R.string.alert2_title)
            summary = getString(R.string.alarm2_description)
            increment = 1
            max = 50
            min = 0
            unit = speedUnit
            multiplier = speedMultipier
            setDefaultValue(WheelLog.AppConfig.wheelKsAlarm2)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.wheel_ks_alarm1)
            title = getString(R.string.alert1_title)
            summary = getString(R.string.alarm1_description)
            increment = 1
            max = 50
            min = 0
            unit = speedUnit
            multiplier = speedMultipier
            setDefaultValue(WheelLog.AppConfig.wheelKsAlarm1)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.ks18l_scaler)
            title = getString(R.string.ks18l_scaler_title)
            summary = getString(R.string.ks18l_scaler_description)
            ps.addPreference(this)
        }
        if (WheelData.getInstance().speed < 1) {
            Preference(context).apply {
                setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_calibration_24))
                title = getString(R.string.wheel_calibration)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    AlertDialog.Builder(context)
                            .setTitle(getString(R.string.wheel_calibration))
                            .setMessage(getString(R.string.wheel_calibration_message_kingsong))
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                WheelData.getInstance().wheelCalibration()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_calibration_24))
                            .show()
                    true
                }
                ps.addPreference(this)
            }
            Preference(context).apply {
                setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_power_off_24))
                title = getString(R.string.power_off)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    AlertDialog.Builder(context)
                            .setTitle(getString(R.string.power_off))
                            .setMessage(getString(R.string.power_off_message))
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                WheelData.getInstance().powerOff()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_power_off_24))
                            .show()
                    true
                }
                ps.addPreference(this)
            }
        }
    }

    private fun begode() {
        ListPreference(context).apply {
            key = mac + getString(R.string.light_mode)
            title = getString(R.string.light_mode_title)
            summary = getString(R.string.on_off_strobe)
            setEntries(R.array.light_mode_gw)
            setEntryValues(R.array.light_mode_values)
            setDefaultValue(WheelLog.AppConfig.lightMode)
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.alarm_mode)
            title = getString(R.string.alarm_mode_title)
            summary = getString(R.string.alarm_settings_title)
            setEntries(R.array.alarm_mode_gw)
            setEntryValues(R.array.alarm_mode_values)
            setDefaultValue(WheelLog.AppConfig.alarmMode)
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.pedals_mode)
            title = getString(R.string.pedals_mode_title)
            summary = getString(R.string.soft_medium_hard)
            setEntries(R.array.pedals_mode)
            setEntryValues(R.array.pedals_mode_values)
            setDefaultValue(WheelLog.AppConfig.pedalsMode)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.wheel_max_speed)
            title = getString(R.string.max_speed_title)
            summary = getString(R.string.tilt_back_description)
            max = 99
            min = 0
            increment = 1
            unit = speedUnit
            multiplier = speedMultipier
            setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
            ps.addPreference(this)
        }
        Preference(context).apply {
            setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_calibration_24))
            title = getString(R.string.wheel_calibration)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AlertDialog.Builder(context)
                        .setTitle(getString(R.string.wheel_calibration))
                        .setMessage(getString(R.string.wheel_calibration_message_begode))
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                            WheelData.getInstance().wheelCalibration()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_calibration_24))
                        .show()
                true
            }
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.use_ratio)
            title = getString(R.string.is_gotway_mcm_title)
            summary = getString(R.string.is_gotway_mcm_description)
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.gotway_voltage)
            title = getString(R.string.battery_voltage_title)
            summary = getString(R.string.battary_voltage_description)
            setEntries(R.array.gotway_voltage)
            setEntryValues(R.array.gotway_voltage_values)
            setDefaultValue(WheelLog.AppConfig.gotwayVoltage)
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.gotway_negative)
            title = getString(R.string.gotway_negative_title)
            summary = getString(R.string.gotway_negative_description)
            setEntries(R.array.gotway_negative)
            setEntryValues(R.array.gotway_negative_values)
            setDefaultValue(WheelLog.AppConfig.gotwayNegative)
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.connect_beep)
            title = getString(R.string.connect_beep_title)
            summary = getString(R.string.connect_beep_description)
            setDefaultValue(true)
            ps.addPreference(this)
        }
    }

    private fun veteran() {
        SwitchPreference(context).apply {
            key = mac + getString(R.string.light_enabled)
            title = getString(R.string.on_headlight_title)
            summary = getString(R.string.on_headlight_description)
            isChecked = WheelLog.AppConfig.lightEnabled
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.pedals_mode)
            title = getString(R.string.pedals_mode_title)
            summary = getString(R.string.soft_medium_hard)
            setEntries(R.array.pedals_mode)
            setEntryValues(R.array.pedals_mode_values)
            setDefaultValue(WheelLog.AppConfig.pedalsMode)
            ps.addPreference(this)
        }
        Preference(context).apply {
            title = getString(R.string.reset_trip)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AlertDialog.Builder(context)
                        .setTitle(getString(R.string.reset_trip))
                        .setMessage(getString(R.string.reset_trip_message))
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                            VeteranAdapter.getInstance().resetTrip()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                true
            }
            ps.addPreference(this)
        }
        SwitchPreference(context).apply {
            key = mac + getString(R.string.connect_beep)
            title = getString(R.string.connect_beep_title)
            summary = getString(R.string.connect_beep_description)
            setDefaultValue(true)
            ps.addPreference(this)
        }
        ListPreference(context).apply {
            key = mac + getString(R.string.gotway_negative)
            title = getString(R.string.gotway_negative_title)
            summary = getString(R.string.gotway_negative_description)
            setEntries(R.array.gotway_negative)
            setEntryValues(R.array.gotway_negative_values)
            setDefaultValue(WheelLog.AppConfig.gotwayNegative)
            ps.addPreference(this)
        }
    }

    private fun forAllWheel() {
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.battery_capacity)
            title = getString(R.string.battery_capacity_title)
            summary = getString(R.string.battery_capacity_description)
            increment = 1
            max = 9999
            min = 0
            unit = "Wh"
            setDefaultValue(0)
            ps.addPreference(this)
        }
        SeekBarPreference(context).apply {
            key = mac + getString(R.string.charging_power)
            title = getString(R.string.charging_power_title)
            summary = getString(R.string.charging_power_description)
            increment = 1
            max = 1000
            min = 0
            unit = "A"
            decimalPlaces = 1
            setDefaultValue(0)
            ps.addPreference(this)
        }
        Preference(context).apply {
            key = getString(R.string.profile_name)
            title = getString(R.string.profile_name_title)
            ps.addPreference(this)
        }
        Preference(context).apply {
            title = getString(R.string.current_mac)
            summary = mac.trimEnd('_')
            ps.addPreference(this)
        }
    }

    override fun fill(mac: String) {
        ps.removeAll()
        if (WheelLog.AppConfig.useMph) {
            speedMultipier = MathsUtil.kmToMilesMultiplier
            speedUnit = getString(R.string.mph)
        } else {
            speedMultipier = 1.0
            speedUnit = getString(R.string.kmh)
        }
        this.mac = mac
        when (WheelData.getInstance().wheelType) {
            Constants.WHEEL_TYPE.NINEBOT_Z -> ninebotZ()
            Constants.WHEEL_TYPE.INMOTION -> inmotion()
            Constants.WHEEL_TYPE.INMOTION_V2 -> inmotionV2()
            Constants.WHEEL_TYPE.KINGSONG -> kingsong()
            Constants.WHEEL_TYPE.GOTWAY -> begode()
            Constants.WHEEL_TYPE.VETERAN -> veteran()
            else -> ps.addPreference(Preference(context).apply {
                key = getString(R.string.no_settings)
                isEnabled = false
                summary = getString(R.string.unknown_device)
            })
        }
        if (WheelData.getInstance().wheelType != Constants.WHEEL_TYPE.Unknown) {
            forAllWheel()
        }
    }
}