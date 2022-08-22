package com.cooper.wheellog.utils;

import android.content.Context;

public abstract class BaseAdapter {
    protected Context mContext;

    public BaseAdapter setContext(Context context) {
        mContext = context;
        return this;
    }

    public abstract boolean decode(byte[] data);

    public void updatePedalsMode(int pedalsMode) {
    }

    public void setLightMode(int lightMode) {
    }

    public void setRollAngleMode(int rollAngleMode) {
    }

    public void updateBeeperVolume(int beeperVolume) {
    }

    public void setMilesMode(boolean milesMode) {
    }

    public void setLightState(boolean on) {
    }

    public void setLedState(boolean on) {
    }

    public void setTailLightState(boolean on) {
    }

    public void setHandleButtonState(boolean on) {
    }

    public void setBrakeAssist(boolean on) {
    }

    public void setLedColor(int value, int ledNum) {
    }

    public void setAlarmEnabled(boolean on, int num) {
    }

    public String getLedModeString() {
        return "";
    }

    public Boolean getLedIsAvailable(int ledNum) {
        return false;
    }

    public void setLimitedModeEnabled(boolean on) {
    }

    public void setLimitedSpeed(int value) {
    }


    public void setAlarmSpeed(int value, int num) {
    }

    public void setRideMode(boolean on) {
    }

    public void setLockMode(boolean on) {
    }

    public void setTransportMode(boolean on) {
    }

    public void setDrl(boolean on) {
    }

    public void setGoHomeMode(boolean on) {
    }

    public void setFancierMode(boolean on) {
    }

    public void setMute(boolean on) {
    }

    public void setFanQuiet(boolean on) {
    }

    public void setFan(boolean on) {
    }

    public void setLightBrightness(int value) {
    }

    public void powerOff() {
    }

    public void switchFlashlight() {
    }

    public void wheelBeep() {
    }

    public void updateMaxSpeed(int wheelMaxSpeed) {
    }

    public void setSpeakerVolume(int speakerVolume) {
    }

    public void setPedalTilt(int angle) {
    }

    public void setPedalSensivity(int sensivity) {
    }

    public void wheelCalibration() {
    }

    public void updateLedMode(int ledMode) {
    }

    public void updateStrobeMode(int strobeMode) {
    }

    public void updateAlarmMode(int alarmMode) {
    }

    public int getCellSForWheel() {
        return 0;
    }

    public boolean isReady() {
        return false;
    }
}