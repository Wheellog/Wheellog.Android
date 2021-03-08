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
        // TODO from WheelData
    }

    public int getCellSForWheel() {
        return 0;
    }
}