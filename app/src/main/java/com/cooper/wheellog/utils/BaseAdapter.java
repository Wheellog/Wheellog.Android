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

    public void switchFlashlight() {
    }

    public void wheelBeep() {
    }

    public void updateMaxSpeed(int wheelMaxSpeed) {
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
}