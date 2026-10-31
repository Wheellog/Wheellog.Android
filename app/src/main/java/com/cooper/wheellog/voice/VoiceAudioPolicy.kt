package com.cooper.wheellog.voice

import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.Voice

/** Media routing matches the ordinary TTS/MUSIC path used by EUC World. */
internal object VoiceAudioPolicy {
    fun attributes(): AudioAttributes = AudioAttributes.Builder()
        .setLegacyStreamType(AudioManager.STREAM_MUSIC).build()

    fun focusGain(mode: String): Int? = when (mode) {
        "duck" -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        "pause" -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        else -> null
    }

    fun chooseVoice(voices: Collection<Voice>, language: String, preferred: String,
                    languageDefault: Voice?, engineDefault: Voice?): Voice? {
        val offline = voices.filter { it.locale.language == language &&
            !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() }
        offline.firstOrNull { it.name == preferred }?.let { return it }
        for (default in listOfNotNull(languageDefault, engineDefault)) {
            if (default.locale.language != language) continue
            offline.firstOrNull { it.name == default.name }?.let { return it }
            // Google's default may be the network variant of an installed local voice.
            if (default.name.endsWith("-network")) {
                val localName = default.name.removeSuffix("-network") + "-local"
                offline.firstOrNull { it.name == localName }?.let { return it }
            }
        }
        return offline.maxByOrNull { it.quality }
    }
}
