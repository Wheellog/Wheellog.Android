package com.cooper.wheellog.utils

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.MainActivity
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.io.IOException
import java.io.Serializable

object SomeUtil: KoinComponent {
    private val appConfig: AppConfig by inject()
    
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

    private val mediaPlayer by lazy { MediaPlayer() }
    private var beepTimer: CountDownTimer? = null

    @Suppress("DEPRECATION")
    @JvmStatic
    fun playBeep(onlyByWheel: Boolean, onlyDefault: Boolean) {
        if (WheelData.getInstance() == null) {
            return
        }

        if (appConfig.beepByWheel || onlyByWheel) {
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
        val context = get<Context>()
        val beepFile = appConfig.beepFile
        // selected file
        if (!onlyDefault && appConfig.useCustomBeep && beepFile !== Uri.EMPTY) {
            try {
                beepTimer?.cancel()
                mediaPlayer.apply {
                    if (isPlaying) {
                        stop()
                    }
                    reset()
                    setDataSource(context, beepFile)
                    setOnPreparedListener { obj: MediaPlayer -> obj.start() }
                    prepareAsync()
                    setOnCompletionListener { obj: MediaPlayer -> obj.reset() }
                    isLooping = false
                    // max 4 sec for beep
                    beepTimer = object : CountDownTimer((appConfig.customBeepTimeLimit * 1000).toLong(), 1000) {
                        override fun onTick(millisUntilFinished: Long) {}
                        override fun onFinish() {
                            if (isPlaying) {
                                stop()
                            }
                        }
                    }.start()
                }
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

    fun getNow() = System.currentTimeMillis()
}