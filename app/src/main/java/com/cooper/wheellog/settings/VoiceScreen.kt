package com.cooper.wheellog.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cooper.wheellog.R
import com.cooper.wheellog.voice.VoiceAnnouncements
import com.cooper.wheellog.voice.VoiceSettings
import java.util.Locale

@Composable
fun voiceScreen() {
    val context = LocalContext.current
    val prefs = remember(context) { VoiceSettings(context) }
    val state by VoiceAnnouncements.state.collectAsState()
    var revision by remember { mutableIntStateOf(0) }
    var externalError by remember { mutableStateOf(false) }
    val changed: () -> Unit = { revision++; VoiceAnnouncements.settingsChanged() }
    DisposableEffect(context) {
        VoiceAnnouncements.openSettings(context)
        onDispose { VoiceAnnouncements.closeSettings() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.voice_language_note), Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium)
        switchPref(name = stringResource(R.string.voice_enable), default = prefs.enabled) {
            prefs.enabled = it; changed()
        }
        switchPref(name = stringResource(R.string.voice_alarms), desc = stringResource(R.string.voice_alarm_note), default = prefs.alarms) {
            prefs.alarms = it; changed()
        }
        switchPref(name = stringResource(R.string.voice_connection), default = prefs.connection) { prefs.connection = it; changed() }
        switchPref(name = stringResource(R.string.voice_reports), default = prefs.reports) { prefs.reports = it; changed() }
        voiceReportTriggers(prefs, state, changed)
        switchPref(name = stringResource(R.string.voice_moving), desc = stringResource(R.string.voice_moving_note), default = prefs.movingOnly) {
            prefs.movingOnly = it; changed()
        }
        group(stringResource(R.string.voice_report_fields)) {
            for ((id, title) in listOf("speed" to R.string.speed,
                    "average_moving_speed" to R.string.average_riding_speed, "average_speed" to R.string.average_speed,
                    "distance" to R.string.distance, "ride_time" to R.string.voice_field_time,
                    "battery" to R.string.battery, "voltage" to R.string.voice_field_voltage,
                    "current" to R.string.voice_field_current, "power" to R.string.voice_field_power, "pwm" to R.string.voice_field_pwm,
                    "temperature" to R.string.voice_field_temperature, "motor_temperature" to R.string.voice_field_motor_temperature)) {
                switchPref(name = stringResource(title), default = id in prefs.fields) { enabled ->
                    prefs.fields = if (enabled) prefs.fields + id else prefs.fields - id; changed()
                }
            }
        }
        TextButton(onClick = { VoiceAnnouncements.speakReport() }, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(stringResource(R.string.voice_report_now))
        }
        group(stringResource(R.string.voice_voices)) {
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                Text(stringResource(R.string.voice_loading), Modifier.padding(20.dp))
            }
            val engineEntries = state.engines.associate { it.name to it.label }
            key(state.engine, engineEntries) {
                list(name = stringResource(R.string.voice_engine), entries = engineEntries, defaultKey = state.engine) { selected ->
                    prefs.engine = selected.first; revision++; VoiceAnnouncements.settingsChanged(restartEngine = true)
                }
            }
            for ((language, title) in listOf("ru" to R.string.voice_russian, "en" to R.string.voice_english)) {
                val choices = state.voices.filter { it.language == language }
                val entries = mapOf("auto" to stringResource(R.string.voice_auto)) + choices.mapIndexed { index, choice ->
                    val country = Locale("", choice.country).getDisplayCountry(context.resources.configuration.let {
                        @Suppress("DEPRECATION")
                        if (android.os.Build.VERSION.SDK_INT >= 24) it.locales[0] else it.locale
                    })
                    choice.name to (context.getString(R.string.voice_number, index + 1) + if (country.isNotBlank()) " · $country" else "")
                }.toMap()
                key(state.engine, language, entries, revision) {
                    list(name = stringResource(title), desc = if (choices.isEmpty()) stringResource(R.string.voice_no_offline_voice)
                        else stringResource(R.string.voice_choose_note), entries = entries, defaultKey = prefs.voice(language).ifEmpty { "auto" }) { selected ->
                        val voiceName = selected.first.takeUnless { it == "auto" } ?: ""
                        prefs.setVoice(language, voiceName); revision++
                        VoiceAnnouncements.preview(context, language, voiceName)
                    }
                }
                TextButton(onClick = { VoiceAnnouncements.preview(context, language) }, enabled = choices.isNotEmpty(),
                    modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(stringResource(if (language == "ru") R.string.voice_preview_ru else R.string.voice_preview_en))
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                TextButton(onClick = { VoiceAnnouncements.stop() }) { Text(stringResource(R.string.voice_stop)) }
                TextButton(onClick = { VoiceAnnouncements.settingsChanged(restartEngine = true) }) { Text(stringResource(R.string.voice_refresh)) }
            }
            if (state.error != 0) Text(stringResource(state.error), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
            if (externalError) Text(stringResource(R.string.voice_install_error), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
            clickablePref(name = stringResource(R.string.voice_install), desc = stringResource(R.string.voice_install_note)) {
                try {
                    val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                        if (state.engine.isNotBlank()) setPackage(state.engine)
                    }
                    context.startActivity(intent)
                    externalError = false
                } catch (_: ActivityNotFoundException) {
                    try { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                    catch (_: ActivityNotFoundException) { externalError = true }
                }
            }
        }
        sliderPref(name = stringResource(R.string.voice_rate), position = prefs.ratePercent.toFloat(), min = 50f, max = 175f) {
            prefs.ratePercent = it.toInt(); changed()
        }
        sliderPref(name = stringResource(R.string.voice_volume), desc = stringResource(R.string.voice_volume_note),
            position = prefs.volumePercent.toFloat(), min = 10f, max = 100f) { prefs.volumePercent = it.toInt(); changed() }
        sliderPref(name = stringResource(R.string.voice_repeat), position = prefs.alarmRepeatSeconds.toFloat(), min = 5f, max = 60f) {
            prefs.alarmRepeatSeconds = it.toInt(); changed()
        }
        list(name = stringResource(R.string.voice_music_mode), desc = stringResource(R.string.voice_music_mode_note),
            entries = mapOf("mix" to stringResource(R.string.voice_music_mix),
                "duck" to stringResource(R.string.voice_music_duck), "pause" to stringResource(R.string.voice_music_pause)),
            defaultKey = prefs.musicMode) { prefs.musicMode = it.first; changed() }
        Spacer(Modifier.height(24.dp))
    }
}
