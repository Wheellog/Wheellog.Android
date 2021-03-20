package com.cooper.wheellog.preferences

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.presentation.preferences.SeekBarPreference
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.InMotionAdapter
import com.cooper.wheellog.utils.VeteranAdapter

class WheelSettings(context: Context) : BaseSettingsClass(context) {
    private var mac: String = ""

    private fun preferenceNinebotZ(): List<Preference> {
        return listOf(
                Preference(context).apply {
                    key = mac + getString(R.string.no_settings)
                    isEnabled = false
                    summary = getString(R.string.ninebotz_settings_title)
                }
        )
    }

    private fun preferenceInmotion(): List<Preference> {
        val prefs: MutableList<Preference> = arrayListOf()
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.light_enabled)
                    title = getString(R.string.on_headlight_title)
                    summary = getString(R.string.on_headlight_description)
                    isChecked = WheelLog.AppConfig.lightEnabled
                }
        )
        if (InMotionAdapter.getInstance().ledThere) {
            prefs.add(
                    SwitchPreference(context).apply {
                        key = mac + getString(R.string.led_enabled)
                        title = getString(R.string.leds_settings_title)
                        summary = getString(R.string.leds_settings_description)
                        isChecked = WheelLog.AppConfig.ledEnabled
                    },
            )
        }
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.handle_button_disabled)
                    title = getString(R.string.disable_handle_button_title)
                    summary = getString(R.string.disable_handle_button_description)
                    isChecked = WheelLog.AppConfig.handleButtonDisabled
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_max_speed)
                    title = getString(R.string.max_speed_title)
                    summary = getString(R.string.tilt_back_description)
                    min = 3
                    max = InMotionAdapter.getInstance().maxSpeed
                    unit = getString(R.string.kmh)
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.speaker_volume)
                    title = getString(R.string.speaker_volume_title)
                    summary = getString(R.string.speaker_volume_description)
                    min = 0
                    max = 100
                    unit = "%"
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.speakerVolume)
                }
        )
        prefs.add(
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
                }
        )
        if (InMotionAdapter.getInstance().wheelModesWheel) {
            prefs.add(
                    SwitchPreference(context).apply {
                        key = mac + getString(R.string.ride_mode)
                        title = getString(R.string.ride_mode_title)
                        summary = getString(R.string.ride_mode_description)
                        isChecked = WheelLog.AppConfig.rideMode
                    }
            )
            prefs.add(
                    SeekBarPreference(context).apply {
                        key = mac + getString(R.string.pedal_sensivity)
                        title = getString(R.string.pedal_sensivity_title)
                        summary = getString(R.string.pedal_sensivity_description)
                        min = 4
                        max = 100
                        unit = "%"
                        increment = 1
                        setDefaultValue(WheelLog.AppConfig.pedalSensivity)
                    }
            )
        }

        if (WheelData.getInstance().speed < 1) {
            prefs.add(
                    Preference(context).apply {
                        setIcon(R.drawable.ic_baseline_power_off_24)
                        title = getString(R.string.power_off)
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.power_off))
                                    .setMessage(getString(R.string.power_off_message))
                                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                        WheelData.getInstance().powerOff()
                                    }
                                    .setNegativeButton(android.R.string.no, null)
                                    .setIcon(R.drawable.ic_baseline_power_off_24)
                                    .show()
                            true
                        }
                    }
            )
            prefs.add(
                    Preference(context).apply {
                        setIcon(R.drawable.ic_baseline_calibration_24)
                        title = getString(R.string.wheel_calibration)
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.wheel_calibration))
                                    .setMessage(getString(R.string.wheel_calibration_message_inmo))
                                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                        WheelData.getInstance().wheelCalibration()
                                    }
                                    .setNegativeButton(android.R.string.no, null)
                                    .setIcon(R.drawable.ic_baseline_calibration_24)
                                    .show()
                            true
                        }
                    }
            )
        }
        return prefs
    }

    private fun preferenceInmotionV2(): List<Preference> {
        val prefs: MutableList<Preference> = arrayListOf()
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.light_enabled)
                    title = getString(R.string.on_headlight_title)
                    summary = getString(R.string.on_headlight_description)
                    isChecked = WheelLog.AppConfig.lightEnabled
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.drl_enabled)
                    title = getString(R.string.drl_settings_title)
                    summary = getString(R.string.drl_settings_description)
                    isChecked = WheelLog.AppConfig.drlEnabled
                },
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.light_brightness)
                    title = getString(R.string.light_brightness_title)
                    summary = getString(R.string.light_brightness_description)
                    min = 0
                    max = 100
                    unit = "%"
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.lightBrightness)
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.fan_enabled)
                    title = getString(R.string.fan_title)
                    summary = getString(R.string.fan_description)
                    isChecked = WheelLog.AppConfig.fanEnabled
                },
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.fan_quiet_enable)
                    title = getString(R.string.fan_quiet_title)
                    summary = getString(R.string.fan_quiet_description)
                    isChecked = WheelLog.AppConfig.fanQuietEnabled
                },
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.handle_button_disabled)
                    title = getString(R.string.disable_handle_button_title)
                    summary = getString(R.string.disable_handle_button_description)
                    isChecked = WheelLog.AppConfig.handleButtonDisabled
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.speaker_volume)
                    title = getString(R.string.speaker_volume_title)
                    summary = getString(R.string.speaker_volume_description)
                    min = 0
                    max = 100
                    unit = "%"
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.speakerVolume)
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.speaker_mute)
                    title = getString(R.string.speaker_mute_title)
                    summary = getString(R.string.speaker_mute_description)
                    isChecked = WheelLog.AppConfig.speakerMute
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_max_speed)
                    title = getString(R.string.max_speed_title)
                    summary = getString(R.string.tilt_back_description)
                    min = 3
                    max = 60
                    unit = getString(R.string.kmh)
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
                }
        )
        prefs.add(
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
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.ride_mode)
                    title = getString(R.string.ride_mode_title)
                    summary = getString(R.string.ride_mode_description)
                    isChecked = WheelLog.AppConfig.rideMode
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.pedal_sensivity)
                    title = getString(R.string.pedal_sensivity_title)
                    summary = getString(R.string.pedal_sensivity_description)
                    min = 0
                    max = 100
                    unit = "%"
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.pedalSensivity)
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.fancier_mode)
                    title = getString(R.string.fancier_mode_title)
                    summary = getString(R.string.fancier_mode_description)
                    isChecked = WheelLog.AppConfig.fancierMode
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.go_home_mode)
                    title = getString(R.string.go_home_mode_title)
                    summary = getString(R.string.go_home_mode_description)
                    isChecked = WheelLog.AppConfig.goHomeMode
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.transport_mode)
                    title = getString(R.string.transport_mode_title)
                    summary = getString(R.string.transport_mode_description)
                    isChecked = WheelLog.AppConfig.transportMode
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.lock_mode)
                    title = getString(R.string.lock_mode_title)
                    summary = getString(R.string.lock_mode_description)
                    isChecked = WheelLog.AppConfig.lockMode
                }
        )


        if (WheelData.getInstance().speed < 1) {
            prefs.add(
                    Preference(context).apply {
                        setIcon(R.drawable.ic_baseline_power_off_24)
                        title = getString(R.string.power_off)
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.power_off))
                                    .setMessage(getString(R.string.power_off_message))
                                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                        WheelData.getInstance().powerOff()
                                    }
                                    .setNegativeButton(android.R.string.no, null)
                                    .setIcon(R.drawable.ic_baseline_power_off_24)
                                    .show()
                            true
                        }
                    }
            )
            prefs.add(
                    Preference(context).apply {
                        setIcon(R.drawable.ic_baseline_calibration_24)
                        title = getString(R.string.wheel_calibration)
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.wheel_calibration))
                                    .setMessage(getString(R.string.wheel_calibration_message_inmo))
                                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                        WheelData.getInstance().wheelCalibration()
                                    }
                                    .setNegativeButton(android.R.string.no, null)
                                    .setIcon(R.drawable.ic_baseline_calibration_24)
                                    .show()
                            true
                        }
                    }
            )
        }
        return prefs
    }

    private fun preferenceKingsong(): List<Preference> {
        val prefs: MutableList<Preference> = arrayListOf()
        prefs.add(
                ListPreference(context).apply {
                    key = mac + getString(R.string.light_mode)
                    title = getString(R.string.light_mode_title)
                    summary = getString(R.string.on_off_auto)
                    setEntries(R.array.light_mode_ks)
                    setEntryValues(R.array.light_mode_values)
                    setDefaultValue(WheelLog.AppConfig.lightMode)
                }
        )
        prefs.add(
                ListPreference(context).apply {
                    key = mac + getString(R.string.strobe_mode)
                    title = getString(R.string.strobe_mode_title)
                    summary = getString(R.string.on_off)
                    setEntries(R.array.strobe_mode_ks)
                    setEntryValues(R.array.strobe_mode_values)
                    setDefaultValue(WheelLog.AppConfig.strobeMode)
                }
        )
        prefs.add(
                ListPreference(context).apply {
                    key = mac + getString(R.string.led_mode)
                    title = getString(R.string.led_mode_title)
                    summary = getString(R.string.on_off)
                    setEntries(R.array.led_mode)
                    setEntryValues(R.array.led_mode_values)
                    setDefaultValue(WheelLog.AppConfig.ledMode)
                }
        )
        prefs.add(
                ListPreference(context).apply {
                    key = mac + getString(R.string.pedals_mode)
                    title = getString(R.string.pedals_mode_title)
                    summary = getString(R.string.soft_medium_hard)
                    setEntries(R.array.pedals_mode)
                    setEntryValues(R.array.pedals_mode_values)
                    setDefaultValue(WheelLog.AppConfig.pedalsMode)
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_max_speed)
                    title = getString(R.string.max_speed_title)
                    summary = getString(R.string.tilt_back_description)
                    increment = 1
                    max = 50
                    min = 0
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_ks_alarm3)
                    title = getString(R.string.alert3_title)
                    summary = getString(R.string.alarm3_description)
                    increment = 1
                    max = 50
                    min = 0
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.wheelKsAlarm3)
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_ks_alarm2)
                    title = getString(R.string.alert2_title)
                    summary = getString(R.string.alarm2_description)
                    increment = 1
                    max = 50
                    min = 0
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.wheelKsAlarm2)
                }
        )
        prefs.add(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_ks_alarm1)
                    title = getString(R.string.alert1_title)
                    summary = getString(R.string.alarm1_description)
                    increment = 1
                    max = 50
                    min = 0
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.wheelKsAlarm1)
                }
        )
        prefs.add(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.ks18l_scaler)
                    title = getString(R.string.ks18l_scaler_title)
                    summary = getString(R.string.ks18l_scaler_description)
                }
        )
        if (WheelData.getInstance().speed < 1) {
            prefs.add(
                    Preference(context).apply {
                        setIcon(R.drawable.ic_baseline_calibration_24)
                        title = getString(R.string.wheel_calibration)
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.wheel_calibration))
                                    .setMessage(getString(R.string.wheel_calibration_message_kingsong))
                                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                        WheelData.getInstance().wheelCalibration()
                                    }
                                    .setNegativeButton(android.R.string.no, null)
                                    .setIcon(R.drawable.ic_baseline_calibration_24)
                                    .show()
                            true
                        }
                    }
            )
            prefs.add(
                    Preference(context).apply {
                        setIcon(R.drawable.ic_baseline_power_off_24)
                        title = getString(R.string.power_off)
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.power_off))
                                    .setMessage(getString(R.string.power_off_message))
                                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                        WheelData.getInstance().powerOff()
                                    }
                                    .setNegativeButton(android.R.string.no, null)
                                    .setIcon(R.drawable.ic_baseline_power_off_24)
                                    .show()
                            true
                        }
                    }
            )
        }
        return prefs

    }

    private fun preferenceBegode(): List<Preference> {
        return listOf(
                ListPreference(context).apply {
                    key = mac + getString(R.string.light_mode)
                    title = getString(R.string.light_mode_title)
                    summary = getString(R.string.on_off_strobe)
                    setEntries(R.array.light_mode_gw)
                    setEntryValues(R.array.light_mode_values)
                    setDefaultValue(WheelLog.AppConfig.lightMode)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.alarm_mode)
                    title = getString(R.string.alarm_mode_title)
                    summary = getString(R.string.alarm_settings_title)
                    setEntries(R.array.alarm_mode_gw)
                    setEntryValues(R.array.alarm_mode_values)
                    setDefaultValue(WheelLog.AppConfig.alarmMode)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.pedals_mode)
                    title = getString(R.string.pedals_mode_title)
                    summary = getString(R.string.soft_medium_hard)
                    setEntries(R.array.pedals_mode)
                    setEntryValues(R.array.pedals_mode_values)
                    setDefaultValue(WheelLog.AppConfig.pedalsMode)
                },
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.wheel_max_speed)
                    title = getString(R.string.max_speed_title)
                    summary = getString(R.string.tilt_back_description)
                    max = 99
                    min = 0
                    increment = 1
                    unit = getString(R.string.kmh)
                    setDefaultValue(WheelLog.AppConfig.wheelMaxSpeed)
                },
                Preference(context).apply {
                    setIcon(R.drawable.ic_baseline_calibration_24)
                    title = getString(R.string.wheel_calibration)
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        AlertDialog.Builder(context)
                                .setTitle(getString(R.string.wheel_calibration))
                                .setMessage(getString(R.string.wheel_calibration_message_begode))
                                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                    WheelData.getInstance().wheelCalibration()
                                }
                                .setNegativeButton(android.R.string.no, null)
                                .setIcon(R.drawable.ic_baseline_calibration_24)
                                .show()
                        true
                    }
                },
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.use_ratio)
                    title = getString(R.string.is_gotway_mcm_title)
                    summary = getString(R.string.is_gotway_mcm_description)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.gotway_voltage)
                    title = getString(R.string.battery_voltage_title)
                    summary = getString(R.string.battary_voltage_description)
                    setEntries(R.array.gotway_voltage)
                    setEntryValues(R.array.gotway_voltage_values)
                    setDefaultValue(WheelLog.AppConfig.gotwayVoltage)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.gotway_negative)
                    title = getString(R.string.gotway_negative_title)
                    summary = getString(R.string.gotway_negative_description)
                    setEntries(R.array.gotway_negative)
                    setEntryValues(R.array.gotway_negative_values)
                    setDefaultValue(WheelLog.AppConfig.gotwayNegative)
                },
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.connect_beep)
                    title = getString(R.string.connect_beep_title)
                    summary = getString(R.string.connect_beep_description)
                    setDefaultValue(true)
                }
        )
    }

    private fun preferenceVeteran(): List<Preference> {
        return listOf(
                ListPreference(context).apply {
                    key = mac + getString(R.string.pedals_mode)
                    title = getString(R.string.pedals_mode_title)
                    summary = getString(R.string.soft_medium_hard)
                    setEntries(R.array.pedals_mode)
                    setEntryValues(R.array.pedals_mode_values)
                    setDefaultValue(WheelLog.AppConfig.pedalsMode)
                },
                Preference(context).apply {
//                    setIcon(R.drawable.ic_baseline_power_off_24)
                    title = getString(R.string.reset_trip)
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        AlertDialog.Builder(context)
                                .setTitle(getString(R.string.reset_trip))
                                .setMessage(getString(R.string.reset_trip_message))
                                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                    VeteranAdapter.getInstance().resetTrip()
                                }
                                .setNegativeButton(android.R.string.no, null)
//                                .setIcon(R.drawable.ic_baseline_power_off_24)
                                .show()
                        true
                    }
                },
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.connect_beep)
                    title = getString(R.string.connect_beep_title)
                    summary = getString(R.string.connect_beep_description)
                    setDefaultValue(true)
                },
                ListPreference(context).apply {
                    key = mac + getString(R.string.gotway_negative)
                    title = getString(R.string.gotway_negative_title)
                    summary = getString(R.string.gotway_negative_description)
                    setEntries(R.array.gotway_negative)
                    setEntryValues(R.array.gotway_negative_values)
                    setDefaultValue(WheelLog.AppConfig.gotwayNegative)
                }
        )
    }

    private fun preferenceForAllWheel(): List<Preference> {
        return listOf(
                SeekBarPreference(context).apply {
                    key = mac + getString(R.string.battery_capacity)
                    title = getString(R.string.battery_capacity_title)
                    summary = getString(R.string.battery_capacity_description)
                    increment = 1
                    max = 9999
                    min = 0
                    unit = "Wh"
                    setDefaultValue(0)
                },
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
                },
                Preference(context).apply {
                    key = getString(R.string.profile_name)
                    title = getString(R.string.profile_name_title)
                },
                Preference(context).apply {
                    title = getString(R.string.current_mac)
                    summary = mac.trimEnd('_')
                }
        )
    }

    fun getPrefs(mac: String): List<Preference> {
        this.mac = mac
        val prefs = mutableListOf<Preference>()
        prefs.addAll(
                when (WheelData.getInstance().wheelType) {
                    Constants.WHEEL_TYPE.NINEBOT_Z -> preferenceNinebotZ()
                    Constants.WHEEL_TYPE.INMOTION -> preferenceInmotion()
                    Constants.WHEEL_TYPE.INMOTION_V2 -> preferenceInmotionV2()
                    Constants.WHEEL_TYPE.KINGSONG -> preferenceKingsong()
                    Constants.WHEEL_TYPE.GOTWAY -> preferenceBegode()
                    Constants.WHEEL_TYPE.VETERAN -> preferenceVeteran()
                    else -> listOf(Preference(context).apply {
                        key = getString(R.string.no_settings)
                        isEnabled = false
                        summary = getString(R.string.unknown_device)
                    })
                })
        if (WheelData.getInstance().wheelType != Constants.WHEEL_TYPE.Unknown) {
            prefs.addAll(preferenceForAllWheel())
        }
        return prefs
    }
}