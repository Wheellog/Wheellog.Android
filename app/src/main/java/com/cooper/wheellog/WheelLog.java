package com.cooper.wheellog;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WheelLog extends Application {
    private int mSpeed;
    private long mTotalDistance;
    private int mCurrent;
    private int mTemperature;
    private int mMode;
    private int mBattery;
    private int mVoltage;
    private long mDistance;
    private int mCurrentTime;
    private int mTopSpeed;
    private int mFanStatus;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private String mName = "";
    private String mModel = "";
    private int mVersion;
    private String mSerialNumber = "";
    private int mWheelType;

    public int getSpeed() { return mSpeed / 10; }
    public int getTemperature() { return mTemperature / 100; }
    public int getBatteryLevel() { return mBattery; }
    public int getFanStatus() { return mFanStatus; }
    public int getConnectionState() { return mConnectionState; }
//    public int getTopSpeed() { return maxSpeed; }
    public int getVersion() { return mVersion; }
//    public int getCurrentTime() { return currentTime; }
    public int getMode() { return mMode; }
    public int getWheelType() { return mWheelType; }

    public String getName() { return mName; }
    public String getModel() { return mModel; }
    public String getSerial() { return mSerialNumber; }
    public String getCurrentTimeString() {
        long hours = TimeUnit.SECONDS.toHours(mCurrentTime);
        long minutes = TimeUnit.SECONDS.toMinutes(mCurrentTime) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(mCurrentTime));
        long seconds = TimeUnit.SECONDS.toSeconds(mCurrentTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(mCurrentTime));
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    public double getSpeedDouble() { return mSpeed / 100.0; }
    public double getVoltageDouble() { return mVoltage / 100.0; }
    public double getPowerDouble() { return (mCurrent/100.0)*(mVoltage/100.0); }
    public double getCurrentDouble() { return mCurrent/100.0; }
    public double getTopSpeedDouble() { return mTopSpeed / 100.0; }
    public double getDistanceDouble() { return mDistance / 1000.0; }
    public double getTotalDistanceDouble() { return mTotalDistance / 1000.0; }

    public void setConnectionState(boolean connected) { mConnectionState = connected ? 1 : 0; }

    private int byteArrayInt2(byte low, byte high) { return (low & 255) + ((high & 255) * 256); }

    private long byteArrayInt4(byte value1, byte value2, byte value3, byte value4) {
        return (((((long) ((value1 & 255) << 16))) | ((long) ((value2 & 255) << 24))) | ((long) (value3 & 255))) | ((long) ((value4 & 255) << 8));
    }

    public void decodeResponse(byte[] data) {
        if (mWheelType == Constants.WHEEL_TYPE_KINGSONG)
            decodeKingSong(data);

        Intent intent = new Intent(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        sendBroadcast(intent);
    }

    private void decodeKingSong(byte[] data) {
        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            if (a1 != 170 || a2 != 85) { // Not sure what this does?
                return;
            }
            if ((data[16] & 255) == 169) { // Live data
                mVoltage = byteArrayInt2(data[2], data[3]);
                mSpeed = byteArrayInt2(data[4], data[5]);
                mTotalDistance = byteArrayInt4(data[6], data[7], data[8], data[9]);
                mCurrent = byteArrayInt2(data[10], data[11]);
                if (mCurrent > 7000) {
                    mCurrent = 7000;
                } else if (mCurrent < 0) {
                    mCurrent = 0;
                }
                mTemperature = byteArrayInt2(data[12], data[13]);
//                currentMode = -1;
                if ((data[15] & 255) == 224) {
                    mMode = data[14];
                }

                if (mVoltage < 5000) {
                    mBattery = 10;
                } else if (mVoltage >= 6600) {
                    mBattery = 100;
                } else {
                    mBattery = (((mVoltage/100) - 50) * 100) / 16;
                }
            } else if ((data[16] & 255) == 185) { // Distance/Time/Fan Data
                mDistance = byteArrayInt4(data[2], data[3], data[4], data[5]);
                mCurrentTime = byteArrayInt2(data[6], data[7]);
                mTopSpeed = byteArrayInt2(data[8], data[9]);
                mFanStatus = data[12];
            } else if ((data[16] & 255) == 187) { // Name and Type data
                int end = 0;
                int i = 0;
                while (i < 14 && data[i + 2] != 0) {
                    end++;
                    i++;
                }
                mName = new String(data, 2, end).trim();
                mModel = "";
                String[] ss = mName.split("-");
                for (i = 0; i < ss.length - 1; i++) {
                    if (i != 0) {
                        mModel += "-";
                    }
                    mModel += ss[i];
                }
                try {
                    mVersion = Integer.parseInt(ss[ss.length - 1]);
                } catch (Exception ignored) {
                }

                if (mSerialNumber.isEmpty())
                    sendBroadcast(new Intent(Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA));

            } else if ((data[16] & 255) == 179) { // Serial Number
                byte[] sndata = new byte[18];
                System.arraycopy(data, 2, sndata, 0, 14);
                System.arraycopy(data, 17, sndata, 14, 3);
                sndata[17] = (byte) 0;
                mSerialNumber = new String(sndata);
            }
        }
    }

    public void reset() {
        mSpeed = 0;
        mTotalDistance = 0;
        mCurrent = 0;
        mTemperature = 0;
        mMode = 0;
        mBattery = 0;
        mVoltage = 0;
        mDistance = 0;
        mCurrentTime = 0;
        mTopSpeed = 0;
        mFanStatus = 0;
        mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
        mName = "";
        mModel = "";
        mVersion = 0;
        mSerialNumber = "";
        mWheelType = 0;
    }

    public void setWheelType(int wheelType) {
        mWheelType = wheelType;
    }

}
