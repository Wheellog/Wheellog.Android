package com.cooper.wheellog.utils;

public abstract class BaseAdapter implements IWheelAdapter {
    @Override
    public abstract boolean decode(byte[] data);

    @Override
    public abstract void updatePedalsMode(int pedalsMode);

    @Override
    public abstract void updateLightMode(int lightMode);

    @Override
    public abstract void updateMaxSpeed(int wheelMaxSpeed);

    @Override
    public int getCellSForWheel() {
        return 0;
    }
}