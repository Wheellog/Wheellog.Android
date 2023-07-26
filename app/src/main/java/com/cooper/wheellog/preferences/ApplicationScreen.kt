package com.cooper.wheellog.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeIconEnum

@Composable
fun ApplicationScreen(
    // onSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
)
{
            Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsGroup(name = R.string.speed_settings_title) {
                //var useEng by remember { mutableStateOf(WheelLog.AppConfig.useEng) }
                SettingsSwitchComp(
                    name = R.string.use_eng_title,
                    desc = R.string.use_eng_description,
                    themeIcon = ThemeIconEnum.SettingsLanguage,
                    isChecked = WheelLog.AppConfig.useEng
                ) {
                    WheelLog.AppConfig.useEng = it
                }
                Divider()
                SettingsClickableComp(
                    name = R.string.app_theme_title,
                    desc = R.string.app_theme_description,
                ) {

                }
            }
            SettingsGroup(name = R.string.alarm_settings_title) {
                SettingsSliderComp(
                    name = R.string.speed_alarm1_phone_title,
                    themeIcon = ThemeIconEnum.TripsMap,
                    desc = R.string.alarm1_description,
                ) {
                    // here you can do anything - navigate - open other settings, ...
                }
            }
        }
}