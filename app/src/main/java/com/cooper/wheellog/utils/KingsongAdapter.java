package com.cooper.wheellog.utils;

import com.cooper.wheellog.WheelData;

import timber.log.Timber;

public class KingsongAdapter implements IWheelAdapter {
    private static KingsongAdapter INSTANCE;

    @Override
    public boolean decode(byte[] data) {
        return WheelData.getInstance().decodeKingSong(data);
    }

    @Override
    public void updatePedalsMode(int pedalsMode) {
        WheelData.getInstance().updatePedalsMode(pedalsMode);
    }

    @Override
    public void updateLightMode(int lightMode) {

    }

    @Override
    public void updateMaxSpeed(int wheelMaxSpeed) {

    }

    public static KingsongAdapter getInstance() {
        Timber.i("Get instance");
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new KingsongAdapter();
        }
        return INSTANCE;
    }
}
