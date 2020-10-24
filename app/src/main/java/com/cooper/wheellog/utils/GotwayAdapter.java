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

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode Begode");
        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();

        for (byte c : data) {
            if (unpacker.addChar(c)) {
                byte[] buff = unpacker.getBuffer();
                Boolean useRatio = WheelLog.AppConfig.getUseRatio();
                Boolean useBetterPercents = WheelLog.AppConfig.getUseBetterPercents();
                int gotwayNegative = WheelLog.AppConfig.getGotwayNegative();
                gotwayNegative = 1;
                if (buff[18] == (byte) 0x00) { // life data
                    Timber.i("Get new life data");
                    int voltage = MathsUtil.shortFromBytesBE(buff, 2);
                    int speed = (int)Math.round(MathsUtil.shortFromBytesBE(buff, 4) * 3.6);
                    long distance = MathsUtil.shortFromBytesBE(buff, 8);
                    int phaseCurrent = MathsUtil.shortFromBytesBE(buff, 10);
                    int phaseCurrent2 =(data[10] * 256) + data[11];
                    int phaseCurrent3 =((data[10]) <<8 ) | (data[11]&0xFF);
                    byte bb1 = data[10];
                    byte bb2 = data[11];
                    int temperature2 = (int)Math.round(((MathsUtil.shortFromBytesBE(buff, 12)/340.0) + 35) * 100);
                    int temperature = (int) Math.round(((((buff[12] * 256) + buff[13]) / 340.0) + 35) * 100);
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
                        distance = (int)Math.round(distance * RATIO_GW);
                        speed = (int)Math.round(speed * RATIO_GW);
                    }
                    voltage = (int) Math.round(getScaledVoltage (voltage));

                    wd.setModel("Begode");
                    wd.setSpeed(speed);
                    wd.setTopSpeed(speed);
                    wd.setDistance(distance);
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
                    int totalDistance = (int)MathsUtil.getInt4(buff, 2);
                    if (useRatio) {
                        wd.setTotalDistance(Math.round(totalDistance * RATIO_GW));
                    } else {
                        wd.setTotalDistance(totalDistance);
                    }
                    int pedalsMode = (buff[6]>>4) & 0x0F;
                    int speedAlarms = buff[6] & 0x0F;
                    int ledMode = buff[13]&0xFF;

                    return false;
                }
            }
        }
        return false;
    }




    public boolean decode_old(byte[] data) {
        Timber.i("Decode Begode");
        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();
        Boolean useRatio = WheelLog.AppConfig.getUseRatio();
        if (data.length >= 20) {
            Timber.i("Len >=20");
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            int a3 = data[2] & 255;
            int a4 = data[3] & 255;
            int a5 = data[4] & 255;
            int a6 = data[5] & 255;
            int a19 = data[18] & 255;
            Boolean useBetterPercents = WheelLog.AppConfig.getUseBetterPercents();
            if ((a1 == 0xDC) && (a2 == 0x5A) && (a3 == 0x5C) && (a4 == 0x20)) {  // Sherman
                Timber.i("Decode Sherman");
                // TODO move Veteran to adapter
                wd.setModel("Veteran");
                wd.setWheelType(Constants.WHEEL_TYPE.VETERAN);
                int voltage = (data[4] & 0xFF) << 8 | (data[5] & 0xFF);
                wd.setVoltage(voltage);

                int speed = ((data[6]) << 8 | (data[7] & 0xFF))*10;
                wd.setSpeed(speed);
                long distance = ((data[10] & 0xFF) << 24 | (data[11] & 0xFF) << 16 | (data[8] & 0xFF) << 8 | (data[9] & 0xFF));
                wd.setDistance(distance);
                wd.setTotalDistance(((data[14] & 0xFF) << 24 | (data[15] & 0xFF) << 16 | (data[12] & 0xFF) << 8 | (data[13] & 0xFF)));
                wd.setPhaseCurrent(((data[16]) << 8 | (data[17] & 0xFF))*10);
                int temperature = (data[18] & 0xFF) << 8 | (data[19] & 0xFF);
                wd.setTemperature(temperature);
                wd.setTemperature2(temperature);
                wd.setTopSpeed(speed);
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

                wd.setBatteryPercent(battery);
                wd.setVoltageSag(voltage);
                wd.updateRideTime();
                return true;
            } else { // Gotway
                wd.setModel("Begode");
                if (a1 != 85 || a2 != 170 || a19 != 0) {
                    if (a1 != 90 || a5 != 85 || a6 != 170) {
                        return false;
                    }
                    int totalDistance = ((data[6] & 0xFF) << 24) | ((data[7] & 0xFF) << 16) | ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
                    wd.setTotalDistance(totalDistance);
                    if (useRatio) {
                        wd.setTotalDistance(Math.round(totalDistance * RATIO_GW));
                    }
                    return false;
                }

                int speed;
                int gotwayNegative = WheelLog.AppConfig.getGotwayNegative();
                if (data[5] >= 0)
                    if (gotwayNegative == 0) {
                        speed = (int) Math.abs(((data[4] * 256.0) + data[5]) * 3.6);
                    }
                    else {
                        speed = ((int) (((data[4] * 256.0) + data[5]) * 3.6)) * gotwayNegative;
                    }
                else if (gotwayNegative == 0) {
                    speed = (int) Math.abs((((data[4] * 256.0) + 256.0) + data[5]) * 3.6);
                }
                else {
                    speed = ((int) ((((data[4] * 256.0) + 256.0) + data[5]) * 3.6)) * gotwayNegative;
                }
                if (useRatio) {
                    speed = (int) Math.round(speed * RATIO_GW);
                }

                wd.setSpeed(speed);
                wd.setTopSpeed(speed);

                int temperature = (int) Math.round(((((data[12] * 256) + data[13]) / 340.0) + 35) * 100);
                wd.setTemperature(temperature);
                wd.setTemperature2(temperature);

                long distance = MathsUtil.getInt2(data, 8);
                if (useRatio) distance = Math.round(distance * RATIO_GW);
                wd.setDistance(distance);

                int voltage = (data[2] * 256) + (data[3] & 255);
                wd.setPhaseCurrent((data[10] * 256) + data[11]);
                wd.setCurrent(gotwayNegative == 0 ? Math.abs(wd.getCurrent()) : wd.getCurrent() * gotwayNegative);

                int battery;
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

                voltage = (int) Math.round(getScaledVoltage (voltage));
                wd.setVoltage(voltage);
                wd.setVoltageSag(voltage);
                wd.setBatteryPercent(battery);
                wd.updateRideTime();
            }

            return true;
        } else if (data.length >= 10 && !isVeteran())  {
            int a1 = data[0];
            int a5 = data[4] & 255;
            int a6 = data[5] & 255;
            if (a1 != 90 || a5 != 85 || a6 != 170) {
                return false;
            }
            long totalDistance = ((data[6]&0xFF) <<24) | ((data[7]&0xFF) << 16) | ((data[8] & 0xFF) <<8) | (data[9] & 0xFF);
            if (useRatio) {
                totalDistance = Math.round(totalDistance * RATIO_GW);
            }
            wd.setTotalDistance(totalDistance);
        } else if (isVeteran()) { // second part of Veteran
            wd.setVersion(String.format(Locale.US, "%d.%d (%d)", data[8], data[9],((data[8]<<8)  | data[9])));
            int autoOffSec = ((data[0]&0xFF)<<8) | data[1] & 0xFF;
            int isCharging = ((data[2]&0xFF)<<8) | data[3] & 0xFF;
            int speedAlert = ((data[4]&0xFF)<<8) | data[5] & 0xFF;
            int speedTiltback = ((data[6]&0xFF)<<8) | data[7] & 0xFF;
            int pedalsMode = ((data[10]&0xFF)<<8) | data[11] & 0xFF;
            wd.setChargingStatus(isCharging);
        }
        return false;
    }

    public boolean isVeteran() {
        return WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE.VETERAN;
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

    @Override
    public int getCellSForWheel() {
        if (isVeteran()) {
            return 24;
        }

        switch (WheelLog.AppConfig.getGotwayVoltage()) {
            case 0: return 16;
            case 1: return 20;
        }

        return 24;
    }

    static class veteranUnpacker {

        enum UnpackerState {
            unknown,
            flagsearch,
            lensearch,
            collecting,
            done
        }


        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int old1 = 0;
        int old2 = 0;
        int old3 = 0;

        int len = 0;
        int flags = 0;
        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {

            switch (state) {

                case collecting:

                    buffer.write(c);
                    if (buffer.size() == len+5) {
                        state = UnpackerState.done;
                        Timber.i("Len %d", len);
                        Timber.i("Step reset");
                        return true;
                    }
                    break;


                default:
                    if (c == (byte) 0xAA && old1 == (byte) 0xAA && old2 == (byte) 0xAA && old3 == (byte) 0xAA) {
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0xAA);
                        buffer.write(0xAA);
                        buffer.write(0xAA);
                        buffer.write(0xAA);
                        state = UnpackerState.collecting;
                    } else if (c == (byte) 0xAA && old1 == (byte) 0xAA && old2 == (byte) 0xAA) {
                        old3 = old2;
                        old2 = old1;
                    } else if (c == (byte) 0xAA && old1 == (byte) 0xAA) {
                        old2 = old1;
                        old3 = 0;
                    } else {
                        old2 = 0;
                        old3 = 0;
                    }
                    old1 = c;

            }
            return false;
        }

        void reset() {
            old1 = 0;
            old2 = 0;
            old3 = 0;
            state = UnpackerState.unknown;

        }
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