package com.cooper.wheellog.voice

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.Voice
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class VoiceAudioPolicyTest {
    private fun voice(name: String, lang: String = "ru", quality: Int = 100,
                      network: Boolean = false, features: Set<String> = emptySet()) =
        Voice(name, Locale(lang), quality, 300, network, features)

    @Test fun `media routing and focus modes match the EUC audio path`() {
        val attributes = VoiceAudioPolicy.attributes()
        assertEquals(AudioAttributes.USAGE_MEDIA, attributes.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_MUSIC, attributes.contentType)
        assertEquals(AudioManager.STREAM_MUSIC, attributes.volumeControlStream)
        assertNull(VoiceAudioPolicy.focusGain("mix"))
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, VoiceAudioPolicy.focusGain("duck"))
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE, VoiceAudioPolicy.focusGain("pause"))
    }

    @Test fun `old music preference survives and the new pause choice persists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("voice_announcements_v1", 0).edit().clear().commit()
        val prefs = VoiceSettings(context)
        assertEquals("duck", prefs.musicMode)
        prefs.duckMusic = false
        assertEquals("mix", VoiceSettings(context).musicMode)
        prefs.musicMode = "pause"
        assertEquals("pause", VoiceSettings(context).musicMode)
        assertTrue(prefs.duckMusic)
        prefs.duckMusic = false
        assertEquals("mix", prefs.musicMode)
        prefs.duckMusic = true
        assertEquals("duck", prefs.musicMode)
    }

    @Test fun `a manually selected offline voice wins over engine defaults`() {
        val manual = voice("manual")
        val default = voice("default", quality = 500)
        assertEquals(manual, VoiceAudioPolicy.chooseVoice(listOf(default, manual), "ru", "manual", default, default))
    }

    @Test fun `automatic voice prefers the language default over an unrelated higher quality voice`() {
        val default = voice("language-default")
        val high = voice("different", quality = 500)
        val english = voice("english", "en")
        assertEquals(default, VoiceAudioPolicy.chooseVoice(listOf(high, english, default), "ru", "", default, english))
        assertEquals(english, VoiceAudioPolicy.chooseVoice(listOf(high, english, default), "en", "", default, english))
    }

    @Test fun `network defaults select their installed local twin and never bypass offline language restrictions`() {
        val network = voice("ru-test-network", network = true)
        val local = voice("ru-test-local")
        val high = voice("other", quality = 500)
        val unavailable = voice("not-installed", features = setOf("notInstalled"))
        assertEquals(local, VoiceAudioPolicy.chooseVoice(listOf(network, high, local, unavailable), "ru", "", network, network))
        assertNull(VoiceAudioPolicy.chooseVoice(listOf(network, unavailable, voice("english", "en")), "ru", "", network, network))
    }
}
