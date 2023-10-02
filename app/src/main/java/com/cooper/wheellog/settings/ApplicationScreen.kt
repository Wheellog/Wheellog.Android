package com.cooper.wheellog.settings

import android.app.Activity
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.WheelLog.Companion.AppConfig
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.ThemeIconEnum


@Composable
fun applicationScreen() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        var restartRequiredAlert by remember { mutableStateOf(false) }
        if (restartRequiredAlert) {
            AlertDialog(
                onDismissRequest ={ restartRequiredAlert = false },
                title = { Text(stringResource(R.string.use_eng_alert_title)) },
                text = { Text(stringResource(R.string.use_eng_alert_description)) },
                confirmButton = {
                    Button(onClick = { restartRequiredAlert = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            )
        }

        switchPref(
            name = stringResource(R.string.use_eng_title),
            desc = stringResource(R.string.use_eng_description),
            themeIcon = ThemeIconEnum.SettingsLanguage,
            default = AppConfig.useEng
        ) {
            restartRequiredAlert = true
            AppConfig.useEng = it
        }

        list(
            name = stringResource(R.string.app_theme_title),
            desc = stringResource(R.string.app_theme_description),
            entries = ThemeEnum.values().associate { it.value.toString() to it.name },
            defaultKey = AppConfig.appThemeInt.toString()
        ) {
            restartRequiredAlert = true
            AppConfig.appThemeInt = it.first.toInt()
        }

        list(
            name = stringResource(R.string.day_night_theme_title),
            entries = mapOf(
                AppCompatDelegate.MODE_NIGHT_UNSPECIFIED.toString() to stringResource(R.string.day_night_theme_as_system),
                AppCompatDelegate.MODE_NIGHT_NO.toString() to stringResource(R.string.day_night_theme_day),
                AppCompatDelegate.MODE_NIGHT_YES.toString() to stringResource(R.string.day_night_theme_night),
            ),
            defaultKey = AppConfig.dayNightThemeMode.toString()
        ) {
            AppConfig.dayNightThemeMode = it.first.toInt()
        }

        switchPref(
            name = stringResource(R.string.use_better_percents_title),
            desc = stringResource(R.string.use_better_percents_description),
            default = AppConfig.useBetterPercents
        ) {
            AppConfig.useBetterPercents = it
        }

        var customPercents by remember { mutableStateOf(AppConfig.customPercents) }
        switchPref(
            name = stringResource(R.string.custom_percents_title),
            desc = stringResource(R.string.custom_percents_description),
            default = AppConfig.customPercents,
            showDiv = customPercents,
        ) {
            AppConfig.customPercents = it
            customPercents = it
        }
        AnimatedVisibility(visible = customPercents) {
            sliderPref(
                name = stringResource(R.string.cell_voltage_tiltback_title),
                desc = stringResource(R.string.cell_voltage_tiltback_description),
                position = AppConfig.cellVoltageTiltback / 100f,
                min = 2.5f,
                max = 4f,
                unit = R.string.volt,
                format = "%.2f",
                showDiv = false,
            ) {
                AppConfig.cellVoltageTiltback = (it * 100).toInt()
                // currentRecomposeScope.invalidate()
            }
        }

        group(name = stringResource(R.string.measurement_systems_category_title)) {

            switchPref(
                name = stringResource(R.string.use_mph_title),
                desc = stringResource(R.string.use_mph_description),
                default = AppConfig.useMph
            ) {
                AppConfig.useMph = it
            }

            switchPref(
                name = stringResource(R.string.use_fahrenheit_title),
                desc = stringResource(R.string.use_fahrenheit_description),
                default = AppConfig.useFahrenheit,
                showDiv = false,
            ) {
                AppConfig.useFahrenheit = it
            }
        }

        group(name = stringResource(R.string.after_connect_category)) {

            switchPref(
                name = stringResource(R.string.auto_log_title),
                desc = stringResource(R.string.auto_log_description),
                themeIcon = ThemeIconEnum.SettingsAutoLog,
                default = AppConfig.autoLog,
            ) {
                AppConfig.autoLog = it
            }

            switchPref(
                name = stringResource(R.string.auto_watch_title),
                desc = stringResource(R.string.auto_watch_description),
                themeIcon = ThemeIconEnum.SettingsWatch,
                default = AppConfig.autoWatch,
                showDiv = false,
            ) {
                AppConfig.autoWatch = it
            }
        }

        group(name = stringResource(R.string.main_view_category)) {

            multiList(
                name = stringResource(R.string.view_blocks_title),
                desc = stringResource(R.string.view_blocks_description),
                themeIcon = ThemeIconEnum.SettingsBlocks,
                entries = mapOf(
                    // TODO: use enum as key instead of localized string resources
                    stringResource(R.string.pwm) to stringResource(R.string.pwm),
                    stringResource(R.string.max_pwm) to stringResource(R.string.max_pwm),
                    stringResource(R.string.voltage) to stringResource(R.string.voltage),
                    stringResource(R.string.battery) to stringResource(R.string.battery),
                    stringResource(R.string.top_speed) to stringResource(R.string.top_speed),
                    stringResource(R.string.average_riding_speed) to stringResource(R.string.average_riding_speed),
                    stringResource(R.string.average_speed) to stringResource(R.string.average_speed),
                    stringResource(R.string.riding_time) to stringResource(R.string.riding_time),
                    stringResource(R.string.ride_time) to stringResource(R.string.ride_time),
                    stringResource(R.string.current) to stringResource(R.string.current),
                    stringResource(R.string.maxcurrent) to stringResource(R.string.maxcurrent),
                    stringResource(R.string.power) to stringResource(R.string.power),
                    stringResource(R.string.maxpower) to stringResource(R.string.maxpower),
                    stringResource(R.string.temperature) to stringResource(R.string.temperature),
                    stringResource(R.string.temperature2) to stringResource(R.string.temperature2),
                    stringResource(R.string.maxtemperature) to stringResource(R.string.maxtemperature),
                    stringResource(R.string.distance) to stringResource(R.string.distance),
                    stringResource(R.string.total) to stringResource(R.string.total),
                    stringResource(R.string.wheel_distance) to stringResource(R.string.wheel_distance),
                    stringResource(R.string.remaining_distance) to stringResource(R.string.remaining_distance),
                    stringResource(R.string.battery_per_km) to stringResource(R.string.battery_per_km),
                    stringResource(R.string.consumption) to stringResource(R.string.consumption),
                    stringResource(R.string.avg_cell_volt) to stringResource(R.string.avg_cell_volt),
                    stringResource(R.string.user_distance) to stringResource(R.string.user_distance),
                    stringResource(R.string.speed) to stringResource(R.string.speed),
                ),
                defaultKeys = AppConfig.viewBlocks.toList(),
                useSort = true,
            ) {
                AppConfig.viewBlocks = it.toTypedArray()
            }
            var usePipMode by remember { mutableStateOf(AppConfig.usePipMode) }

            switchPref(
                name = stringResource(R.string.use_pip_mode_title),
                desc = stringResource(R.string.use_pip_mode_description),
                default = AppConfig.usePipMode,
            ) {
                AppConfig.usePipMode = it
                usePipMode = it
            }
            AnimatedVisibility(visible = usePipMode) {
                list(
                    name = stringResource(R.string.pip_block_title),
                    entries = mapOf(
                        "0" to stringResource(R.string.speed),
                        "1" to stringResource(R.string.consumption),
                    ),
                    defaultKey = AppConfig.pipBlock,
                ) {
                    AppConfig.pipBlock = it.first
                }
            }

            multiList(
                name = stringResource(R.string.notification_buttons_title),
                desc = stringResource(R.string.notification_buttons_description),
                themeIcon = ThemeIconEnum.SettingsNotification,
                entries = mapOf(
                    stringResource(R.string.icon_connection) to stringResource(R.string.icon_connection),
                    stringResource(R.string.icon_logging) to stringResource(R.string.icon_logging),
                    stringResource(R.string.icon_watch) to stringResource(R.string.icon_watch),
                    stringResource(R.string.icon_beep) to stringResource(R.string.icon_beep),
                    stringResource(R.string.icon_light) to stringResource(R.string.icon_light),
                    stringResource(R.string.icon_miband) to stringResource(R.string.icon_miband),
                ),
                defaultKeys = AppConfig.notificationButtons.toList()
            ) {
                AppConfig.notificationButtons = it.toTypedArray()
                WheelLog.Notifications.update()
            }

            multiList(
                name = stringResource(R.string.main_menu_buttons_title),
                entries = mapOf(
                    "watch" to stringResource(R.string.start_pebble_service),
                    "miband" to stringResource(R.string.miband_desc),
                    "reset" to stringResource(R.string.reset_max_values_title),
                ),
                keyIcons = mapOf(
                    "watch" to R.drawable.ic_action_watch_white,
                    "miband" to R.drawable.ajdm_ic_mi_med,
                    "reset" to R.drawable.ic_action_reset,
                ),
                defaultKeys = AppConfig.mainMenuButtons.toList()
            ) {
                AppConfig.mainMenuButtons = it.toTypedArray()
            }

            switchPref(
                name = stringResource(R.string.show_clock_title),
                default = AppConfig.showClock,
            ) {
                AppConfig.showClock = it
            }

            sliderPref(
                name = stringResource(R.string.max_speed_dial_title),
                desc = stringResource(R.string.max_speed_dial_description),
                min = 10f,
                max = 100f,
                position = AppConfig.maxSpeed.toFloat(),
            ) {
                AppConfig.maxSpeed = it.toInt()
            }

            switchPref(
                name = stringResource(R.string.current_on_dial_title),
                desc = stringResource(R.string.current_on_dial_description),
                default = AppConfig.currentOnDial,
            ) {
                AppConfig.currentOnDial = it
            }

            switchPref(
                name = stringResource(R.string.use_short_pwm_title),
                desc = stringResource(R.string.use_short_pwm_description),
                default = AppConfig.useShortPwm,
                showDiv = false,
            ) {
                AppConfig.useShortPwm = it
            }
        }

        switchPref(
            name = stringResource(R.string.show_page_graph_title),
            default = AppConfig.pageGraph,
        ) {
            AppConfig.pageGraph = it
        }

        switchPref(
            name = stringResource(R.string.show_page_events_title),
            desc = stringResource(R.string.show_page_events_description),
            themeIcon = ThemeIconEnum.SettingsPageEvents,
            default = AppConfig.pageEvents,
        ) {
            AppConfig.pageEvents = it
        }

        switchPref(
            name = stringResource(R.string.show_page_trips_title),
            themeIcon = ThemeIconEnum.SettingsPageTrips,
            default = AppConfig.pageTrips,
        ) {
            AppConfig.pageTrips = it
        }

        switchPref(
            name = stringResource(R.string.connection_sound_title),
            desc = stringResource(R.string.connection_sound_description),
            themeIcon = ThemeIconEnum.SettingsConnectionSound,
            default = AppConfig.connectionSound,
        ) {
            AppConfig.connectionSound = it
        }

        sliderPref(
            name = stringResource(R.string.no_connection_sound_title),
            desc = stringResource(R.string.no_connection_sound_description),
            min = 0f,
            max = 60f,
            unit = R.string.sec,
            position = AppConfig.noConnectionSound.toFloat(),
            showSwitch = true,
            disableSwitchAtMin = true,
        ) {
            AppConfig.noConnectionSound = it.toInt()
        }

        switchPref(
            name = stringResource(R.string.use_stop_music_title),
            desc = stringResource(R.string.use_stop_music_description),
            themeIcon = ThemeIconEnum.SettingsAutoMute,
            default = AppConfig.useStopMusic,
        ) {
            AppConfig.useStopMusic = it
        }

        switchPref(
            name = stringResource(R.string.show_unknown_devices_title),
            desc = stringResource(R.string.show_unknown_devices_description),
            default = AppConfig.showUnknownDevices,
        ) {
            AppConfig.showUnknownDevices = it
        }

        switchPref(
            name = stringResource(R.string.use_reconnect_title),
            desc = stringResource(R.string.use_reconnect_description),
            default = AppConfig.useReconnect,
            showDiv = false,
        ) {
            AppConfig.useReconnect = it
        }

        group(
            name = stringResource(R.string.beep_category),
        ) {
            switchPref(
                name = stringResource(R.string.beep_on_single_tap_title),
                desc = stringResource(R.string.beep_on_single_tap_description),
                default = AppConfig.useBeepOnSingleTap,
            ) {
                AppConfig.useBeepOnSingleTap = it
            }

            switchPref(
                name = stringResource(R.string.beep_on_volume_up_title),
                desc = stringResource(R.string.beep_on_volume_up_description),
                default = AppConfig.useBeepOnVolumeUp,
            ) {
                AppConfig.useBeepOnVolumeUp = it
            }

            var beepByWheel by remember { mutableStateOf(AppConfig.beepByWheel) }
            switchPref(
                name = stringResource(R.string.beep_by_wheel_title),
                desc = stringResource(R.string.beep_by_wheel_description),
                default = AppConfig.beepByWheel,
            ) {
                AppConfig.beepByWheel = it
                beepByWheel = it
            }

            var useCustomBeep by remember { mutableStateOf(AppConfig.useCustomBeep) }
            AnimatedVisibility(!beepByWheel) {
                // var showBeepDialog by remember { mutableStateOf(false) }
                Column {
                    switchPref(
                        name = stringResource(R.string.use_custom_beep_title),
                        default = AppConfig.useCustomBeep,
                        showDiv = false,
                    ) {
                        AppConfig.useCustomBeep = it
                        useCustomBeep = it
                    }

                    AnimatedVisibility (useCustomBeep) {
                        var ringtone by remember { mutableStateOf(AppConfig.beepFile) }
                        Column {
                            alarmsList(
                                name = stringResource(R.string.custom_beep_title),
                                default = ringtone,
                            ) {
                                AppConfig.beepFile = it.second
                                ringtone = it.second
                            }
                            sliderPref(
                                name = stringResource(R.string.beep_time_limit_title),
                                min = 0.5f,
                                max = 20f,
                                unit = R.string.sec,
                                position = AppConfig.customBeepTimeLimit,
                                format = "%.1f",
                                showDiv = false,
                            ) {
                                AppConfig.customBeepTimeLimit = it
                            }
                        }
                    }
                }
            }
        }

        switchPref(
            name = stringResource(R.string.use_detect_battery_optimization_title),
            default = AppConfig.detectBatteryOptimization,
        ) {
            AppConfig.detectBatteryOptimization = it
        }

        switchPref(
            name = stringResource(R.string.send_yandex_metriсa_title),
            desc = stringResource(R.string.send_yandex_metriсa_description),
            default = AppConfig.yandexMetricaAccepted,
        ) {
            AppConfig.yandexMetricaAccepted = it
        }
    }
}