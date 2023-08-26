package com.cooper.wheellog.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog.Companion.AppConfig
import com.wheellog.shared.WearPage
import java.util.EnumSet

@Composable
fun watchScreen( )
{
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        list(
            name = R.string.horn_mode_title,
            desc = R.string.horn_mode_description,
            entries = mapOf(
                "0" to stringResource(R.string.disabled),
                "1" to stringResource(R.string.on_board_horn_ks),
                "2" to stringResource(R.string.bluetooth_audio_horn)
            ),
            defaultKey = AppConfig.hornMode.toString(),
        ) {
            AppConfig.hornMode = it.first.toInt()
        }

        switchPref(
            name = R.string.garmin_connectiq_enable_title,
            desc = R.string.garmin_connectiq_enable_description,
            default = AppConfig.garminConnectIqEnable,
        ) {
            AppConfig.garminConnectIqEnable = it
        }

        switchPref(
            name = R.string.garmin_connectiq_use_beta_title,
            desc = R.string.garmin_connectiq_use_beta_description,
            default = AppConfig.useGarminBetaCompanion,
        ) {
            AppConfig.useGarminBetaCompanion = it
        }

        switchPref(
            name = R.string.miband_on_mainscreen_title,
            desc = R.string.miband_on_mainscreen_description,
            default = AppConfig.mibandOnMainscreen,
        ) {
            AppConfig.mibandOnMainscreen = it
        }

        switchPref(
            name = R.string.miband_fixrs_title,
            desc = R.string.miband_fixrs_description,
            default = AppConfig.mibandFixRs,
        ) {
            AppConfig.mibandFixRs = it
        }

        multiList(
            name = R.string.wearos_pages_title,
            desc = R.string.wearos_pages_description,
            entries = WearPage.values().associate { it.name to it.name },
            defaultKeys = AppConfig.wearOsPages.map { it.name },
        ) { list ->
            EnumSet.copyOf(list.map { WearPage.valueOf(it) }).also { enumSet ->
                AppConfig.wearOsPages = enumSet
            }
        }
    }
}