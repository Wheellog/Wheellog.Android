package com.cooper.wheellog.utils;

public abstract class BaseAdapter {
    public abstract boolean decode(byte[] data);
    public abstract void updatePedalsMode(int pedalsMode);
    public abstract void updateLightMode(int lightMode);
    public abstract void updateMaxSpeed(int wheelMaxSpeed);
    public int getCellSForWheel() {
        return 0;
    }
}