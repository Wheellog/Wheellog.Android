package com.cooper.wheellog.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.ThemeIconEnum

@Composable
fun applicationScreen() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        switchPref(
            name = R.string.use_eng_title,
            desc = R.string.use_eng_description,
            themeIcon = ThemeIconEnum.SettingsLanguage,
            isChecked = WheelLog.AppConfig.useEng
        ) {
            WheelLog.AppConfig.useEng = it
        }

        list(
            name = R.string.app_theme_title,
            desc = R.string.app_theme_description,
            entries = ThemeEnum.values().associate { it.value.toString() to it.name },
            selectedKey = WheelLog.AppConfig.appThemeInt.toString()
        ) {
            WheelLog.AppConfig.appThemeInt = it.first.toInt()
        }

        list(
            name = R.string.day_night_theme_title,
            entries = mapOf(
                AppCompatDelegate.MODE_NIGHT_UNSPECIFIED.toString() to stringResource(R.string.day_night_theme_as_system),
                AppCompatDelegate.MODE_NIGHT_NO.toString() to stringResource(R.string.day_night_theme_day),
                AppCompatDelegate.MODE_NIGHT_YES.toString() to stringResource(R.string.day_night_theme_night),
            ),
            selectedKey = WheelLog.AppConfig.dayNightThemeMode.toString()
        ) {
            WheelLog.AppConfig.dayNightThemeMode = it.first.toInt()
        }

        switchPref(
            name = R.string.use_better_percents_title,
            desc = R.string.use_better_percents_description,
            isChecked = WheelLog.AppConfig.useBetterPercents
        ) {
            WheelLog.AppConfig.useBetterPercents = it
        }

        var customPercents by remember { mutableStateOf(WheelLog.AppConfig.customPercents) }
        switchPref(
            name = R.string.custom_percents_title,
            desc = R.string.custom_percents_description,
            isChecked = WheelLog.AppConfig.customPercents
        ) {
            WheelLog.AppConfig.customPercents = it
            customPercents = it
        }
        if (customPercents) {
            sliderPref(
                name = R.string.cell_voltage_tiltback_title,
                desc = R.string.cell_voltage_tiltback_description,
                position = WheelLog.AppConfig.cellVoltageTiltback / 100f,
                min = 2.5f,
                max = 4f,
                unit = R.string.volt,
                format = "%.2f",
            ) {
                WheelLog.AppConfig.cellVoltageTiltback = (it * 100).toInt()
                // currentRecomposeScope.invalidate()
            }
        }

        group(name = R.string.measurement_systems_category_title) {

            switchPref(
                name = R.string.use_mph_title,
                desc = R.string.use_mph_description,
                isChecked = WheelLog.AppConfig.useMph
            ) {
                WheelLog.AppConfig.useMph = it
            }

            switchPref(
                name = R.string.use_fahrenheit_title,
                desc = R.string.use_fahrenheit_description,
                isChecked = WheelLog.AppConfig.useFahrenheit,
            ) {
                WheelLog.AppConfig.useFahrenheit = it
            }
        }

        group(name = R.string.after_connect_category) {

            switchPref(
                name = R.string.auto_log_title,
                desc = R.string.auto_log_description,
                themeIcon = ThemeIconEnum.SettingsAutoLog,
                isChecked = WheelLog.AppConfig.autoLog,
            ) {
                WheelLog.AppConfig.autoLog = it
            }

            switchPref(
                name = R.string.auto_watch_title,
                desc = R.string.auto_watch_description,
                themeIcon = ThemeIconEnum.SettingsWatch,
                isChecked = WheelLog.AppConfig.autoWatch,
            ) {
                WheelLog.AppConfig.autoWatch = it
            }
        }

        group(name = R.string.main_view_category) {

            multiList(
                name = R.string.view_blocks_title,
                desc = R.string.view_blocks_description,
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
                selectedKeys = WheelLog.AppConfig.viewBlocks.toList(),
                useSort = true,
            ) {
                WheelLog.AppConfig.viewBlocks = it.toTypedArray()
            }
            var usePipMode by remember { mutableStateOf(WheelLog.AppConfig.usePipMode) }

            switchPref(
                name = R.string.use_pip_mode_title,
                desc = R.string.use_pip_mode_description,
                isChecked = WheelLog.AppConfig.usePipMode,
            ) {
                WheelLog.AppConfig.usePipMode = it
                usePipMode = it
            }
            if (usePipMode) {
                list(
                    name = R.string.pip_block_title,
                    entries = mapOf(
                        "0" to stringResource(R.string.speed),
                        "1" to stringResource(R.string.consumption),
                    ),
                    selectedKey = WheelLog.AppConfig.pipBlock,
                ) {
                    WheelLog.AppConfig.pipBlock = it.first
                }
            }

            multiList(
                name = R.string.notification_buttons_title,
                desc = R.string.notification_buttons_description,
                themeIcon = ThemeIconEnum.SettingsNotification,
                entries = mapOf(
                    stringResource(R.string.icon_connection) to stringResource(R.string.icon_connection),
                    stringResource(R.string.icon_logging) to stringResource(R.string.icon_logging),
                    stringResource(R.string.icon_watch) to stringResource(R.string.icon_watch),
                    stringResource(R.string.icon_beep) to stringResource(R.string.icon_beep),
                    stringResource(R.string.icon_light) to stringResource(R.string.icon_light),
                    stringResource(R.string.icon_miband) to stringResource(R.string.icon_miband),
                ),
                selectedKeys = WheelLog.AppConfig.notificationButtons.toList()
            ) {
                WheelLog.AppConfig.notificationButtons = it.toTypedArray()
            }

            sliderPref(
                name = R.string.max_speed_dial_title,
                desc = R.string.max_speed_dial_description,
                min = 10f,
                max = 100f,
                position = WheelLog.AppConfig.maxSpeed.toFloat(),
            ) {
                WheelLog.AppConfig.maxSpeed = it.toInt()
            }

            switchPref(
                name = R.string.current_on_dial_title,
                desc = R.string.current_on_dial_description,
                isChecked = WheelLog.AppConfig.currentOnDial,
            ) {
                WheelLog.AppConfig.currentOnDial = it
            }

            switchPref(
                name = R.string.use_short_pwm_title,
                desc = R.string.use_short_pwm_description,
                isChecked = WheelLog.AppConfig.useShortPwm,
            ) {
                WheelLog.AppConfig.useShortPwm = it
            }
        }

        switchPref(
            name = R.string.show_page_graph_title,
            isChecked = WheelLog.AppConfig.pageGraph,
        ) {
            WheelLog.AppConfig.pageGraph = it
        }

        switchPref(
            name = R.string.show_page_events_title,
            desc = R.string.show_page_events_description,
            themeIcon = ThemeIconEnum.SettingsPageEvents,
            isChecked = WheelLog.AppConfig.pageEvents,
        ) {
            WheelLog.AppConfig.pageEvents = it
        }

        switchPref(
            name = R.string.show_page_trips_title,
            themeIcon = ThemeIconEnum.SettingsPageTrips,
            isChecked = WheelLog.AppConfig.pageTrips,
        ) {
            WheelLog.AppConfig.pageTrips = it
        }

        switchPref(
            name = R.string.connection_sound_title,
            desc = R.string.connection_sound_description,
            themeIcon = ThemeIconEnum.SettingsConnectionSound,
            isChecked = WheelLog.AppConfig.connectionSound,
        ) {
            WheelLog.AppConfig.connectionSound = it
        }

        sliderPref(
            name = R.string.no_connection_sound_title,
            desc = R.string.no_connection_sound_description,
            min = 0f,
            max = 60f,
            unit = R.string.sec,
            position = WheelLog.AppConfig.noConnectionSound.toFloat(),
        ) {
            WheelLog.AppConfig.noConnectionSound = it.toInt()
        }

        switchPref(
            name = R.string.use_stop_music_title,
            desc = R.string.use_stop_music_description,
            themeIcon = ThemeIconEnum.SettingsAutoMute,
            isChecked = WheelLog.AppConfig.useStopMusic,
        ) {
            WheelLog.AppConfig.useStopMusic = it
        }

        switchPref(
            name = R.string.show_unknown_devices_title,
            desc = R.string.show_unknown_devices_description,
            isChecked = WheelLog.AppConfig.showUnknownDevices,
        ) {
            WheelLog.AppConfig.showUnknownDevices = it
        }

        switchPref(
            name = R.string.use_reconnect_title,
            desc = R.string.use_reconnect_description,
            isChecked = WheelLog.AppConfig.useReconnect,
        ) {
            WheelLog.AppConfig.useReconnect = it
        }

        group(
            name = R.string.beep_category,
        ) {
            switchPref(
                name = R.string.beep_on_single_tap_title,
                desc = R.string.beep_on_single_tap_description,
                isChecked = WheelLog.AppConfig.useBeepOnSingleTap,
            ) {
                WheelLog.AppConfig.useBeepOnSingleTap = it
            }

            switchPref(
                name = R.string.beep_on_volume_up_title,
                desc = R.string.beep_on_volume_up_description,
                isChecked = WheelLog.AppConfig.useBeepOnVolumeUp,
            ) {
                WheelLog.AppConfig.useBeepOnVolumeUp = it
            }

            var beepByWheel by remember { mutableStateOf(WheelLog.AppConfig.beepByWheel) }
            switchPref(
                name = R.string.beep_by_wheel_title,
                desc = R.string.beep_by_wheel_description,
                isChecked = WheelLog.AppConfig.beepByWheel,
            ) {
                WheelLog.AppConfig.beepByWheel = it
                beepByWheel = it
            }

            if (!beepByWheel) {
                switchPref(
                    name = R.string.custom_beep_title,
                    isChecked = WheelLog.AppConfig.useCustomBeep,
                ) {
                    WheelLog.AppConfig.useCustomBeep = it
                }
            }
        }

        switchPref(
            name = R.string.use_detect_battery_optimization_title,
            isChecked = WheelLog.AppConfig.detectBatteryOptimization,
        ) {
            WheelLog.AppConfig.detectBatteryOptimization = it
        }

        switchPref(
            name = R.string.send_yandex_metriсa_title,
            desc = R.string.send_yandex_metriсa_description,
            isChecked = WheelLog.AppConfig.yandexMetricaAccepted,
        ) {
            WheelLog.AppConfig.yandexMetricaAccepted = it
        }
    }
}