package com.cooper.wheellog.voice

import android.content.Context
import android.media.MediaPlayer
import timber.log.Timber
import java.io.File

/** Own speech playback in the same app that owns audio focus. Main-thread only. */
internal class VoiceAudioOutput(context: Context, private val complete: (String, Boolean) -> Unit) {
    private val directory = File(context.cacheDir, "voice-playback")
    private var source: File? = null
    private var id: String? = null
    private var player: MediaPlayer? = null

    init {
        directory.mkdirs()
        // Only our temporary synthesis files, never trip recordings or preferences.
        directory.listFiles()?.filter { it.name.startsWith("utterance-") && it.extension == "wav" }
            ?.forEach { it.delete() }
    }

    fun prepare(utteranceId: String): File {
        stop()
        return File.createTempFile("utterance-", ".wav", directory).also {
            source = it; id = utteranceId
        }
    }

    fun play(utteranceId: String, volume: Float): Boolean {
        val file = source ?: return false
        if (id != utteranceId || player != null || file.length() == 0L) return false
        return try {
            val audio = MediaPlayer()
            player = audio
            audio.setAudioAttributes(VoiceAudioPolicy.attributes())
            val level = volume.coerceIn(0f, 1f)
            audio.setVolume(level, level)
            audio.setOnPreparedListener { ready ->
                if (player === ready && id == utteranceId) {
                    try {
                        ready.start()
                        Timber.i("Voice local playback started id=%s bytes=%d session=%d", utteranceId, file.length(), ready.audioSessionId)
                    } catch (error: RuntimeException) {
                        Timber.w(error, "Cannot start local voice playback")
                        complete(utteranceId, true)
                    }
                }
            }
            audio.setOnCompletionListener { finished ->
                if (player === finished && id == utteranceId) complete(utteranceId, false)
            }
            audio.setOnErrorListener { failed, what, extra ->
                if (player === failed && id == utteranceId) {
                    Timber.w("Voice local playback error id=%s what=%d extra=%d", utteranceId, what, extra)
                    complete(utteranceId, true)
                }
                true
            }
            audio.setDataSource(file.absolutePath)
            audio.prepareAsync()
            true
        } catch (error: Exception) {
            Timber.w(error, "Cannot prepare local voice playback")
            stop()
            false
        }
    }

    fun stop() {
        val previous = player
        player = null; id = null
        runCatching { previous?.release() }.onFailure { Timber.w(it, "Cannot release voice player") }
        source?.delete(); source = null
    }
}
