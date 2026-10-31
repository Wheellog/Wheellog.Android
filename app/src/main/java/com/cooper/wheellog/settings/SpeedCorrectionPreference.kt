package com.cooper.wheellog.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelSpeedCorrection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.util.Locale

@Composable
fun speedCorrectionPreference(config: AppConfig) {
    var enabled by remember { mutableStateOf(config.useSpeedCorrection) }
    var percent by remember { mutableDoubleStateOf(config.speedCorrectionPercent) }
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    fun updatePercent(value: Double) {
        config.speedCorrectionPercent = value
        percent = config.speedCorrectionPercent
    }
    switchPref(name = stringResource(R.string.speed_correction_title),
        desc = stringResource(R.string.speed_correction_description), default = enabled) {
        enabled = it; config.useSpeedCorrection = it
    }
    AnimatedVisibility(enabled) {
        Column {
            Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = percent > WheelSpeedCorrection.MIN_PERCENT, onClick = { updatePercent(percent - WheelSpeedCorrection.STEP_PERCENT) }) {
                    Icon(Icons.Default.Remove, stringResource(R.string.speed_correction_decrease))
                }
                Slider(value = percent.toFloat(), onValueChange = {
                    updatePercent(it.toDouble())
                }, steps = WheelSpeedCorrection.SLIDER_STEPS,
                    valueRange = WheelSpeedCorrection.MIN_PERCENT.toFloat()..WheelSpeedCorrection.MAX_PERCENT.toFloat(), modifier = Modifier.weight(1f))
                IconButton(enabled = percent < WheelSpeedCorrection.MAX_PERCENT, onClick = { updatePercent(percent + WheelSpeedCorrection.STEP_PERCENT) }) {
                    Icon(Icons.Default.Add, stringResource(R.string.speed_correction_increase))
                }
                Text(WheelSpeedCorrection.formatPercent(percent, locale),
                    modifier = Modifier.widthIn(min = 64.dp).padding(start = 4.dp),
                    textAlign = TextAlign.End, maxLines = 1, softWrap = false)
            }
        }
    }
}
