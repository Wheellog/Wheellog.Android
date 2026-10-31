package com.cooper.wheellog.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.voice.VoiceSettings
import com.cooper.wheellog.voice.VoiceUiState
import org.koin.compose.koinInject

private data class DistanceEditor(val field: Int, val metres: Long, val arm: Boolean = false)

@Composable
private fun distanceSwitch(name: String, checked: Boolean, desc: String = "", onChange: (Boolean) -> Unit) {
    baseSettings(name = name, desc = desc, rightContent = {
        Switch(checked = checked, onCheckedChange = onChange)
    })
}

@Composable
fun voiceReportTriggers(prefs: VoiceSettings, state: VoiceUiState, changed: () -> Unit) {
    val config: AppConfig = koinInject()
    val mac = state.wheelMac.ifBlank { config.lastMac }.takeUnless { it == "default" }.orEmpty()
    var distances by remember(prefs, mac) { mutableStateOf(prefs.distanceReports(mac)) }
    var editor by remember(mac) { mutableStateOf<DistanceEditor?>(null) }
    DisposableEffect(prefs, mac) {
        val stopObserving = prefs.observeDistanceReports(mac) { distances = it }
        onDispose { stopObserving() }
    }
    val notify = { distances = prefs.distanceReports(mac); changed() }
    val live = state.totalMetres.takeIf { state.wheelMac == mac }
    group(stringResource(R.string.voice_triggers)) {
        Text(stringResource(R.string.voice_triggers_note), Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium)
        switchPref(name = stringResource(R.string.voice_time_enabled), default = prefs.timedReports) {
            prefs.timedReports = it; notify()
        }
        sliderPref(name = stringResource(R.string.voice_interval), position = prefs.intervalSeconds.toFloat(), min = 15f, max = 1800f,
            desc = stringResource(R.string.voice_interval_note)) { prefs.intervalSeconds = it.toInt(); notify() }
        if (mac.isBlank()) {
            Text(stringResource(R.string.voice_distance_connect), Modifier.padding(20.dp))
        } else {
            key(mac) {
                distanceSwitch(name = stringResource(R.string.voice_distance_periodic), checked = distances.periodic) {
                    prefs.setPeriodicDistance(mac, it); notify()
                }
                clickablePref(name = stringResource(R.string.voice_distance_interval),
                    desc = stringResource(R.string.voice_distance_amount, distances.intervalMetres / 1000, distances.intervalMetres % 1000)) {
                    editor = DistanceEditor(1, distances.intervalMetres)
                }
                distanceSwitch(name = stringResource(R.string.voice_distance_after), desc = stringResource(R.string.voice_distance_after_note),
                    checked = distances.after) { prefs.setAfterDistance(mac, it, live); notify() }
                clickablePref(name = stringResource(R.string.voice_distance_after_value),
                    desc = stringResource(R.string.voice_distance_amount, distances.afterMetres / 1000, distances.afterMetres % 1000)) {
                    editor = DistanceEditor(2, distances.afterMetres)
                }
                distanceSwitch(name = stringResource(R.string.voice_distance_target), desc = stringResource(R.string.voice_distance_target_note),
                    checked = distances.target) { enabled ->
                    val target = distances.targetMetres
                    if (enabled && (target == null || live?.let { target <= it } == true)) {
                        editor = DistanceEditor(3, target ?: live ?: 0, arm = true)
                    } else prefs.setTargetDistance(mac, enabled)
                    notify()
                }
                clickablePref(name = stringResource(R.string.voice_distance_target_value),
                    desc = distances.targetMetres?.let { stringResource(R.string.voice_distance_amount, it / 1000, it % 1000) }
                        ?: stringResource(R.string.voice_distance_waiting)) {
                    editor = DistanceEditor(3, distances.targetMetres ?: live ?: 0)
                }
            }
            Text(live?.let { stringResource(R.string.voice_distance_current, it / 1000, it % 1000) }
                ?: stringResource(R.string.voice_distance_waiting), Modifier.padding(20.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    editor?.let { edit ->
        val title = stringResource(when (edit.field) {
            1 -> R.string.voice_distance_interval
            2 -> R.string.voice_distance_after_value
            else -> R.string.voice_distance_target_value
        })
        distanceDialog(title, edit.metres, if (edit.field == 3) live else null,
            onDismiss = { editor = null }, onSave = { metres ->
                when (edit.field) {
                    1 -> prefs.setDistanceInterval(mac, metres)
                    2 -> prefs.setAfterMetres(mac, metres, live)
                    else -> {
                        prefs.setTargetMetres(mac, metres)
                        if (edit.arm) prefs.setTargetDistance(mac, true)
                    }
                }
                editor = null; notify()
            })
    }
}

@Composable
private fun distanceDialog(title: String, initial: Long, current: Long?, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var km by remember { mutableStateOf(TextFieldValue((initial / 1000).toString(), TextRange(0, (initial / 1000).toString().length))) }
    var m by remember { mutableStateOf(TextFieldValue((initial % 1000).toString(), TextRange(0, (initial % 1000).toString().length))) }
    val kilometres = km.text.toLongOrNull()
    val metres = m.text.toLongOrNull()
    val amount = if (kilometres != null && kilometres in 0..9_999_999 && metres != null && metres in 0..999)
        kilometres * 1000 + metres else null
    val valid = amount != null && amount > (current ?: 0)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = km, onValueChange = { if (it.text.length <= 7) km = it },
                    modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.voice_kilometres)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = !valid)
                OutlinedTextField(value = m, onValueChange = { if (it.text.length <= 3) m = it },
                    modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.voice_metres)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = !valid)
            }
            if (!valid) Text(stringResource(if (current != null) R.string.voice_distance_future else R.string.voice_distance_invalid),
                color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        TextButton(onClick = { amount?.let(onSave) }, enabled = valid) { Text(stringResource(android.R.string.ok)) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } })
}
