package com.cooper.wheellog;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;

import com.cooper.wheellog.Utils.Constants;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WheelData {
    private static WheelData mInstance;
    private static Context mContext;

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
    private long rideStartTime;

    private WheelData(){
    }
    
    public static void initiate(Context context) {
        if(mInstance == null)
        {
            mInstance = new WheelData();
        }
        mContext = context.getApplicationContext();
    }

    public static WheelData getInstance(){
        return mInstance;
    }

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
    public double getPowerDouble() { return (mCurrent*mVoltage)/10000.0; }
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

//        StringBuilder stringBuilder = new StringBuilder(data.length);
//        for (byte aData : data)
//            stringBuilder.append(String.format("%02d ", aData));
//        Timber.i("OUTPUT", stringBuilder.toString());

        if (mWheelType == Constants.WHEEL_TYPE_KINGSONG)
            decodeKingSong(data);
        else if (mWheelType == Constants.WHEEL_TYPE_GOTWAY)
            decodeGotway(data);

        Intent intent = new Intent(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        mContext.sendBroadcast(intent);
    }

    private void decodeKingSong(byte[] data) {

        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            if (a1 != 170 || a2 != 85) {
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

                if ((data[15] & 255) == 224) {
                    mMode = data[14];
                }

                if (mVoltage < 5000) {
                    mBattery = 0;
                } else if (mVoltage >= 6600) {
                    mBattery = 100;
                } else {
                    mBattery = (mVoltage - 5000) / 16;
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
                    mContext.sendBroadcast(new Intent(Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA));

            } else if ((data[16] & 255) == 179) { // Serial Number
                byte[] sndata = new byte[18];
                System.arraycopy(data, 2, sndata, 0, 14);
                System.arraycopy(data, 17, sndata, 14, 3);
                sndata[17] = (byte) 0;
                mSerialNumber = new String(sndata);
            }
        }
    }

    private void decodeGotway(byte[] data) {
        if (rideStartTime == 0)
            rideStartTime = Calendar.getInstance().getTimeInMillis();

        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            int a19 = data[18] & 255;
            if (a1 != 85 || a2 != 170 || a19 != 0) {
                return;
            }

            if (data[5] >= 0)
                mSpeed = (int) Math.abs(((data[4] * 256.0) + data[5]) * 3.6);
            else
                mSpeed = (int) Math.abs((((data[4] * 256.0) + 256.0) + data[5]) * 3.6);

            if (mSpeed > mTopSpeed)
                mTopSpeed = mSpeed;

            mTemperature = (int) Math.round(((((data[12] * 256) + data[13]) / 340.0) + 35) * 100);

            mDistance = byteArrayInt2(data[9], data[8]);

            mVoltage = (data[2] * 256) + (data[3] & 255);

            mCurrent = Math.abs((data[10] * 256) + data[11]);

            if (mVoltage <= 5290) {
                mBattery = 0;
            } else if (mVoltage >= 6580) {
                mBattery = 100;
            } else {
                mBattery = (mVoltage - 5290) / 13;
            }

            mCurrentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;

        } else if (data.length >= 10) {
            int a1 = data[0];
            int a5 = data[4] & 255;
            int a6 = data[5] & 255;
            if (a1 != 90 || a5 != 85 || a6 != 170) {
                return;
            }

            mTotalDistance = ((((data[6] * 256) + data[7]) * 65536) + (((data[8] & 255) * 256) + (data[9] & 255)));
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
        rideStartTime = 0;
    }

    public boolean detectWheel(BluetoothGatt gatt) {
        Class<R.array> res = R.array.class;
        String wheel_types[] = mContext.getResources().getStringArray(R.array.wheel_types);
        for (String wheel_Type : wheel_types) {
            boolean detected_wheel = true;
            java.lang.reflect.Field services_res = null;
            try { services_res = res.getField(wheel_Type+"_services"); } catch (Exception ignored) {}
            int services_res_id = 0;
            if (services_res != null)
                try { services_res_id = services_res.getInt(null); } catch (Exception ignored) {}

            String services[] = mContext.getResources().getStringArray(services_res_id);

            for (String service_uuid : services) {
                UUID s_uuid = UUID.fromString(service_uuid.replace("_", "-"));
                BluetoothGattService service = gatt.getService(s_uuid);
                if (service != null) {
                    java.lang.reflect.Field characteristic_res = null;
                    try { characteristic_res = res.getField(wheel_Type+"_"+service_uuid); } catch (Exception ignored) {}
                    int characteristic_res_id = 0;
                    if (characteristic_res != null)
                        try { characteristic_res_id = characteristic_res.getInt(null); } catch (Exception ignored) {}
                    String characteristics[] = mContext.getResources().getStringArray(characteristic_res_id);
                    for (String characteristic_uuid : characteristics) {
                        UUID c_uuid = UUID.fromString(characteristic_uuid.replace("_", "-"));
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(c_uuid);
                        if (characteristic == null) {
                            detected_wheel = false;
                            break;
                        }
                    }
                } else {
                    detected_wheel = false;
                    break;
                }
            }

            if (detected_wheel) {
                if (mContext.getResources().getString(R.string.kingsong).equals(wheel_Type)) {
                    mWheelType = Constants.WHEEL_TYPE_KINGSONG;
                    BluetoothGattService targetService = gatt.getService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID));
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.KINGSONG_NOTITY_CHARACTER_UUID));
                    gatt.setCharacteristicNotification(notifyCharacteristic, true);
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.KINGSONG_DESCRIPTER_UUID));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                } else if (mContext.getResources().getString(R.string.gotway).equals(wheel_Type)) {
                    mWheelType = Constants.WHEEL_TYPE_GOTWAY;
                    BluetoothGattService targetService = gatt.getService(UUID.fromString(Constants.GOTWAY_SERVICE_UUID));
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.GOTWAY_READ_CHARACTER_UUID));
                    gatt.setCharacteristicNotification(notifyCharacteristic, true);
                    // Let the user know it's working by making the wheel beep
                    notifyCharacteristic.setValue("b".getBytes());
                    gatt.writeCharacteristic(notifyCharacteristic);
                }
                return true;
            }
        }
        return false;
    }
}
