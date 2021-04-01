package com.cooper.wheellog.preferences

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.presentation.preferences.MultiSelectPreference
import com.cooper.wheellog.presentation.preferences.SeekBarPreference
import com.cooper.wheellog.utils.ThemeEnum
import timber.log.Timber

class SpeedSettings(context: Context) : BaseSettingsClass(context) {

    fun getPrefs(mac: String): List<Preference> {
        return listOf(
                SwitchPreference(context).apply {
                    key = getString(R.string.use_eng)
                    title = getString(R.string.use_eng_title)
                    summary = getString(R.string.use_eng_description)
                    icon = getDrawable(R.drawable.ic_baseline_translate_24)
                },
                ListPreference(context).apply {
                    key = getString(R.string.app_theme)
                    title = getString(R.string.app_theme_title)
                    summary = getString(R.string.app_theme_description)
                    entries = ThemeEnum.values().map { x -> x.name }.toTypedArray()
                    entryValues = ThemeEnum.values().map { x -> x.value.toString() }.toTypedArray()
                },
                ListPreference(context).apply {
                    key = getString(R.string.day_night_theme)
                    title = getString(R.string.day_night_theme_title)
                    summary = when (WheelLog.AppConfig.dayNightThemeMode) {
                        AppCompatDelegate.MODE_NIGHT_NO ->  getString(R.string.day_night_theme_day)
                        AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.day_night_theme_night)
                        else -> getString(R.string.day_night_theme_as_system)
                    }
                    entries = arrayOf(getString(R.string.day_night_theme_as_system), getString(R.string.day_night_theme_day), getString(R.string.day_night_theme_night))
                    entryValues = arrayOf(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)
                            .map {x -> x.toString()}.toTypedArray()
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
                    key = mac + getString(R.string.cell_voltage_tiltback)
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
                        MultiSelectPreference(context).apply {
                            key = getString(R.string.notification_buttons)
                            title = getString(R.string.notification_buttons_title)
                            dialogTitle = title
                            dialogIcon = icon
                            entries = arrayOf(getString(R.string.icon_connection),
                                    getString(R.string.icon_logging),
                                    getString(R.string.icon_watch),
                                    getString(R.string.icon_beep),
                                    getString(R.string.icon_light))
                            useSort = false
                            if (WheelLog.AppConfig.notifivationButtons == null) {
                                setValues(listOf(getString(R.string.icon_connection),
                                        getString(R.string.icon_logging),
                                        getString(R.string.icon_watch)))
                            }
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
                    setDefaultValue(WheelLog.AppConfig.connectionSound)
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
                            setDefaultValue(WheelLog.AppConfig.useCustomBeep)
                        },
                )
        )
    }

    fun selectCustomBeep(fragment: PreferencesFragment, mediaRequestCode: Int) {
        if (!WheelLog.AppConfig.useCustomBeep) {
            return
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            // Android 11+
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            if (uri == null) {
                Timber.wtf(getString(R.string.no_file_read_permission))
                WheelLog.AppConfig.useCustomBeep = false
                return
            }
            val projection = arrayOf(MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media._ID)
            val cursor = context.contentResolver.query(uri,
                    projection,
                    null,
                    null,
                    MediaStore.Downloads.DATE_MODIFIED + " DESC")
            val sounds = mutableMapOf<String, String>()
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val title = cursor.getString(0)
                    val mediaId = cursor.getString(1)
                    sounds[title] = mediaId
                } while (cursor.moveToNext())
                cursor.close()
            }
            if (sounds.isNotEmpty()) {
                val fileNames = sounds.keys.toTypedArray()
                AlertDialog.Builder(context)
                        .setTitle(getString(R.string.custom_beep_title))
                        .setItems(fileNames) { _, which ->
                            val fileName = fileNames[which]
                            val id = sounds[fileName]
                            fragment.findPreference<SwitchPreference>(getString(R.string.custom_beep))?.summary = fileName
                            WheelLog.AppConfig.beepFile = Uri.withAppendedPath(uri, id)
                        }
                        .setCancelable(false)
                        .create()
                        .show()
            } else {
                Timber.wtf(getString(R.string.files_not_found))
                WheelLog.AppConfig.useCustomBeep = false
            }
        } else {
            // Android 10 or less
            val intent = Intent("android.intent.action.OPEN_DOCUMENT").apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            fragment.startActivityForResult(intent, mediaRequestCode)
        }
    }
}