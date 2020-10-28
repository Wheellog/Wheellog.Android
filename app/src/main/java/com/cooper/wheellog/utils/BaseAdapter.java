package com.cooper.wheellog.utils;

public abstract class BaseAdapter {
    public abstract boolean decode(byte[] data);

    public void updatePedalsMode(int pedalsMode) {
    }

    public void updateLightMode(int lightMode) {
        // TODO from WheelData
    }

    public void updateMaxSpeed(int wheelMaxSpeed) {
        // TODO from WheelData
    }

    public int getCellSForWheel() {
        return 0;
    }
}