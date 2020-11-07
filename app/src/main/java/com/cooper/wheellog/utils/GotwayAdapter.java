package com.cooper.wheellog.utils;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import timber.log.Timber;

public class GotwayAdapter extends BaseAdapter {
    private static GotwayAdapter INSTANCE;
    gotwayUnpacker unpacker = new gotwayUnpacker();
    private static final double RATIO_GW = 0.875;
    private static final int WAITING_TIME = 100;
    private long time_old = 0;

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode Begode");
        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();
        long time_new = System.currentTimeMillis();
        if ((time_new-time_old) > WAITING_TIME) // need to reset state in case of packet loose
            unpacker.reset();
        time_old = time_new;
        for (byte c : data) {
            if (unpacker.addChar(c)) {
                byte[] buff = unpacker.getBuffer();
                Boolean useRatio = WheelLog.AppConfig.getUseRatio();
                Boolean useBetterPercents = WheelLog.AppConfig.getUseBetterPercents();
                int gotwayNegative = WheelLog.AppConfig.getGotwayNegative();
                if (buff[18] == (byte) 0x00) { // life data
                    Timber.i("Get new life data");
                    int voltage = MathsUtil.shortFromBytesBE(buff, 2);
                    int speed = (int) Math.round(MathsUtil.signedShortFromBytesBE(buff, 4) * 3.6);
                    long distance = MathsUtil.shortFromBytesBE(buff, 8);
                    int phaseCurrent = MathsUtil.signedShortFromBytesBE(buff, 10);
                    int temperature = (int) Math.round((((float) MathsUtil.signedShortFromBytesBE(buff, 12) / 340.0) + 36.53) * 100); // new formula based on MPU6050 datasheet
                    //int temperature = (int) Math.round(((((buff[12] * 256) + buff[13]) / 340.0) + 35) * 100); // old formula, let's leave it commented
                    if (gotwayNegative == 0) {
                        speed = Math.abs(speed);
                        phaseCurrent = Math.abs(phaseCurrent);
                    } else {
                        speed = speed * gotwayNegative;
                        phaseCurrent = phaseCurrent * gotwayNegative;
                    }

                    int battery = 0;
                    if (useBetterPercents) {
                        if (voltage > 6680) {
                            battery = 100;
                        } else if (voltage > 5440) {
                            battery = (voltage - 5380) / 13;
                        } else if (voltage > 5290) {
                            battery = (int) Math.round((voltage - 5290) / 32.5);
                        } else {
                            battery = 0;
                        }
                    } else {
                        if (voltage <= 5290) {
                            battery = 0;
                        } else if (voltage >= 6580) {
                            battery = 100;
                        } else {
                            battery = (voltage - 5290) / 13;
                        }
                    }

                    if (useRatio) {
                        distance = (int) Math.round(distance * RATIO_GW);
                        speed = (int) Math.round(speed * RATIO_GW);
                    }
                    voltage = (int) Math.round(getScaledVoltage(voltage));

                    wd.setSpeed(speed);
                    wd.setTopSpeed(speed);
                    wd.setWheelDistance(distance);
                    wd.setTemperature(temperature);
                    wd.setPhaseCurrent(phaseCurrent);
                    wd.setCurrent(phaseCurrent);
                    wd.setVoltage(voltage);
                    wd.setVoltageSag(voltage);
                    wd.setBatteryPercent(battery);
                    wd.updateRideTime();
                    return true;
                } else if (buff[18] == (byte) 0x04) { // total data
                    Timber.i("Get new total data");
                    int totalDistance = (int) MathsUtil.getInt4(buff, 2);
                    if (useRatio) {
                        wd.setTotalDistance(Math.round(totalDistance * RATIO_GW));
                    } else {
                        wd.setTotalDistance(totalDistance);
                    }
                    int pedalsMode = (buff[6] >> 4) & 0x0F;
                    int speedAlarms = buff[6] & 0x0F;
                    int ledMode = buff[13] & 0xFF;

                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public void updatePedalsMode(int pedalsMode) {
        WheelData.getInstance().updatePedalsMode(pedalsMode);
    }

    @Override
    public int getCellSForWheel() {
        switch (WheelLog.AppConfig.getGotwayVoltage()) {
            case 0:
                return 16;
            case 1:
                return 20;
        }
        return 24;
    }

    static class gotwayUnpacker {

        enum UnpackerState {
            unknown,
            collecting,
            done
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        gotwayUnpacker.UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {

            switch (state) {
                case collecting:
                    buffer.write(c);
                    if (buffer.size() == 20) {
                        state = UnpackerState.done;
                        oldc = 0;
                        Timber.i("Step reset");
                        return true;
                    }
                    break;
                default:
                    if (c == (byte) 0xAA && oldc == (byte) 0x55) {
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0x55);
                        buffer.write(0xAA);
                        state = UnpackerState.collecting;
                    }
                    oldc = c;
            }
            return false;
        }

        void reset() {
            oldc = 0;
            state = UnpackerState.unknown;
        }
    }

    public static GotwayAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GotwayAdapter();
        }
        return INSTANCE;
    }

    private double getScaledVoltage(double value) {
        return value * (1 + (0.25 * WheelLog.AppConfig.getGotwayVoltage()));
    }
}
