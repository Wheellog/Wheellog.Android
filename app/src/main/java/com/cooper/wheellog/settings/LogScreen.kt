package com.cooper.wheellog.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog.Companion.AppConfig
import com.cooper.wheellog.utils.PermissionsUtil
import com.cooper.wheellog.utils.ThemeIconEnum

@Composable
fun logScreen()
{
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        var autoLogDependency by remember { mutableStateOf(AppConfig.autoLog) }
        val writePermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            AppConfig.autoLog = granted
            autoLogDependency = granted
        }
        switchPref(
            name = stringResource(R.string.auto_log_title),
            desc = stringResource(R.string.auto_log_description),
            themeIcon = ThemeIconEnum.SettingsAutoLog,
            default = AppConfig.autoLog,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AppConfig.autoLog = it
                autoLogDependency = it
            } else {
                if (it) {
                    writePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    AppConfig.autoLog = false
                    autoLogDependency = false
                }
            }
        }

        AnimatedVisibility (autoLogDependency) {
            sliderPref(
                name = stringResource(R.string.auto_log_when_moving_title),
                desc = stringResource(R.string.auto_log_when_moving_description),
                position = AppConfig.startAutoLoggingWhenIsMovingMore,
                min = 0f,
                max = 20f,
                unit = R.string.kmh,
                format = "%.1f",
                showSwitch = true,
                disableSwitchAtMin = true,
            ) {
                AppConfig.startAutoLoggingWhenIsMovingMore = it
            }
        }

        val context = LocalContext.current
        var locationDependency by remember { mutableStateOf(AppConfig.logLocationData) }

        switchPref(
            name = stringResource(R.string.log_location_title),
            desc = stringResource(R.string.log_location_description),
            themeIcon = ThemeIconEnum.SettingsLocation,
            default = locationDependency,
        ) {
            AppConfig.logLocationData = it
            locationDependency = it
        }

        val gpsDependency = remember { mutableStateOf(AppConfig.useGps) }
        if (gpsDependency.value && !PermissionsUtil.checkLocationPermission(context)) {
            gpsDependency.value = false
            AppConfig.useGps = false
        }

        var alertGps by remember { mutableStateOf(false) }
        val locationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            gpsDependency.value = granted.all { gr -> gr.value }
            AppConfig.useGps = gpsDependency.value
            alertGps = false
        }

        AnimatedVisibility (locationDependency) {
            switchPref(
                name = stringResource(R.string.use_gps_title),
                desc = stringResource(R.string.use_gps_description),
                defaultState = gpsDependency,
            ) {
                AppConfig.useGps = it
                gpsDependency.value = it
                alertGps = it && !PermissionsUtil.checkLocationPermission(context)
            }
        }

        if (alertGps) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.log_location_title)) },
                text = { Text(stringResource(R.string.log_location_pop_up)) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            } else {
                                locationPermission.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    )
                                )
                            }
                            val mLocationManager = ContextCompat.getSystemService(context, LocationManager::class.java) as LocationManager
                            val mGPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                            if (!mGPS) {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                ContextCompat.startActivity(context, intent, null)
                            }
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        gpsDependency.value = false
                        AppConfig.useGps = false
                        alertGps = false
                    }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                })
        }

        AnimatedVisibility (locationDependency && gpsDependency.value) {
            var autoUploadDependency by remember { mutableStateOf(AppConfig.autoUploadEc) }
            Column {
                switchPref(
                    name = stringResource(R.string.auto_upload_log_ec_title),
                    desc = stringResource(R.string.auto_upload_log_ec_description),
                    default = AppConfig.autoUploadEc,
                ) {
                    AppConfig.autoUploadEc = it
                    autoUploadDependency = it
                }

                if (autoUploadDependency && WheelData.getInstance().isConnected) {
                    val activity = LocalContext.current as Activity
                    clickablePref(
                        name = stringResource(R.string.select_garage_ec_title),
                    ) {
                        AppConfig.ecGarage = null
                        ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                            mac = "",
                            activity = activity,
                        ) { }
                    }
                }
            }
        }

        switchPref(
            name = stringResource(R.string.use_raw_title),
            desc = stringResource(R.string.use_raw_description),
            default = AppConfig.enableRawData,
        ) {
            AppConfig.enableRawData = it
        }

        switchPref(
            name = stringResource(R.string.continue_this_day_log_title),
            desc = stringResource(R.string.continue_this_day_log_description),
            default = AppConfig.continueThisDayLog,
        ) {
            AppConfig.continueThisDayLog = it
        }
    }
}
