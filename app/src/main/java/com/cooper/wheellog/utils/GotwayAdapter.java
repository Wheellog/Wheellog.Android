package com.cooper.wheellog.utils;

import com.cooper.wheellog.WheelData;
import java.util.Locale;
import timber.log.Timber;

public class GotwayAdapter implements IWheelAdapter {
    private static GotwayAdapter INSTANCE;

    private static final double RATIO_GW = 0.875;
    private int mGotwayVoltageScaler = 0;
    private int mGotwayNegative = -1;

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode Begode");
        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();
        if (data.length >= 20) {
            Timber.i("Len >=20");
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            int a3 = data[2] & 255;
            int a4 = data[3] & 255;
            int a5 = data[4] & 255;
            int a6 = data[5] & 255;
            int a19 = data[18] & 255;
            if ((a1 == 0xDC) && (a2 == 0x5A) && (a3 == 0x5C) && (a4 == 0x20)) {  // Sherman
                Timber.i("Decode Sherman");
                // TODO move Veteran to adapter
                wd.setModel("Veteran");
                wd.setWheelType(Constants.WHEEL_TYPE.VETERAN);
                int voltage = (data[4] & 0xFF) << 8 | (data[5] & 0xFF);
                wd.setVoltage(voltage);
                wd.setSpeed(((data[6]) << 8 | (data[7] & 0xFF))*10);
                long distance = ((data[10] & 0xFF) << 24 | (data[11] & 0xFF) << 16 | (data[8] & 0xFF) << 8 | (data[9] & 0xFF));
                wd.setDistance(distance);
                wd.setTotalDistance(((data[14] & 0xFF) << 24 | (data[15] & 0xFF) << 16 | (data[12] & 0xFF) << 8 | (data[13] & 0xFF)));
                wd.setPhaseCurrent(((data[16]) << 8 | (data[17] & 0xFF))*10);
                int temperature = (data[18] & 0xFF) << 8 | (data[19] & 0xFF);
                wd.setTemperature(temperature);
                wd.setTemperature2(temperature);
                wd.setTopSpeed(wd.getSpeed());
                int battery;
                if (wd.getBetterPercents()) {
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
                    if (wd.getUseRatio()) {
                        wd.setTotalDistance(Math.round(totalDistance * RATIO_GW));
                    }
                    return false;
                }

                int speed;
                if (data[5] >= 0)
                    if (mGotwayNegative == 0) {
                        speed = (int) Math.abs(((data[4] * 256.0) + data[5]) * 3.6);
                    }
                    else {
                        speed = ((int) (((data[4] * 256.0) + data[5]) * 3.6)) * mGotwayNegative;
                    }
                else if (mGotwayNegative == 0) {
                    speed = (int) Math.abs((((data[4] * 256.0) + 256.0) + data[5]) * 3.6);
                }
                else {
                    speed = ((int) ((((data[4] * 256.0) + 256.0) + data[5]) * 3.6)) * mGotwayNegative;
                }
                if (wd.getUseRatio()) {
                    speed = (int) Math.round(speed * RATIO_GW);
                }

                wd.setSpeed(speed);
                wd.setTopSpeed(speed);

                int temperature = (int) Math.round(((((data[12] * 256) + data[13]) / 340.0) + 35) * 100);
                wd.setTemperature(temperature);
                wd.setTemperature2(temperature);

                long distance = MathsUtil.getInt2(data, 8);
                if (wd.getUseRatio()) distance = Math.round(distance * RATIO_GW);
                wd.setDistance(distance);

                int voltage = (data[2] * 256) + (data[3] & 255);
                wd.setPhaseCurrent((data[10] * 256) + data[11]);
                wd.setCurrent(mGotwayNegative == 0 ? Math.abs(wd.getCurrent()) : wd.getCurrent() * mGotwayNegative);

                int battery;
                if (wd.getBetterPercents()) {
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
            if (wd.getUseRatio()) {
                totalDistance = Math.round(totalDistance * RATIO_GW);
            }
            wd.setTotalDistance(totalDistance);
        } else if (isVeteran()) { // second part
            wd.setVersion(String.format(Locale.US, "%d.%d (%d)", data[8], data[9],((data[8]<<8)  | data[9])));
        }
        return false;
    }

    public boolean isVeteran() {
        return WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE.VETERAN;
    }

    public double getCorrectedTiltbackVoltage(double tiltbackVoltage) {
        if (isVeteran()) {
            return tiltbackVoltage > 79.2 || tiltbackVoltage < 72
                    ? 75.6
                    : tiltbackVoltage;
        }

        double min = getScaledVoltage(48);
        double max = getScaledVoltage(52.8);
        return tiltbackVoltage > max || tiltbackVoltage < min
                ? max
                : tiltbackVoltage;
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

    public static GotwayAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GotwayAdapter();
        }
        return INSTANCE;
    }

    public int getGotwayVoltageScaler() {
        return mGotwayVoltageScaler;
    }

    public void setGotwayVoltageScaler(int value) {
        mGotwayVoltageScaler = value;
    }

    public void setGotwayNegative(int value) {
        mGotwayNegative = value;
    }

    private double getScaledVoltage(double value) {
        return value * (1 + (0.25 * mGotwayVoltageScaler));
    }
}
