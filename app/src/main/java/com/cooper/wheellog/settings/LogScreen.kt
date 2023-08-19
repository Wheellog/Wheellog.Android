package com.cooper.wheellog.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog.Companion.AppConfig
import com.cooper.wheellog.utils.ThemeIconEnum

@Composable
fun logScreen()
{
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        var autoLogDependency by remember { mutableStateOf(AppConfig.autoLog) }
        switchPref(
            name = R.string.auto_log_title,
            desc = R.string.auto_log_description,
            themeIcon = ThemeIconEnum.SettingsAutoLog,
            default = AppConfig.autoLog,
        ) {
            AppConfig.autoLog = it
            autoLogDependency = it
        }

        if (autoLogDependency) {
            sliderPref(
                name = R.string.auto_log_when_moving_title,
                desc = R.string.auto_log_when_moving_description,
                position = 7f,
                min = 3f,
                max = 20f,
                unit = R.string.kmh,
                showSwitch = true,

            ) {
                AppConfig.startAutoLoggingWhenIsMoving = it > 0
            }
//            switchPref(
//                name = R.string.auto_log_when_moving_title,
//                desc = R.string.auto_log_when_moving_description,
//                default = AppConfig.startAutoLoggingWhenIsMoving,
//            ) {
//                AppConfig.startAutoLoggingWhenIsMoving = it
//            }
        }

        var locationDependency by remember { mutableStateOf(AppConfig.logLocationData) }
        switchPref(
            name = R.string.log_location_title,
            desc = R.string.log_location_description,
            themeIcon = ThemeIconEnum.SettingsLocation,
            default = AppConfig.logLocationData,
        ) {
            AppConfig.logLocationData = it
            locationDependency = it
        }

        if (locationDependency) {
            var autoUploadDependency by remember { mutableStateOf(AppConfig.autoUploadEc) }
            switchPref(
                name = R.string.auto_upload_log_ec_title,
                desc = R.string.auto_upload_log_ec_description,
                default = AppConfig.autoUploadEc,
            ) {
                AppConfig.autoUploadEc = it
                autoUploadDependency = it
            }

            if (autoUploadDependency && WheelData.getInstance().isConnected) {
                val activity = LocalContext.current as Activity
                clickablePref(
                    name = R.string.select_garage_ec_title,
                ) {
                    AppConfig.ecGarage = null
                    ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                        mac = "",
                        activity = activity,
                    ) { }
                }
            }
        }

        switchPref(
            name = R.string.use_raw_title,
            desc = R.string.use_raw_description,
            default = AppConfig.enableRawData,
        ) {
            AppConfig.enableRawData = it
        }

        switchPref(
            name = R.string.continue_this_day_log_title,
            desc = R.string.continue_this_day_log_description,
            default = AppConfig.continueThisDayLog,
        ) {
            AppConfig.continueThisDayLog = it
        }
    }
}
