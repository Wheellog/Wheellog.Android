package com.cooper.wheellog.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.AudioUtil.playAlarm
import com.cooper.wheellog.utils.Constants.ALARM_TYPE
import com.cooper.wheellog.utils.SomeUtil.Companion.playSound
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import kotlin.math.roundToInt

object Alarms {

    private var speedAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 170 }
    private var currentAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 170 }
    private var temperatureAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 570 }
    private var batteryAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 970 }
    private var lastPlayWarningSpeedTime = System.currentTimeMillis()
    private var alarmTimer: Timer? = null
    private const val checkPeriod: Long = 200
    private lateinit var timerTask: TimerTask
    private var isStarted: Boolean = false

    private fun newTimerTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {
                val wd = WheelData.getInstance() ?: return
                val mContext: Context = WheelLog.appContext ?: return
                if (!reCheckAlarm(wd.calculatedPwm / 100, mContext)) {
                    stop()
                }
            }
        }
    }

    val alarm: Int
        get() {
            var alarm = 0
            if (speedAlarmExecuting.value) {
                alarm = alarm or 0x01
            }
            if (temperatureAlarmExecuting.value) {
                alarm = alarm or 0x04
            }
            if (currentAlarmExecuting.value) {
                alarm = alarm or 0x02
            }
            if (batteryAlarmExecuting.value) {
                alarm = alarm or 0x08
            }
            return alarm
        }

    @Synchronized
    fun start() {
        stop()
        isStarted = true
        timerTask = newTimerTask()
        alarmTimer = Timer().apply {
            scheduleAtFixedRate(timerTask, 0, checkPeriod)
        }
    }

    @Synchronized
    fun stop() {
        if (isStarted) {
            timerTask.cancel()
            alarmTimer?.cancel()
            alarmTimer = null
            isStarted = false
        }
    }

    fun checkAlarm(pwm: Double, mContext: Context): Boolean {
        if (isStarted) {
            return true
        }
        return reCheckAlarm(pwm, mContext)
    }

    private fun reCheckAlarm(pwm: Double, mContext: Context): Boolean {
        val executed = if (WheelLog.AppConfig.alteredAlarms) {
            alertedAlarms(pwm, mContext)
        } else {
            oldAlarms(mContext)
        }
                || currentAlarms(mContext)
                || temperatureAlarms(mContext)
                || batteryAlarms(mContext)
        if (executed && !isStarted) {
            start()
        }
        return executed
    }

    private fun alertedAlarms(pwm: Double, mContext: Context): Boolean {
        if (pwm > WheelLog.AppConfig.alarmFactor1 / 100.0) {
            AudioUtil.toneDuration =
                (200 * (pwm - WheelLog.AppConfig.alarmFactor1 / 100.0) / (WheelLog.AppConfig.alarmFactor2 / 100.0 - WheelLog.AppConfig.alarmFactor1 / 100.0)).roundToInt()
            AudioUtil.toneDuration = MathsUtil.clamp(AudioUtil.toneDuration, 20, 200)
            raiseAlarm(ALARM_TYPE.PWM, pwm, mContext)
            return true
        } else {
            // check if speed alarm executing and stop it
            speedAlarmExecuting.value = false
            // pre alarm
            val warningPwm = WheelLog.AppConfig.warningPwm / 100.0
            val warningSpeedPeriod = WheelLog.AppConfig.warningSpeedPeriod * 1000
            if (warningPwm != 0.0 && warningSpeedPeriod != 0 && pwm >= warningPwm && System.currentTimeMillis() - lastPlayWarningSpeedTime > warningSpeedPeriod) {
                lastPlayWarningSpeedTime = System.currentTimeMillis()
                playSound(mContext, R.raw.warning_pwm)
            } else {
                val warningSpeed = WheelLog.AppConfig.warningSpeed
                if (warningSpeed != 0 && warningSpeedPeriod != 0 && WheelData.getInstance().speedDouble >= warningSpeed && System.currentTimeMillis() - lastPlayWarningSpeedTime > warningSpeedPeriod) {
                    lastPlayWarningSpeedTime = System.currentTimeMillis()
                    playSound(mContext, R.raw.sound_warning_speed)
                }
            }
        }
        return false
    }

    private fun oldAlarms(mContext: Context): Boolean {
        if (checkOldAlarmSpeed(WheelLog.AppConfig.alarm1Speed, WheelLog.AppConfig.alarm1Battery)) {
            AudioUtil.toneDuration = 50
            raiseAlarm(ALARM_TYPE.SPEED1, WheelData.getInstance().speedDouble, mContext)
            return true
        } else if (checkOldAlarmSpeed(
                WheelLog.AppConfig.alarm2Speed,
                WheelLog.AppConfig.alarm2Battery
            )
        ) {
            AudioUtil.toneDuration = 100
            raiseAlarm(ALARM_TYPE.SPEED2, WheelData.getInstance().speedDouble, mContext)
            return true
        } else if (checkOldAlarmSpeed(
                WheelLog.AppConfig.alarm3Speed,
                WheelLog.AppConfig.alarm3Battery
            )
        ) {
            AudioUtil.toneDuration = 180
            raiseAlarm(ALARM_TYPE.SPEED3, WheelData.getInstance().speedDouble, mContext)
            return true
        } else {
            // check if speed alarm executing and stop it
            speedAlarmExecuting.value = false
        }
        return false
    }

    private fun checkOldAlarmSpeed(alarmSpeed: Int, alarmBattery: Int): Boolean {
        return alarmSpeed > 0
            && alarmBattery > 0
            && WheelData.getInstance().batteryLevel <= alarmBattery
            && WheelData.getInstance().speedDouble >= alarmSpeed
    }

    private fun temperatureAlarms(mContext: Context): Boolean {
        if (temperatureAlarmExecuting.value) {
            return true
        }
        val alarmTemperature = WheelLog.AppConfig.alarmTemperature
        if (alarmTemperature > 0 && WheelData.getInstance().temperature >= alarmTemperature) {
            raiseAlarm(
                ALARM_TYPE.TEMPERATURE,
                WheelData.getInstance().temperature.toDouble(),
                mContext
            )
            temperatureAlarmExecuting.value = true
        }
        return temperatureAlarmExecuting.value
    }

    private fun currentAlarms(mContext: Context): Boolean {
        if (currentAlarmExecuting.value) {
            return true
        }
        val alarmCurrent = WheelLog.AppConfig.alarmCurrent * 100
        if (alarmCurrent > 0 && WheelData.getInstance().current >= alarmCurrent) {
            raiseAlarm(
                ALARM_TYPE.CURRENT,
                WheelData.getInstance().currentDouble,
                mContext
            )
            currentAlarmExecuting.value = true
        }
        return currentAlarmExecuting.value
    }

    private fun batteryAlarms(mContext: Context): Boolean {
        if (batteryAlarmExecuting.value) {
            return true
        }
        val alarmBattery = WheelLog.AppConfig.alarmBattery
        if (alarmBattery > 0 && WheelData.getInstance().batteryLevel <= alarmBattery) {
            raiseAlarm(
                    ALARM_TYPE.BATTERY,
                    WheelData.getInstance().batteryLevel.toDouble(),
                    mContext
            )
            batteryAlarmExecuting.value = true
        }
        return batteryAlarmExecuting.value
    }

    private fun raiseAlarm(alarmType: ALARM_TYPE, value: Double, mContext: Context) {
        val intent = Intent(Constants.ACTION_ALARM_TRIGGERED)
        intent.putExtra(Constants.INTENT_EXTRA_ALARM_TYPE, alarmType)
        intent.putExtra(Constants.INTENT_EXTRA_ALARM_VALUE, value)
        val pattern: LongArray = when (alarmType) {
            ALARM_TYPE.SPEED1,
            ALARM_TYPE.SPEED2,
            ALARM_TYPE.SPEED3,
            ALARM_TYPE.PWM -> longArrayOf(0, 100, 100)
            ALARM_TYPE.CURRENT -> longArrayOf(0, 50, 50, 50, 50)
            ALARM_TYPE.TEMPERATURE -> longArrayOf(0, 500, 500)
            ALARM_TYPE.BATTERY -> longArrayOf(0, 100, 500)

        }
        if (!WheelLog.AppConfig.disablePhoneVibrate) {
            vibrate(mContext, pattern)
        }
        if (!WheelLog.AppConfig.disablePhoneBeep) {
            CoroutineScope(Job()).launch {
                Timber.i("Scheduled alarm. $alarmType")
                if (alarmType == ALARM_TYPE.BATTERY) {
                    playSound(mContext, R.raw.lowbat)
                }
                else playAlarm(alarmType)
            }
        }
        mContext.sendBroadcast(intent)
        if (WheelLog.AppConfig.mibandMode === MiBandEnum.Alarm) {
            val miText: String = when (alarmType) {
                ALARM_TYPE.SPEED1,
                ALARM_TYPE.SPEED2,
                ALARM_TYPE.SPEED3,
                ALARM_TYPE.PWM ->
                    String.format(
                        Locale.US,
                        mContext.getString(R.string.alarm_text_speed_v),
                        WheelData.getInstance().speedDouble
                    )
                ALARM_TYPE.CURRENT ->
                    String.format(
                        Locale.US,
                        mContext.getString(R.string.alarm_text_current_v),
                        WheelData.getInstance().currentDouble
                    )
                ALARM_TYPE.TEMPERATURE ->
                    String.format(
                        Locale.US,
                        mContext.getString(R.string.alarm_text_temperature_v),
                        WheelData.getInstance().temperature
                    )
                ALARM_TYPE.BATTERY ->
                    String.format(
                            Locale.US,
                            mContext.getString(R.string.alarm_text_battery_v),
                            WheelData.getInstance().batteryLevel
                    )
            }
            WheelLog.Notifications.alarmText = miText
            WheelLog.Notifications.update()
        }
    }

    private fun vibrate(mContext: Context, pattern: LongArray) {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                mContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else  {
            @Suppress("DEPRECATION")
            mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vib.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
                vib.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, -1)
            }
        }
    }
}