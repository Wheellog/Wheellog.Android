package com.cooper.wheellog.utils

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.cooper.wheellog.MainActivity
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
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
            if (WheelData.getInstance().isHardwarePWM || WheelLog.AppConfig.rotationIsSet) {
                return
            }

            val inflater: LayoutInflater = LayoutInflater.from(context)
            val dialogView: View = inflater.inflate(R.layout.update_pwm_settings, null)
            val svLayout : LinearLayout = dialogView.findViewById(R.id.set_speed_voltage_layout)
            val templatesBox: Spinner = dialogView.findViewById(R.id.spinner_templates)
            templatesBox.visibility = View.GONE
            val templates = mutableMapOf(
                    "Begode MTen 67v" to Pair(44.0, 67.2), // first - speed, second - voltage
                    "Begode MTen 84v" to Pair(56.0, 84.0),
                    "Begode MCM5 67v" to Pair(44.0, 67.2),
                    "Begode MCM5v2 67v" to Pair(51.2, 67.2),
                    "Begode MCM5 84v" to Pair(56.0, 84.0),
                    "Begode MCM5v2 84v" to Pair(64.0, 84.0),
                    "Begode Tesla/T3 84v" to Pair(66.5, 84.0),
                    "Begode Nikola 84v" to Pair(70.6, 84.0),
                    "Begode Nikola 100v" to Pair(85.5, 100.8),
                    "Begode MSX 84v" to Pair(79.0, 84.0),
                    "Begode MSX 100v" to Pair(95.0, 100.8),
                    "Begode MSP HS (C30)" to Pair(100.5, 100.8),
                    "Begode MSP HT (C38)" to Pair(79.0, 100.8),
                    "Begode EX (C40)" to Pair(79.0, 100.8),
                    "Begode EX.N (C30)" to Pair(107.1, 100.8),
                    "Begode RS HS (C30)" to Pair(105.0, 100.8),
                    "Begode RS HT (C38)" to Pair(79.0, 100.8),
                    "Begode Hero HS (C30)" to Pair(105.0, 100.8),
                    "Begode Hero HT (C38)" to Pair(79.0, 100.8),
                    "Begode Master (C38)" to Pair(113.0, 134.4),
                    "Begode Monster 84v" to Pair(74.4, 100.8),
                    "Begode Monster 100v" to Pair(93.0, 100.8),

                    "Veteran Sherman" to Pair(102.0, 100.8),

                    "Ninebot Z6" to Pair(61.5, 57.7),
                    "Ninebot Z8/Z10" to Pair(81.5, 57.7),

                    "Inmotion V5F" to Pair(37.0, 84.0),
                    "Inmotion V8" to Pair(45.0, 84.0),
                    "Inmotion V8F/V8S" to Pair(58.0, 84.0),
                    "Inmotion V10/V10F" to Pair(55.0, 84.0),

                    )
            templatesBox.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1,
                templates.toList().map { it.first })
            var selectedOption = 1
            dialogView.findViewById<RadioGroup>(R.id.selected_pwm_variant)
                .setOnCheckedChangeListener { _, checkedId ->
                    svLayout.visibility =
                        if (checkedId == R.id.radioButton1) View.VISIBLE else View.GONE
                    templatesBox.visibility =
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
                    speedValue.text = String.format("%02d %s", progress, context.getString(R.string.kmh))
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
                    voltageValue.text = String.format("%03d %s", progress, context.getString(R.string.volt))
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
                    when (selectedOption) {
                        1 -> {
                            WheelLog.AppConfig.apply {
                                rotationSpeed = seekbarSpeed.progress
                                rotationVoltage = seekbarVoltage.progress
                            }
                            WheelLog.AppConfig.rotationIsSet = true
                        }
                        2 -> TODO("доделать как-то Авто")
                        3 -> {
                            val temp = templates.getOrDefault(templatesBox.selectedItem, null)
                            if (temp != null) {
                                WheelLog.AppConfig.apply {
                                    rotationSpeed = temp.first
                                    rotationVoltage = temp.second
                                }
                                WheelLog.AppConfig.rotationIsSet = true
                            }
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
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