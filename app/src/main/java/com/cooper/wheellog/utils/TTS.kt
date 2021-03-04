package com.cooper.wheellog.utils

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import java.util.*

class TTS(var context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    var initialized: Boolean = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = when (WheelLog.AppConfig.useEng) {
                true -> Locale.US
                tts.isLanguageAvailable(Locale(Locale.getDefault().language))
                        == TextToSpeech.LANG_AVAILABLE -> Locale(Locale.getDefault().language)
                else -> Locale.US
            }
            tts.setPitch(1.3f)
            tts.setSpeechRate(0.7f)
            initialized = true
        } else if (status == TextToSpeech.ERROR) {
            Toast.makeText(context, R.string.tts_error, Toast.LENGTH_LONG).show()
            initialized = false
        }
    }

    fun speak(text: String) {
        if (!initialized) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val utteranceId = this.hashCode().toString() + ""
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        } else {
            val map: HashMap<String, String> = HashMap()
            map[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "MessageId"
            tts.speak(text, TextToSpeech.QUEUE_ADD, map)
        }
    }
}