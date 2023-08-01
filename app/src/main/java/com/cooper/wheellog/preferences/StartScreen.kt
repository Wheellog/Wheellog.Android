package com.cooper.wheellog.preferences

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.BuildConfig
import com.cooper.wheellog.DialogHelper.setBlackIcon
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager

@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit = {},
)
{
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        val context: Context = LocalContext.current
        SettingsClickableComp(
            name = R.string.speed_settings_title,
            themeIcon = ThemeIconEnum.SettingsSpeedometer
        ) {
            onSelect(SettingsScreenEnum.Application.name)
        }
        SettingsClickableComp(
            name = R.string.logs_settings_title,
            themeIcon = ThemeIconEnum.SettingsLog
        ) {
            onSelect(SettingsScreenEnum.Log.name)
        }
        SettingsClickableComp(
            name = R.string.alarm_settings_title,
            themeIcon = ThemeIconEnum.SettingsVibration
        ) {
            onSelect(SettingsScreenEnum.Alarm.name)
        }
        SettingsClickableComp(
            name = R.string.watch_settings_title,
            themeIcon = ThemeIconEnum.SettingsWatch
        ) {
            onSelect(SettingsScreenEnum.Watch.name)
        }
        SettingsClickableComp(
            name = R.string.wheel_settings_title,
            themeIcon = ThemeIconEnum.SettingsWheel
        ) {
            onSelect(SettingsScreenEnum.Wheel.name)
        }
        SettingsClickableComp(
            name = R.string.trip_settings_title,
            themeIcon = ThemeIconEnum.SettingsTrips
        ) {
            onSelect(SettingsScreenEnum.Trip.name)
        }
        SettingsClickableComp(
            name = R.string.bug_report,
            themeIcon = ThemeIconEnum.SettingsBug,
            showArrowIcon = false
        ) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/Wheellog/Wheellog.Android/issues")
            }
            context.startActivity(intent)
        }
        SettingsClickableComp(
            name = R.string.donate_title,
            themeIcon = ThemeIconEnum.SettingsDonate,
            showArrowIcon = false
        ) {
            val kvm = mapOf(
                "Crypto" to "https://paymicro.github.io/donate",
                "Credit card (only from russian bank)" to "https://tinkoff.ru/sl/6iw4b0ugfpC")
            AlertDialog.Builder(context)
                .setTitle(R.string.donate_title)
                .setItems(kvm.keys.toTypedArray()) { _, which ->
                    val uri = Uri.parse(kvm[kvm.keys.elementAt(which)])
                    startActivity(context, Intent(Intent.ACTION_VIEW, uri), null)
                }
                .setIcon(R.drawable.ic_donate_24)
                .show()
                .setBlackIcon()
        }
        SettingsClickableComp(
            name = R.string.about_app_title,
            themeIcon = ThemeIconEnum.SettingsAbout,
            showArrowIcon = false
        ) {
            val versionName = BuildConfig.VERSION_NAME
            val buildTime = BuildConfig.BUILD_TIME
            AlertDialog.Builder(context)
                .setTitle(R.string.about_app_title)
                .setMessage(String.format("Version %s \n build at %s github.com/Wheellog/Wheellog.Android \n Thanks to all contributors", versionName, buildTime))
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
                .setIcon(android.R.drawable.ic_dialog_info)
                .show()
                .setBlackIcon()
        }
    }
}

@Preview
@Composable
fun StartScreenPreview()
{
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    StartScreen()
}