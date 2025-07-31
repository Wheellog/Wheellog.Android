package com.cooper.wheellog.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.*
import com.cooper.wheellog.utils.StringUtil.inArray
import org.koin.compose.koinInject

@Composable
fun wheelScreen(appConfig: AppConfig = koinInject())
{
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        when (WheelData.getInstance().wheelType) {
            Constants.WHEEL_TYPE.NINEBOT_Z -> ninebotZ()
            Constants.WHEEL_TYPE.INMOTION -> inmotion()
            Constants.WHEEL_TYPE.INMOTION_V2 -> inmotionV2()
            Constants.WHEEL_TYPE.KINGSONG -> kingsong()
            Constants.WHEEL_TYPE.GOTWAY -> begode()
            Constants.WHEEL_TYPE.VETERAN -> veteran()
            else -> baseSettings(
                name = stringResource(R.string.unknown_device),
            )
        }
        if (WheelData.getInstance().wheelType != Constants.WHEEL_TYPE.Unknown) {
            forAllWheel()
        }
    }
}

@Composable
private fun ninebotZ(appConfig: AppConfig = koinInject()) {
    val adapter by remember { mutableStateOf(NinebotZAdapter.getInstance()) }
    switchPref(
        name = stringResource(R.string.on_headlight_title),
        desc = stringResource(R.string.on_headlight_description),
        default = appConfig.lightEnabled,
    ) {
        appConfig.lightEnabled = it
        adapter.setLightState(it)
    }
    switchPref(
        name = stringResource(R.string.drl_settings_title),
        desc = stringResource(R.string.drl_settings_description_nb),
        default = appConfig.drlEnabled,
    ) {
        appConfig.drlEnabled = it
        adapter.setDrl(it)
    }
    switchPref(
        name = stringResource(R.string.taillight_settings_title),
        desc = stringResource(R.string.taillight_settings_description),
        default = appConfig.taillightEnabled,
    ) {
        appConfig.taillightEnabled = it
        adapter.setTailLightState(it)
    }
    switchPref(
        name = stringResource(R.string.disable_handle_button_title),
        desc = stringResource(R.string.disable_handle_button_description),
        default = appConfig.handleButtonDisabled,
    ) {
        appConfig.handleButtonDisabled = it
        adapter.setHandleButtonState(it)
    }
    var alarm1 by remember { mutableStateOf(appConfig.wheelAlarm1Enabled) }
    switchPref(
        name = stringResource(R.string.wheel_alarm1_enabled_title),
        desc = stringResource(R.string.wheel_alarm1_enabled_description),
        default = appConfig.wheelAlarm1Enabled,
    ) {
        appConfig.wheelAlarm1Enabled = it
        alarm1 = it
        adapter.setAlarmEnabled(it, 1)
    }
    if (alarm1) {
        sliderPref(
            name = stringResource(R.string.wheel_alarm1_title),
            desc = stringResource(R.string.wheel_alarm1_description),
            position = appConfig.wheelAlarm1Speed.toFloat(),
            min = 0f,
            max = NinebotZAdapter.getInstance().wheelAlarmMax.toFloat(),
            unit = R.string.kmh,
        ) {
            appConfig.wheelAlarm1Speed = it.toInt()
            adapter.setAlarmSpeed(it.toInt(), 1)
        }
    }
    var alarm2 by remember { mutableStateOf(appConfig.wheelAlarm2Enabled) }
    switchPref(
        name = stringResource(R.string.wheel_alarm2_enabled_title),
        desc = stringResource(R.string.wheel_alarm2_enabled_description),
        default = appConfig.wheelAlarm2Enabled,
    ) {
        appConfig.wheelAlarm2Enabled = it
        alarm2 = it
        adapter.setAlarmEnabled(it, 2)
    }
    if (alarm2) {
        sliderPref(
            name = stringResource(R.string.wheel_alarm2_title),
            desc = stringResource(R.string.wheel_alarm2_description),
            position = appConfig.wheelAlarm2Speed.toFloat(),
            min = 0f,
            max = NinebotZAdapter.getInstance().wheelAlarmMax.toFloat(),
            unit = R.string.kmh,
        ) {
            appConfig.wheelAlarm2Speed = it.toInt()
            adapter.setAlarmSpeed(it.toInt(), 2)
        }
    }
    var alarm3 by remember { mutableStateOf(appConfig.wheelAlarm3Enabled) }
    switchPref(
        name = stringResource(R.string.wheel_alarm3_enabled_title),
        desc = stringResource(R.string.wheel_alarm3_enabled_description),
        default = appConfig.wheelAlarm3Enabled,
    ) {
        appConfig.wheelAlarm3Enabled = it
        alarm3 = it
        adapter.setAlarmEnabled(it, 3)
    }
    if (alarm3) {
        sliderPref(
            name = stringResource(R.string.wheel_alarm3_title),
            desc = stringResource(R.string.wheel_alarm3_description),
            position = appConfig.wheelAlarm3Speed.toFloat(),
            min = 0f,
            max = NinebotZAdapter.getInstance().wheelAlarmMax.toFloat(),
            unit = R.string.kmh,
        ) {
            appConfig.wheelAlarm3Speed = it.toInt()
            adapter.setAlarmSpeed(it.toInt(), 3)
        }
    }
    var limitedMode by remember { mutableStateOf(appConfig.wheelLimitedModeEnabled) }
    switchPref(
        name = stringResource(R.string.wheel_limited_mode_title),
        desc = stringResource(R.string.wheel_limited_mode_description),
        default = appConfig.wheelLimitedModeEnabled,
    ) {
        appConfig.wheelLimitedModeEnabled = it
        limitedMode = it
        adapter.setLimitedModeEnabled(it)
    }
    if (limitedMode) {
        sliderPref(
            name = stringResource(R.string.wheel_limited_speed_title),
            desc = stringResource(R.string.wheel_limited_speed_description),
            position = appConfig.wheelLimitedModeSpeed.toFloat() / 10f,
            min = 0f,
            max = 65.5f,
            unit = R.string.kmh,
            format = "%.1f",
        ) {
            val value = it.toInt() * 10
            appConfig.wheelLimitedModeSpeed = value
            adapter.setLimitedSpeed(value)
        }
    }
    switchPref(
        name = stringResource(R.string.brake_assistant_title),
        desc = stringResource(R.string.brake_assistant_description),
        default = appConfig.brakeAssistantEnabled,
    ) {
        appConfig.brakeAssistantEnabled = it
        adapter.setBrakeAssist(it)
    }
    sliderPref(
        name = stringResource(R.string.pedal_sensivity_title),
        desc = stringResource(R.string.pedal_sensivity_description),
        position = appConfig.pedalSensivity.toFloat(),
        min = 0f,
        max = 4f,
    ) {
        appConfig.pedalSensivity = it.toInt()
        adapter.pedalSensivity = it.toInt()
    }
    list(
        name = stringResource(R.string.led_mode_title),
        desc = NinebotZAdapter.getInstance().ledModeString ?: "",
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
        defaultKey = appConfig.ledMode,
    ) {
        appConfig.ledMode = it.first
        adapter.updateLedMode(Integer.parseInt(it.first))
    }
    if (NinebotZAdapter.getInstance().getLedIsAvailable(1)) {
        sliderPref(
            name = stringResource(R.string.nb_led_color1_title),
            desc = stringResource(R.string.nb_led_color_description),
            position = appConfig.ledColor1.toFloat(),
            min = 0f,
            max = 256f,
        ) {
            appConfig.ledColor1 = it.toInt()
            adapter.setLedColor(it.toInt(), 1)
        }
    }
    if (NinebotZAdapter.getInstance().getLedIsAvailable(2)) {
        sliderPref(
            name = stringResource(R.string.nb_led_color2_title),
            desc = stringResource(R.string.nb_led_color_description),
            position = appConfig.ledColor2.toFloat(),
            min = 0f,
            max = 256f,
        ) {
            appConfig.ledColor2 = it.toInt()
            adapter.setLedColor(it.toInt(), 2)
        }
    }
    if (NinebotZAdapter.getInstance().getLedIsAvailable(3)) {
        sliderPref(
            name = stringResource(R.string.nb_led_color3_title),
            desc = stringResource(R.string.nb_led_color_description),
            position = appConfig.ledColor3.toFloat(),
            min = 0f,
            max = 256f,
        ) {
            appConfig.ledColor3 = it.toInt()
            adapter.setLedColor(it.toInt(), 3)
        }
    }
    if (NinebotZAdapter.getInstance().getLedIsAvailable(4)) {
        sliderPref(
            name = stringResource(R.string.nb_led_color4_title),
            desc = stringResource(R.string.nb_led_color_description),
            position = appConfig.ledColor4.toFloat(),
            min = 0f,
            max = 256f,
        ) {
            appConfig.ledColor4 = it.toInt()
            adapter.setLedColor(it.toInt(), 4)
        }
    }
    sliderPref(
        name = stringResource(R.string.speaker_volume_title),
        desc = stringResource(R.string.speaker_volume_description),
        position = appConfig.speakerVolume.toFloat(),
        min = 0f,
        max = 127f,
    ) {
        appConfig.speakerVolume = it.toInt()
        adapter.speakerVolume = it.toInt()
    }
    switchPref(
        name = stringResource(R.string.lock_mode_title),
        desc = stringResource(R.string.lock_mode_description),
        default = appConfig.lockMode,
    ) {
        appConfig.lockMode = it
        adapter.setLockMode(it)
    }
    var showDialogCalibration by remember { mutableStateOf(false) }
    if (showDialogCalibration) {
        AlertDialog(
            onDismissRequest = { showDialogCalibration = false },
            title = { Text(stringResource(R.string.wheel_calibration)) },
            text = {
                Text(stringResource(R.string.wheel_calibration_message_nb))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        adapter.wheelCalibration()
                    },
                ) {
                    Text(stringResource(R.string.wheel_calibration))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialogCalibration = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    clickablePref(
        name = stringResource(R.string.wheel_calibration),
        themeIcon = ThemeIconEnum.SettingsCalibration,
    ) {
        if (WheelData.getInstance().speed < 1) {
            showDialogCalibration = true
        }
    }
}

@Composable
private fun inmotion(appConfig: AppConfig = koinInject()) {
    val adapter by remember { mutableStateOf(InMotionAdapter.getInstance()) }
    var speedMultipier = 1.0f
    var speedUnit = R.string.kmh
    if (appConfig.useMph) {
        speedMultipier = MathsUtil.kmToMilesMultiplier.toFloat()
        speedUnit = R.string.mph
    }
    switchPref(
        name = stringResource(R.string.on_headlight_title),
        desc = stringResource(R.string.on_headlight_description),
        default = appConfig.lightEnabled,
    ) {
        appConfig.lightEnabled = it
        adapter.setLightState(it)
    }
    if (InMotionAdapter.getInstance().ledThere) {
        switchPref(
            name = stringResource(R.string.leds_settings_title),
            desc = stringResource(R.string.leds_settings_description),
            default = appConfig.ledEnabled,
        ) {
            appConfig.ledEnabled = it
            adapter.setLedState(it)
        }
    }
    switchPref(
        name = stringResource(R.string.disable_handle_button_title),
        desc = stringResource(R.string.disable_handle_button_description),
        default = appConfig.handleButtonDisabled,
    ) {
        appConfig.handleButtonDisabled = it
        adapter.setHandleButtonState(it)
    }
    sliderPref(
        name = stringResource(R.string.max_speed_title),
        desc = stringResource(R.string.tilt_back_description),
        position = appConfig.wheelMaxSpeed.toFloat(),
        min = 3f,
        max = InMotionAdapter.getInstance().maxSpeed.toFloat(),
        unit = speedUnit,
        visualMultiple = speedMultipier,
    ) {
        appConfig.wheelMaxSpeed = it.toInt()
        adapter.updateMaxSpeed(it.toInt())
    }
    sliderPref(
        name = stringResource(R.string.pedal_horizont_title),
        desc = stringResource(R.string.pedal_horizont_description),
        position = appConfig.pedalsAdjustment / 10f,
        min = -8f,
        max = 8f,
        unit = R.string.degree,
        format = "%.1f",
    ) {
        appConfig.pedalsAdjustment = (it * 10).toInt()
        adapter.setPedalTilt((it * 10).toInt())
    }
    if (InMotionAdapter.getInstance().wheelModesWheel) {
        switchPref(
            name = stringResource(R.string.ride_mode_title),
            desc = stringResource(R.string.ride_mode_description),
            default = appConfig.rideMode,
        ) {
            appConfig.rideMode = it
            adapter.setRideMode(it)
        }
        sliderPref(
            name = stringResource(R.string.pedal_sensivity_title),
            desc = stringResource(R.string.pedal_sensivity_description),
            position = appConfig.pedalSensivity.toFloat(),
            min = 4f,
            max = 100f,
            unit = R.string.persent,
        ) {
            appConfig.pedalSensivity = it.toInt()
            adapter.setPedalSensivity(it.toInt())
        }
    }
    clickableAndAlert(
        name = stringResource(R.string.power_off),
        confirmButtonText = stringResource(R.string.power_off),
        alertDesc = stringResource(R.string.power_off_message),
        themeIcon = ThemeIconEnum.SettingsPowerOff,
        condition = { WheelData.getInstance().speed < 1 },
        onConfirm = { adapter.powerOff() },
    )
    clickableAndAlert(
        name = stringResource(R.string.wheel_calibration),
        confirmButtonText = stringResource(R.string.wheel_calibration),
        alertDesc = stringResource(R.string.wheel_calibration_message_inmo),
        themeIcon = ThemeIconEnum.SettingsCalibration,
        condition = { WheelData.getInstance().speed < 1 },
        onConfirm = { adapter.wheelCalibration() },
    )
}

@Composable
private fun inmotionV2(appConfig: AppConfig = koinInject()) {
    val adapter by remember { mutableStateOf(InmotionAdapterV2.getInstance()) }
    var splitModeEnabled by remember { mutableStateOf(appConfig.splitMode) }
    var speedMultipier = 1.0f
    var speedUnit = R.string.kmh
    if (appConfig.useMph) {
        speedMultipier = MathsUtil.kmToMilesMultiplier.toFloat()
        speedUnit = R.string.mph
    }
    // models with HighBeam/LowBeam, currently V12 family
    if (adapter.model in setOf(InmotionAdapterV2.Model.V12HS, InmotionAdapterV2.Model.V12HT, InmotionAdapterV2.Model.V12PRO)){
        switchPref(
            name = stringResource(R.string.on_lowbeam_title),
            desc = stringResource(R.string.on_lowbeam_description),
            default = appConfig.lowBeamEnabled,
        ) {
            appConfig.lowBeamEnabled = it
            adapter.setBeamState(it, appConfig.highBeamEnabled)
        }
        switchPref(
            name = stringResource(R.string.on_highbeam_title),
            desc = stringResource(R.string.on_highbeam_description),
            default = appConfig.highBeamEnabled,
        ) {
            appConfig.highBeamEnabled = it
            adapter.setBeamState(appConfig.lowBeamEnabled, it)
        }
        sliderPref(
            name = stringResource(R.string.lowbeam_brightness_title),
            desc = stringResource(R.string.lowbeam_brightness_description),
            position = appConfig.lowBeamBrightness.toFloat(),
            min = 0f,
            max = 100f,
            unit = R.string.persent,
        ) {
            appConfig.lowBeamBrightness = it.toInt()
            adapter.setBeamBrightness(it.toInt(), appConfig.highBeamBrightness)
        }
        sliderPref(
            name = stringResource(R.string.highbeam_brightness_title),
            desc = stringResource(R.string.highbeam_brightness_description),
            position = appConfig.highBeamBrightness.toFloat(),
            min = 0f,
            max = 100f,
            unit = R.string.persent,
        ) {
            appConfig.highBeamBrightness = it.toInt()
            adapter.setBeamBrightness(appConfig.lowBeamBrightness, it.toInt())
        }
        switchPref(
            name = stringResource(R.string.sound_wave_title),
            desc = stringResource(R.string.sound_wave_description),
            default = appConfig.soundWave,
        ) {
            appConfig.soundWave = it
            adapter.setSoundWave(it)
        }

    } else {
        switchPref(
            name = stringResource(R.string.on_headlight_title),
            desc = stringResource(R.string.on_headlight_description),
            default = appConfig.lightEnabled,
        ) {
            appConfig.lightEnabled = it
            adapter.setLightState(it)
        }
        switchPref(
            name = stringResource(R.string.drl_settings_title),
            desc = stringResource(R.string.drl_settings_description),
            default = appConfig.drlEnabled,
        ) {
            appConfig.drlEnabled = it
            adapter.setDrl(it)
        }
        // With brightness regulation, only v11 is applicable
        if (adapter.model == InmotionAdapterV2.Model.V11) {
            sliderPref(
                name = stringResource(R.string.light_brightness_title),
                desc = stringResource(R.string.light_brightness_description),
                position = appConfig.lightBrightness.toFloat(),
                min = 0f,
                max = 100f,
                unit = R.string.persent,
            ) {
                appConfig.lightBrightness = it.toInt()
                adapter.setLightBrightness(it.toInt())
            }
        }
    }
    // auto light: V12 family, V13 (only from built-in screen)
    if (adapter.model in setOf(InmotionAdapterV2.Model.V12HS, InmotionAdapterV2.Model.V12HT,
            InmotionAdapterV2.Model.V12PRO, InmotionAdapterV2.Model.V13, InmotionAdapterV2.Model.V13PRO)) {
        switchPref(
            name = stringResource(R.string.autolight_title),
            desc = stringResource(R.string.autolight_description),
            default = appConfig.autoLight,
        ) {
            appConfig.autoLight = it
            adapter.setAutoLight(it)
        }
    }
    //V11 only, V11y doesn't have Fan and Quiet mode
    if (adapter.model == InmotionAdapterV2.Model.V11) {
        switchPref(
            name = stringResource(R.string.fan_title),
            desc = stringResource(R.string.fan_description),
            default = appConfig.fanQuietEnabled,
        ) {
            appConfig.fanQuietEnabled = it
            adapter.setFan(it)
        }
        switchPref(
            name = stringResource(R.string.fan_quiet_title),
            desc = stringResource(R.string.fan_quiet_description),
            default = appConfig.fanQuietEnabled,
        ) {
            appConfig.fanQuietEnabled = it
            adapter.setFanQuiet(it)
        }
    }
    // Inmotion V13 and V14 don't have handle button (kill-switch)
    if (adapter.model in setOf(InmotionAdapterV2.Model.V12HS, InmotionAdapterV2.Model.V12HT,
            InmotionAdapterV2.Model.V12PRO, InmotionAdapterV2.Model.V11, InmotionAdapterV2.Model.V11Y, InmotionAdapterV2.Model.V9)) {
        switchPref(
            name = stringResource(R.string.disable_handle_button_title),
            desc = stringResource(R.string.disable_handle_button_description),
            default = appConfig.handleButtonDisabled,
        ) {
            appConfig.handleButtonDisabled = it
            adapter.setHandleButtonState(it)
        }
    }
    // Inmotion V11Y and V14 don't have speaker
    if (adapter.model in setOf(InmotionAdapterV2.Model.V12HS, InmotionAdapterV2.Model.V12HT,
            InmotionAdapterV2.Model.V12PRO, InmotionAdapterV2.Model.V11, InmotionAdapterV2.Model.V13,
            InmotionAdapterV2.Model.V13PRO, InmotionAdapterV2.Model.V9)) {
        sliderPref(
            name = stringResource(R.string.speaker_volume_title),
            desc = stringResource(R.string.speaker_volume_description),
            position = appConfig.speakerVolume.toFloat(),
            min = 0f,
            max = 100f,
            unit = R.string.persent,
        ) {
            appConfig.speakerVolume = it.toInt()
            adapter.setSpeakerVolume(it.toInt())
        }

    }
    // beeper for wheels which don't have speaker
    switchPref(
        name = stringResource(R.string.speaker_mute_title),
        desc = stringResource(R.string.speaker_mute_description),
        default = appConfig.speakerMute,
    ) {
        appConfig.speakerMute = it
        adapter.setMute(it)
    }
    sliderPref(
        name = stringResource(R.string.max_speed_title),
        desc = stringResource(R.string.tilt_back_description),
        position = appConfig.wheelMaxSpeed.toFloat(),
        min = 3f,
        max = adapter.maxSpeed.toFloat(),
        unit = speedUnit,
        visualMultiple = speedMultipier,
    ) {
        appConfig.wheelMaxSpeed = it.toInt()
        adapter.updateMaxSpeed(it.toInt())
    }
    // alarms: two on V12 family, 1 on V14, others don't have
    // temporally check it for V13 and V11y
    if (adapter.model in setOf(InmotionAdapterV2.Model.V12HS, InmotionAdapterV2.Model.V12HT,
            InmotionAdapterV2.Model.V12PRO, InmotionAdapterV2.Model.V14s, InmotionAdapterV2.Model.V14g,
            InmotionAdapterV2.Model.V13, InmotionAdapterV2.Model.V13PRO, InmotionAdapterV2.Model.V11Y,
            InmotionAdapterV2.Model.V9)) {
        sliderPref(
            name = stringResource(R.string.wheel_alarm1_title),
            desc = stringResource(R.string.wheel_alarm1_description),
            position = appConfig.wheelAlarm1Speed.toFloat(),
            min = 3f,
            max = adapter.maxSpeed.toFloat(),
            unit = speedUnit,
            visualMultiple = speedMultipier,
        ) {
            appConfig.wheelAlarm1Speed = it.toInt()
            adapter.updateAlarmSpeed(
                it.toInt(),
                appConfig.wheelAlarm2Speed,
                appConfig.wheelMaxSpeed
            )
        }
    }
    // two alarms only V12 family
    if (adapter.model in setOf(InmotionAdapterV2.Model.V12HS, InmotionAdapterV2.Model.V12HT,
            InmotionAdapterV2.Model.V12PRO)) {
       sliderPref(
            name = stringResource(R.string.wheel_alarm2_title),
            desc = stringResource(R.string.wheel_alarm2_description),
            position = appConfig.wheelAlarm2Speed.toFloat(),
            min = 3f,
            max = adapter.maxSpeed.toFloat(),
            unit = speedUnit,
            visualMultiple = speedMultipier,
        ) {
            appConfig.wheelAlarm2Speed = it.toInt()
            adapter.updateAlarmSpeed(appConfig.wheelAlarm1Speed, it.toInt(), appConfig.wheelMaxSpeed)
        }
    }
    sliderPref(
        name = stringResource(R.string.pedal_horizont_title),
        desc = stringResource(R.string.pedal_horizont_description),
        position = appConfig.pedalsAdjustment / 10f,
        min = -10f,
        max = 10f,
        unit = R.string.degree,
        format = "%.1f",
    ) {
        appConfig.pedalsAdjustment = (it * 10).toInt()
        adapter.setPedalTilt((it * 10).toInt())
    }
    switchPref(
        name = stringResource(R.string.ride_mode_title),
        desc = stringResource(R.string.ride_mode_description),
        default = appConfig.rideMode,
    ) {
        appConfig.rideMode = it
        adapter.setRideMode(it)
    }
    sliderPref(
        name = stringResource(R.string.pedal_sensivity_title),
        desc = stringResource(R.string.pedal_sensivity_description),
        position = appConfig.pedalSensivity.toFloat(),
        min = 0f,
        max = 100f,
        unit = R.string.persent,
    ) {
        appConfig.pedalSensivity = it.toInt()
        adapter.setPedalSensivity(it.toInt())
    }
    // seems not applicable for V13, leave it for now here
    switchPref(
        name = stringResource(R.string.fancier_mode_title),
        desc = stringResource(R.string.fancier_mode_description),
        default = appConfig.fancierMode,
    ) {
        appConfig.fancierMode = it
        adapter.setFancierMode(it)
    }

    switchPref(
        name = stringResource(R.string.split_mode_title),
        desc = stringResource(R.string.split_mode_description),
        default = appConfig.splitMode,
    ) {
        appConfig.splitMode = it
        splitModeEnabled = it
        adapter.setSplitMode(it)
    }
    AnimatedVisibility(splitModeEnabled) {
        group(
            name = stringResource(R.string.split_mode_title)
        ) {
            sliderPref(
                name = stringResource(R.string.split_mode_accel_title),
                desc = stringResource(R.string.split_mode_accel_description),
                position = appConfig.splitModeAccel.toFloat(),
                min = 0f,
                max = 100f,
                unit = R.string.persent,
            ) {
                appConfig.splitModeAccel = it.toInt()
                adapter.setSplitModeConf(it.toInt(), appConfig.splitModeBreak)
            }
            sliderPref(
                name = stringResource(R.string.split_mode_break_title),
                desc = stringResource(R.string.split_mode_break_description),
                position = appConfig.splitModeBreak.toFloat(),
                min = 0f,
                max = 100f,
                unit = R.string.persent,
            ) {
                appConfig.splitModeBreak = it.toInt()
                adapter.setSplitModeConf(appConfig.splitModeAccel, it.toInt())
            }
        }
    }
    // Still exist for V11 and V11y, but different name and logic
    if (adapter.model in setOf(InmotionAdapterV2.Model.V11, InmotionAdapterV2.Model.V11Y)) {
        switchPref(
            name = stringResource(R.string.go_home_mode_title),
            desc = stringResource(R.string.go_home_mode_description),
            default = appConfig.goHomeMode,
        ) {
            appConfig.goHomeMode = it
            adapter.setGoHomeMode(it)
        }
    }
    // V13 and V14 applicable
    if (adapter.model in setOf(InmotionAdapterV2.Model.V14g, InmotionAdapterV2.Model.V14s, InmotionAdapterV2.Model.V13, InmotionAdapterV2.Model.V13PRO)) {
        switchPref(
            name = stringResource(R.string.berm_angle_mode_title),
            desc = stringResource(R.string.berm_angle_mode_description),
            default = appConfig.bermAngleMode,
        ) {
            appConfig.bermAngleMode = it
            adapter.setBermAngleMode(it)
        }
    }
    // V11, V12HS, V12PRO, V14 have it, I think it is applicable for all. UPD: V13 seems not have :0
    sliderPref(
        name = stringResource(R.string.standby_delay_title),
        desc = stringResource(R.string.standby_delay_description),
        position = appConfig.standbyDelay.toFloat(),
        min = 0f,
        max = 240f,
        unit = R.string.min,
    ) {
        appConfig.standbyDelay = it.toInt()
        adapter.setStandbyDelay(it.toInt())
    }

    switchPref(
        name = stringResource(R.string.transport_mode_title),
        desc = stringResource(R.string.transport_mode_description),
        default = appConfig.transportMode,
    ) {
        appConfig.transportMode = it
        adapter.setTransportMode(it)
    }
    switchPref(
        name = stringResource(R.string.lock_mode_title),
        desc = stringResource(R.string.lock_mode_description),
        default = appConfig.lockMode,
    ) {
        appConfig.lockMode = it
        adapter.setLockMode(it)
    }
    clickableAndAlert(
        name = stringResource(R.string.power_off),
        confirmButtonText = stringResource(R.string.power_off),
        alertDesc = stringResource(R.string.power_off_message),
        themeIcon = ThemeIconEnum.SettingsPowerOff,
        condition = { WheelData.getInstance().speed < 1 },
        onConfirm = { adapter.powerOff() },
    )

    clickableAndAlert(
        name = stringResource(R.string.wheel_calibration),
        confirmButtonText = stringResource(R.string.wheel_calibration),
        alertDesc = stringResource(R.string.wheel_calibration_message_inmo),
        themeIcon = ThemeIconEnum.SettingsCalibration,
        condition = { WheelData.getInstance().speed < 1 },
        onConfirm = { adapter.wheelCalibration() },
    )

    clickableAndAlert(
        name = stringResource(R.string.wheel_calibration_balance),
        confirmButtonText = stringResource(R.string.wheel_calibration_balance),
        alertDesc = stringResource(R.string.wheel_calibration_balance_message_inmo),
        themeIcon = ThemeIconEnum.SettingsCalibration,
        condition = { WheelData.getInstance().speed < 1 },
        onConfirm = { adapter.wheelCalibrationBalance() },
    )

}

@Composable
private fun kingsong(appConfig: AppConfig = koinInject()) {
    val adapter by remember { mutableStateOf(KingsongAdapter.getInstance()) }
    var speedMultipier = 1.0f
    var speedUnit = R.string.kmh
    if (appConfig.useMph) {
        speedMultipier = MathsUtil.kmToMilesMultiplier.toFloat()
        speedUnit = R.string.mph
    }
    list(
        name = stringResource(R.string.light_mode_title),
        desc = stringResource(R.string.on_off_auto),
        entries = mapOf(
            "0" to stringResource(R.string.on),
            "1" to stringResource(R.string.off),
            "2" to stringResource(R.string.auto),
        ),
        defaultKey = appConfig.lightMode,
    ) {
        appConfig.lightMode = it.first
        adapter.setLightMode(it.first.toInt())
    }
    list(
        name = stringResource(R.string.led_mode_title),
        desc = stringResource(R.string.on_off),
        entries = mapOf(
            "0" to stringResource(R.string.on),
            "1" to stringResource(R.string.off),
        ),
        defaultKey = appConfig.ledMode,
    ) {
        appConfig.ledMode = it.first
        adapter.updateLedMode(it.first.toInt())
    }
    list(
        name = stringResource(R.string.pedals_mode_title),
        desc = stringResource(R.string.soft_medium_hard),
        entries = mapOf(
            "0" to stringResource(R.string.hard),
            "1" to stringResource(R.string.medium),
            "2" to stringResource(R.string.soft),
        ),
        defaultKey = appConfig.pedalsMode,
    ) {
        appConfig.pedalsMode = it.first
        adapter.setPedalTilt(it.first.toInt())
    }
    sliderPref(
        name = stringResource(R.string.max_speed_title),
        desc = stringResource(R.string.tilt_back_description),
        position = appConfig.wheelMaxSpeed.toFloat(),
        min = 0f,
        max = 100f,
        unit = speedUnit,
        visualMultiple = speedMultipier,
    ) {
        appConfig.wheelMaxSpeed = it.toInt()
        adapter.updateMaxSpeed(it.toInt())
    }
    sliderPref(
        name = stringResource(R.string.alert3_title),
        desc = stringResource(R.string.alarm3_description),
        position = appConfig.wheelKsAlarm3.toFloat(),
        min = 0f,
        max = 100f,
        unit = speedUnit,
        visualMultiple = speedMultipier,
    ) {
        appConfig.wheelKsAlarm3 = it.toInt()
        adapter.updateKSAlarm3(it.toInt())
    }
    sliderPref(
        name = stringResource(R.string.alert2_title),
        desc = stringResource(R.string.alarm2_description),
        position = appConfig.wheelKsAlarm2.toFloat(),
        min = 0f,
        max = 100f,
        unit = speedUnit,
        visualMultiple = speedMultipier,
    ) {
        appConfig.wheelKsAlarm2 = it.toInt()
        adapter.updateKSAlarm2(it.toInt())
    }
    sliderPref(
        name = stringResource(R.string.alert1_title),
        desc = stringResource(R.string.alarm1_description),
        position = appConfig.wheelKsAlarm1.toFloat(),
        min = 0f,
        max = 100f,
        unit = speedUnit,
        visualMultiple = speedMultipier,
    ) {
        appConfig.wheelKsAlarm1 = it.toInt()
        adapter.updateKSAlarm1(it.toInt())
    }
    switchPref(
        name = stringResource(R.string.ks18l_scaler_title),
        desc = stringResource(R.string.ks18l_scaler_description),
        default = appConfig.ks18LScaler,
    ) {
        appConfig.ks18LScaler = it
    }
    clickableAndAlert(
        name = stringResource(R.string.power_off),
        confirmButtonText = stringResource(R.string.power_off),
        alertDesc = stringResource(R.string.power_off_message),
        themeIcon = ThemeIconEnum.SettingsPowerOff,
        condition = { WheelData.getInstance().speed < 1 },
        onConfirm = { adapter.powerOff() },
    )
    clickableAndAlert(
        name = stringResource(R.string.wheel_calibration),
        confirmButtonText = stringResource(R.string.wheel_calibration),
        alertDesc = stringResource(R.string.wheel_calibration_message_inmo),
        themeIcon = ThemeIconEnum.SettingsCalibration,
        condition = { WheelData.getInstance().speed < 1 },
        onConfirm = { adapter.wheelCalibration() },
    )
}

@Composable
private fun begode(appConfig: AppConfig = koinInject()) {
    val adapter by remember { mutableStateOf(GotwayAdapter.getInstance()) }
    var isAlexovikFW = appConfig.IsAlexovikFW
    var speedMultipier = 1.0f
    var speedUnit = R.string.kmh
    var percentUnit = R.string.persent
    var degreeUnit = R.string.degree
    if (appConfig.useMph) {
        speedMultipier = MathsUtil.kmToMilesMultiplier.toFloat()
        speedUnit = R.string.mph
    }
    if (!isAlexovikFW) {
        list(
            name = stringResource(R.string.light_mode_title),
            desc = stringResource(R.string.on_off_strobe),
            entries = mapOf(
                "0" to stringResource(R.string.off),
                "1" to stringResource(R.string.on),
                "2" to stringResource(R.string.strobe),
            ),
            defaultKey = appConfig.lightMode,
        ) {
            appConfig.lightMode = it.first
            adapter.setLightMode(it.first.toInt())
        }
        list(
            name = stringResource(R.string.alarm_mode_title),
            desc = stringResource(R.string.alarm_settings_title),
            entries = if (appConfig.hwPwm) {
                mapOf(
                    "0" to stringResource(R.string.on_level_alarm),
                    "1" to stringResource(R.string.off_level_1_alarm),
                    "2" to stringResource(R.string.off_level_2_alarm),
                    "3" to stringResource(R.string.pwm_tiltback_alarm),
                )
            } else {
                mapOf(
                    "0" to stringResource(R.string.on_level_alarm),
                    "1" to stringResource(R.string.off_level_1_alarm),
                    "2" to stringResource(R.string.off_level_2_alarm),
                )
            },
            defaultKey = appConfig.alarmMode,
        ) {
            appConfig.alarmMode = it.first
            adapter.updateAlarmMode(it.first.toInt())
        }
        list(
            name = stringResource(R.string.pedals_mode_title),
            desc = stringResource(R.string.soft_medium_hard),
            entries = mapOf(
                "0" to stringResource(R.string.hard),
                "1" to stringResource(R.string.medium),
                "2" to stringResource(R.string.soft),
            ),
            defaultKey = appConfig.pedalsMode,
        ) {
            appConfig.pedalsMode = it.first
            adapter.updatePedalsMode(it.first.toInt())
        }
        list(
            name = stringResource(R.string.roll_angle_title),
            desc = stringResource(R.string.roll_angle_description),
            entries = mapOf(
                "0" to stringResource(R.string.low),
                "1" to stringResource(R.string.medium),
                "2" to stringResource(R.string.high),
            ),
            defaultKey = appConfig.rollAngle,
        ) {
            appConfig.rollAngle = it.first
            adapter.setRollAngleMode(it.first.toInt())
        }
        list(
            name = stringResource(R.string.led_mode_title),
            desc = stringResource(R.string.on_off),
            entries = mapOf(
                "0" to stringResource(R.string.zero),
                "1" to stringResource(R.string.one),
                "2" to stringResource(R.string.two),
                "3" to stringResource(R.string.three),
                "4" to stringResource(R.string.four),
                "5" to stringResource(R.string.five),
                "6" to stringResource(R.string.six),
                "7" to stringResource(R.string.seven),
                "8" to stringResource(R.string.eight),
                "9" to stringResource(R.string.nine),
            ),
            defaultKey = appConfig.ledMode,
        ) {
            appConfig.ledMode = it.first
            adapter.updateLedMode(it.first.toInt())
        }
        switchPref(
            name = stringResource(R.string.gw_in_miles_title),
            desc = stringResource(R.string.gw_in_miles_description),
            default = appConfig.gwInMiles,
        ) {
            appConfig.gwInMiles = it
            adapter.setMilesMode(it)
        }
        sliderPref(
            name = stringResource(R.string.max_speed_title),
            desc = stringResource(R.string.tilt_back_description),
            position = appConfig.wheelMaxSpeed.toFloat(),
            min = 0f,
            max = 99f,
            unit = speedUnit,
            visualMultiple = speedMultipier,
        ) {
            appConfig.wheelMaxSpeed = it.toInt()
            adapter.updateMaxSpeed(it.toInt())
        }
        sliderPref(
            name = stringResource(R.string.beeper_volume_title),
            desc = stringResource(R.string.beeper_volume_description),
            min = 1f,
            max = 9f,
            position = appConfig.beeperVolume.toFloat(),
        ) {
            appConfig.beeperVolume = it.toInt()
            adapter.updateBeeperVolume(it.toInt())
        }
        clickableAndAlert(
            name = stringResource(R.string.wheel_calibration),
            confirmButtonText = stringResource(R.string.wheel_calibration),
            alertDesc = stringResource(R.string.wheel_calibration_message_inmo),
            themeIcon = ThemeIconEnum.SettingsCalibration,
            condition = { WheelData.getInstance().speed < 1 },
            onConfirm = { adapter.wheelCalibration() },
        )
        switchPref(
            name = stringResource(R.string.is_gotway_mcm_title),
            desc = stringResource(R.string.is_gotway_mcm_description),
            default = appConfig.useRatio,
        ) {
            appConfig.useRatio = it
        }
        switchPref(
            name = stringResource(R.string.auto_voltage_title),
            desc = stringResource(R.string.auto_voltage_description),
            default = appConfig.autoVoltage,
        ) {
            appConfig.autoVoltage = it
        }
    }
    else
    {
        list(
            name = stringResource(R.string.pedals_mode_title),
            desc = stringResource(R.string.mode1_2_3_4),
            entries = mapOf(
                "2" to stringResource(R.string.mode1),
                "1" to stringResource(R.string.mode2),
                "0" to stringResource(R.string.mode3),
                "3" to stringResource(R.string.mode4),
            ),
            defaultKey = appConfig.pedalsMode,
        ) {
            appConfig.pedalsMode = it.first
            adapter.updatePedalsMode(it.first.toInt())
        }

        switchPref(
            name = stringResource(R.string.extreme_mode_title),
            desc = stringResource(R.string.extreme_mode_description),
            default = appConfig.extremeMode,
        ) {
            appConfig.extremeMode = it
            adapter.updateExtremeMode(it)
        }

        sliderPref(
            name = stringResource(R.string.braking_current_title),
            desc = stringResource(R.string.braking_current_description),
            position = appConfig.brakingCurrent.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.brakingCurrent = it.toInt()
            adapter.updateBrakingCurrent(it.toInt())
        }

        switchPref(
            name = stringResource(R.string.rotation_control_title),
            desc = stringResource(R.string.rotation_control_description),
            default = appConfig.rotationControl,
        ) {
            appConfig.rotationControl = it
            adapter.updateRotationControl(it)
        }

        sliderPref(
            name = stringResource(R.string.rotation_angle_title),
            desc = stringResource(R.string.rotation_angle_description),
            position = appConfig.rotationAngle.toFloat(),
            min = 260f,
            max = 360f,
            unit = degreeUnit,
        ) {
            appConfig.rotationAngle = it.toInt()
            adapter.updateRotationAngle(it.toInt())
        }

        switchPref(
            name = stringResource(R.string.advanced_settings_title),
            desc = stringResource(R.string.advanced_settings_decsription),
            default = appConfig.advancedSettings,
        ) {
            appConfig.advancedSettings = it
            adapter.updateAdvancedSettings(it)
        }

        sliderPref(
            name = stringResource(R.string.proportional_factor_title),
            desc = stringResource(R.string.proportional_factor_decsription),
            position = appConfig.proportionalFactor.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.proportionalFactor = it.toInt()
            adapter.updateProportionalFactor(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.integral_factor_title),
            desc = stringResource(R.string.integral_factor_decsription),
            position = appConfig.integralFactor.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.integralFactor = it.toInt()
            adapter.updateIntegralFactor(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.differential_factor_title),
            desc = stringResource(R.string.differential_factor_decsription),
            position = appConfig.differentialFactor.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.differentialFactor = it.toInt()
            adapter.updateDifferentialFactor(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.dynamic_compensation_title),
            desc = stringResource(R.string.dynamic_compensation_description),
            position = appConfig.dynamicCompensation.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.dynamicCompensation = it.toInt()
            adapter.updateDynamicCompensation(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.dynamic_compensation_filter_title),
            desc = stringResource(R.string.dynamic_compensation_filter_description),
            position = appConfig.dynamicCompensationFilter.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.dynamicCompensationFilter = it.toInt()
            adapter.updateDynamicCompensationFilter(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.acceleration_compensation_title),
            desc = stringResource(R.string.acceleration_compensation_description),
            position = appConfig.accelerationCompensation.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.accelerationCompensation = it.toInt()
            adapter.updateAccelerationCompensation(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.proportional_q_current_factor_title),
            desc = stringResource(R.string.proportional_q_current_factor_decsription),
            position = appConfig.proportionalCurrentFactorQ.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.proportionalCurrentFactorQ = it.toInt()
            adapter.updatePCurrentQ(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.integral_q_current_factor_title),
            desc = stringResource(R.string.integral_q_current_factor_decsription),
            position = appConfig.integralCurrentFactorQ.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.integralCurrentFactorQ = it.toInt()
            adapter.updateICurrentQ(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.proportional_d_current_factor_title),
            desc = stringResource(R.string.proportional_d_current_factor_decsription),
            position = appConfig.proportionalCurrentFactorD.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.proportionalCurrentFactorD = it.toInt()
            adapter.updatePCurrentD(it.toInt())
        }

        sliderPref(
            name = stringResource(R.string.integral_d_current_factor_title),
            desc = stringResource(R.string.integral_d_current_factor_decsription),
            position = appConfig.integralCurrentFactorD.toFloat(),
            min = 0f,
            max = 100f,
            unit = percentUnit,
        ) {
            appConfig.integralCurrentFactorD = it.toInt()
            adapter.updateICurrentD(it.toInt())
        }

        list(
            name = stringResource(R.string.trick_title),
            desc = stringResource(R.string.trick_description),
            entries = mapOf(
                "0" to stringResource(R.string.trick_0),
                "1" to stringResource(R.string.trick_1),
                "2" to stringResource(R.string.trick_2),
            ),
            defaultKey = appConfig.trick.toString(),
        ) {
            appConfig.trick = it.first.toInt()
            adapter.setTrick(it.first.toInt())
        }

    }
    list(
        name = stringResource(R.string.battery_voltage_title),
        desc = stringResource(R.string.battery_voltage_description),
        entries = mapOf(
            "0" to "67.2V",
            "1" to "84V",
            "2" to "100.8V",
            "3" to "117.6V",
            "4" to "134.4V",
            "5" to "168V",
            "6" to "151.2V"
        ),
        defaultKey = appConfig.gotwayVoltage,
    ) {
        appConfig.gotwayVoltage = it.first
    }

    list(
        name = stringResource(R.string.gotway_negative_title),
        desc = stringResource(R.string.gotway_negative_description),
        entries = mapOf(
            "-1" to stringResource(R.string.straight),
            "0" to stringResource(R.string.absolute),
            "1" to stringResource(R.string.reverse),
        ),
    ) {
        appConfig.gotwayNegative = it.first
    }
    switchPref(
        name = stringResource(R.string.connect_beep_title),
        desc = stringResource(R.string.connect_beep_description),
        default = appConfig.connectBeep,
    ) {
        appConfig.connectBeep = it
    }
}

@Composable
private fun veteran(appConfig: AppConfig = koinInject()) {
    val adapter by remember { mutableStateOf(VeteranAdapter.getInstance()) }
    switchPref(
        name = stringResource(R.string.on_headlight_title),
        desc = stringResource(R.string.on_headlight_description),
        default = appConfig.lightEnabled,
    ) {
        appConfig.lightEnabled = it
        adapter.setLightState(it)
    }
    if (!inArray(WheelData.getInstance().model, arrayOf("Nosfet Apex", "Nosfet Aero"))) {
        list(
            name = stringResource(R.string.pedals_mode_title),
            desc = stringResource(R.string.soft_medium_hard),
            entries = mapOf(
                "0" to stringResource(R.string.hard),
                "1" to stringResource(R.string.medium),
                "2" to stringResource(R.string.soft),
            ),
            defaultKey = appConfig.pedalsMode,
        ) {
            appConfig.pedalsMode = it.first
            adapter.updatePedalsMode(it.first.toInt())
        }
    }
    clickableAndAlert(
        name = stringResource(R.string.reset_trip),
        confirmButtonText = stringResource(R.string.reset_trip),
        alertDesc = stringResource(R.string.reset_trip_message),
        onConfirm = { adapter.resetTrip() },
    )
    switchPref(
        name = stringResource(R.string.connect_beep_title),
        desc = stringResource(R.string.connect_beep_description),
        default = appConfig.connectBeep,
    ) {
        appConfig.connectBeep = it
    }
    list(
        name = stringResource(R.string.gotway_negative_title),
        desc = stringResource(R.string.gotway_negative_description),
        entries = mapOf(
            "-1" to stringResource(R.string.straight),
            "0" to stringResource(R.string.absolute),
            "1" to stringResource(R.string.reverse),
        ),
    ) {
        appConfig.gotwayNegative = it.first
    }
    switchPref(
        name = stringResource(R.string.hw_pwm_title),
        desc = stringResource(R.string.hw_pwm_description),
        default = appConfig.hwPwm,
    ) {
        appConfig.hwPwm = it
    }
}

@Composable
private fun forAllWheel(appConfig: AppConfig = koinInject()) {
    sliderPref(
        name = stringResource(R.string.battery_capacity_title),
        desc = stringResource(R.string.battery_capacity_description),
        position = appConfig.batteryCapacity.toFloat(),
        min = 0f,
        max = 9999f,
        unit = R.string.wh,
    ) {
        appConfig.batteryCapacity = it.toInt()
    }
    sliderPref(
        name = stringResource(R.string.charging_power_title),
        desc = stringResource(R.string.charging_power_description),
        position = appConfig.chargingPower.toFloat() / 10f,
        min = 0f,
        max = 100.0f,
        unit = R.string.amp,
        format = "%.1f",
    ) {
        appConfig.chargingPower = it.toInt() * 10
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var profileText by remember { mutableStateOf(appConfig.profileName) }
    clickablePref(
        name = stringResource(R.string.profile_name_title),
        desc = profileText
    ) {
        showProfileDialog = true
    }
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(stringResource(R.string.profile_name_title)) },
            text = {
                TextField(
                    value = profileText,
                    onValueChange = { newText ->
                        profileText = newText
                        appConfig.profileName = newText
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
        name = stringResource(R.string.current_mac),
    ) {
        Text(appConfig.lastMac.trimEnd('_'))
    }
}