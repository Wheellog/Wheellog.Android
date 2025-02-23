package com.cooper.wheellog.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.AudioUtil.playAlarm
import com.cooper.wheellog.utils.Constants.ALARM_TYPE
import com.cooper.wheellog.utils.SomeUtil.playSound
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import timber.log.Timber
import java.util.*
import kotlin.math.roundToInt

object Alarms: KoinComponent {
    private val appConfig: AppConfig by inject()
    private val notifications: NotificationUtil by inject()

    private var speedAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 170 }
    private var currentAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 170 }
    private var temperatureAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 570 }
    private var batteryAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 970 }
    private var wheelAlarmExecuting = TempBoolean().apply { timeToResetToDefault = 170 }
    private var lastPlayWarningSpeedTime = System.currentTimeMillis()
    private var alarmTimer: Timer? = null
    private const val checkPeriod: Long = 200
    private lateinit var timerTask: TimerTask
    private var isStarted: Boolean = false

    private fun newTimerTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {
                val wd = WheelData.getInstance() ?: return
                val mContext: Context = get()
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
            if (wheelAlarmExecuting.value) {
                alarm = alarm or 0x10
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
        val executed = if (appConfig.pwmBasedAlarms) {
            alertedAlarms(pwm, mContext)
        } else {
            oldAlarms(mContext)
        }
                || currentAlarms(mContext)
                || temperatureAlarms(mContext)
                || batteryAlarms(mContext)
                || wheelAlarms(mContext)
        if (executed && !isStarted) {
            start()
        }
        return executed
    }

    private fun alertedAlarms(pwm: Double, mContext: Context): Boolean {
        if (pwm > appConfig.alarmFactor1 / 100.0) {
            AudioUtil.toneDuration =
                (200 * (pwm - appConfig.alarmFactor1 / 100.0) / (appConfig.alarmFactor2 / 100.0 - appConfig.alarmFactor1 / 100.0)).roundToInt()
            AudioUtil.toneDuration = MathsUtil.clamp(AudioUtil.toneDuration, 20, 200)
            raiseAlarm(ALARM_TYPE.PWM, pwm, mContext)
            return true
        } else {
            // check if speed alarm executing and stop it
            speedAlarmExecuting.value = false
            // pre alarm
            val warningPwm = appConfig.warningPwm / 100.0
            val warningSpeedPeriod = appConfig.warningSpeedPeriod * 1000
            if (warningPwm != 0.0 && warningSpeedPeriod != 0 && pwm >= warningPwm && System.currentTimeMillis() - lastPlayWarningSpeedTime > warningSpeedPeriod) {
                lastPlayWarningSpeedTime = System.currentTimeMillis()
                playSound(mContext, R.raw.warning_pwm)
            } else {
                val warningSpeed = appConfig.warningSpeed
                if (warningSpeed != 0 && warningSpeedPeriod != 0 && WheelData.getInstance().speedDouble >= warningSpeed && System.currentTimeMillis() - lastPlayWarningSpeedTime > warningSpeedPeriod) {
                    lastPlayWarningSpeedTime = System.currentTimeMillis()
                    playSound(mContext, R.raw.sound_warning_speed)
                }
            }
        }
        return false
    }

    private fun oldAlarms(mContext: Context): Boolean {
        if (checkOldAlarmSpeed(appConfig.alarm1Speed, appConfig.alarm1Battery)) {
            AudioUtil.toneDuration = 50
            raiseAlarm(ALARM_TYPE.SPEED1, WheelData.getInstance().speedDouble, mContext)
            return true
        } else if (checkOldAlarmSpeed(
                appConfig.alarm2Speed,
                appConfig.alarm2Battery
            )
        ) {
            AudioUtil.toneDuration = 100
            raiseAlarm(ALARM_TYPE.SPEED2, WheelData.getInstance().speedDouble, mContext)
            return true
        } else if (checkOldAlarmSpeed(
                appConfig.alarm3Speed,
                appConfig.alarm3Battery
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
        val alarmTemperature = appConfig.alarmTemperature
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
        val alarmCurrent = appConfig.alarmCurrent * 100
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
        val alarmBattery = appConfig.alarmBattery
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

    private fun wheelAlarms(mContext: Context): Boolean {
        if (wheelAlarmExecuting.value) {
            return true
        }
        val alarmWheel = appConfig.alarmWheel
        if (alarmWheel && WheelData.getInstance().wheelAlarm) {
            raiseAlarm(
                    ALARM_TYPE.WHEEL,
                    WheelData.getInstance().calculatedPwm,
                    mContext
                )
                wheelAlarmExecuting.value = true
            }
        return wheelAlarmExecuting.value
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
            ALARM_TYPE.WHEEL -> longArrayOf(0, 50, 50)
        }
        if (!appConfig.disablePhoneVibrate) {
            vibrate(mContext, pattern)
        }
        if (!appConfig.disablePhoneBeep) {
            CoroutineScope(Job()).launch {
                Timber.i("Scheduled alarm. $alarmType")
                if (alarmType == ALARM_TYPE.BATTERY) {
                    playSound(mContext, R.raw.lowbat)
                }
                else playAlarm(alarmType)
            }
        }
        mContext.sendBroadcast(intent)
        if (appConfig.mibandMode === MiBandEnum.Alarm) {
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
                ALARM_TYPE.WHEEL ->
                    String.format(
                            Locale.US,
                            mContext.getString(R.string.alarm_text_wheel_v)
                    )
            }
            notifications.alarmText = miText
            notifications.update()
        }
    }

    fun vibrate(mContext: Context, pattern: LongArray) {
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