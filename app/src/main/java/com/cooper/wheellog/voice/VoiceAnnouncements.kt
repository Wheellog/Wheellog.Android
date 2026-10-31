package com.cooper.wheellog.voice

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.LocaleManager
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.Constants.ALARM_TYPE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.java.KoinJavaComponent.get
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class VoiceEngineChoice(val name: String, val label: String)
data class VoiceChoice(val name: String, val language: String, val country: String)
data class VoiceUiState(val loading: Boolean = false, val error: Int = 0, val engine: String = "",
    val engines: List<VoiceEngineChoice> = emptyList(), val voices: List<VoiceChoice> = emptyList(),
    val speaking: Boolean = false, val wheelMac: String = "", val totalMetres: Long? = null,
    val distanceRevision: Int = 0)

/** Bluetooth owns telemetry, timing and the shared foreground media lifecycle. */
object VoiceAnnouncements {
    private val handler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(VoiceUiState())
    val state = mutableState.asStateFlow()
    private var context: Context? = null
    private var tts: TextToSpeech? = null
    private var audioOutput: VoiceAudioOutput? = null
    private var ready = false
    private var attached = false
    @Volatile private var settingsOpen = false
    private var connected = false
    private var connectedMac = ""
    private var connectionAnnounced = false
    private var pendingConnection: Boolean? = null
    private var sample: VoiceSample? = null
    private var generation = 0
    private var utterance = 0L
    private var activeId: String? = null
    private var activeManual = false
    private var pendingPreview: Pair<String, String>? = null
    private val captured = AtomicLong(-1000)
    private val policy = VoicePolicy()
    private val reportSchedule = VoiceReportSchedule()
    private val initializedOdometers = mutableSetOf<String>()
    private var focusRequest: AudioFocusRequest? = null
    private var heldFocusGain: Int? = null
    private val speechTimeout = Runnable { stopSpeech() }
    private val attributes = VoiceAudioPolicy.attributes()
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        Timber.i("Voice audio focus change=%d", change)
        if (change < 0) handler.post { stopSpeech() }
    }
    private val settings: VoiceSettings get() = VoiceSettings(requireNotNull(context))
    private val config: AppConfig get() = get(AppConfig::class.java)
    private val tick = object : Runnable {
        override fun run() {
            if (!attached) return
            val prefs = settings
            val now = SystemClock.elapsedRealtime()
            if (!prefs.enabled) {
                if (activeId != null && !activeManual) stopSpeech()
            } else {
                initialize()
                announceConnectionIfReady()
                val current = sample
                if (prefs.reports && current != null && fresh(current)) {
                    val due = reportSchedule.due(current.mac, now, current.totalMetres, current.tripMetres,
                        prefs.timedReports, prefs.intervalSeconds * 1000L, prefs.distanceReports(current.mac))
                    if (ready && due.any && policy.canReport(now) &&
                        (!prefs.movingOnly || current.speedKmh >= 10) && report(false)) {
                        reportSchedule.spoken(now, due)
                        prefs.finishDistanceReports(current.mac, due.after, due.target)
                        if (due.after || due.target) mutableState.value = mutableState.value.copy(
                            distanceRevision = mutableState.value.distanceRevision + 1)
                    }
                }
            }
            if (sample?.let { fresh(it) } != true && mutableState.value.totalMetres != null)
                mutableState.value = mutableState.value.copy(totalMetres = null)
            handler.postDelayed(this, 1000)
        }
    }
    private val connectionLost = Runnable {
        if (!connected && attached) {
            connectionAnnounced = false
            if (settings.enabled && settings.connection) {
                pendingConnection = false
                initialize(); announceConnectionIfReady()
            }
        }
    }

    private fun announceConnectionIfReady() {
        val event = pendingConnection ?: return
        if (!attached || !ready || !settings.enabled || !settings.connection || policy.priority >= 0) return
        if (event && (!connected || sample?.let { fresh(it) } != true) || !event && connected) return
        if (say(localized().getString(if (event) R.string.voice_connected else R.string.voice_disconnected), 1)) {
            pendingConnection = null
            if (event) connectionAnnounced = true
        }
    }

    private fun observeReportSchedule(value: VoiceSample) {
        val prefs = settings
        if (prefs.enabled && prefs.reports && fresh(value)) reportSchedule.due(value.mac, SystemClock.elapsedRealtime(),
            value.totalMetres, value.tripMetres, prefs.timedReports, prefs.intervalSeconds * 1000L, prefs.distanceReports(value.mac))
    }

    private fun bind(context: Context) {
        if (this.context !== context.applicationContext) { audioOutput?.stop(); audioOutput = null }
        this.context = context.applicationContext
        if (audioOutput == null) audioOutput = VoiceAudioOutput(context.applicationContext) { id, error ->
            handler.post { finished(id, error) }
        }
    }
    private fun language(): String = VoiceText.language(if (config.useEng) "en" else
        LocaleManager.getLocale(config.context.resources).language)
    private fun localized(language: String = language()) = VoiceText.localized(requireNotNull(context), language)
    private fun fresh(value: VoiceSample) = connected && SystemClock.elapsedRealtime() - value.capturedAt in 0..5000

    @JvmStatic fun attach(context: Context) = handler.post {
        bind(context); attached = true
        handler.removeCallbacks(tick); handler.post(tick)
    }
    @JvmStatic fun detach() = handler.post {
        attached = false; connected = false; sample = null; captured.set(-1000)
        connectedMac = ""; connectionAnnounced = false; pendingConnection = null
        handler.removeCallbacks(tick); handler.removeCallbacks(connectionLost)
        stopSpeech(); policy.reset(); reportSchedule.reset(); initializedOdometers.clear()
        mutableState.value = mutableState.value.copy(totalMetres = null)
        if (!settingsOpen) release()
    }
    @JvmStatic fun onConnected(context: Context, mac: String = "") = handler.post {
        bind(context)
        val address = mac.ifBlank { config.lastMac }
        // Duplicate discovery callbacks are not new connections and must not reset reports.
        if (connected && connectedMac == address) return@post
        if (connectedMac != address) connectionAnnounced = false
        connectedMac = address; connected = true; sample = null; captured.set(-1000)
        pendingConnection = null
        handler.removeCallbacks(connectionLost); policy.reset()
        reportSchedule.postponeTime(SystemClock.elapsedRealtime())
        mutableState.value = mutableState.value.copy(wheelMac = config.lastMac, totalMetres = null)
        if (settings.enabled) {
            initialize()
            // Wait for real telemetry and a ready TTS engine, regardless of startup order.
            if (settings.connection && !connectionAnnounced) pendingConnection = true
        }
    }
    @JvmStatic fun onDisconnected(context: Context, intentional: Boolean) = handler.post {
        bind(context)
        if (!connected) {
            if (intentional) {
                handler.removeCallbacks(connectionLost); pendingConnection = null; connectionAnnounced = false
            }
            return@post
        }
        connected = false; sample = null; captured.set(-1000); pendingConnection = null
        stopSpeech(); policy.reset(); handler.removeCallbacks(connectionLost)
        mutableState.value = mutableState.value.copy(totalMetres = null)
        if (!intentional) handler.postDelayed(connectionLost, 2000) else connectionAnnounced = false
    }
    @JvmStatic fun onTelemetry(context: Context, data: WheelData) {
        // Keep the original telemetry path untouched when voice is off and no
        // manual listening/report screen is open.
        if (!settingsOpen && !VoiceSettings(context).enabled) return
        val now = SystemClock.elapsedRealtime()
        val previous = captured.get()
        if (now - previous < 500 || !captured.compareAndSet(previous, now)) return
        val totalMetres = data.totalDistance
        val snapshot = VoiceSample(data.mac, now, data.speedDouble, data.batteryLevel,
            data.distanceDouble, data.voltageDouble, data.currentDouble, data.powerDouble,
            data.temperature, data.temperature2, data.calculatedPwm,
            totalMetres, data.getDistanceAtOdometer(totalMetres),
            data.averageRidingSpeedDouble, data.averageSpeedDouble, data.rideTimeString)
        handler.post {
            bind(context); sample = snapshot
            observeReportSchedule(snapshot)
            announceConnectionIfReady()
            if (fresh(snapshot) && snapshot.totalMetres > 0 && snapshot.mac.isNotBlank() && snapshot.mac != "default") {
                val first = initializedOdometers.add(snapshot.mac)
                settings.initializeDistance(snapshot.mac, snapshot.totalMetres, first)
                mutableState.value = mutableState.value.copy(wheelMac = snapshot.mac, totalMetres = snapshot.totalMetres,
                    distanceRevision = mutableState.value.distanceRevision + if (first) 1 else 0)
            }
        }
    }
    @JvmStatic fun onAlarm(context: Context, type: ALARM_TYPE, value: Double) = handler.post {
        bind(context)
        val prefs = settings
        if (!prefs.enabled || !prefs.alarms || !ready || sample?.let { fresh(it) } != true) return@post
        val severity = when (type) {
            ALARM_TYPE.WHEEL -> 5
            ALARM_TYPE.CURRENT, ALARM_TYPE.TEMPERATURE, ALARM_TYPE.SPEED3 -> 4
            ALARM_TYPE.PWM, ALARM_TYPE.SPEED2 -> 3
            else -> 2
        }
        if (policy.alarm(type.name, severity, SystemClock.elapsedRealtime(), prefs.alarmRepeatSeconds * 1000L)) {
            say(VoiceText.alarm(localized(), type, value), severity)
        }
    }

    fun openSettings(context: Context) = handler.post { bind(context); settingsOpen = true; initialize() }
    fun closeSettings() = handler.post {
        settingsOpen = false; pendingPreview = null
        if (!attached || !settings.enabled) release()
    }
    fun settingsChanged(restartEngine: Boolean = false) = handler.post {
        val gain = VoiceAudioPolicy.focusGain(settings.musicMode)
        if (activeId != null && heldFocusGain != gain) stopSpeech()
        if (restartEngine) { release(); initialize() }
        if (!settings.enabled && activeId != null && !activeManual) stopSpeech()
        policy.postponeReport(SystemClock.elapsedRealtime())
        reportSchedule.postponeTime(SystemClock.elapsedRealtime())
        sample?.let { observeReportSchedule(it) }
    }
    fun preview(context: Context, language: String, voice: String = "") = handler.post {
        bind(context)
        if (policy.priority > 1) return@post
        val selectedLanguage = VoiceText.language(language)
        if (!ready) { pendingPreview = selectedLanguage to voice; initialize(); return@post }
        say(localized(selectedLanguage).getString(R.string.voice_preview_text), 0, selectedLanguage, voice, manual = true)
    }
    fun speakReport() = handler.post { report(true) }
    fun stop() = handler.post { pendingPreview = null; stopSpeech() }

    private fun report(manual: Boolean): Boolean {
        if (policy.priority >= 0) return false
        if (!ready) {
            initialize()
            if (manual) mutableState.value = mutableState.value.copy(error = R.string.voice_engine_error)
            return false
        }
        val value = sample
        if (value == null || !fresh(value)) {
            if (manual) mutableState.value = mutableState.value.copy(error = R.string.voice_no_live_data)
            return false
        }
        if (!manual && !settings.enabled) return false
        val spoken = say(VoiceText.report(localized(), value, settings.fields, config.useMph), 0, manual = manual)
        if (spoken) {
            policy.postponeReport(SystemClock.elapsedRealtime())
            reportSchedule.postponeTime(SystemClock.elapsedRealtime())
        }
        return spoken
    }

    private fun initialize() {
        if (tts != null || context == null) return
        val token = ++generation
        ready = false
        mutableState.value = mutableState.value.copy(loading = true, error = 0)
        val callback = TextToSpeech.OnInitListener { result -> handler.post {
            if (token != generation) return@post
            if (result != TextToSpeech.SUCCESS) {
                mutableState.value = mutableState.value.copy(loading = false, error = R.string.voice_engine_error,
                    engines = tts?.engines.orEmpty().map { VoiceEngineChoice(it.name, it.label) })
                return@post
            }
            val engine = tts ?: return@post
            ready = true
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) { Timber.i("Voice synthesis started id=%s", id) }
                override fun onDone(id: String?) { handler.post {
                    if (id != activeId || id == null) return@post
                    Timber.i("Voice synthesis finished id=%s", id)
                    if (audioOutput?.play(id, settings.volumePercent / 100f) != true) finished(id, true)
                } }
                @Deprecated("TTS compatibility callback")
                override fun onError(id: String?) { handler.post { finished(id, true) } }
                override fun onStop(id: String?, interrupted: Boolean) { handler.post { finished(id, false) } }
            })
            val voices = offlineVoices()
            mutableState.value = mutableState.value.copy(loading = false, error = 0,
                engine = settings.engine.ifEmpty { engine.defaultEngine ?: "" },
                engines = engine.engines.map { VoiceEngineChoice(it.name, it.label) },
                voices = voices.map { VoiceChoice(it.name, it.locale.language, it.locale.country) })
            announceConnectionIfReady()
            pendingPreview?.let { (language, voice) ->
                pendingPreview = null
                say(localized(language).getString(R.string.voice_preview_text), 0, language, voice, manual = true)
            }
        } }
        try {
            tts = if (settings.engine.isEmpty()) TextToSpeech(context, callback) else TextToSpeech(context, callback, settings.engine)
        } catch (error: Exception) {
            Timber.w(error, "Cannot initialize voice engine")
            mutableState.value = mutableState.value.copy(loading = false, error = R.string.voice_engine_error)
        }
    }

    private fun offlineVoices(): List<Voice> = try {
        tts?.voices.orEmpty().filter { !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() &&
            it.locale.language in setOf("ru", "en") }.sortedWith(compareBy<Voice> { it.locale.toLanguageTag() }.thenBy { it.name })
    } catch (error: Exception) { Timber.w(error); emptyList() }

    private fun say(text: String, severity: Int, language: String = language(), requestedVoice: String = "", manual: Boolean = false): Boolean {
        val engine = tts
        if (!ready || engine == null || text.isBlank()) { policy.complete(); return false }
        val candidates = offlineVoices().filter { it.locale.language == language }
        val preferred = requestedVoice.ifEmpty { settings.voice(language) }
        val saved = candidates.firstOrNull { it.name == preferred }
        val voice = saved ?: run {
            // As in EUC World, ask the engine for its default for the language.
            // Never speak with a network-only voice, even if it is the engine default.
            val languageVoice = runCatching {
                if (engine.setLanguage(Locale(language)) >= 0) engine.voice else null
            }.getOrNull()
            VoiceAudioPolicy.chooseVoice(candidates, language, preferred, languageVoice,
                runCatching { engine.defaultVoice }.getOrNull())
        }
        if (voice == null || engine.setVoice(voice) != TextToSpeech.SUCCESS) {
            mutableState.value = mutableState.value.copy(error = R.string.voice_no_offline_voice)
            policy.complete(); return false
        }
        if (VoiceAudioPolicy.focusGain(settings.musicMode) != null && !requestFocus()) {
            mutableState.value = mutableState.value.copy(error = R.string.voice_audio_unavailable)
            policy.complete(); return false
        }
        engine.setSpeechRate(settings.ratePercent / 100f)
        activeId = "voice-${++utterance}"
        activeManual = manual
        handler.removeCallbacks(speechTimeout)
        handler.postDelayed(speechTimeout, 60000)
        policy.occupy(severity)
        val result = try {
            // Replace both queued synthesis and local playback when an alarm preempts a report.
            engine.stop()
            val file = requireNotNull(audioOutput).prepare(requireNotNull(activeId))
            engine.synthesizeToFile(text, Bundle(), file, requireNotNull(activeId))
        } catch (error: Exception) {
            Timber.w(error, "Cannot synthesize voice audio")
            TextToSpeech.ERROR
        }
        Timber.i("Voice synthesis result=%d mode=%s volume=%d voice=%s", result, settings.musicMode, settings.volumePercent, voice.name)
        mutableState.value = mutableState.value.copy(error = 0, speaking = result == TextToSpeech.SUCCESS)
        if (result != TextToSpeech.SUCCESS) finished(activeId, true)
        return result == TextToSpeech.SUCCESS
    }

    private fun finished(id: String?, error: Boolean) {
        if (id != activeId) return
        Timber.i("Voice playback finished id=%s error=%s", id, error)
        activeId = null; handler.removeCallbacks(speechTimeout); audioOutput?.stop(); policy.complete(); abandonFocus()
        mutableState.value = mutableState.value.copy(speaking = false, error = if (error) R.string.voice_engine_error else 0)
    }
    private fun stopSpeech() {
        activeId = null; handler.removeCallbacks(speechTimeout); tts?.stop(); audioOutput?.stop(); policy.complete(); abandonFocus()
        mutableState.value = mutableState.value.copy(speaking = false)
    }
    @Suppress("DEPRECATION")
    private fun requestFocus(): Boolean {
        val audio = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val gain = VoiceAudioPolicy.focusGain(settings.musicMode) ?: return true
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val request = focusRequest?.takeIf { it.focusGain == gain } ?: AudioFocusRequest.Builder(gain)
                .setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener, handler).build().also { focusRequest = it }
            audio.requestAudioFocus(request)
        } else audio.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, gain)
        Timber.i("Voice focus result=%d gain=%d musicActive=%s bluetoothAttached=%s", result, gain, audio.isMusicActive, attached)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) heldFocusGain = gain
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        if (heldFocusGain == null) return
        heldFocusGain = null
        val audio = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        else audio.abandonAudioFocus(focusListener)
    }
    private fun release() {
        stopSpeech(); generation++; ready = false; tts?.shutdown(); tts = null
        mutableState.value = VoiceUiState(wheelMac = mutableState.value.wheelMac,
            totalMetres = mutableState.value.totalMetres, distanceRevision = mutableState.value.distanceRevision)
    }
}
