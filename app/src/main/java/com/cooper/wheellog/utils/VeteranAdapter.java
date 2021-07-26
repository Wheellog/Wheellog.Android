package com.cooper.wheellog.utils;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import timber.log.Timber;

public class VeteranAdapter extends BaseAdapter {
    private static VeteranAdapter INSTANCE;
    veteranUnpacker unpacker = new veteranUnpacker();
    private static final int WAITING_TIME = 100;
    private long time_old = 0;

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode Veteran");
        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();
        long time_new = System.currentTimeMillis();
        if ((time_new-time_old) > WAITING_TIME) // need to reset state in case of packet loose
            unpacker.reset();
        time_old = time_new;
        boolean newDataFound = false;
        for (byte c : data) {
            if (unpacker.addChar(c)) {
                byte[] buff = unpacker.getBuffer();
                Boolean useBetterPercents = WheelLog.AppConfig.getUseBetterPercents();
                int veteranNegative = Integer.parseInt(WheelLog.AppConfig.getGotwayNegative());
                int voltage = MathsUtil.shortFromBytesBE(buff,4);
                int speed = MathsUtil.signedShortFromBytesBE(buff,6) * 10;
                int distance = MathsUtil.intFromBytesRevBE(buff,8);
                int totalDistance = MathsUtil.intFromBytesRevBE(buff, 12);
                int phaseCurrent = MathsUtil.signedShortFromBytesBE(buff,16) * 10;
                int temperature = MathsUtil.signedShortFromBytesBE(buff, 18);
                int autoOffSec = MathsUtil.shortFromBytesBE(buff,20);
                int chargeMode = MathsUtil.shortFromBytesBE(buff,22);
                int speedAlert = MathsUtil.shortFromBytesBE(buff, 24) * 10;
                int speedTiltback = MathsUtil.shortFromBytesBE(buff,26) * 10;
                String version = String.format(Locale.US, "%d.%d (%d)", buff[28], buff[29],MathsUtil.shortFromBytesBE(buff,28));
                int pedalsMode = MathsUtil.shortFromBytesBE(buff, 30);
                int reserved1 = MathsUtil.shortFromBytesBE(buff,32);
                int reserved2 = MathsUtil.shortFromBytesBE(buff, 34);

                int battery;
                if (useBetterPercents) {
                    if (voltage > 10020) {
                        battery = 100;
                    } else if (voltage > 8160) {
                        battery = (int) Math.round((voltage - 8070) / 19.5);
                    } else if (voltage > 7935) {
                        battery = (int) Math.round((voltage - 7935) / 48.75);
                    } else {
                        battery = 0;
                    }
                } else {
                    if (voltage <= 7935) {
                        battery = 0;
                    } else if (voltage >= 9870) {
                        battery = 100;
                    } else {
                        battery = (int) Math.round((voltage - 7935) / 19.5);
                    }
                }

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
                wd.updateRideTime();
                newDataFound = true;
            }
        }
        return newDataFound;
    }

    public void resetTrip() {
        WheelData.getInstance().bluetoothCmd("CLEARMETER".getBytes());
    }

    @Override
    public void updatePedalsMode(int pedalsMode) {
        switch (pedalsMode) {
            case 0:
                WheelData.getInstance().bluetoothCmd("SETh".getBytes());
                break;
            case 1:
                WheelData.getInstance().bluetoothCmd("SETm".getBytes());
                break;
            case 2:
                WheelData.getInstance().bluetoothCmd("SETs".getBytes());
                break;
        }
    }

    @Override
    public void switchFlashlight() {
        boolean light = !WheelLog.AppConfig.getLightEnabled();
        WheelLog.AppConfig.setLightEnabled(light);
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
    public int getCellSForWheel() {
        return 24;
    }

    @Override
    public void wheelBeep() {
        WheelData.getInstance().bluetoothCmd("b".getBytes());
    }

    static class veteranUnpacker {

        enum UnpackerState {
            unknown,
            collecting,
            lensearch,
            done
        }


        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int old1 = 0;
        int old2 = 0;
        int len = 0;

        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {

            switch (state) {

                case collecting:

                    int bsize = buffer.size();
                    if (((bsize == 22 || bsize == 30 || bsize > 34) && (c != 0x00)) || ((bsize == 23) && ((c & 0xFE) != 0x00)) || ((bsize == 31) && ((c & 0xFC) != 0x00))) {
                        state = UnpackerState.done;
                        Timber.i("Data verification failed");
                        reset();
                        return false;
                    }
                    buffer.write(c);
                    if (bsize == len+3) {
                        state = UnpackerState.done;
                        Timber.i("Len %d", len);
                        Timber.i("Step reset");
                        reset();
                        return true;
                    }
                    break;

                case lensearch:
                    buffer.write(c);
                    len = c & 0xff;
                    state = UnpackerState.collecting;
                    old2 = old1;
                    old1 = c;
                    break;


                default:
                    if (c == (byte) 0x5C && old1 == (byte) 0x5A && old2 == (byte) 0xDC) {
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0xDC);
                        buffer.write(0x5A);
                        buffer.write(0x5C);
                        state = UnpackerState.lensearch;
                    } else if (c == (byte) 0x5A && old1 == (byte) 0xDC) {
                        old2 = old1;
                    } else {
                        old2 = 0;
                    }
                    old1 = c;

            }
            return false;
        }

        void reset() {
            old1 = 0;
            old2 = 0;
            state = UnpackerState.unknown;

        }
    }


    public static VeteranAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VeteranAdapter();
        }
        return INSTANCE;
    }

}