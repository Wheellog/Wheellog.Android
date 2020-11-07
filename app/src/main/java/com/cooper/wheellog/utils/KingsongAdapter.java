package com.cooper.wheellog.utils;
import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;

import java.util.Locale;
import timber.log.Timber;

public class KingsongAdapter extends BaseAdapter {
    private static KingsongAdapter INSTANCE;

    private int mKSAlarm1Speed = 0;
    private int mKSAlarm2Speed = 0;
    private int mKSAlarm3Speed = 0;
    private boolean mKSAlertsAndSpeedupdated = false;
    private boolean m18Lkm = true;
    private int mMode;
    private static final double KS18L_SCALER = 0.83;
    private double mSpeedLimit;

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode KingSong");
        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();
        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            if (a1 != 170 || a2 != 85) {
                return false;
            }
            if ((data[16] & 255) == 0xA9) {
                // Live data
                int voltage = MathsUtil.getInt2R(data, 2);
                wd.setVoltage(voltage);
                wd.setSpeed(MathsUtil.getInt2R(data, 4));
                wd.setTotalDistance(MathsUtil.getInt4R(data, 6));
                if ((wd.getModel().compareTo("KS-18L") == 0) && !m18Lkm) {
                    wd.setTotalDistance(Math.round(wd.getTotalDistance() * KS18L_SCALER));
                }
                wd.setCurrent((data[10] & 0xFF) + (data[11] << 8));

                wd.setTemperature(MathsUtil.getInt2R(data, 12));
                wd.setVoltageSag(voltage);
                if ((data[15] & 255) == 224) {
                    mMode = data[14];
                    wd.setModeStr(String.format(Locale.US, "%d", mMode));
                }

                int battery;
                Boolean useBetterPercents = WheelLog.AppConfig.getUseBetterPercents();
                if (is84vWheel()) {
                    if (useBetterPercents) {
                        if (voltage > 8350) {
                            battery = 100;
                        } else if (voltage > 6800) {
                            battery = (voltage - 6650) / 17;
                        } else if (voltage > 6400) {
                            battery = (voltage - 6400) / 45;
                        } else {
                            battery = 0;
                        }
                    } else {
                        if (voltage < 6250) {
                            battery = 0;
                        } else if (voltage >= 8250) {
                            battery = 100;
                        } else {
                            battery = (voltage - 6250) / 20;
                        }
                    }
                } else {
                    if (useBetterPercents) {
                        if (voltage > 6680) {
                            battery = 100;
                        } else if (voltage > 5440) {
                            battery = (int) Math.round((voltage - 5320) / 13.6);
                        } else if (voltage > 5120) {
                            battery = (voltage - 5120) / 36;
                        } else {
                            battery = 0;
                        }
                    } else {
                        if (voltage < 5000) {
                            battery = 0;
                        } else if (voltage >= 6600) {
                            battery = 100;
                        } else {
                            battery = (voltage - 5000) / 16;
                        }
                    }
                }
                wd.setBatteryPercent(battery);
                return true;
            } else if ((data[16] & 255) == 0xB9) { // Distance/Time/Fan Data
                long distance = MathsUtil.getInt4R(data, 2);
                wd.setWheelDistance(distance);
                wd.updateRideTime();
                wd.setTopSpeed(MathsUtil.getInt2R(data, 8));
                wd.setFanStatus(data[12]);
                wd.setChargingStatus(data[13]);
                wd.setTemperature2(MathsUtil.getInt2R(data, 14));
                return false;
            } else if ((data[16] & 255) == 187) { // Name and Type data
                int end = 0;
                int i = 0;
                while (i < 14 && data[i + 2] != 0) {
                    end++;
                    i++;
                }
                wd.setName(new String(data, 2, end).trim());
                wd.setModel("");
                String[] ss = wd.getName().split("-");
                StringBuilder model = new StringBuilder();
                for (i = 0; i < ss.length - 1; i++) {
                    if (i != 0) {
                        model.append("-");
                    }
                    model.append(ss[i]);
                }
                wd.setModel(model.toString());
                try {
                    wd.setVersion(String.format(Locale.US, "%.2f", ((double) (Integer.parseInt(ss[ss.length - 1]) / 100.0))));
                } catch (Exception ignored) {
                }
                return false;
            } else if ((data[16] & 255) == 179) { // Serial Number
                byte[] sndata = new byte[18];
                System.arraycopy(data, 2, sndata, 0, 14);
                System.arraycopy(data, 17, sndata, 14, 3);
                sndata[17] = (byte) 0;
                wd.setSerial(new String(sndata));
                updateKSAlarmAndSpeed();
                return false;
            } else if ((data[16] & 255) == 0xF5) { //cpu load
                wd.setCpuLoad(data[14]);
                wd.setOutput(data[15]);
                return false;
            } else if ((data[16] & 255) == 0xF6) { //speed limit (PWM?)
                mSpeedLimit = MathsUtil.getInt2R(data, 2) / 100.0;
                wd.setSpeedLimit(mSpeedLimit);
                return false;
            } else if ((data[16] & 255) == 164 || (data[16] & 255) == 181) { //0xa4 || 0xb5 max speed and alerts
                wd.setWheelMaxSpeed(data[10] & 255);
                mKSAlarm3Speed = (data[8] & 255);
                mKSAlarm2Speed = (data[6] & 255);
                mKSAlarm1Speed = (data[4] & 255);
                mKSAlertsAndSpeedupdated = true;
                // after received 0xa4 send same repeat data[2] =0x01 data[16] = 0x98
                if ((data[16] & 255) == 164) {
                    data[16] = (byte) 0x98;
                    wd.getBluetoothLeService().writeBluetoothGattCharacteristic(data);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void updatePedalsMode(int pedalsMode) {
        WheelData.getInstance().updatePedalsMode(pedalsMode);
    }

    private boolean is84vWheel() {
        return StringUtil.inArray(WheelData.getInstance().getModel(), new String[]{"KS-18L", "KS-16X", "RW", "KS-18LH", "KS-S18"}) || WheelData.getInstance().getModel().startsWith("ROCKW");
    }

    @Override
    public int getCellSForWheel() {
        return is84vWheel() ? 20 : 16;
    }

    public static KingsongAdapter getInstance() {
        Timber.i("Get instance");
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new KingsongAdapter();
        }
        return INSTANCE;
    }

    public void updateKSAlarmAndSpeed() {
        byte[] data = new byte[20];
        data[0] = (byte) 0xAA;
        data[1] = (byte) 0x55;
        data[2] = (byte) mKSAlarm1Speed;
        data[4] = (byte) mKSAlarm2Speed;
        data[6] = (byte) mKSAlarm3Speed;
        data[8] = (byte) WheelData.getInstance().getWheelMaxSpeed();
        data[16] = (byte) 0x85;

        if ((WheelData.getInstance().getWheelMaxSpeed() | mKSAlarm3Speed | mKSAlarm2Speed | mKSAlarm1Speed) == 0) {
            data[16] = (byte) 0x98; // request speed & alarm values from wheel
        }

        data[17] = (byte) 0x14;
        data[18] = (byte) 0x5A;
        data[19] = (byte) 0x5A;
        WheelData.getInstance().getBluetoothLeService().writeBluetoothGattCharacteristic(data);
    }

    public void updateKSAlarm1(int wheelKSAlarm1) {
        if (mKSAlarm1Speed != wheelKSAlarm1) {
            mKSAlarm1Speed = wheelKSAlarm1;
            updateKSAlarmAndSpeed();
        }
    }

    public void updateKSAlarm2(int wheelKSAlarm2) {
        if (mKSAlarm2Speed != wheelKSAlarm2) {
            mKSAlarm2Speed = wheelKSAlarm2;
            updateKSAlarmAndSpeed();
        }
    }

    public void updateKSAlarm3(int wheelKSAlarm3) {
        if (mKSAlarm3Speed != wheelKSAlarm3) {
            mKSAlarm3Speed = wheelKSAlarm3;
            updateKSAlarmAndSpeed();
        }
    }

    public void set18Lkm(boolean enabled) {
        m18Lkm = enabled;

        WheelData wd = WheelData.getInstance();
        if ((wd.getModel().compareTo("KS-18L") == 0) && !m18Lkm) {
            wd.setTotalDistance(Math.round(wd.getTotalDistance() * KS18L_SCALER));
        }
    }

    public int getMode() {
        return mMode;
    }

    public double getSpeedLimit() {
        return mSpeedLimit;
    }

    public Boolean getKSAlertsAndSpeedupdated() {
        return mKSAlertsAndSpeedupdated;
    }
}
