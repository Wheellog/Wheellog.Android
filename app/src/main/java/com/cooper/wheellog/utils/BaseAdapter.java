package com.cooper.wheellog.utils;

import android.content.Context;

public abstract class BaseAdapter {
    public void setContext(Context context) {
    }

    public abstract boolean decode(byte[] data);

    public void updatePedalsMode(int pedalsMode) {
    }

    public void setLightMode(int lightMode) {
    }

    public void switchFlashlight() {
    }

    public void updateMaxSpeed(int wheelMaxSpeed) {
        // TODO from WheelData
    }

    public int getCellSForWheel() {
        return 0;
    }
}