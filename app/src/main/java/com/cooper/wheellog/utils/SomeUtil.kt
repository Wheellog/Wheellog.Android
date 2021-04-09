package com.cooper.wheellog.utils

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.cooper.wheellog.*
import java.io.IOException

class SomeUtil {
    companion object {
        @Suppress("DEPRECATION")
        @ColorInt
        fun View.getColorEx(@ColorRes id: Int): Int {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                resources.getColor(id)
            else
                context.getColor(id)
        }

        @JvmStatic
        fun playSound(context: Context, resId: Int) {
            MediaPlayer.create(context, resId).let {
                it.start()
                it.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
            }
        }

        @JvmStatic
        fun playBeep(context: Context) {
            playBeep(context, onlyByWheel = false, onlyDefault = false)
        }

        @Suppress("DEPRECATION")
        @JvmStatic
        fun playBeep(context: Context, onlyByWheel: Boolean, onlyDefault: Boolean) {
            if (WheelLog.AppConfig.beepByWheel || onlyByWheel) {
                WheelData.getInstance().wheelBeep()
                return
            }

            // no mute
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                MainActivity.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            } else {
                MainActivity.audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
            }

            // max volume
//        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        int volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            val beepFile = WheelLog.AppConfig.beepFile
            // selected file
            if (!onlyDefault && WheelLog.AppConfig.useCustomBeep && beepFile !== Uri.EMPTY) {
                val mp = MediaPlayer()
                try {
                    mp.setDataSource(context, beepFile)
                    mp.setOnPreparedListener { obj: MediaPlayer -> obj.start() }
                    mp.prepareAsync()
                    mp.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
                } catch (e: IOException) {
                    e.printStackTrace()
                    playBeep(context, onlyByWheel = false, onlyDefault = true)
                }
            } else {
                // default beep
                playSound(context, R.raw.beep)
            }
        }
    }
}