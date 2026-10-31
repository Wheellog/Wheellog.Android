package com.cooper.wheellog.voice

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class VoiceAudioOutputTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun register(file: File, delay: Int = 0) {
        file.writeText("simulated synthesized WAV")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(file.absolutePath), ShadowMediaPlayer.MediaInfo(1000, delay))
    }

    @Test fun `synthesized audio plays on media and completes only after playback`() {
        val events = mutableListOf<Pair<String, Boolean>>()
        var player: MediaPlayer? = null
        ShadowMediaPlayer.setCreateListener { media, _ -> player = media }
        val output = VoiceAudioOutput(context) { id, error -> events += id to error }
        val file = output.prepare("one"); register(file)
        assertTrue(output.play("one", 0.75f))
        shadowOf(Looper.getMainLooper()).idle()
        val media = requireNotNull(player)
        assertTrue(media.isPlaying)
        assertEquals(AudioAttributes.USAGE_MEDIA, shadowOf(media).audioAttributes.usage)
        assertEquals(0.75f, shadowOf(media).leftVolume, 0f)
        assertTrue(events.isEmpty())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(listOf("one" to false), events)
        output.stop()
        assertFalse(file.exists())
        assertEquals(ShadowMediaPlayer.State.END, shadowOf(media).state)
    }

    @Test fun `preemption during preparation cannot start an old utterance`() {
        var player: MediaPlayer? = null
        ShadowMediaPlayer.setCreateListener { media, _ -> player = media }
        val events = mutableListOf<Pair<String, Boolean>>()
        val output = VoiceAudioOutput(context) { id, error -> events += id to error }
        val old = output.prepare("old"); register(old, 2000)
        assertTrue(output.play("old", 1f))
        val current = output.prepare("new")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertFalse(old.exists())
        assertTrue(current.exists())
        assertFalse(output.play("old", 1f))
        assertTrue(events.isEmpty())
        assertEquals(ShadowMediaPlayer.State.END, shadowOf(requireNotNull(player)).state)
        output.stop()
    }

    @Test fun `invalid synthesis does not leave an active player`() {
        val events = mutableListOf<Pair<String, Boolean>>()
        val output = VoiceAudioOutput(context) { id, error -> events += id to error }
        val empty = output.prepare("empty")
        assertFalse(output.play("empty", 1f))
        output.stop()
        assertFalse(empty.exists())
        val broken = output.prepare("broken")
        broken.writeText("invalid") // Unregistered source forces the preparation failure path.
        assertFalse(output.play("broken", 1f))
        assertFalse(broken.exists())
        assertTrue(events.isEmpty())
    }

    @Test fun `startup cleanup touches only owned temporary speech files`() {
        val directory = File(context.cacheDir, "voice-playback").apply { mkdirs() }
        val old = File(directory, "utterance-orphan.wav").apply { writeText("orphan") }
        val note = File(directory, "note.txt").apply { writeText("keep") }
        val trip = File(context.filesDir, "trip.csv").apply { writeText("keep recording") }
        val output = VoiceAudioOutput(context) { _, _ -> }
        assertFalse(old.exists())
        assertEquals("keep", note.readText())
        assertEquals("keep recording", trip.readText())
        output.stop()
    }
}
