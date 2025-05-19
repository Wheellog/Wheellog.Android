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
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.BuildConfig
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.MainActivity
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.FileUtil
import com.cooper.wheellog.utils.PermissionsUtil
import com.cooper.wheellog.utils.ThemeIconEnum
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun logScreen(appConfig: AppConfig = koinInject())
{
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        var autoLogDependency by remember { mutableStateOf(appConfig.autoLog) }
        val writePermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            appConfig.autoLog = granted
            autoLogDependency = granted
        }
        switchPref(
            name = stringResource(R.string.auto_log_title),
            desc = stringResource(R.string.auto_log_description),
            themeIcon = ThemeIconEnum.SettingsAutoLog,
            default = appConfig.autoLog,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appConfig.autoLog = it
                autoLogDependency = it
            } else {
                if (it) {
                    writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    appConfig.autoLog = false
                    autoLogDependency = false
                }
            }
        }

        AnimatedVisibility (autoLogDependency) {
            sliderPref(
                name = stringResource(R.string.auto_log_when_moving_title),
                desc = stringResource(R.string.auto_log_when_moving_description),
                position = appConfig.startAutoLoggingWhenIsMovingMore,
                min = 0f,
                max = 20f,
                unit = R.string.kmh,
                format = "%.1f",
                showSwitch = true,
                disableSwitchAtMin = true,
            ) {
                appConfig.startAutoLoggingWhenIsMovingMore = it
            }
        }

        val context = LocalContext.current
        var locationDependency by remember { mutableStateOf(appConfig.logLocationData) }

        switchPref(
            name = stringResource(R.string.log_location_title),
            desc = stringResource(R.string.log_location_description),
            themeIcon = ThemeIconEnum.SettingsLocation,
            default = locationDependency,
        ) {
            appConfig.logLocationData = it
            locationDependency = it
        }

        val gpsDependency = remember { mutableStateOf(appConfig.useGps) }
        if (gpsDependency.value && !PermissionsUtil.checkLocationPermission(context)) {
            gpsDependency.value = false
            appConfig.useGps = false
        }

        var alertGps by remember { mutableStateOf(false) }
        val locationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            gpsDependency.value = granted.all { gr -> gr.value }
            appConfig.useGps = gpsDependency.value
            alertGps = false
        }

        AnimatedVisibility (locationDependency) {
            switchPref(
                name = stringResource(R.string.use_gps_title),
                desc = stringResource(R.string.use_gps_description),
                defaultState = gpsDependency,
            ) {
                appConfig.useGps = it
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
                        appConfig.useGps = false
                        alertGps = false
                    }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                })
        }

        AnimatedVisibility (locationDependency && gpsDependency.value) {
            val autoUploadDependency = remember { mutableStateOf(appConfig.autoUploadEc) }
            Column {
                switchPref(
                    name = stringResource(R.string.auto_upload_log_ec_title),
                    desc = stringResource(R.string.auto_upload_log_ec_description),
                    defaultState = autoUploadDependency,
                ) {
                    appConfig.autoUploadEc = it
                    autoUploadDependency.value = it
                    if (!it) {
                        ElectroClub.instance.logout()
                    }
                }

                if (autoUploadDependency.value && WheelData.getInstance().isConnected) {
                    val activity = LocalContext.current as Activity
                    clickablePref(
                        name = stringResource(R.string.select_garage_ec_title),
                        desc = appConfig.ecGarage ?: "",
                    ) {
                        appConfig.ecGarage = null
                        ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                            mac = "",
                            activity = activity,
                        ) { }
                    }
                }

                if (autoUploadDependency.value) {
                    loginAlertDialog(
                        title = "electro.club",
                        onDismiss = {
                            autoUploadDependency.value = false
                            appConfig.autoUploadEc = false
                        },
                    ) { login, password ->
                        suspendCoroutine { continuation ->
                            ElectroClub.instance.login(
                                email = login,
                                password = password,
                            ) { success ->
                                val errorMessage = ElectroClub.instance.lastError ?: ""
                                if (success) {
                                    ElectroClub.instance.getAndSelectGarageByMacOrShowChooseDialog(
                                        WheelData.getInstance().mac,
                                        context as Activity
                                    ) { }
                                }
                                continuation.resume(Pair(success, errorMessage))
                            }
                        }
                    }
                }
            }
        }

        switchPref(
            name = stringResource(R.string.use_raw_title),
            desc = stringResource(R.string.use_raw_description),
            default = appConfig.enableRawData,
        ) {
            appConfig.enableRawData = it
        }

        switchPref(
            name = stringResource(R.string.continue_this_day_log_title),
            desc = stringResource(R.string.continue_this_day_log_description),
            default = appConfig.continueThisDayLog,
        ) {
            appConfig.continueThisDayLog = it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || BuildConfig.DEBUG) {
            val activity = (LocalContext.current as? MainActivity)
            clickablePref(
                name = stringResource(R.string.import_log),
            ) {
                activity?.getCsvResult?.launch("text/*")
            }

            clickablePref(
                name = stringResource(R.string.create_test_log),
            ) {
                activity?.let {
                    val fileUtil = FileUtil(activity.applicationContext)
                    val sdFormatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
                    val filename = sdFormatter.format(Date()) + ".csv"
                    if (fileUtil.prepareFile(filename, "test")) {
                        fileUtil.writeLine(
                        "date,time,latitude,longitude,gps_speed,gps_alt,gps_heading,gps_distance,speed,voltage,phase_current,current,power,torque,pwm,battery_level,distance,totaldistance,system_temp,temp2,tilt,roll,mode,alert\n" +
                             "2025-01-29,23:05:53.835,53.921541,27.4574593,0.0,259.5,0.0,0,14.72,83.28,0.00,0.88,73.29,0.00,44.06,95,1,4778555,24,0,0.13,1.42,Drive,\n" +
                             "2025-01-29,23:05:54.021,53.921541,27.4574593,0.0,259.5,0.0,0,19.92,83.28,0.00,1.04,86.61,0.00,59.62,95,1,4778555,24,0,0.14,1.26,Drive,\n" +
                             "2025-01-29,23:05:54.021,53.921541,27.4574593,0.0,259.5,0.0,0,19.92,83.28,0.00,1.04,86.61,0.00,59.62,95,1,4778555,24,0,0.14,1.26,Drive,\n" +
                             "2025-01-29,23:05:54.205,53.921541,27.4574593,0.0,259.5,0.0,0,22.05,83.28,0.00,1.33,110.76,0.00,65.99,95,1,4778555,24,0,0.09,0.51,Drive,"
                        )
                        fileUtil.close()
                    }
                }
            }
        }
    }
}
