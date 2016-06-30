package com.cooper.wheellog;

import java.util.concurrent.TimeUnit;

public class Wheel {
    private static int speed;
    private static long totalDistance;
    private static int current;
    private static int temperature;
    private static int currentMode;
    private static int battery;
    private static int voltage;
    private static long currentDistance;
    private static int currentTime;
    private static int maxSpeed;
    private static int fanStatus;
    private static int connectionState = BluetoothLeService.STATE_DISCONNECTED;
    private static String mDeviceNameString;
    private static String mUnicycleType;
    private static int mVersion;
    private static String mUnicycleSN;

    private static Wheel mInstance = null;

    public static Wheel getInstance() {
        if(mInstance == null)
        {
            mInstance = new Wheel();
        }
        return mInstance;
    }

    public int getSpeed() { return speed; }
    public int getTemperature() { return temperature; }
    public int getBatteryLevel() { return battery; }
    public int getFanStatus() { return fanStatus; }
    public int getConnectionState() { return connectionState; }
    public int getMaxSpeed() { return maxSpeed; }
    public int getVersion() { return mVersion; }
    public int getCurrentTime() { return currentTime; };

    public String getName() { return mDeviceNameString; }
    public String getType() { return mUnicycleType; }
    public String getSerial() { return mUnicycleSN; }
    public String getCurrentTimeString() {
        long hours = TimeUnit.SECONDS.toHours(currentTime);
        long minutes = TimeUnit.SECONDS.toMinutes(currentTime) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(currentTime));
        long seconds = TimeUnit.SECONDS.toSeconds(currentTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(currentTime));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public double getSpeedDouble() { return (double) speed / 10.0F; }
    public double getVoltageDouble() { return (double) voltage / 10.0F; }
    public double getCurrentDouble() { return (double) current / 10.0F; }
    public double getMaxSpeedDouble() { return (double) maxSpeed / 10.0F; }
    public double getCurrentDistanceDouble() { return (double) currentDistance / 1000.0F; }
    public double getTotalDistanceDouble() { return (double) totalDistance / 1000.0F; }

    public void setConnectionState(boolean connected) { connectionState = connected ? 1 : 0; }

    private int byteArrayInt2(byte low, byte high) {
        return (low & 255) + ((high & 255) * 256);
    }

    private long byteArrayInt4(byte value1, byte value2, byte value3, byte value4) {
        return (((((long) ((value1 & 255) << 16))) | ((long) ((value2 & 255) << 24))) | ((long) (value3 & 255))) | ((long) ((value4 & 255) << 8));
    }

    public int decodeResponse(byte[] data) {
        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            if (a1 != 170 || a2 != 85) {
                return 0;
            }
            if ((data[16] & 255) == 169) {
                voltage = byteArrayInt2(data[2], data[3]) / 10;
                speed = byteArrayInt2(data[4], data[5]) / 10;
                totalDistance = byteArrayInt4(data[6], data[7], data[8], data[9]);
                current = byteArrayInt2(data[10], data[11]);
                temperature = byteArrayInt2(data[12], data[13]) / 100;
                currentMode = -1;
                if ((data[15] & 255) == 224) {
                    currentMode = data[14];
                }

                if (voltage < 500) {
                    battery = 10;
                } else if (voltage >= 660) {
                    battery = 100;
                } else {
                    battery = (((voltage/10) - 50) * 100) / 16;
                }
            } else if ((data[16] & 255) == 185) {
                currentDistance = byteArrayInt4(data[2], data[3], data[4], data[5]);
                currentTime = byteArrayInt2(data[6], data[7]);
                maxSpeed = byteArrayInt2(data[8], data[9]) / 10;
                fanStatus = data[12];
            } else if ((data[16] & 255) == 187) {
                int end = 0;
                int i = 0;
                while (i < 14 && data[i + 2] != 0) {
                    end++;
                    i++;
                }
                mDeviceNameString = new String(data, 2, end).trim();
                mUnicycleType = "";
                String[] ss = mDeviceNameString.split("-");
                for (i = 0; i < ss.length - 1; i++) {
                    if (i != 0) {
                        mUnicycleType += "-";
                    }
                    mUnicycleType += ss[i];
                }
                try {
                    mVersion = Integer.parseInt(ss[ss.length - 1]);
                } catch (Exception e) {
                }
                return Constants.REQUEST_SERIAL_DATA;
            } else if ((data[16] & 255) == 179) {
                byte[] sndata = new byte[18];
                for (int i = 0; i < 14; i++) {
                    sndata[i] = data[i + 2];
                }
                for (int i = 14; i < 17; i++) {
                    sndata[i] = data[i + 3];
                }
                sndata[17] = (byte) 0;
                mUnicycleSN = new String(sndata);
            }
        }
        return 0;
    }

}
