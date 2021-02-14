package com.cooper.wheellog.utils

import android.content.Context
import android.media.MediaPlayer

class SomeUtil {
    companion object {
        @JvmStatic
        fun playSound(context: Context, resId: Int) {
            MediaPlayer.create(context, resId).let {
                it.start()
                it.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
            }
        }
    }
}