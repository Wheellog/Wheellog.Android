package com.cooper.wheellog.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.MathsUtil
import org.koin.compose.koinInject

@Composable
fun alarmScreen(appConfig: AppConfig = koinInject()) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        var alarmsEnabled by remember { mutableStateOf(appConfig.alarmsEnabled) }
        var pwmBasedAlarms by remember { mutableStateOf(appConfig.pwmBasedAlarms) }
        val ksAlteredAlarms =
            WheelData.getInstance().wheelType == Constants.WHEEL_TYPE.KINGSONG
                    && WheelData.getInstance().model.compareTo("KS-18A") != 0
        val wheelAlarm =
                WheelData.getInstance().wheelType == Constants.WHEEL_TYPE.GOTWAY

        switchPref(
            name = stringResource(R.string.enable_alarms_title),
            desc = stringResource(R.string.enable_alarms_description),
            default = alarmsEnabled,
        ) {
            appConfig.alarmsEnabled = it
            alarmsEnabled = it
        }

        AnimatedVisibility(alarmsEnabled) {
            switchPref(
                name = stringResource(R.string.pwm_based_alarms_title),
                desc = stringResource(R.string.pwm_based_alarms_description),
                default = pwmBasedAlarms,
                showDiv = false,
            ) {
                appConfig.pwmBasedAlarms = it
                pwmBasedAlarms = it
            }
        }

        val speedUnit: Int
        val speedMultipier: Double
        if (appConfig.useMph) {
            speedMultipier = MathsUtil.kmToMilesMultiplier
            speedUnit = R.string.mph
        } else {
            speedMultipier = 1.0
            speedUnit = R.string.kmh
        }

        if (alarmsEnabled) {

            switchPref(
                name = stringResource(R.string.disable_phone_vibration_title),
                desc = stringResource(R.string.disable_phone_vibration_description),
                default = appConfig.disablePhoneVibrate,
            ) {
                appConfig.disablePhoneVibrate = it
            }

            switchPref(
                name = stringResource(R.string.disable_phone_beep_title),
                desc = stringResource(R.string.disable_phone_beep_description),
                default = appConfig.disablePhoneBeep,
            ) {
                appConfig.disablePhoneBeep = it
            }

            switchPref(
                name = stringResource(R.string.use_wheel_beep_for_alarm_title),
                desc = stringResource(R.string.use_wheel_beep_for_alarm_description),
                default = appConfig.useWheelBeepForAlarm,
            ) {
                appConfig.useWheelBeepForAlarm = it
            }

            if (!pwmBasedAlarms) {
                // Default alarms
                group(
                    name = stringResource(R.string.speed_alarm1_phone_title)
                ) {
                    sliderPref(
                        name = stringResource(R.string.speed),
                        desc = stringResource(R.string.speed_trigger_description),
                        position = (appConfig.alarm1Speed * speedMultipier).toFloat(),
                        unit = speedUnit,
                        min = 0f,
                        max = 100f,
                    ) {
                        appConfig.alarm1Speed = (it / speedMultipier).toInt()
                    }

                    sliderPref(
                        name = stringResource(R.string.alarm_1_battery_title),
                        desc = stringResource(R.string.alarm_1_battery_description),
                        position = appConfig.alarm1Battery.toFloat(),
                        unit = R.string.persent,
                        min = 0f,
                        max = 100f,
                        showDiv = false,
                    ) {
                        appConfig.alarm1Battery = it.toInt()
                    }
                }
                group(
                    name = stringResource(R.string.speed_alarm2_phone_title)
                ) {
                    sliderPref(
                        name = stringResource(R.string.speed),
                        desc = stringResource(R.string.speed_trigger_description),
                        position = (appConfig.alarm2Speed * speedMultipier).toFloat(),
                        unit = speedUnit,
                        min = 0f,
                        max = 100f,
                    ) {
                        appConfig.alarm2Speed = (it / speedMultipier).toInt()
                    }

                    sliderPref(
                        name = stringResource(R.string.alarm_2_battery_title),
                        desc = stringResource(R.string.alarm_1_battery_description),
                        position = appConfig.alarm2Battery.toFloat(),
                        unit = R.string.persent,
                        min = 0f,
                        max = 100f,
                        showDiv = false,
                    ) {
                        appConfig.alarm2Battery = it.toInt()
                    }
                }
                group(
                    name = stringResource(R.string.speed_alarm3_phone_title)
                ) {
                    sliderPref(
                        name = stringResource(R.string.speed),
                        desc = stringResource(R.string.speed_trigger_description),
                        position = (appConfig.alarm3Speed * speedMultipier).toFloat(),
                        unit = speedUnit,
                        min = 0f,
                        max = 100f,
                    ) {
                        appConfig.alarm3Speed = (it / speedMultipier).toInt()
                    }

                    sliderPref(
                        name = stringResource(R.string.alarm_3_battery_title),
                        desc = stringResource(R.string.alarm_1_battery_description),
                        position = appConfig.alarm3Battery.toFloat(),
                        unit = R.string.persent,
                        min = 0f,
                        max = 100f,
                        showDiv = false,
                    ) {
                        appConfig.alarm3Battery = it.toInt()
                    }
                }
            } else {
                // Altered alarms
                group(
                    name = stringResource(R.string.pwm_based_alarms_title)
                ) {
                    if (!ksAlteredAlarms && !appConfig.hwPwm) {
                        sliderPref(
                            name = stringResource(R.string.rotation_speed_title),
                            desc = stringResource(R.string.rotation_speed_description),
                            position = (appConfig.rotationSpeed * speedMultipier / 10).toFloat(),
                            unit = speedUnit,
                            min = 0f,
                            max = 250f,
                            format = "%.1f",
                        ) {
                            appConfig.rotationSpeed = (it / speedMultipier * 10).toInt()
                        }

                        sliderPref(
                            name = stringResource(R.string.rotation_voltage_title),
                            desc = stringResource(R.string.rotation_voltage_description),
                            position = (appConfig.rotationVoltage / 10).toFloat(),
                            unit = R.string.volt,
                            min = 0f,
                            max = 250f,
                            format = "%.1f",
                        ) {
                            appConfig.rotationVoltage = (it * 10).toInt()
                        }

                        sliderPref(
                            name = stringResource(R.string.power_factor_title),
                            desc = stringResource(R.string.power_factor_description),
                            position = appConfig.powerFactor.toFloat(),
                            unit = R.string.persent,
                            min = 0f,
                            max = 100f,
                        ) {
                            appConfig.powerFactor = it.toInt()
                        }
                    }

                    sliderPref(
                        name = stringResource(R.string.alarm_factor1_title),
                        desc = stringResource(R.string.alarm_factor1_description),
                        position = appConfig.alarmFactor1.toFloat(),
                        unit = R.string.persent,
                        min = 0f,
                        max = 99f,
                    ) {
                        appConfig.alarmFactor1 = it.toInt()
                    }

                    sliderPref(
                        name = stringResource(R.string.alarm_factor2_title),
                        desc = stringResource(R.string.alarm_factor2_description),
                        position = appConfig.alarmFactor2.toFloat(),
                        unit = R.string.persent,
                        min = 0f,
                        max = 99f,
                    ) {
                        appConfig.alarmFactor2 = it.toInt()
                    }

                    var warnSpeedEnabled by remember { mutableStateOf(appConfig.warningSpeed != 0) }
                    var warnPwmEnabled by remember { mutableStateOf(appConfig.warningPwm != 0) }

                    sliderPref(
                        name = stringResource(R.string.warning_speed_title),
                        desc = stringResource(R.string.warning_speed_description),
                        position = (appConfig.warningSpeed * speedMultipier).toFloat(),
                        unit = speedUnit,
                        min = 0f,
                        max = 120f,
                        showSwitch = true,
                        disableSwitchAtMin = true,
                    ) {
                        appConfig.warningSpeed = (it / speedMultipier).toInt()
                        warnSpeedEnabled = it > 0
                    }

                    sliderPref(
                        name = stringResource(R.string.warning_pwm_title),
                        desc = stringResource(R.string.warning_pwm_description),
                        position = appConfig.warningPwm.toFloat(),
                        unit = R.string.persent,
                        min = 0f,
                        max = 99f,
                        showSwitch = true,
                        disableSwitchAtMin = true,
                    ) {
                        appConfig.warningPwm = it.toInt()
                        warnPwmEnabled = it > 0
                    }

                    AnimatedVisibility (warnSpeedEnabled || warnPwmEnabled) {
                        var position = appConfig.warningSpeedPeriod.toFloat()
                        if (position == 0f) {
                            appConfig.warningSpeedPeriod = 5
                            position = 5f
                        }
                        sliderPref(
                            name = stringResource(R.string.warning_speed_period_title),
                            desc = stringResource(R.string.warning_speed_period_description),
                            position = position,
                            unit = R.string.sec,
                            min = 0f,
                            max = 60f,
                            showDiv = false,
                            showSwitch = true,
                            disableSwitchAtMin = true,
                        ) {
                            appConfig.warningSpeedPeriod = it.toInt()
                        }
                    }
                }
            }

            sliderPref(
                name = stringResource(R.string.current_alarm_title),
                desc = stringResource(R.string.alarm_current_description),
                position = appConfig.alarmCurrent.toFloat(),
                unit = R.string.amp,
                min = 0f,
                max = 300f,
            ) {
                appConfig.alarmCurrent = it.toInt()
            }

            // TODO: Add Fahrenheit support
            sliderPref(
                name = stringResource(R.string.temperature_alarm_title),
                desc = stringResource(R.string.alarm_temperature_description),
                position = appConfig.alarmTemperature.toFloat(),
                unit = R.string.degree,
                min = 0f,
                max = 120f,
            ) {
                appConfig.alarmTemperature = it.toInt()
            }

            sliderPref(
                name = stringResource(R.string.battery_alarm_title),
                desc = stringResource(R.string.alarm_battery_description),
                position = appConfig.alarmBattery.toFloat(),
                unit = R.string.persent,
                min = 0f,
                max = 100f,
            ) {
                appConfig.alarmBattery = it.toInt()
            }
            if (wheelAlarm) {
                switchPref(
                        name = stringResource(R.string.alarm_wheel_title),
                        desc = stringResource(R.string.alarm_wheel_description),
                        default = appConfig.alarmWheel,
                ) {
                    appConfig.alarmWheel = it
                }
            }
        }
    }
}
