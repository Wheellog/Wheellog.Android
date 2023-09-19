package com.cooper.wheellog.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.BuildConfig
import com.cooper.wheellog.LocaleManager
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager

@Composable
fun startScreen(
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit = {},
)
{
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        val context: Context = LocalContext.current

        var isSpecificVisible by remember {
            mutableStateOf(
                WheelData.getInstance()?.wheelType != Constants.WHEEL_TYPE.Unknown
            )
        }
        systemBroadcastReceiver(systemAction = Constants.ACTION_WHEEL_MODEL_CHANGED) { intent ->
            if (intent?.action == Constants.ACTION_WHEEL_MODEL_CHANGED) {
                isSpecificVisible =
                    WheelData.getInstance()?.wheelType != Constants.WHEEL_TYPE.Unknown
            }
        }

        clickablePref(
            name = stringResource(R.string.speed_settings_title),
            themeIcon = ThemeIconEnum.SettingsSpeedometer,
        ) {
            onSelect(SettingsScreenEnum.Application.name)
        }
        clickablePref(
            name = stringResource(R.string.logs_settings_title),
            themeIcon = ThemeIconEnum.SettingsLog,
        ) {
            onSelect(SettingsScreenEnum.Log.name)
        }
        if (isSpecificVisible) {
            clickablePref(
                name = stringResource(R.string.alarm_settings_title),
                themeIcon = ThemeIconEnum.SettingsVibration,
            ) {
                onSelect(SettingsScreenEnum.Alarm.name)
            }
        }
        clickablePref(
            name = stringResource(R.string.watch_settings_title),
            themeIcon = ThemeIconEnum.SettingsWatch,
        ) {
            onSelect(SettingsScreenEnum.Watch.name)
        }
        if (isSpecificVisible) {
            clickablePref(
                name = stringResource(R.string.wheel_settings_title),
                themeIcon = ThemeIconEnum.SettingsWheel,
            ) {
                onSelect(SettingsScreenEnum.Wheel.name)
            }
            clickablePref(
                name = stringResource(R.string.trip_settings_title),
                themeIcon = ThemeIconEnum.SettingsTrips,
            ) {
                onSelect(SettingsScreenEnum.Trip.name)
            }
        }
        var showDonateDialog by remember { mutableStateOf(false) }
        if (showDonateDialog) {
            AlertDialog(
                shape = RoundedCornerShape(8.dp),
                onDismissRequest = { showDonateDialog = false },
                title = {
                    Row {
                        Icon(
                            painter = painterResource(
                                id = ThemeManager.getId(ThemeIconEnum.SettingsDonate)
                            ),
                            contentDescription = "info",
                            modifier = Modifier.size(32.dp).padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.donate_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                text = {
                    Column {
                        clickablePref(
                            name = stringResource(R.string.donate_crypto),
                        ) {
                            startActivity(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://paymicro.github.io/donate".toUri()
                                ),
                                null
                            )
                        }
                        clickablePref(
                            name = stringResource(R.string.donate_bank_ru),
                            showDiv = false
                        ) {
                            startActivity(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://tinkoff.ru/sl/6iw4b0ugfpC".toUri()
                                ),
                                null
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showDonateDialog = false }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                },
            )
        }
        var showAboutDialog by remember { mutableStateOf(false) }

        if (showAboutDialog) {
            AlertDialog(
                shape = RoundedCornerShape(8.dp),
                onDismissRequest = { showAboutDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.about_app_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}\n" +
                                    "Build at ${BuildConfig.BUILD_TIME}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        clickablePref(
                            name = stringResource(R.string.github),
                            desc = stringResource(R.string.github_desc),
                        ) {
                            startActivity(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://github.com/Wheellog/Wheellog.Android".toUri()
                                ),
                                null
                            )
                        }
                        clickablePref(
                            name = stringResource(R.string.FAQ),
                        ) {
                            startActivity(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://github.com/Wheellog/Wheellog.Android/wiki".toUri()
                                ),
                                null
                            )
                        }
                        clickablePref(
                            name = stringResource(R.string.bug_report),
                        ) {
                            startActivity(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://github.com/Wheellog/Wheellog.Android/issues".toUri()
                                ),
                                null
                            )
                        }
                        if (isSpecificVisible) {
                            clickablePref(
                                name = stringResource(R.string.donate_title),
                            ) {
                                showAboutDialog = false
                                showDonateDialog = true
                            }
                        }
                        clickablePref(
                            name = stringResource(R.string.telegram),
                            showDiv = false,
                        ) {
                            val url = if (LocaleManager.getLocale(context.resources).language == "ru")
                                "https://t.me/wheelLog"
                            else
                                "https://t.me/WheelLog_eng"
                            startActivity(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    url.toUri()
                                ),
                                null
                            )
                        }
                        Text(
                            text = stringResource(R.string.about_app_desc),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showAboutDialog = false }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                },
            )
        }
        clickablePref(
            name = stringResource(R.string.about_app_title),
            themeIcon = ThemeIconEnum.SettingsAbout,
            showArrowIcon = false,
            showDiv = false,
        ) {
            showAboutDialog = true
        }
    }
}

@Preview
@Composable
fun startScreenPreview()
{
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    startScreen()
}