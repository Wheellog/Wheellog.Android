package com.cooper.wheellog.utils

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.cooper.wheellog.MainActivity
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import kotlinx.coroutines.*
import java.io.IOException
import java.io.Serializable

class SomeUtil {
    companion object {
        @ColorInt
        fun View.getColorEx(@ColorRes id: Int): Int {
            return context.getColorEx(id)
        }

        @Suppress("DEPRECATION")
        @ColorInt
        fun Context.getColorEx(@ColorRes id: Int): Int {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                resources.getColor(id)
            else
                getColor(id)
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        @Suppress("DEPRECATION")
        fun Fragment.getDrawableEx(@DrawableRes id: Int): Drawable? {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                resources.getDrawable(id)
            else
                requireContext().getDrawable(id)
        }

        @JvmStatic
        fun playSound(context: Context, resId: Int) {
            MediaPlayer.create(context, resId).let {
                it.start()
                it.setOnCompletionListener { obj: MediaPlayer -> obj.release() }
            }
        }

        @JvmStatic
        fun playBeep() {
            playBeep(onlyByWheel = false, onlyDefault = false)
        }

        @Suppress("DEPRECATION")
        @JvmStatic
        fun playBeep(onlyByWheel: Boolean, onlyDefault: Boolean) {
            if (WheelData.getInstance() == null) {
                return
            }

            if (WheelLog.AppConfig.beepByWheel || onlyByWheel) {
                WheelData.getInstance().wheelBeep()
                return
            }

            // no mute
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                MainActivity.audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
            } else {
                MainActivity.audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
            }

            // max volume
//        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        int volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            val context = WheelLog.appContext ?: return
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
                    playBeep(onlyByWheel = false, onlyDefault = true)
                }
            } else {
                // default beep
                playSound(context, R.raw.beep)
            }
        }

        private fun isIntentResolved(context: Context, intent: Intent): Boolean {
            return context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ) != null
        }

        fun <T : Serializable?> Intent.getSerializable(key: String, m_class: Class<T>): T? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                this.getSerializableExtra(key, m_class)
            else
                @Suppress("DEPRECATION") this.getSerializableExtra(key) as T
        }

        fun isMIUI(context: Context): Boolean {
            return isIntentResolved(
                context,
                Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
            )
                    || isIntentResolved(
                context,
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            )
                    || isIntentResolved(
                context,
                Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").addCategory(Intent.CATEGORY_DEFAULT)
            )
                    || isIntentResolved(
                context,
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.powercenter.PowerSettings"
                    )
                )
            )
        }

        inline fun <T, reified R : View> R.doAsync(
            crossinline backgroundTask: suspend () -> T?,
            crossinline onResult: R.(T?) -> Unit
        ) {
            val job = CoroutineScope(Dispatchers.Default)
            val attachListener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) { }
                override fun onViewDetachedFromWindow(v: View) {
                    job.cancel()
                }
            }
            addOnAttachStateChangeListener(attachListener)
            job.launch {
                val data = try {
                    backgroundTask()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                if (isActive) {
                    try {
                        withContext(Dispatchers.Main) { onResult(data) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                removeOnAttachStateChangeListener(attachListener)
            }
        }
    }
}