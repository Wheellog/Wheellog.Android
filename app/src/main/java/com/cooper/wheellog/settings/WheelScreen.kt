package com.cooper.wheellog.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog.Companion.AppConfig
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.NinebotZAdapter
import com.cooper.wheellog.utils.ThemeIconEnum

@Composable
fun wheelScreen()
{
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        when (WheelData.getInstance().wheelType) {
            Constants.WHEEL_TYPE.NINEBOT_Z -> ninebotZ()
            Constants.WHEEL_TYPE.INMOTION -> inmotion()
            Constants.WHEEL_TYPE.INMOTION_V2 -> inmotionV2()
            Constants.WHEEL_TYPE.KINGSONG -> kingsong()
            Constants.WHEEL_TYPE.GOTWAY -> begode()
            Constants.WHEEL_TYPE.VETERAN -> veteran()
            else -> baseSettings(
                name = R.string.unknown_device,
            )
        }
        if (WheelData.getInstance().wheelType != Constants.WHEEL_TYPE.Unknown) {
            forAllWheel()
        }
    }
}

@Composable
private fun ninebotZ() {
    switchPref(
        name = R.string.on_headlight_title,
        desc = R.string.on_headlight_description,
        default = AppConfig.lightEnabled,
    ) {
        AppConfig.lightEnabled = it
    }
    switchPref(
        name = R.string.drl_settings_title,
        desc = R.string.drl_settings_description_nb,
        default = AppConfig.drlEnabled,
    ) {
        AppConfig.drlEnabled = it
    }
    switchPref(
        name = R.string.taillight_settings_title,
        desc = R.string.taillight_settings_description,
        default = AppConfig.taillightEnabled,
    ) {
        AppConfig.taillightEnabled = it
    }
    switchPref(
        name = R.string.disable_handle_button_title,
        desc = R.string.disable_handle_button_description,
        default = AppConfig.handleButtonDisabled,
    ) {
        AppConfig.handleButtonDisabled = it
    }
    var alarm1 by remember { mutableStateOf(AppConfig.wheelAlarm1Enabled) }
    switchPref(
        name = R.string.wheel_alarm1_enabled_title,
        desc = R.string.wheel_alarm1_enabled_description,
        default = AppConfig.wheelAlarm1Enabled,
    ) {
        AppConfig.wheelAlarm1Enabled = it
        alarm1 = it
    }
    if (alarm1) {
        sliderPref(
            name = R.string.wheel_alarm1_title,
            desc = R.string.wheel_alarm1_description,
            position = AppConfig.wheelAlarm1Speed.toFloat(),
            min = 0f,
            max = NinebotZAdapter.getInstance().wheelAlarmMax.toFloat(),
            unit = R.string.kmh,
        ) {
            AppConfig.wheelAlarm1Speed = it.toInt()
        }
    }
    var alarm2 by remember { mutableStateOf(AppConfig.wheelAlarm2Enabled) }
    switchPref(
        name = R.string.wheel_alarm2_enabled_title,
        desc = R.string.wheel_alarm2_enabled_description,
        default = AppConfig.wheelAlarm2Enabled,
    ) {
        AppConfig.wheelAlarm2Enabled = it
        alarm2 = it
    }
    if (alarm2) {
        sliderPref(
            name = R.string.wheel_alarm2_title,
            desc = R.string.wheel_alarm2_description,
            position = AppConfig.wheelAlarm2Speed.toFloat(),
            min = 0f,
            max = NinebotZAdapter.getInstance().wheelAlarmMax.toFloat(),
            unit = R.string.kmh,
        ) {
            AppConfig.wheelAlarm2Speed = it.toInt()
        }
    }
    var alarm3 by remember { mutableStateOf(AppConfig.wheelAlarm3Enabled) }
    switchPref(
        name = R.string.wheel_alarm3_enabled_title,
        desc = R.string.wheel_alarm3_enabled_description,
        default = AppConfig.wheelAlarm3Enabled,
    ) {
        AppConfig.wheelAlarm3Enabled = it
        alarm3 = it
    }
    if (alarm3) {
        sliderPref(
            name = R.string.wheel_alarm3_title,
            desc = R.string.wheel_alarm3_description,
            position = AppConfig.wheelAlarm3Speed.toFloat(),
            min = 0f,
            max = NinebotZAdapter.getInstance().wheelAlarmMax.toFloat(),
            unit = R.string.kmh,
        ) {
            AppConfig.wheelAlarm3Speed = it.toInt()
        }
    }
    var limitedMode by remember { mutableStateOf(AppConfig.wheelLimitedModeEnabled) }
    switchPref(
        name = R.string.wheel_limited_mode_title,
        desc = R.string.wheel_limited_mode_description,
        default = AppConfig.wheelLimitedModeEnabled,
    ) {
        AppConfig.wheelLimitedModeEnabled = it
        limitedMode = it
    }
    if (limitedMode) {
        sliderPref(
            name = R.string.wheel_limited_speed_title,
            desc = R.string.wheel_limited_speed_description,
            position = AppConfig.wheelLimitedModeSpeed.toFloat() / 10f,
            min = 0f,
            max = 65.5f,
            unit = R.string.kmh,
            format = "%.1f",
        ) {
            AppConfig.wheelLimitedModeSpeed = it.toInt() * 10
        }
        switchPref(
            name = R.string.brake_assistant_title,
            desc = R.string.brake_assistant_description,
            default = AppConfig.brakeAssistantEnabled,
        ) {
            AppConfig.brakeAssistantEnabled = it
        }
        sliderPref(
            name = R.string.pedal_sensivity_title,
            desc = R.string.pedal_sensivity_description,
            position = AppConfig.pedalSensivity.toFloat(),
            min = 0f,
            max = 4f,
        ) {
            AppConfig.pedalSensivity = it.toInt()
        }
        list(
            name = R.string.led_mode_title,
            entries = mapOf(
                "0" to stringResource(R.string.off),
                "1" to stringResource(R.string.led_type1),
                "2" to stringResource(R.string.led_type2),
                "3" to stringResource(R.string.led_type3),
                "4" to stringResource(R.string.led_type4),
                "5" to stringResource(R.string.led_type5),
                "6" to stringResource(R.string.led_type6),
                "7" to stringResource(R.string.led_type7),
            ),
            defaultKey = AppConfig.ledMode,
        ) {
            AppConfig.ledMode = it.first
        }
        if (NinebotZAdapter.getInstance().getLedIsAvailable(1)) {
            sliderPref(
                name = R.string.nb_led_color1_title,
                desc = R.string.nb_led_color_description,
                position = AppConfig.ledColor1.toFloat(),
                min = 0f,
                max = 256f,
            ) {
                AppConfig.ledColor1 = it.toInt()
            }
        }
        if (NinebotZAdapter.getInstance().getLedIsAvailable(2)) {
            sliderPref(
                name = R.string.nb_led_color2_title,
                desc = R.string.nb_led_color_description,
                position = AppConfig.ledColor2.toFloat(),
                min = 0f,
                max = 256f,
            ) {
                AppConfig.ledColor2 = it.toInt()
            }
        }
        if (NinebotZAdapter.getInstance().getLedIsAvailable(3)) {
            sliderPref(
                name = R.string.nb_led_color3_title,
                desc = R.string.nb_led_color_description,
                position = AppConfig.ledColor3.toFloat(),
                min = 0f,
                max = 256f,
            ) {
                AppConfig.ledColor3 = it.toInt()
            }
        }
        if (NinebotZAdapter.getInstance().getLedIsAvailable(4)) {
            sliderPref(
                name = R.string.nb_led_color4_title,
                desc = R.string.nb_led_color_description,
                position = AppConfig.ledColor4.toFloat(),
                min = 0f,
                max = 256f,
            ) {
                AppConfig.ledColor4 = it.toInt()
            }
        }
        sliderPref(
            name = R.string.speaker_volume_title,
            desc = R.string.speaker_volume_description,
            position = AppConfig.speakerVolume.toFloat(),
            min = 0f,
            max = 127f,
        ) {
            AppConfig.speakerVolume = it.toInt()
        }
        switchPref(
            name = R.string.lock_mode_title,
            desc = R.string.lock_mode_description,
            default = AppConfig.lockMode,
        ) {
            AppConfig.lockMode = it
        }
        if (WheelData.getInstance().speed < 1) {
            var showDialog by remember { mutableStateOf(false) }
            clickablePref(
                name = R.string.wheel_calibration,
                themeIcon = ThemeIconEnum.SettingsCalibration,
            ) {
                showDialog = true
            }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(stringResource(R.string.wheel_calibration)) },
                    text = {
                        Text(stringResource(R.string.wheel_calibration_message_nb))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                WheelData.getInstance().wheelCalibration()
                            },
                        ) {
                            Text(stringResource(R.string.wheel_calibration))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDialog = false },
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun inmotion() {

}

@Composable
private fun inmotionV2() {

}

@Composable
private fun kingsong() {

}

@Composable
private fun begode() {

}

@Composable
private fun veteran() {

}

@Composable
private fun forAllWheel() {
    sliderPref(
        name = R.string.battery_capacity_title,
        desc = R.string.battery_capacity_description,
        position = AppConfig.batteryCapacity.toFloat(),
        min = 0f,
        max = 9999f,
        unit = R.string.wh,
    ) {
        AppConfig.batteryCapacity = it.toInt()
    }
    sliderPref(
        name = R.string.charging_power_title,
        desc = R.string.charging_power_description,
        position = AppConfig.chargingPower.toFloat() / 10f,
        min = 0f,
        max = 100.0f,
        unit = R.string.amp,
        format = "%.1f",
    ) {
        AppConfig.chargingPower = it.toInt() * 10
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    clickablePref(
        name = R.string.profile_name_title,
    ) {
        showProfileDialog = true
    }
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(stringResource(R.string.profile_name_title)) },
            text = {
                TextField(
                    value = AppConfig.profileName,
                    onValueChange = {
                        AppConfig.profileName = it
                    },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProfileDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showProfileDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
            ),
        )
    }
    baseSettings(
        name = R.string.current_mac,
    ) {
        Text(AppConfig.lastMac.trimEnd('_'))
    }
}
