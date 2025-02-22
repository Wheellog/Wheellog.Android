package com.cooper.wheellog.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.NotificationUtil
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.ThemeIconEnum
import org.koin.compose.koinInject

@Composable
fun applicationScreen(
    appConfig: AppConfig = koinInject(),
    notifications: NotificationUtil = koinInject()) {
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
            default = appConfig.useEng
        ) {
            restartRequiredAlert = true
            appConfig.useEng = it
        }

        list(
            name = stringResource(R.string.app_theme_title),
            desc = stringResource(R.string.app_theme_description),
            entries = ThemeEnum.values().associate { it.value.toString() to it.name },
            defaultKey = appConfig.appThemeInt.toString()
        ) {
            restartRequiredAlert = true
            appConfig.appThemeInt = it.first.toInt()
        }

        list(
            name = stringResource(R.string.day_night_theme_title),
            entries = mapOf(
                AppCompatDelegate.MODE_NIGHT_UNSPECIFIED.toString() to stringResource(R.string.day_night_theme_as_system),
                AppCompatDelegate.MODE_NIGHT_NO.toString() to stringResource(R.string.day_night_theme_day),
                AppCompatDelegate.MODE_NIGHT_YES.toString() to stringResource(R.string.day_night_theme_night),
            ),
            defaultKey = appConfig.dayNightThemeMode.toString()
        ) {
            appConfig.dayNightThemeMode = it.first.toInt()
        }

        switchPref(
            name = stringResource(R.string.use_better_percents_title),
            desc = stringResource(R.string.use_better_percents_description),
            default = appConfig.useBetterPercents
        ) {
            appConfig.useBetterPercents = it
        }

        var customPercents by remember { mutableStateOf(appConfig.customPercents) }
        switchPref(
            name = stringResource(R.string.custom_percents_title),
            desc = stringResource(R.string.custom_percents_description),
            default = appConfig.customPercents,
            showDiv = customPercents,
        ) {
            appConfig.customPercents = it
            customPercents = it
        }
        AnimatedVisibility(visible = customPercents) {
            sliderPref(
                name = stringResource(R.string.cell_voltage_tiltback_title),
                desc = stringResource(R.string.cell_voltage_tiltback_description),
                position = appConfig.cellVoltageTiltback / 100f,
                min = 2.5f,
                max = 4f,
                unit = R.string.volt,
                format = "%.2f",
                showDiv = false,
            ) {
                appConfig.cellVoltageTiltback = (it * 100).toInt()
                // currentRecomposeScope.invalidate()
            }
        }

        group(name = stringResource(R.string.measurement_systems_category_title)) {

            switchPref(
                name = stringResource(R.string.use_mph_title),
                desc = stringResource(R.string.use_mph_description),
                default = appConfig.useMph
            ) {
                appConfig.useMph = it
            }

            switchPref(
                name = stringResource(R.string.use_fahrenheit_title),
                desc = stringResource(R.string.use_fahrenheit_description),
                default = appConfig.useFahrenheit,
                showDiv = false,
            ) {
                appConfig.useFahrenheit = it
            }
        }

        group(name = stringResource(R.string.after_connect_category)) {

            switchPref(
                name = stringResource(R.string.auto_log_title),
                desc = stringResource(R.string.auto_log_description),
                themeIcon = ThemeIconEnum.SettingsAutoLog,
                default = appConfig.autoLog,
            ) {
                appConfig.autoLog = it
            }

            switchPref(
                name = stringResource(R.string.auto_watch_title),
                desc = stringResource(R.string.auto_watch_description),
                themeIcon = ThemeIconEnum.SettingsWatch,
                default = appConfig.autoWatch,
                showDiv = false,
            ) {
                appConfig.autoWatch = it
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
                defaultKeys = appConfig.viewBlocks.toList(),
                useSort = true,
            ) {
                appConfig.viewBlocks = it.toTypedArray()
            }
            var usePipMode by remember { mutableStateOf(appConfig.usePipMode) }

            switchPref(
                name = stringResource(R.string.use_pip_mode_title),
                desc = stringResource(R.string.use_pip_mode_description),
                default = appConfig.usePipMode,
            ) {
                appConfig.usePipMode = it
                usePipMode = it
            }
            AnimatedVisibility(visible = usePipMode) {
                list(
                    name = stringResource(R.string.pip_block_title),
                    entries = mapOf(
                        "0" to stringResource(R.string.speed),
                        "1" to stringResource(R.string.consumption),
                    ),
                    defaultKey = appConfig.pipBlock,
                ) {
                    appConfig.pipBlock = it.first
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
                defaultKeys = appConfig.notificationButtons.toList()
            ) {
                appConfig.notificationButtons = it.toTypedArray()
                notifications.update()
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
                defaultKeys = appConfig.mainMenuButtons.toList()
            ) {
                appConfig.mainMenuButtons = it.toTypedArray()
            }

            switchPref(
                name = stringResource(R.string.show_clock_title),
                default = appConfig.showClock,
            ) {
                appConfig.showClock = it
            }

            var valueOnDial by remember { mutableStateOf(appConfig.valueOnDial) }
            list(
                name = stringResource(R.string.value_on_dial_title),
                desc = stringResource(R.string.value_on_dial_description),
                entries = mapOf(
                    "0" to stringResource(R.string.speed),
                    "1" to stringResource(R.string.current),
                    "2" to stringResource(R.string.pwm),
                ),
                defaultKey = valueOnDial,
            ) {
                valueOnDial = it.first
                appConfig.valueOnDial = valueOnDial
                if (valueOnDial == "2")
                    appConfig.maxSpeed = 100
            }

            val pwmDial = (valueOnDial == "2")
            AnimatedVisibility(visible = !pwmDial) {
                sliderPref(
                    name = stringResource(R.string.max_speed_dial_title),
                    desc = stringResource(R.string.max_speed_dial_description),
                    min = 10f,
                    max = 100f,
                    position = appConfig.maxSpeed.toFloat(),
                ) {
                    appConfig.maxSpeed = it.toInt()
                }
            }

            var shortPwm by remember { mutableStateOf(appConfig.useShortPwm) }
            switchPref(
                name = stringResource(R.string.use_short_pwm_title),
                desc = stringResource(R.string.use_short_pwm_description),
                default = shortPwm,
                showDiv = (shortPwm || pwmDial),
            ) {
                shortPwm = it
                appConfig.useShortPwm = shortPwm
                if (!shortPwm)
                    appConfig.swapSpeedPwm = false
            }

            AnimatedVisibility(visible = shortPwm) {
                switchPref(
                    name = stringResource(R.string.swap_speedpwm_title),
                    desc = stringResource(R.string.swap_speedpwm_description),
                    default = appConfig.swapSpeedPwm,
                ) {
                    appConfig.swapSpeedPwm = it
                }
            }

            var colorPwmStart by remember { mutableStateOf(appConfig.colorPwmStart.toFloat()) }
            var colorPwmEnd by remember { mutableStateOf(appConfig.colorPwmEnd.toFloat()) }
            AnimatedVisibility(visible = (shortPwm || pwmDial)) {
                sliderPref(
                    name = stringResource(R.string.color_pwm_start_title),
                    desc = stringResource(R.string.color_pwm_start_description),
                    min = 0f,
                    max = 100f,
                    position = colorPwmStart,
                    showDiv = false,
                    unit = R.string.persent,
                ) {
                    colorPwmStart = it

                    if (colorPwmStart > colorPwmEnd) {
                        colorPwmEnd = colorPwmStart
                    }

                    appConfig.colorPwmStart = colorPwmStart.toInt()
                    appConfig.colorPwmEnd = colorPwmEnd.toInt()
                }
            }

            AnimatedVisibility(visible = (shortPwm || pwmDial)) {
                sliderPref(
                    name = stringResource(R.string.color_pwm_end_title),
                    desc = stringResource(R.string.color_pwm_end_description),
                    min = 0f,
                    max = 100f,
                    position = colorPwmEnd,
                    showDiv = false,
                    unit = R.string.persent,
                ) {
                    colorPwmEnd = it

                    if (colorPwmEnd < colorPwmStart) {
                        colorPwmStart = colorPwmEnd
                    }

                    appConfig.colorPwmStart = colorPwmStart.toInt()
                    appConfig.colorPwmEnd = colorPwmEnd.toInt()
                }
            }
        }

        switchPref(
            name = stringResource(R.string.show_page_graph_title),
            default = appConfig.pageGraph,
        ) {
            appConfig.pageGraph = it
        }

        switchPref(
            name = stringResource(R.string.show_page_events_title),
            desc = stringResource(R.string.show_page_events_description),
            themeIcon = ThemeIconEnum.SettingsPageEvents,
            default = appConfig.pageEvents,
        ) {
            appConfig.pageEvents = it
        }

        switchPref(
            name = stringResource(R.string.show_page_trips_title),
            themeIcon = ThemeIconEnum.SettingsPageTrips,
            default = appConfig.pageTrips,
        ) {
            appConfig.pageTrips = it
        }

        switchPref(
            name = stringResource(R.string.connection_sound_title),
            desc = stringResource(R.string.connection_sound_description),
            themeIcon = ThemeIconEnum.SettingsConnectionSound,
            default = appConfig.connectionSound,
        ) {
            appConfig.connectionSound = it
        }

        sliderPref(
            name = stringResource(R.string.no_connection_sound_title),
            desc = stringResource(R.string.no_connection_sound_description),
            min = 0f,
            max = 60f,
            unit = R.string.sec,
            position = appConfig.noConnectionSound.toFloat(),
            showSwitch = true,
            disableSwitchAtMin = true,
        ) {
            appConfig.noConnectionSound = it.toInt()
        }

        switchPref(
            name = stringResource(R.string.use_stop_music_title),
            desc = stringResource(R.string.use_stop_music_description),
            themeIcon = ThemeIconEnum.SettingsAutoMute,
            default = appConfig.useStopMusic,
        ) {
            appConfig.useStopMusic = it
        }

        switchPref(
            name = stringResource(R.string.show_unknown_devices_title),
            desc = stringResource(R.string.show_unknown_devices_description),
            default = appConfig.showUnknownDevices,
        ) {
            appConfig.showUnknownDevices = it
        }

        switchPref(
            name = stringResource(R.string.use_reconnect_title),
            desc = stringResource(R.string.use_reconnect_description),
            default = appConfig.useReconnect,
            showDiv = false,
        ) {
            appConfig.useReconnect = it
        }

        group(
            name = stringResource(R.string.beep_category),
        ) {
            switchPref(
                name = stringResource(R.string.beep_on_single_tap_title),
                desc = stringResource(R.string.beep_on_single_tap_description),
                default = appConfig.useBeepOnSingleTap,
            ) {
                appConfig.useBeepOnSingleTap = it
            }

            switchPref(
                name = stringResource(R.string.beep_on_volume_up_title),
                desc = stringResource(R.string.beep_on_volume_up_description),
                default = appConfig.useBeepOnVolumeUp,
            ) {
                appConfig.useBeepOnVolumeUp = it
            }

            var beepByWheel by remember { mutableStateOf(appConfig.beepByWheel) }
            switchPref(
                name = stringResource(R.string.beep_by_wheel_title),
                desc = stringResource(R.string.beep_by_wheel_description),
                default = appConfig.beepByWheel,
            ) {
                appConfig.beepByWheel = it
                beepByWheel = it
            }

            var useCustomBeep by remember { mutableStateOf(appConfig.useCustomBeep) }
            AnimatedVisibility(!beepByWheel) {
                // var showBeepDialog by remember { mutableStateOf(false) }
                Column {
                    switchPref(
                        name = stringResource(R.string.use_custom_beep_title),
                        default = appConfig.useCustomBeep,
                        showDiv = false,
                    ) {
                        appConfig.useCustomBeep = it
                        useCustomBeep = it
                    }

                    AnimatedVisibility (useCustomBeep) {
                        var ringtone by remember { mutableStateOf(appConfig.beepFile) }
                        Column {
                            alarmsList(
                                name = stringResource(R.string.custom_beep_title),
                                default = ringtone,
                            ) {
                                appConfig.beepFile = it.second
                                ringtone = it.second
                            }
                            sliderPref(
                                name = stringResource(R.string.beep_time_limit_title),
                                min = 0.5f,
                                max = 20f,
                                unit = R.string.sec,
                                position = appConfig.customBeepTimeLimit,
                                format = "%.1f",
                                showDiv = false,
                            ) {
                                appConfig.customBeepTimeLimit = it
                            }
                        }
                    }
                }
            }
        }

        switchPref(
            name = stringResource(R.string.use_detect_battery_optimization_title),
            default = appConfig.detectBatteryOptimization,
        ) {
            appConfig.detectBatteryOptimization = it
        }

        switchPref(
            name = stringResource(R.string.send_yandex_metriсa_title),
            desc = stringResource(R.string.send_yandex_metriсa_description),
            default = appConfig.yandexMetricaAccepted,
        ) {
            appConfig.yandexMetricaAccepted = it
        }
    }
}