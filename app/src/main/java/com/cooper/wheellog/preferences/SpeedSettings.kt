package com.cooper.wheellog.preferences

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.presentation.preferences.MultiSelectPreference
import com.cooper.wheellog.presentation.preferences.SeekBarPreference

class SpeedSettings(context: Context) : BaseSettingsClass(context) {

    fun getPrefs(): List<Preference> {
        return listOf(
                SwitchPreference(context).apply {
                    key = getString(R.string.use_eng)
                    title = getString(R.string.use_eng_title)
                    summary = getString(R.string.use_eng_description)
                    icon = getDrawable(R.drawable.ic_baseline_translate_24)
                },
                SwitchPreference(context).apply {
                    key = getString(R.string.use_better_percents)
                    title = getString(R.string.use_better_percents_title)
                    summary = getString(R.string.use_better_percents_description)
                },
                SwitchPreference(context).apply {
                    key = getString(R.string.fixed_percents)
                    title = getString(R.string.fixed_percents_title)
                    summary = getString(R.string.fixed_percents_description)
                    addDependency(this, getString(R.string.use_better_percents))
                },
                SeekBarPreference(context).apply {
                    key = getString(R.string.cell_voltage_tiltback)
                    title = getString(R.string.cell_voltage_tiltback_title)
                    summary = getString(R.string.cell_voltage_tiltback_description)
                    min = 250
                    max = 400
                    unit = getString(R.string.volt)
                    increment = 1
                    decimalPlaces = 2
                    setDefaultValue(WheelLog.AppConfig.cellVoltageTiltback)
                    addDependency(this, getString(R.string.fixed_percents))
                },
                SwitchPreference(context).apply {
                    key = getString(R.string.use_mph)
                    title = getString(R.string.use_mph_title)
                    summary = getString(R.string.use_mph_description)
                },
                addPreferenceCategory(getString(R.string.main_view_category), null,
                        MultiSelectPreference(context).apply {
                            key = getString(R.string.view_blocks_string)
                            title = getString(R.string.view_blocks_title)
                            summary = getString(R.string.view_blocks_description)
                            icon = getDrawable(R.drawable.ic_baseline_dashboard_customize_24)
                            dialogTitle = title
                            dialogIcon = icon
                            setDefaultValue(WheelLog.AppConfig.viewBlocksString)
                            entries = context.resources.getTextArray(R.array.view_blocks_values)
                        },
                        SeekBarPreference(context).apply {
                            key = getString(R.string.max_speed)
                            title = getString(R.string.max_speed_dial_title)
                            summary = getString(R.string.max_speed_dial_description)
                            min = 10
                            max = 100
                            unit = getString(R.string.dial_measurements)
                            increment = 1
                            setDefaultValue(WheelLog.AppConfig.maxSpeed)
                        },
                        SwitchPreference(context).apply {
                            key = getString(R.string.current_on_dial)
                            title = getString(R.string.current_on_dial_title)
                            summary = getString(R.string.current_on_dial_description)
                        },
                        SwitchPreference(context).apply {
                            key = getString(R.string.use_short_pwm)
                            title = getString(R.string.use_short_pwm_title)
                            summary = getString(R.string.use_short_pwm_description)
                            setDefaultValue(WheelLog.AppConfig.useShortPwm)
                        }
                ),
                SwitchPreference(context).apply {
                    key = getString(R.string.show_page_events)
                    title = getString(R.string.show_page_events_title)
                    summary = getString(R.string.show_page_events_description)
                    icon = getDrawable(R.drawable.ic_baseline_event_note_24)
                },
                SwitchPreference(context).apply {
                    key = getString(R.string.show_page_trips)
                    title = getString(R.string.show_page_trips_title)
                    icon = getDrawable(R.drawable.ic_baseline_road_with_two_placeholders_24)
                    setDefaultValue(WheelLog.AppConfig.pageTrips)
                },
                SwitchPreference(context).apply {
                    key = getString(R.string.connection_sound)
                    title = getString(R.string.connection_sound_title)
                    summary = getString(R.string.connection_sound_description)
                    icon = getDrawable(R.drawable.ic_baseline_music_note_24)
                    setDefaultValue(WheelLog.AppConfig.pageTrips)
                },
                SeekBarPreference(context).apply {
                    key = getString(R.string.no_connection_sound)
                    title = getString(R.string.no_connection_sound_title)
                    summary = getString(R.string.no_connection_sound_description)
                    min = 0
                    max = 60
                    unit = getString(R.string.sec)
                    increment = 1
                    setDefaultValue(WheelLog.AppConfig.noConnectionSound)
                    addDependency(this, getString(R.string.connection_sound))
                },
                SwitchPreference(context).apply {
                    key = getString(R.string.use_stop_music)
                    title = getString(R.string.use_stop_music_title)
                    summary = getString(R.string.use_stop_music_description)
                    icon = getDrawable(R.drawable.ic_baseline_volume_off_24)
                },
                SwitchPreference(context).apply {
                    key = getString(R.string.show_unknown_devices)
                    title = getString(R.string.show_unknown_devices_title)
                    summary = getString(R.string.show_unknown_devices_description)
                    setDefaultValue(WheelLog.AppConfig.showUnknownDevices)
                },
                addPreferenceCategory(
                        getString(R.string.beep_category), null,
                        SwitchPreference(context).apply {
                            key = getString(R.string.beep_on_single_tap)
                            title = getString(R.string.beep_on_single_tap_title)
                            summary = getString(R.string.beep_on_single_tap_description)
                            setDefaultValue(WheelLog.AppConfig.useBeepOnSingleTap)
                        },
                        SwitchPreference(context).apply {
                            key = getString(R.string.beep_on_volume_up)
                            title = getString(R.string.beep_on_volume_up_title)
                            summary = getString(R.string.beep_on_volume_up_description)
                            setDefaultValue(WheelLog.AppConfig.useBeepOnVolumeUp)
                        },
                        SwitchPreference(context).apply {
                            key = getString(R.string.beep_by_wheel)
                            title = getString(R.string.beep_by_wheel_title)
                            summary = getString(R.string.beep_by_wheel_description)
                            setDefaultValue(WheelLog.AppConfig.beepByWheel)
                        },
                        SwitchPreference(context).apply {
                            key = getString(R.string.custom_beep)
                            title = getString(R.string.custom_beep_title)
                            summary = WheelLog.AppConfig.beepFile.lastPathSegment
                            setDefaultValue(WheelLog.AppConfig.useCustomBeep)
                        },
                )
        )
    }
}