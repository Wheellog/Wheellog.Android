package com.cooper.wheellog.preferences

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.presentation.preferences.SeekBarPreference
import com.cooper.wheellog.utils.Constants

class AlarmSettings(context: Context) : BaseSettingsClass(context) {

    fun getPrefs(mac: String): List<Preference> {
        return listOf(
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.alarms_enabled)
                    title = getString(R.string.enable_alarms_title)
                    summary = getString(R.string.enable_alarms_description)
                },
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.disable_phone_vibrate)
                    title = getString(R.string.disable_phone_vibrate_title)
                    summary = getString(R.string.disable_phone_vibration_description)
                },
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.disable_phone_beep)
                    title = getString(R.string.disable_phone_beep_title)
                    summary = getString(R.string.disable_phone_beep_description)
                },
                SwitchPreference(context).apply {
                    key = mac + getString(R.string.altered_alarms)
                    title = getString(R.string.altered_alarms_title)
                    summary = getString(R.string.altered_alarms_description)
                },
                addPreferenceCategory(getString(R.string.speed_alarm1_phone_title),
                        mac + getString(R.string.speed_alarm1),
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_1_speed)
                            title = getString(R.string.speed)
                            summary = getString(R.string.speed_trigger_description)
                            min = 0
                            max = 100
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(29)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_1_battery)
                            title = getString(R.string.alarm_1_battery_title)
                            summary = getString(R.string.alarm_1_battery_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(100)
                        }),
                addPreferenceCategory(getString(R.string.speed_alarm2_phone_title),
                        mac + getString(R.string.speed_alarm2),
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_2_speed)
                            title = getString(R.string.speed)
                            summary = getString(R.string.speed_trigger_description)
                            min = 0
                            max = 100
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(0)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_2_battery)
                            title = getString(R.string.alarm_2_battery_title)
                            summary = getString(R.string.alarm_1_battery_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(0)
                        }),
                addPreferenceCategory(getString(R.string.speed_alarm3_phone_title),
                        mac + getString(R.string.speed_alarm3),
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_3_speed)
                            title = getString(R.string.speed)
                            summary = getString(R.string.speed_trigger_description)
                            min = 0
                            max = 100
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(0)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_3_battery)
                            title = getString(R.string.alarm_3_battery_title)
                            summary = getString(R.string.alarm_1_battery_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(0)
                        }),
                addPreferenceCategory(getString(R.string.altered_alarms_pref_title),
                        mac + getString(R.string.altered_alarms_section),
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.rotation_speed)
                            title = getString(R.string.rotation_speed_title)
                            summary = getString(R.string.rotation_speed_description)
                            min = 0
                            max = 2000
                            decimalPlaces = 1
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(500)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.rotation_voltage)
                            title = getString(R.string.rotation_voltage_title)
                            summary = getString(R.string.rotation_voltage_description)
                            min = 0
                            max = 1200
                            decimalPlaces = 1
                            unit = getString(R.string.volt)
                            increment = 1
                            setDefaultValue(840)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.power_factor)
                            title = getString(R.string.power_factor_title)
                            summary = getString(R.string.power_factor_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(90)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_factor1)
                            title = getString(R.string.alarm_factor1_title)
                            summary = getString(R.string.alarm_factor1_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(80)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_factor2)
                            title = getString(R.string.alarm_factor2_title)
                            summary = getString(R.string.alarm_factor2_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(90)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_factor3)
                            title = getString(R.string.alarm_factor3_title)
                            summary = getString(R.string.alarm_factor3_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(95)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.warning_speed)
                            title = getString(R.string.warning_speed_title)
                            summary = getString(R.string.warning_speed_description)
                            min = 0
                            max = 100
                            unit = getString(R.string.kmh)
                            increment = 1
                            setDefaultValue(0)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.warning_pwm)
                            title = getString(R.string.warning_pwm_title)
                            summary = getString(R.string.warning_pwm_description)
                            min = 0
                            max = 100
                            unit = "%"
                            increment = 1
                            setDefaultValue(0)
                        },
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.warning_speed_period)
                            title = getString(R.string.warning_speed_period_title)
                            summary = getString(R.string.warning_speed_period_description)
                            min = 0
                            max = 60
                            unit = getString(R.string.sec)
                            increment = 1
                            setDefaultValue(0)
                        }),
                addPreferenceCategory(getString(R.string.current_alarm_title), null,
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_current)
                            title = getString(R.string.current_title)
                            summary = getString(R.string.alarm_current_description)
                            min = 0
                            max = 300
                            unit = getString(R.string.amp)
                            increment = 1
                            setDefaultValue(WheelLog.AppConfig.alarmCurrent)
                        }),
                addPreferenceCategory(getString(R.string.temperature_alarm_title), null,
                        SeekBarPreference(context).apply {
                            key = mac + getString(R.string.alarm_temperature)
                            title = getString(R.string.temperature_title)
                            summary = getString(R.string.alarm_temperature_description)
                            min = 0
                            max = 120
                            unit = "°"
                            increment = 1
                            setDefaultValue(WheelLog.AppConfig.alarmTemperature)
                        })
        )
    }

    fun switchAlarmsIsVisible(fragment: PreferencesFragment) {
        val alarmsEnabled = WheelLog.AppConfig.alarmsEnabled
        val alteredAlarms = WheelLog.AppConfig.alteredAlarms
        val ksAlteredAlarms = WheelData.getInstance().wheelType == Constants.WHEEL_TYPE.KINGSONG
        val categoryPreferencesNormal = arrayOf(
                getString(R.string.speed_alarm1),
                getString(R.string.speed_alarm2),
                getString(R.string.speed_alarm3)
        )
        val preferencesCommon = arrayOf(
                getString(R.string.alarm_current),
                getString(R.string.alarm_temperature)
        )
        val preferencesKs = arrayOf(
                getString(R.string.rotation_voltage),
                getString(R.string.rotation_speed),
                getString(R.string.power_factor)
        )
        val mac = WheelData.getInstance().mac + "_"
        for (preference in categoryPreferencesNormal) {
            fragment.findPreference<PreferenceCategory>(mac + preference)?.isVisible = alarmsEnabled && !alteredAlarms
        }
        fragment.findPreference<PreferenceCategory>(mac + getString(R.string.altered_alarms_section))?.isVisible = alarmsEnabled && alteredAlarms
        for (preference in preferencesCommon) {
            fragment.findPreference<Preference>(mac + preference)?.isVisible = alarmsEnabled
        }
        for (preference in preferencesKs) {
            fragment.findPreference<Preference>(mac + preference)?.isVisible = alarmsEnabled && !ksAlteredAlarms && alteredAlarms
        }
    }
}