package com.cooper.wheellog.utils;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.zip.CRC32;
import timber.log.Timber;

public class VeteranAdapter extends BaseAdapter {
    private static VeteranAdapter INSTANCE;
    veteranUnpacker unpacker = new veteranUnpacker();
    private static final int WAITING_TIME = 100;
    private long time_old = 0;
    private int mVer = 0;

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
                Boolean hwPwmEnabled = WheelLog.AppConfig.getHwPwm();
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
                int ver = MathsUtil.shortFromBytesBE(buff,28);
                mVer = ver/1000;
                String version = String.format(Locale.US, "%03d.%01d.%02d", ver/1000, (ver%1000)/100, (ver%100));
                int pedalsMode = MathsUtil.shortFromBytesBE(buff, 30);
                int pitchAngle = MathsUtil.signedShortFromBytesBE(buff,32);
                int hwPwm = MathsUtil.shortFromBytesBE(buff, 34);

                int battery;
                if (mVer < 4) { // not Patton
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
                } else if (mVer == 4) { // Patton
                    if (useBetterPercents) {
                        if (voltage > 12525) {
                            battery = 100;
                        } else if (voltage > 10200) {
                            battery = (int) Math.round((voltage - 9975) / 25.5);
                        } else if (voltage > 9600) {
                            battery = (int) Math.round((voltage - 9600) / 67.5);
                        } else {
                            battery = 0;
                        }
                    } else {
                        if (voltage <= 9918) {
                            battery = 0;
                        } else if (voltage >= 12337) {
                            battery = 100;
                        } else {
                            battery = (int) Math.round((voltage - 9918) / 24.2);
                        }
                    }
                } else if (mVer >= 5) { // Lynx
                    if (useBetterPercents) {
                        if (voltage > 15030) {
                            battery = 100;
                        } else if (voltage > 12240) {
                            battery = (int) Math.round((voltage - 11970) / 30.6);
                        } else if (voltage > 11520) {
                            battery = (int) Math.round((voltage - 11520) / 81);
                        } else {
                            battery = 0;
                        }
                    } else {
                        if (voltage <= 11902) {
                            battery = 0;
                        } else if (voltage >= 14805) {
                            battery = 100;
                        } else {
                            battery = (int) Math.round((voltage - 11902) / 29.03);
                        }
                    }
                } else battery = 1; // for new wheels, set 1% by default

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
                wd.setVoltage(voltage);
                wd.setBatteryLevel(battery);
                wd.setChargingStatus(chargeMode);
                wd.setAngle(pitchAngle/100.0);
                if (hwPwmEnabled) {
                    wd.setOutput(hwPwm);
                    wd.updatePwm();
                } else {
                    wd.calculatePwm();
                }
                wd.calculateCurrent();
                wd.calculatePower();
                newDataFound = true;
            }
        }
        return newDataFound;
    }

    @Override
    public boolean isReady() {
        return WheelData.getInstance().getVoltage() != 0 && mVer != 0;
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
    public int getVer() {
        if (mVer >=2) {
            WheelLog.AppConfig.setHwPwm(true);
        }
        return mVer;
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
    public int getCellsForWheel() {
        if (mVer >= 5) {
            return 36;
        } else if (mVer == 4) {
            return 30;
        } else {
            return 24;
        }
    }

    @Override
    public void wheelBeep() {
        if (mVer < 3) {
            WheelData.getInstance().bluetoothCmd("b".getBytes());
        } else {
            WheelData.getInstance().bluetoothCmd(new byte[]{ 0x4c, 0x6b, 0x41, 0x70, 0x0e, 0x00, (byte) 0x80,(byte) 0x80,(byte) 0x80, 0x01,(byte) 0xca,(byte) 0x87,(byte) 0xe6, 0x6f});
        }

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
                    if (((bsize == 22 || bsize == 30) && (c != 0x00)) || ((bsize == 23) && ((c & 0xFE) != 0x00)) ) {
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
                        if (len > 38) { // new format with crc32
                            CRC32 crc = new CRC32();
                            crc.update(getBuffer(), 0, len);
                            long calc_crc = crc.getValue();
                            long provided_crc = MathsUtil.intFromBytesBE(getBuffer(), len);
                            if (calc_crc == provided_crc) {
                                Timber.i("CRC32 ok");
                                return true;
                            } else {
                                Timber.i("CRC32 fail");
                                return false;
                            }
                        }
                        return true; // old format without crc32

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