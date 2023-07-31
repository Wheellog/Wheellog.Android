package com.cooper.wheellog.preferences

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.ThemeIconEnum

@Composable
fun ApplicationScreen(
    // onSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsGroup(name = R.string.speed_settings_title) {
            SettingsSwitchComp(
                name = R.string.use_eng_title,
                desc = R.string.use_eng_description,
                themeIcon = ThemeIconEnum.SettingsLanguage,
                isChecked = WheelLog.AppConfig.useEng
            ) {
                WheelLog.AppConfig.useEng = it
            }
            Divider()
            SettingsListComp(
                name = R.string.app_theme_title,
                desc = R.string.app_theme_description,
                entries = ThemeEnum.values().associate { it.value.toString() to it.name },
                selectedKey = WheelLog.AppConfig.appThemeInt.toString()
            ) {
                WheelLog.AppConfig.appThemeInt = it.first.toInt()
            }
            Divider()
            SettingsListComp(
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
        }
        SettingsSwitchComp(
            name = R.string.use_better_percents_title,
            desc = R.string.use_better_percents_description,
            isChecked = WheelLog.AppConfig.useBetterPercents
        ) {
            WheelLog.AppConfig.useBetterPercents = it
        }
        var customPercents by remember { mutableStateOf(WheelLog.AppConfig.customPercents) }
        SettingsSwitchComp(
            name = R.string.custom_percents_title,
            desc = R.string.custom_percents_description,
            isChecked = WheelLog.AppConfig.customPercents
        ) {
            WheelLog.AppConfig.customPercents = it
            customPercents = it
        }
        if (customPercents) {
            SettingsSliderComp(
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
        SettingsGroup(name = R.string.measurement_systems_category_title) {
            SettingsSwitchComp(
                name = R.string.use_mph_title,
                desc = R.string.use_mph_description,
                isChecked = WheelLog.AppConfig.useMph
            ) {
                WheelLog.AppConfig.useMph = it
            }
            SettingsSwitchComp(
                name = R.string.use_fahrenheit_title,
                desc = R.string.use_fahrenheit_description,
                isChecked = WheelLog.AppConfig.useFahrenheit,
            ) {
                WheelLog.AppConfig.useFahrenheit = it
            }
        }
        SettingsGroup(name = R.string.after_connect_category) {
            SettingsSwitchComp(
                name = R.string.auto_log_title,
                desc = R.string.auto_log_description,
                themeIcon = ThemeIconEnum.SettingsAutoLog,
                isChecked = WheelLog.AppConfig.autoLog,
            ) {
                WheelLog.AppConfig.autoLog = it
            }
            SettingsSwitchComp(
                name = R.string.auto_watch_title,
                desc = R.string.auto_watch_description,
                themeIcon = ThemeIconEnum.SettingsWatch,
                isChecked = WheelLog.AppConfig.autoWatch,
            ) {
                WheelLog.AppConfig.autoWatch = it
            }
        }
        SettingsGroup(name = R.string.main_view_category) {
            // TODO: create a new multi-select component
            SettingsListComp(
                name = R.string.view_blocks_title,
                desc = R.string.view_blocks_description,
                themeIcon = ThemeIconEnum.SettingsBlocks,
                entries = mapOf(
                    "0" to stringResource(R.string.pwm),
                    "1" to stringResource(R.string.max_pwm),
                    "2" to stringResource(R.string.voltage),
                    "3" to stringResource(R.string.battery),
                    "4" to stringResource(R.string.top_speed),
                    "5" to stringResource(R.string.average_riding_speed),
                    "6" to stringResource(R.string.average_speed),
                    "7" to stringResource(R.string.riding_time),
                    "8" to stringResource(R.string.ride_time),
                    "9" to stringResource(R.string.current),
                    "10" to stringResource(R.string.maxcurrent),
                    "11" to stringResource(R.string.power),
                    "12" to stringResource(R.string.maxpower),
                    "13" to stringResource(R.string.temperature),
                    "14" to stringResource(R.string.temperature2),
                    "15" to stringResource(R.string.maxtemperature),
                    "16" to stringResource(R.string.distance),
                    "17" to stringResource(R.string.total),
                    "18" to stringResource(R.string.wheel_distance),
                    "19" to stringResource(R.string.remaining_distance),
                    "20" to stringResource(R.string.battery_per_km),
                    "21" to stringResource(R.string.consumption),
                    "22" to stringResource(R.string.avg_cell_volt),
                    "23" to stringResource(R.string.user_distance),
                    "24" to stringResource(R.string.speed),
                ),
                selectedKey = WheelLog.AppConfig.viewBlocks.toString()
            ) {
                // WheelLog.AppConfig.viewBlocks = it
            }
            var usePipMode by remember { mutableStateOf(WheelLog.AppConfig.usePipMode) }
            SettingsSwitchComp(
                name = R.string.use_pip_mode_title,
                desc = R.string.use_pip_mode_description,
                isChecked = WheelLog.AppConfig.usePipMode,
            ) {
                WheelLog.AppConfig.usePipMode = it
                usePipMode = it
            }
            if (usePipMode) {
                SettingsListComp(
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
        }

//        SettingsGroup(name = R.string.alarm_settings_title) {
//            SettingsSliderComp(
//                name = R.string.speed_alarm1_phone_title,
//                themeIcon = ThemeIconEnum.TripsMap,
//                desc = R.string.alarm1_description,
//            ) {
//
//            }
//        }
    }
}