package com.cooper.wheellog.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.wheellog.shared.WearPage
import org.koin.compose.koinInject
import java.util.EnumSet

@Composable
fun watchScreen(appConfig: AppConfig = koinInject()) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        group(name = stringResource(R.string.watch_wearos_group_title)) {
            multiList(
                name = stringResource(R.string.wearos_pages_title),
                desc = stringResource(R.string.wearos_pages_description),
                entries = WearPage.values().associate { it.name to it.name },
                defaultKeys = appConfig.wearOsPages.map { it.name },
                showDiv = false,
            ) { list ->
                EnumSet.copyOf(list.map { WearPage.valueOf(it) }).also { enumSet ->
                    appConfig.wearOsPages = enumSet
                }
            }
        }

        group(name = stringResource(R.string.watch_pebble_group_title)) {
            list(
                name = stringResource(R.string.horn_mode_title),
                desc = stringResource(R.string.horn_mode_description),
                entries = mapOf(
                    "0" to stringResource(R.string.disabled),
                    "1" to stringResource(R.string.on_board_horn_ks),
                    "2" to stringResource(R.string.bluetooth_audio_horn)
                ),
                defaultKey = appConfig.hornMode.toString(),
                showDiv = false,
            ) {
                appConfig.hornMode = it.first.toInt()
            }
        }

        group(name = stringResource(R.string.watch_garmin_group_title)) {
            switchPref(
                name = stringResource(R.string.garmin_connectiq_enable_title),
                desc = stringResource(R.string.garmin_connectiq_enable_description),
                default = appConfig.garminConnectIqEnable,
            ) {
                appConfig.garminConnectIqEnable = it
            }

            switchPref(
                name = stringResource(R.string.garmin_connectiq_use_beta_title),
                desc = stringResource(R.string.garmin_connectiq_use_beta_description),
                default = appConfig.useGarminBetaCompanion,
                showDiv = false,
            ) {
                appConfig.useGarminBetaCompanion = it
            }
        }

        group(name = stringResource(R.string.watch_miband_group_title)) {
            switchPref(
                name = stringResource(R.string.miband_fixrs_title),
                desc = stringResource(R.string.miband_fixrs_description),
                default = appConfig.mibandFixRs,
                showDiv = false,
            ) {
                appConfig.mibandFixRs = it
            }
        }
    }
}