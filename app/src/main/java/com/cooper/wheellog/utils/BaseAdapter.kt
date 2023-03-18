package com.cooper.wheellog.utils

import android.content.Context
import com.cooper.wheellog.WheelLog

abstract class BaseAdapter {
    val context: Context?
        get() = WheelLog.appContext

    abstract fun decode(data: ByteArray?): Boolean
    open fun updatePedalsMode(pedalsMode: Int) {}
    open fun setLightMode(lightMode: Int) {}
    open fun setRollAngleMode(rollAngleMode: Int) {}
    open fun updateBeeperVolume(beeperVolume: Int) {}
    open fun setMilesMode(milesMode: Boolean) {}
    open fun setLightState(on: Boolean) {}
    open fun setLedState(on: Boolean) {}
    open fun setTailLightState(on: Boolean) {}
    open fun setHandleButtonState(on: Boolean) {}
    open fun setBrakeAssist(on: Boolean) {}
    open fun setLedColor(value: Int, ledNum: Int) {}
    open fun setAlarmEnabled(on: Boolean, num: Int) {}
    open val ledModeString: String?
        get() = ""

    open fun getLedIsAvailable(ledNum: Int): Boolean {
        return false
    }

    open fun setLimitedModeEnabled(on: Boolean) {}
    open fun setLimitedSpeed(value: Int) {}
    open fun setAlarmSpeed(value: Int, num: Int) {}
    open fun setRideMode(on: Boolean) {}
    open fun setLockMode(on: Boolean) {}
    open fun setTransportMode(on: Boolean) {}
    open fun setDrl(on: Boolean) {}
    open fun setGoHomeMode(on: Boolean) {}
    open fun setFancierMode(on: Boolean) {}
    open fun setMute(on: Boolean) {}
    open fun setFanQuiet(on: Boolean) {}
    open fun setFan(on: Boolean) {}
    open fun setLightBrightness(value: Int) {}
    open fun powerOff() {}
    open fun switchFlashlight() {}
    open fun wheelBeep() {}
    open fun updateMaxSpeed(wheelMaxSpeed: Int) {}
    open fun setSpeakerVolume(speakerVolume: Int) {}
    open fun setPedalTilt(angle: Int) {}
    open fun setPedalSensivity(sensivity: Int) {}
    open fun wheelCalibration() {}
    open fun updateLedMode(ledMode: Int) {}
    open fun updateStrobeMode(strobeMode: Int) {}
    open fun updateAlarmMode(alarmMode: Int) {}
    open val cellsForWheel: Int
        get() = 1
    open val isReady: Boolean
        get() = false
}