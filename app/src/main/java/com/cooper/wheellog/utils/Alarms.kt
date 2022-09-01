package com.cooper.wheellog.utils

import android.content.Context
import android.content.Intent
import android.os.Vibrator
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.AudioUtil.playAlarmAsync
import com.cooper.wheellog.utils.Constants.ALARM_TYPE
import com.cooper.wheellog.utils.SomeUtil.Companion.playSound
import timber.log.Timber
import java.util.*
import kotlin.math.roundToInt

object Alarms {

    private var mSpeedAlarmExecuting = false
    private var mCurrentAlarmExecuting = false
    private var mTemperatureAlarmExecuting = false
    private var speedAlarmTimer: Timer? = null
    private var speedAlarmWatchdogTimer: Timer? = null
    private var mLastPlayWarningSpeedTime = System.currentTimeMillis()

    var alarm: Int
        get() {
            var alarm = 0
            if (mSpeedAlarmExecuting) {
                alarm = alarm or 0x01
            }
            if (mTemperatureAlarmExecuting) {
                alarm = alarm or 0x04
            }
            if (mCurrentAlarmExecuting) {
                alarm = alarm or 0x02
            }
            return alarm
        }
        private set(_) {}

    fun checkAlarmStatus(pwm: Double, mContext: Context) {
        if (WheelLog.AppConfig.alteredAlarms) {
            alertedAlarm(pwm, mContext)
        } else {
            oldAlarms(mContext)
        }
        val alarmCurrent = WheelLog.AppConfig.alarmCurrent * 100
        if (alarmCurrent > 0 && WheelData.getInstance().current >= alarmCurrent && !mCurrentAlarmExecuting) {
            startCurrentAlarmCount()
            raiseAlarm(
                ALARM_TYPE.CURRENT,
                WheelData.getInstance().currentDouble,
                mContext)
        }
        val alarmTemperature = WheelLog.AppConfig.alarmTemperature
        if (alarmTemperature > 0 && WheelData.getInstance().temperature >= alarmTemperature && !mTemperatureAlarmExecuting) {
            startTempAlarmCount()
            raiseAlarm(
                ALARM_TYPE.TEMPERATURE,
                WheelData.getInstance().temperature.toDouble(),
                mContext
            )
        }
    }

    private fun alertedAlarm(pwm: Double, mContext: Context) {
        if (pwm > WheelLog.AppConfig.alarmFactor1 / 100.0) {
            AudioUtil.toneDuration =
                (200 * (pwm - WheelLog.AppConfig.alarmFactor1 / 100.0) / (WheelLog.AppConfig.alarmFactor2 / 100.0 - WheelLog.AppConfig.alarmFactor1 / 100.0)).roundToInt()
            AudioUtil.toneDuration = MathsUtil.clamp(AudioUtil.toneDuration, 20, 200)
            raiseAlarm(ALARM_TYPE.PWM, pwm * 100.0, mContext)
        } else {
            // check if speed alarm executing and stop it
            mSpeedAlarmExecuting = false
            if (speedAlarmTimer != null) {
                speedAlarmTimer?.cancel()
                speedAlarmTimer = null
            }
            // prealarm
            val warningPwm = WheelLog.AppConfig.warningPwm / 100.0
            val warningSpeedPeriod = WheelLog.AppConfig.warningSpeedPeriod * 1000
            if (warningPwm != 0.0 && warningSpeedPeriod != 0 && pwm >= warningPwm && System.currentTimeMillis() - mLastPlayWarningSpeedTime > warningSpeedPeriod) {
                mLastPlayWarningSpeedTime = System.currentTimeMillis()
                playSound(mContext, R.raw.warning_pwm)
            } else {
                val warningSpeed = WheelLog.AppConfig.warningSpeed
                if (warningSpeed != 0 && warningSpeedPeriod != 0 && WheelData.getInstance().speedDouble >= warningSpeed && System.currentTimeMillis() - mLastPlayWarningSpeedTime > warningSpeedPeriod) {
                    mLastPlayWarningSpeedTime = System.currentTimeMillis()
                    playSound(mContext, R.raw.sound_warning_speed)
                }
            }
        }
    }

    private fun oldAlarms(mContext: Context)
    {
        if (alarmSpeedCheck(WheelLog.AppConfig.alarm1Speed, WheelLog.AppConfig.alarm1Battery)) {
            AudioUtil.toneDuration = 50
            raiseAlarm(ALARM_TYPE.SPEED1, WheelData.getInstance().speedDouble, mContext)
        } else if (alarmSpeedCheck(
                WheelLog.AppConfig.alarm2Speed,
                WheelLog.AppConfig.alarm2Battery
            )
        ) {
            AudioUtil.toneDuration = 100
            raiseAlarm(ALARM_TYPE.SPEED2, WheelData.getInstance().speedDouble, mContext)
        } else if (alarmSpeedCheck(
                WheelLog.AppConfig.alarm3Speed,
                WheelLog.AppConfig.alarm3Battery
            )
        ) {
            AudioUtil.toneDuration = 180
            raiseAlarm(ALARM_TYPE.SPEED3, WheelData.getInstance().speedDouble, mContext)
        } else {
            // check if speed alarm executing and stop it
            mSpeedAlarmExecuting = false
            if (speedAlarmTimer != null) {
                speedAlarmTimer?.cancel()
                speedAlarmTimer = null
            }
        }
    }

    private fun alarmSpeedCheck(alarmSpeed: Int, alarmBattery: Int): Boolean {
        return alarmSpeed > 0
            && alarmBattery > 0
            && WheelData.getInstance().batteryLevel <= alarmBattery
            && WheelData.getInstance().speedDouble >= alarmSpeed
    }

    private fun startSpeedAlarmCount() {
        if (!mSpeedAlarmExecuting) {
            mSpeedAlarmExecuting = true
            val playBeepAgain: TimerTask = object : TimerTask() {
                override fun run() {
                    playAlarmAsync(ALARM_TYPE.PWM)
                    Timber.i("Scheduled alarm")
                }
            }
            speedAlarmTimer = Timer().apply {
                scheduleAtFixedRate(playBeepAgain, 0, 200)
            }
        }
        speedAlarmWatchdogTimer?.cancel()
        speedAlarmWatchdogTimer = null
        val alarmWatchdog: TimerTask = object : TimerTask() {
            override fun run() {
                if (speedAlarmTimer != null) {
                    speedAlarmTimer?.cancel()
                    speedAlarmTimer = null
                }
                Timber.i("Alarm canceled by watchdog")
            }
        }
        speedAlarmWatchdogTimer = Timer().apply {
            schedule(alarmWatchdog, 5000)
        }
    }

    private fun startTempAlarmCount() {
        mTemperatureAlarmExecuting = true
        val stopTempAlarmExecuting: TimerTask = object : TimerTask() {
            override fun run() {
                mTemperatureAlarmExecuting = false
                Timber.i("Stop Temp <<<<<<<<<")
            }
        }
        val timerTemp = Timer()
        timerTemp.schedule(stopTempAlarmExecuting, 570)
    }

    private fun startCurrentAlarmCount() {
        mCurrentAlarmExecuting = true
        val stopCurrentAlarmExecuring: TimerTask = object : TimerTask() {
            override fun run() {
                mCurrentAlarmExecuting = false
                Timber.i("Stop Curr <<<<<<<<<")
            }
        }
        val timerCurrent = Timer()
        timerCurrent.schedule(stopCurrentAlarmExecuring, 170)
    }

    private fun raiseAlarm(alarmType: ALARM_TYPE, value: Double, mContext: Context) {
        val v = mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        var pattern = longArrayOf(0)
        val intent = Intent(Constants.ACTION_ALARM_TRIGGERED)
        intent.putExtra(Constants.INTENT_EXTRA_ALARM_TYPE, alarmType)
        intent.putExtra(Constants.INTENT_EXTRA_ALARM_VALUE, value)
        pattern =
            when (alarmType) {
                ALARM_TYPE.SPEED1, ALARM_TYPE.SPEED2, ALARM_TYPE.SPEED3, ALARM_TYPE.PWM -> longArrayOf(
                    0,
                    100,
                    100
                )
                ALARM_TYPE.CURRENT -> longArrayOf(0, 50, 50, 50, 50)
                ALARM_TYPE.TEMPERATURE -> longArrayOf(0, 500, 500)
            }
        if (v.hasVibrator() && !WheelLog.AppConfig.disablePhoneVibrate) v.vibrate(pattern, -1)
        if (!WheelLog.AppConfig.disablePhoneBeep) {
            if (alarmType.value > 3 && alarmType.value != 6) {
                playAlarmAsync(ALARM_TYPE.PWM)
            } else {
                startSpeedAlarmCount()
            }
        }
        mContext.sendBroadcast(intent)
        if (WheelLog.AppConfig.mibandMode === MiBandEnum.Alarm) {
            var mi_text = ""
            mi_text =
                when (alarmType) {
                    ALARM_TYPE.SPEED1, ALARM_TYPE.SPEED2, ALARM_TYPE.SPEED3, ALARM_TYPE.PWM -> String.format(
                        Locale.US,
                        mContext.getString(R.string.alarm_text_speed_v),
                        WheelData.getInstance().speedDouble
                    )
                    ALARM_TYPE.CURRENT -> String.format(
                        Locale.US,
                        mContext.getString(R.string.alarm_text_current_v),
                        WheelData.getInstance().currentDouble
                    )
                    ALARM_TYPE.TEMPERATURE -> String.format(
                        Locale.US,
                        mContext.getString(R.string.alarm_text_temperature_v),
                        WheelData.getInstance().temperature
                    )
                }
            WheelLog.Notifications.alarmText = mi_text
            WheelLog.Notifications.update()
        }
    }
}