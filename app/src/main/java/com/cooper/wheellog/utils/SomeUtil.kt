package com.cooper.wheellog.utils

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.provider.Settings
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.cooper.wheellog.*
import java.io.IOException


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

        /**
         * return false if in App's Battery settings "Not optimized" and true if "Optimizing battery use"
         */
        private fun isBatteryOptimizations(context: Context): Boolean {
            val powerManager =
                context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val name = context.applicationContext.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return !powerManager.isIgnoringBatteryOptimizations(name)
            }
            return false
        }

        fun checkBatteryOptimizationsAndShowAlert(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isBatteryOptimizations(context)) {
                AlertDialog.Builder(context, R.style.OriginalTheme_Dialog_Alert)
                    .setTitle(R.string.detected_battery_optimization_title)
                    .setMessage(R.string.detected_battery_optimization)
                    .setCancelable(false)
                    .setPositiveButton(R.string.detected_battery_optimization_app_button) { _: DialogInterface?, _: Int ->
                        try {
                            //Open the specific App Info page:
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            //Open the generic Apps page:
                            val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                    .setNegativeButton(R.string.detected_battery_optimization_settings_button) { _: DialogInterface?, _: Int ->
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                    .setNeutralButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
                    .show()
                return true
            }
            return false
        }

        fun checkPWMsettedAndShowAlert(context: Context) {
            if (WheelData.getInstance().isHardwarePWM) {
                return
            }
            val inflater: LayoutInflater = LayoutInflater.from(context)
            val dialogView: View = inflater.inflate(R.layout.update_pwm_settings, null)
            val svLayout : LinearLayout = dialogView.findViewById(R.id.set_speed_voltage_layout)
            val dropDownBox: Spinner = dialogView.findViewById(R.id.spinner_templates)
            dropDownBox.visibility = View.GONE
            var selectedOption: Int = 1
            dialogView.findViewById<RadioGroup>(R.id.selected_pwm_variant)
                .setOnCheckedChangeListener { _, checkedId ->
                    svLayout.visibility =
                        if (checkedId == R.id.radioButton1) View.VISIBLE else View.GONE
                    dropDownBox.visibility =
                        if (checkedId == R.id.radioButton3) View.VISIBLE else View.GONE
                    when (checkedId) {
                        R.id.radioButton1 -> selectedOption = 1
                        R.id.radioButton2 -> selectedOption = 2
                        R.id.radioButton3 -> selectedOption = 3
                    }
                }
            val speedValue: TextView = dialogView.findViewById(R.id.speed_value)
            val seekbarSpeed: SeekBar = dialogView.findViewById(R.id.seekBar_speed)
            seekbarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    speedValue.text = String.format("%03d km/h", progress)
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {
                }
                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            })

            val voltageValue: TextView = dialogView.findViewById(R.id.voltage_value)
            val seekbarVoltage: SeekBar = dialogView.findViewById(R.id.seekBar_voltage)
            seekbarVoltage.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    voltageValue.text = String.format("%03d V", progress)
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {
                }
                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            })
            // TODO данные загружать из настроек
            seekbarSpeed.progress = 50
            seekbarVoltage.progress = 100
            AlertDialog.Builder(context, R.style.OriginalTheme_Dialog_Alert)
                .setCancelable(false)
                .setTitle(R.string.setup_pwm_dialog_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    // TODO записывать настройки
                    // или автоматически замерять раскрутку
                    // или выбирать из шаблона
                }
                .show()
        }

        private fun isIntentResolved(context: Context, intent: Intent): Boolean {
            return context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ) != null
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
    }
}