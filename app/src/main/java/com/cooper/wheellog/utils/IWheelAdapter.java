package com.cooper.wheellog.utils;

public interface IWheelAdapter {
    boolean decode(byte[] data);

    void updatePedalsMode(int pedalsMode);

    void updateLightMode(int lightMode);

    void updateMaxSpeed(int wheelMaxSpeed);
}
