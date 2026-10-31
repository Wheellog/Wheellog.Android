package com.cooper.wheellog.voice

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.time.Duration
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class VoiceRuntimeTest {
    private lateinit var context: Context
    private lateinit var config: AppConfig
    private lateinit var engine: TextToSpeech
    private var player: MediaPlayer? = null
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun addVoice(name: String, language: String, network: Boolean = false) =
        ShadowTextToSpeech.addVoice(Voice(name, Locale.forLanguageTag(language), 300, 300, network, emptySet()))

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        context = VoiceText.localized(app, "ru")
        app.getSharedPreferences("voice_announcements_v1", Context.MODE_PRIVATE).edit().clear().commit()
        VoiceSettings(context).duckMusic = false
        config = AppConfig(context)
        config.useEng = false
        startKoin { modules(module { single { config } }) }
        VoiceAnnouncements.detach(); VoiceAnnouncements.closeSettings(); idle()
        ShadowTextToSpeech.reset()
        addVoice("ru-a", "ru-RU"); addVoice("ru-b", "ru-RU"); addVoice("en-a", "en-US")
        addVoice("ru-network", "ru-RU", true)
        VoiceAnnouncements.openSettings(context)
        idle()
        engine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        shadowOf(engine).onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(engine).simulateSynthesizeToFileResult(TextToSpeech.SUCCESS)
        ShadowMediaPlayer.setCreateListener { media, _ ->
            player = media
            ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(shadowOf(engine).lastSynthesizeToFile.absolutePath),
                ShadowMediaPlayer.MediaInfo(10000, 0))
        }
        idle()
    }
    @After fun teardown() {
        VoiceAnnouncements.detach(); VoiceAnnouncements.closeSettings(); idle()
        stopKoin(); ShadowTextToSpeech.reset()
    }
    @Test fun `the picker exposes offline voices and previews the saved voice`() {
        assertEquals(setOf("ru-a","ru-b","en-a"), VoiceAnnouncements.state.value.voices.map { it.name }.toSet())
        VoiceSettings(context).setVoice("ru","ru-b")
        VoiceAnnouncements.preview(context,"ru"); idle()
        assertEquals("ru-b",shadowOf(engine).currentVoice.name)
        assertTrue(shadowOf(engine).lastSynthesizeToFileText.contains("Скорость"))
    }
    @Test fun `music is ducked or paused on the media path and focus is released without changing system volume`() {
        val manager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audio = shadowOf(manager)
        audio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        val volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        VoiceSettings(context).musicMode = "duck"
        VoiceAnnouncements.preview(context, "ru"); idle()
        val duck = audio.lastAudioFocusRequest.audioFocusRequest
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, duck.focusGain)
        assertEquals(AudioAttributes.USAGE_MEDIA, duck.audioAttributes.usage)
        VoiceAnnouncements.stop(); idle()
        assertEquals(duck, audio.lastAbandonedAudioFocusRequest)
        VoiceSettings(context).musicMode = "pause"
        VoiceAnnouncements.preview(context, "ru"); idle()
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE, audio.lastAudioFocusRequest.audioFocusRequest.focusGain)
        assertEquals(volume, manager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    @Test fun `denied focus is reported without starting speech`() {
        val manager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        shadowOf(manager).setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        VoiceSettings(context).musicMode = "duck"
        VoiceAnnouncements.preview(context, "ru"); idle()
        assertNull(shadowOf(engine).lastSynthesizeToFileText)
        assertEquals(R.string.voice_audio_unavailable, VoiceAnnouncements.state.value.error)
    }
    @Test fun `focus and the temporary file live until local audio actually finishes`() {
        val audio = shadowOf(context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        audio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        VoiceSettings(context).musicMode = "pause"
        VoiceAnnouncements.preview(context, "ru"); idle()
        val file = shadowOf(engine).lastSynthesizeToFile
        assertTrue(file.exists())
        assertTrue(requireNotNull(player).isPlaying)
        assertTrue(VoiceAnnouncements.state.value.speaking)
        assertNull(audio.lastAbandonedAudioFocusRequest)
        shadowOf(requireNotNull(player)).invokeCompletionListener(); idle()
        assertFalse(file.exists())
        assertFalse(VoiceAnnouncements.state.value.speaking)
        assertEquals(audio.lastAudioFocusRequest.audioFocusRequest, audio.lastAbandonedAudioFocusRequest)
    }
    @Test fun `focus loss stops the local voice player and removes its temporary audio`() {
        val audio = shadowOf(context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        audio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        VoiceSettings(context).musicMode = "duck"
        VoiceAnnouncements.preview(context, "ru"); idle()
        val file = shadowOf(engine).lastSynthesizeToFile
        audio.lastAudioFocusRequest.listener!!
            .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        idle()
        assertFalse(file.exists())
        assertFalse(VoiceAnnouncements.state.value.speaking)
        assertEquals(ShadowMediaPlayer.State.END, shadowOf(requireNotNull(player)).state)
        assertEquals(audio.lastAudioFocusRequest.audioFocusRequest, audio.lastAbandonedAudioFocusRequest)
    }
    @Test fun `preview works with announcements disabled and the Bluetooth timer running`() {
        assertFalse(VoiceSettings(context).enabled)
        VoiceAnnouncements.attach(context); idle()
        VoiceAnnouncements.preview(context,"en"); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertTrue(shadowOf(engine).lastSynthesizeToFileText.contains("Speed"))
        assertTrue(requireNotNull(player).isPlaying)
    }
    @Test fun `missing Russian offline voice cannot silently speak the English fallback`() {
        ShadowTextToSpeech.reset(); addVoice("en-only","en-US")
        VoiceAnnouncements.preview(context,"ru"); idle()
        assertEquals(R.string.voice_no_offline_voice, VoiceAnnouncements.state.value.error)
        assertNull(shadowOf(engine).lastSynthesizeToFileText)
    }
    @Test fun `live reports follow the interface language and reject stale telemetry`() {
        val data = mockk<WheelData>(relaxed = true)
        every { data.mac } returns "AA:BB:CC:DD:EE:FF"
        every { data.speedDouble } returns 25.0
        every { data.batteryLevel } returns 80
        every { data.distanceDouble } returns 12.5
        VoiceAnnouncements.onConnected(context); idle()
        VoiceAnnouncements.onTelemetry(context,data); idle()
        VoiceAnnouncements.speakReport(); idle()
        assertNotNull("Speech state: ${VoiceAnnouncements.state.value}", shadowOf(engine).lastSynthesizeToFileText)
        assertTrue(shadowOf(engine).lastSynthesizeToFileText.contains("Скорость"))
        assertEquals("ru",shadowOf(engine).currentVoice.locale.language)
        VoiceAnnouncements.stop(); idle()
        config.useEng = true
        VoiceAnnouncements.speakReport(); idle()
        assertTrue(shadowOf(engine).lastSynthesizeToFileText.contains("Speed"))
        assertEquals("en",shadowOf(engine).currentVoice.locale.language)
        VoiceAnnouncements.stop(); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
        VoiceAnnouncements.speakReport(); idle()
        assertEquals(R.string.voice_no_live_data,VoiceAnnouncements.state.value.error)
    }
    @Test fun `closing settings releases TTS when no wheel service is running`() {
        VoiceAnnouncements.preview(context,"en"); idle()
        VoiceAnnouncements.closeSettings(); idle()
        assertTrue(shadowOf(engine).isShutdown)
        assertFalse(VoiceAnnouncements.state.value.speaking)
    }
    @Test fun `disabled voice never reads wheel counters outside the listening screen`() {
        VoiceAnnouncements.closeSettings(); idle()
        val untouched = mockk<WheelData>()
        // A getter call on this strict mock would fail: disabled voice must be inert.
        VoiceAnnouncements.onTelemetry(context, untouched); idle()
        assertFalse(VoiceAnnouncements.state.value.speaking)
    }
    @Test fun `distance conditions read the selected contents once and disable completed one shot conditions`() {
        val prefs = VoiceSettings(context)
        prefs.enabled = true; prefs.connection = false; prefs.timedReports = false; prefs.fields = setOf("battery")
        prefs.setPeriodicDistance("wheel", true); prefs.setDistanceInterval("wheel", 1000)
        prefs.setAfterMetres("wheel", 1000, 100000); prefs.setAfterDistance("wheel", true, 100000)
        prefs.setTargetMetres("wheel", 101000); prefs.setTargetDistance("wheel", true)
        var odometer = 100000L
        val data = mockk<WheelData>(relaxed = true)
        every { data.mac } returns "wheel"
        every { data.totalDistance } answers { odometer }
        every { data.getDistanceAtOdometer(any()) } answers { odometer - 100000 }
        every { data.speedDouble } returns 25.0
        every { data.batteryLevel } returns 80
        VoiceAnnouncements.onConnected(context); VoiceAnnouncements.attach(context); idle()
        VoiceAnnouncements.onTelemetry(context, data); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        odometer = 101003
        VoiceAnnouncements.onTelemetry(context, data); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals("Заряд: 80 процентов.", shadowOf(engine).lastSynthesizeToFileText)
        assertFalse(prefs.distanceReports("wheel").after)
        assertFalse(prefs.distanceReports("wheel").target)
        assertTrue(prefs.distanceReports("wheel").periodic)
        VoiceAnnouncements.stop(); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertFalse(VoiceAnnouncements.state.value.speaking)
    }
    private fun connectionSample(): WheelData = mockk<WheelData>(relaxed = true).also { data ->
        every { data.mac } returns "wheel"
        every { data.totalDistance } returns 100000
        every { data.speedDouble } returns 25.0
        every { data.batteryLevel } returns 80
    }
    @Test fun `duplicate connection events every five seconds do not repeat the announcement`() {
        VoiceSettings(context).apply { enabled = true; reports = false }
        VoiceAnnouncements.attach(context); VoiceAnnouncements.onConnected(context, "wheel"); idle()
        val data = connectionSample()
        VoiceAnnouncements.onTelemetry(context, data); idle()
        assertEquals("Колесо подключено.", shadowOf(engine).lastSynthesizeToFileText)
        VoiceAnnouncements.stop(); idle()
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
            VoiceAnnouncements.onConnected(context, "wheel"); VoiceAnnouncements.onTelemetry(context, data); idle()
            assertFalse(VoiceAnnouncements.state.value.speaking)
        }
        VoiceAnnouncements.onDisconnected(context, false); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        VoiceAnnouncements.onDisconnected(context, false); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals("Связь с колесом потеряна.", shadowOf(engine).lastSynthesizeToFileText)
    }
    @Test fun `a brief reconnect does not announce either a loss or another connection`() {
        VoiceSettings(context).apply { enabled = true; reports = false }
        val data = connectionSample()
        VoiceAnnouncements.attach(context); VoiceAnnouncements.onConnected(context, "wheel"); idle()
        VoiceAnnouncements.onTelemetry(context, data); idle()
        VoiceAnnouncements.stop(); idle()
        VoiceAnnouncements.onDisconnected(context, false); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        VoiceAnnouncements.onConnected(context, "wheel"); idle()
        VoiceAnnouncements.onTelemetry(context, data); idle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertFalse(VoiceAnnouncements.state.value.speaking)
        assertEquals("Колесо подключено.", shadowOf(engine).lastSynthesizeToFileText)
    }
    @Test fun `a wheel connected before TTS initialization is announced after fresh telemetry and engine readiness`() {
        VoiceSettings(context).apply { enabled = true; reports = false }
        VoiceAnnouncements.settingsChanged(restartEngine = true); idle()
        engine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        VoiceAnnouncements.attach(context); VoiceAnnouncements.onConnected(context, "wheel"); idle()
        VoiceAnnouncements.onTelemetry(context, connectionSample()); idle()
        assertNull(shadowOf(engine).lastSynthesizeToFileText)
        shadowOf(engine).onInitListener.onInit(TextToSpeech.SUCCESS); idle()
        assertEquals("Колесо подключено.", shadowOf(engine).lastSynthesizeToFileText)
    }
    @Test fun `repeated discovery cannot restart the time report interval`() {
        VoiceSettings(context).apply { enabled = true; connection = false; intervalSeconds = 15; fields = setOf("battery") }
        val data = connectionSample()
        VoiceAnnouncements.attach(context); VoiceAnnouncements.onConnected(context, "wheel"); idle()
        VoiceAnnouncements.onTelemetry(context, data); idle()
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
            VoiceAnnouncements.onConnected(context, "wheel"); VoiceAnnouncements.onTelemetry(context, data); idle()
        }
        assertEquals("Заряд: 80 процентов.", shadowOf(engine).lastSynthesizeToFileText)
    }
}
