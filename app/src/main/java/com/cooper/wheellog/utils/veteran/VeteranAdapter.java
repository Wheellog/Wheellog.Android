package com.cooper.wheellog.utils.veteran;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;
import com.cooper.wheellog.utils.BaseAdapter;
import com.cooper.wheellog.utils.MathsUtil;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import timber.log.Timber;

/**
 * Adapter for Veteran wheels.
 * Responsible for decoding data from the wheel, and sending commands to the wheel.
 */
public class VeteranAdapter extends BaseAdapter {
    private static VeteranAdapter INSTANCE;
    private static final int WAITING_TIME = 100;
    private long time_old = 0;
    private int mVer = 0;

    private VeteranUnpacker unpacker;
    private VeteranBatteryCalculator batteryCalculator;
    private WheelData wd;
    private AppConfig appConfig;

    public VeteranAdapter(
            final VeteranBatteryCalculator batteryCalculator,
            final VeteranUnpacker unpacker,
            final WheelData wheelData,
            final AppConfig appConfig
    ) {
        this.batteryCalculator = batteryCalculator;
        this.unpacker = unpacker;
        this.wd = wheelData;
        this.appConfig = appConfig;
    }

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode Veteran");
        wd.resetRideTime();
        long time_new = System.currentTimeMillis();
        if ((time_new - time_old) > WAITING_TIME) // need to reset state in case of packet loose
            unpacker.reset();
        time_old = time_new;
        boolean newDataFound = false;
        for (byte c : data) {
            if (unpacker.addChar(c)) {
                byte[] buff = unpacker.getBuffer();
                boolean useBetterPercents = appConfig.getUseBetterPercents();
                int veteranNegative = Integer.parseInt(appConfig.getGotwayNegative());
                int voltage = MathsUtil.shortFromBytesBE(buff, 4);
                int speed = MathsUtil.signedShortFromBytesBE(buff, 6) * 10;
                int distance = MathsUtil.intFromBytesRevBE(buff, 8);
                int totalDistance = MathsUtil.intFromBytesRevBE(buff, 12);
                int phaseCurrent = MathsUtil.signedShortFromBytesBE(buff, 16) * 10;
                int temperature = MathsUtil.signedShortFromBytesBE(buff, 18);
                int autoOffSec = MathsUtil.shortFromBytesBE(buff, 20);
                int chargeMode = MathsUtil.shortFromBytesBE(buff, 22);
                int speedAlert = MathsUtil.shortFromBytesBE(buff, 24) * 10;
                int speedTiltback = MathsUtil.shortFromBytesBE(buff, 26) * 10;
                int ver = MathsUtil.shortFromBytesBE(buff, 28);
                mVer = ver / 1000;
                String version = String.format(Locale.US, "%03d.%01d.%02d", ver / 1000, (ver % 1000) / 100, (ver % 100));
                int pedalsMode = MathsUtil.shortFromBytesBE(buff, 30);
                int pitchAngle = MathsUtil.signedShortFromBytesBE(buff, 32);
                int hwPwm = MathsUtil.shortFromBytesBE(buff, 34);

                int battery = batteryCalculator.calculateBattery(voltage, mVer, useBetterPercents);
                if (veteranNegative == 0) {
                    speed = Math.abs(speed);
                    phaseCurrent = Math.abs(phaseCurrent);
                } else {
                    speed = speed * veteranNegative;
                    phaseCurrent = phaseCurrent * veteranNegative;
                }

                wd.setVersion(version);
                wd.setSpeed(speed);
                wd.setTopSpeed(speed);
                wd.setWheelDistance(distance);
                wd.setTotalDistance(totalDistance);
                wd.setTemperature(temperature);
                wd.setPhaseCurrent(phaseCurrent);
                wd.setCurrent(phaseCurrent);
                wd.setVoltage(voltage);
                wd.setVoltageSag(voltage);
                wd.setBatteryLevel(battery);
                wd.setChargingStatus(chargeMode);
                wd.setAngle(pitchAngle / 100.0);
                wd.setOutput(hwPwm);
                wd.updateRideTime();
                newDataFound = true;
            }
        }
        return newDataFound;
    }

    @Override
    public boolean isReady() {
        return wd.getVoltage() != 0 && mVer != 0;
    }

    public void resetTrip() {
        wd.bluetoothCmd("CLEARMETER".getBytes());
    }

    @Override
    public void updatePedalsMode(int pedalsMode) {
        switch (pedalsMode) {
            case 0 -> WheelData.getInstance().bluetoothCmd("SETh".getBytes());
            case 1 -> WheelData.getInstance().bluetoothCmd("SETm".getBytes());
            case 2 -> WheelData.getInstance().bluetoothCmd("SETs".getBytes());
        }
    }

    public int getVer() {
        if (mVer >= 2) {
            appConfig.setHwPwm(true);
        }
        return mVer;
    }

    @Override
    public void switchFlashlight() {
        boolean light = !appConfig.getLightEnabled();
        appConfig.setLightEnabled(light);
        setLightState(light);
    }

    @Override
    public void setLightState(final boolean lightEnable) {
        String command = "";
        if (lightEnable) {
            command = "SetLightON";
        } else {
            command = "SetLightOFF";
        }
        WheelData.getInstance().bluetoothCmd(command.getBytes());
    }

    @Override
    public int getCellsForWheel() {
        if (mVer > 3) {
            return 30;
        } else {
            return 24;
        }
    }

    @Override
    public void wheelBeep() {
        WheelData.getInstance().bluetoothCmd("b".getBytes());
    }

    public static VeteranAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VeteranAdapter(
                    new VeteranBatteryCalculator(),
                    new VeteranUnpacker(
                            new ByteArrayOutputStream()
                    ),
                    WheelData.getInstance(),
                    WheelLog.AppConfig
            );
        }
        return INSTANCE;
    }
}