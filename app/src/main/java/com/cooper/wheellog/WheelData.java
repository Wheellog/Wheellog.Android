package com.cooper.wheellog;

import static com.cooper.wheellog.BroadcastKt.broadcastData;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.cooper.wheellog.utils.*;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.koin.java.KoinJavaComponent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class WheelData {
    private final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
    private static final int TIME_BUFFER = 10;
    private static WheelData mInstance;
    private Timer ridingTimerControl;
    private BluetoothService mBluetoothService;

    private long graph_last_update_time;
    private static final int GRAPH_UPDATE_INTERVAL = 1000; // milliseconds
    private static final int RIDING_SPEED = 200; // 2km/h
    private final ArrayList<String> xAxis = new ArrayList<>();
    private final ArrayList<Float> currentAxis = new ArrayList<>();
    private final ArrayList<Float> speedAxis = new ArrayList<>();
    // BMS
    private final SmartBms mSmartBms1 = new SmartBms();
    private final SmartBms mSmartBms2 = new SmartBms();
    //all
    private int mSpeed;
    private double mTorque;
    private double mMotorPower;
    private int mCpuTemp;
    private int mImuTemp;
    private double mSpeedLimit;
    private double mCurrentLimit;
    private long mTotalDistance;
    private int mCurrent;
    private int mPower = 0;
    private int mPhaseCurrent;
    private int mTemperature;
    private int mMaxTemp;
    private double mMaxCurrent = 0;
    private double mMaxPower = 0;
    private int mTemperature2;
    private int mCpuLoad;
    private int mOutput;
    private double mAngle;
    private double mRoll;
    private boolean mWheelIsReady = false;

    private int mBattery;
    private int mBatteryStart = -1;
    private int mBatteryLowest = 101;
    private int mVoltage;
    private long mDistance;
    private long mUserDistance;
    private int mRideTime;
    private int mRidingTime;
    private int mSleepTimer;
    private int mLastRideTime;
    private int mTopSpeed;
    private int mVoltageSag;
    private int mFanStatus;
    private int mChargingStatus;
    private boolean mWheelAlarm = false;
    private boolean mConnectionState = false;
    private String mName = "Unknown";
    private String mModel = "Unknown";
    private String mModeStr = "Unknown";
    private String mBtName = "";

    private StringBuilder mAlert = new StringBuilder();

    //    private int mVersion; # sorry King, but INT not good for Inmo
    private String mVersion = "";
    private String mError = "";
    private String mSerialNumber = "Unknown";
    private WHEEL_TYPE mWheelType = WHEEL_TYPE.Unknown;
    private long rideStartTime;
    private long mStartTotalDistance;

    private double mCalculatedPwm = 0.0;
    private double mMaxPwm = 0.0;
    private long mLowSpeedMusicTime = 0;

    private boolean mBmsView = false;
    private String protoVer = "";

    private long timestamp_raw;
    private long timestamp_last;
    private long mLastLifeData = -1;

    public BaseAdapter getAdapter() {
        switch (mWheelType) {
            case GOTWAY_VIRTUAL:
                return GotwayVirtualAdapter.getInstance();
            case GOTWAY:
                return GotwayAdapter.getInstance();
            case VETERAN:
                return VeteranAdapter.getInstance();
            case KINGSONG:
                return KingsongAdapter.getInstance();
            case NINEBOT:
                return NinebotAdapter.getInstance();
            case NINEBOT_Z:
                return NinebotZAdapter.getInstance();
            case INMOTION:
                return InMotionAdapter.getInstance();
            case INMOTION_V2:
                return InmotionAdapterV2.getInstance();
            default:
                return null;
        }
    }

    public BluetoothService getBluetoothService() {
        return mBluetoothService;
    }

    public boolean bluetoothCmd(byte[] cmd) {
        if (mBluetoothService == null) {
            return false;
        }
        return mBluetoothService.writeWheelCharacteristic(cmd);
    }

    public void setBluetoothService(BluetoothService value) {
        mBluetoothService = value;
    }

    static void initiate() {
        if (mInstance == null)
            mInstance = new WheelData();
        else {
            if (mInstance.ridingTimerControl != null) {
                mInstance.ridingTimerControl.cancel();
                mInstance.ridingTimerControl = null;
            }
        }

        mInstance.full_reset();
        mInstance.startRidingTimerControl();
        // mInstance.startAlarmTest(); // test
    }

    public void startRidingTimerControl() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (mConnectionState && (mSpeed > RIDING_SPEED)) mRidingTime += 1;
            }
        };
        ridingTimerControl = new Timer();
        ridingTimerControl.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    ///// test purpose, please let it be
    public void startAlarmTest() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Context mContext =  KoinJavaComponent.get(Context.class);
                Alarms.INSTANCE.checkAlarm(mCalculatedPwm, mContext);
            }
        }, 1000, 200000);

//        mCalculatedPwm = 70 / 100.0;
//        mSpeed = 50_00;
//        mBattery = 10;
//        mCurrent = 100_00;
        mTemperature = 60_00;

        appConfig.setAlarmTemperature(10);
//        WheelLog.AppConfig.setAlarmCurrent(10);

//        WheelLog.AppConfig.setAlarm1Speed(1);
//        WheelLog.AppConfig.setAlarm1Battery(70);
//        WheelLog.AppConfig.setAlarmFactor1(10_00);
        appConfig.setPwmBasedAlarms(false);
    }
    /////

    public static WheelData getInstance() {
        return mInstance;
    }

    public boolean isHardwarePWM()
    {
        switch (getWheelType())
        {
            case KINGSONG:
            case Unknown:
                return true;
            case INMOTION_V2:
                return InmotionAdapterV2.getInstance().getProto() >= 2;
            case VETERAN:
                return VeteranAdapter.getInstance().getVer() >= 2; // 2+
            default:
                return false;
        }
    }

    public boolean isWheelIsReady() {
        return mWheelIsReady;
    }

    public int getSpeed() {
        return (int) Math.round(mSpeed / 10.0);
    }

    public void setSpeed(int speed) {
        mSpeed = speed;
    }

    public double getTorque() {
        return mTorque;
    }

    public void setTorque(double value) {
        mTorque = value;
    }

    public double getMotorPower() {
        return mMotorPower;
    }

    public void setMotorPower(double value) {
        mMotorPower = value;
    }

    public int getCpuTemp() {
        return mCpuTemp;
    }

    public void setCpuTemp(int value) {
        mCpuTemp = value;
    }

    public int getImuTemp() {
        return mImuTemp;
    }

    public void setImuTemp(int value) {
        mImuTemp = value;
    }

    public double getSpeedLimit() {
        return mSpeedLimit;
    }

    public void setSpeedLimit(double value) {
        mSpeedLimit = value;
    }

    public double getCurrentLimit() {
        return mCurrentLimit;
    }

    public void setCurrentLimit(double value) {
        mCurrentLimit = value;
    }

    public void setBtName(String btName) {
        if (btName != null) {
            mBtName = btName;
        }
    }

    public String getBtName() {
        return mBtName;
    }

    public String getProtoVer() {
        return protoVer;
    }

    public void updateLight(boolean enabledLight) {
        if (getAdapter() != null) {
            getAdapter().setLightState(enabledLight);
        }
    }

    public void updateLed(boolean enabledLed) {
        if (getAdapter() != null) {
            getAdapter().setLedState(enabledLed);
        }
    }

    public void updateTailLight(boolean tailLight) {
        if (getAdapter() != null) {
            getAdapter().setTailLightState(tailLight);
        }
    }

    public void wheelBeep() {
        if (getAdapter() != null) {
            getAdapter().wheelBeep();
        }
    }

    public void updatePedalsMode(int pedalsMode) {
        if (getAdapter() != null) {
            getAdapter().updatePedalsMode(pedalsMode);
        }
    }

    public void updateStrobe(int strobeMode) {
        if (getAdapter() != null) {
            getAdapter().updateStrobeMode(strobeMode);
        }
    }

    public void updateLedMode(int ledMode) {
        if (getAdapter() != null) {
            getAdapter().updateLedMode(ledMode);
        }
    }


    public void updateAlarmMode(int alarmMode) {
        if (getAdapter() != null) {
            getAdapter().updateAlarmMode(alarmMode);
        }
    }

    public void wheelCalibration() {
        if (getAdapter() != null) {
            getAdapter().wheelCalibration();
        }
    }

    public void powerOff() {
        if (getAdapter() != null) {
            getAdapter().powerOff();
        }
    }


    public void updateHandleButton(boolean enabledButton) {
        if (getAdapter() != null) {
            getAdapter().setHandleButtonState(enabledButton);
        }
    }

    public void updateBrakeAssistant(boolean brakeAssist) {
        if (getAdapter() != null) {
            getAdapter().setBrakeAssist(brakeAssist);
        }
    }

    public void setLedColor(int value, int ledNum) {
        if (getAdapter() != null) {
            getAdapter().setLedColor(value, ledNum);
        }
    }

    public void updateAlarmEnabled(boolean value, int num) {
        if (getAdapter() != null) {
            getAdapter().setAlarmEnabled(value, num);
        }
    }

    public void updateAlarmSpeed(int value, int num) {
        if (getAdapter() != null) {
            getAdapter().setAlarmSpeed(value, num);
        }
    }

    public void updateLimitedModeEnabled(boolean value) {
        if (getAdapter() != null) {
            getAdapter().setLimitedModeEnabled(value);
        }
    }

    public void updateLimitedSpeed(int value) {
        if (getAdapter() != null) {
            getAdapter().setLimitedSpeed(value);
        }
    }

    public void updateMaxSpeed(int wheelMaxSpeed) {
        if (getAdapter() != null) {
            getAdapter().updateMaxSpeed(wheelMaxSpeed);
        }
    }

    public void updateSpeakerVolume(int speakerVolume) {
        if (getAdapter() != null) {
            getAdapter().setSpeakerVolume(speakerVolume);
        }
    }

    public void updatePedals(int pedalAdjustment) {
        if (getAdapter() != null) {
            getAdapter().setPedalTilt(pedalAdjustment);
        }
    }

    public void updatePedalSensivity(int pedalSensivity) {
        if (getAdapter() != null) {
            getAdapter().setPedalSensivity(pedalSensivity);
        }
    }

    public void updateRideMode(boolean rideMode) {
        if (getAdapter() != null) {
            getAdapter().setRideMode(rideMode);
        }
    }

    public void updateLockMode(boolean enable) {
        if (getAdapter() != null) {
            getAdapter().setLockMode(enable);
        }
    }

    public void updateTransportMode(boolean enable) {
        if (getAdapter() != null) {
            getAdapter().setTransportMode(enable);
        }
    }

    public void updateDrl(boolean enable) {
        if (getAdapter() != null) {
            getAdapter().setDrl(enable);
        }
    }

    public void updateGoHome(boolean enable) {
        if (getAdapter() != null) {
            getAdapter().setGoHomeMode(enable);
        }
    }

    public void updateFancierMode(boolean enable) {
        if (getAdapter() != null) {
            getAdapter().setFancierMode(enable);
        }
    }

    public void updateMute(boolean enable) {
        if (getAdapter() != null) {
            getAdapter().setMute(enable);
        }
    }

    public void updateFanQuiet(boolean enable) {
        if (getAdapter() != null) {
            getAdapter().setFanQuiet(enable);
        }
    }

    public void updateFanState(boolean enable) {
        if (getAdapter() != null) {
            getAdapter().setFan(enable);
        }
    }

    public void updateLightBrightness(int brightness) {
        if (getAdapter() != null) {
            getAdapter().setLightBrightness(brightness);
        }
    }

    public int getTemperature() {
        return mTemperature / 100;
    }

    public void setTemperature(int value) {
        mTemperature = value;
    }

    public int getMaxTemp() {
        return mMaxTemp / 100;
    }

    public int getTemperature2() {
        return mTemperature2 / 100;
    }

    public double getMaxCurrentDouble() {
        return mMaxCurrent / 100;
    }

    public double getMaxPowerDouble() {
        return mMaxPower / 100;
    }

    public int getCpuLoad() {
        return mCpuLoad;
    }

    public void setCpuLoad(int value) {
        mCpuLoad = value;
    }

    public int getOutput() {
        return mOutput/100;
    }

    public void setOutput(int value) {
        mOutput = value;
    }

    public void setTemperature2(int value) {
        mTemperature2 = value;
    }

    public double getAngle() {
        return mAngle;
    }

    public void setAngle(double angle) {
        mAngle = angle;
    }

    public double getRoll() {
        return mRoll;
    }

    public void setRoll(double roll) {
        mRoll = roll;
    }

    public int getBatteryLevel() {
        return mBattery;
    }

    public int getBatteryLowestLevel() {
        return mBatteryLowest;
    }

    public int getFanStatus() {
        return mFanStatus;
    }

    public void setFanStatus(int value) {
        mFanStatus = value;
    }

    public int getChargingStatus() {
        return mChargingStatus;
    }

    public int setChargingStatus(int charging) {
        return mChargingStatus = charging;
    }

    public boolean isConnected() {
        return mConnectionState;
    }

    public String getVersion() {
        return Objects.equals(mVersion, "") ? "Unknown" : mVersion;
    }

    public void setVersion(String value) {
        mVersion = value;
    }

    public String getError() {
        return Objects.equals(mError, "") ? "No" : mError;
    }

    public void setError(String value) {
        mError = value;
    }

    public void setWheelType(WHEEL_TYPE wheelType) {
        boolean isChanged = wheelType != mWheelType;
        mWheelType = wheelType;
        if (isChanged) {
            Context mContext = KoinJavaComponent.get(Context.class);
            Intent intent = new Intent(Constants.ACTION_WHEEL_TYPE_CHANGED);
            mContext.sendBroadcast(intent);
        }
    }

    public WHEEL_TYPE getWheelType() {
        return mWheelType;
    }

    public String getName() {
        return mName;
    }

    public void setName(String value) {
        mName = value;
    }

    public String getModel() {
        return mModel;
    }

    public void setModel(String model) {
        boolean isChanged = model != mModel;
        mModel = model;
        if (isChanged) {
            Intent intent = new Intent(Constants.ACTION_WHEEL_MODEL_CHANGED);
            Context mContext = KoinJavaComponent.get(Context.class);
            mContext.sendBroadcast(intent);
        }

    }

    public String getModeStr() {
        return mModeStr;
    }

    public void setModeStr(String value) {
        mModeStr = value;
    }

    double getMaxVoltageForWheel() {
        BaseAdapter adapter = getAdapter();
        if (adapter == null) {
            return 0;
        }
        return Constants.MAX_CELL_VOLTAGE * adapter.getCellsForWheel();
    }

    double getVoltageTiltbackForWheel() {
        BaseAdapter adapter = getAdapter();
        if (adapter == null) {
            return 0;
        }
        return appConfig.getCellVoltageTiltback() / 100d * adapter.getCellsForWheel();
    }

    public boolean isVoltageTiltbackUnsupported() {
        return mWheelType == WHEEL_TYPE.NINEBOT || mWheelType == WHEEL_TYPE.NINEBOT_Z;
    }

    String getChargeTime() {
        double maxVoltage = getMaxVoltageForWheel();
        double minVoltage = getVoltageTiltbackForWheel();
        double whInOneV = appConfig.getBatteryCapacity() / (maxVoltage - minVoltage);
        double needToMax = maxVoltage - getVoltageDouble();
        double needToMaxInWh = needToMax * whInOneV;
        double chargePower = maxVoltage * appConfig.getChargingPower() / 10d;
        int chargeTime = (int) (needToMaxInWh / chargePower * 60);
        return getSpeed() == 0
                ? String.format(Locale.US, "~%d min", chargeTime)
                : String.format(Locale.US, "~%d min *", chargeTime);
    }

    String getAlert() {
        String nAlert = mAlert.toString();
        mAlert = new StringBuilder();
        return nAlert;
    }

    public void setAlert(String value) {
        if (mAlert.length() != 0) {
            if (mAlert.length() > 1000) {
                mAlert = new StringBuilder("... | ");
            } else {
                mAlert.append(" | ");
            }
        }
        mAlert.append(value);
    }

    public String getSerial() {
        return mSerialNumber;
    }


    public void setSerial(String value) {
        mSerialNumber = value;

    }

    int getRideTime() {
        return mRideTime;
    }

    public double getAverageBatteryConsumption() {
        return MathsUtil.clamp(mBatteryStart - mBattery, 0, 100);
    }

    public double getDistanceFromStart() {
        if (mTotalDistance != 0) {
            return  (mTotalDistance - mStartTotalDistance);
        } else return 0;
    }

    public double getBatteryPerKm() {
        double distance = getDistanceFromStart();
        if (distance != 0) {
            return getAverageBatteryConsumption() * 1000 / distance;
        } else {
            return 0;
        }
    }

    public double getAvgVoltagePerCell() {
        var adapter = getAdapter();
        if (adapter == null) {
            return 0.0;
        }
        var cells = Math.max(1, adapter.getCellsForWheel());
        return mVoltage / (cells * 100.0);
    }

    public double getRemainingDistance() {
        double batteryByKm = getBatteryPerKm();
        if (batteryByKm != 0) {
            return mBattery / batteryByKm;
        } else {
            return 0;
        }
    }

    public double getAverageSpeedDouble() {
        if (mTotalDistance != 0 && mRideTime != 0) {
            // 3.6 = (60 sec * 60 mim) / 1000 meters.
            return getDistanceFromStart() * 3.6 / (mRideTime + mLastRideTime);
        } else return 0.0;
    }

    public double getAverageRidingSpeedDouble() {
        if (mTotalDistance != 0 && mRidingTime != 0) {
            // 3.6 = (60 sec * 60 mim) / 1000 meters.
            return getDistanceFromStart() * 3.6 / mRidingTime;
        } else return 0.0;
    }

    public String getRideTimeString() {
        int currentTime = mRideTime + mLastRideTime;
        long hours = TimeUnit.SECONDS.toHours(currentTime);
        long minutes = TimeUnit.SECONDS.toMinutes(currentTime) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(currentTime));
        long seconds = TimeUnit.SECONDS.toSeconds(currentTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(currentTime));
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    String getRidingTimeString() {
        long hours = TimeUnit.SECONDS.toHours(mRidingTime);
        long minutes = TimeUnit.SECONDS.toMinutes(mRidingTime) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(mRidingTime));
        long seconds = TimeUnit.SECONDS.toSeconds(mRidingTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(mRidingTime));
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    public double getSpeedDouble() {
        return mSpeed / 100.0;
    }

    public double getVoltageDouble() {
        return mVoltage / 100.0;
    }

    public int getVoltage() {
        return mVoltage;
    }

    public void setVoltage(int voltage) {
        mVoltage = voltage;
    }

    public void setSleepTimer(int sleepsec) {
        mSleepTimer = sleepsec;
    }

    public String getSleepTimerString() {
        long hours = TimeUnit.SECONDS.toHours(mSleepTimer);
        long minutes = TimeUnit.SECONDS.toMinutes(mSleepTimer) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(mSleepTimer));
        long seconds = TimeUnit.SECONDS.toSeconds(mSleepTimer) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(mSleepTimer));
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    double getVoltageSagDouble() {
        return mVoltageSag / 100.0;
    }

    public double getPowerDouble() {
        return mPower / 100.0;
    }

    private void setMaxPower(int power) {
        mMaxPower = Math.max(mMaxPower, power);
    }

    public void setPower(int value) {
        mPower = value;
        setMaxPower(value);
        Calculator.INSTANCE.pushPower(getPowerDouble(), getDistance());
    }

    public void setWheelAlarm(boolean value) {
        mWheelAlarm = value;
    }

    public boolean getWheelAlarm() {
        return mWheelAlarm;
    }

    public void calculatePower() {
        setPower((int) Math.round(getCurrentDouble() * mVoltage));
    }

    public double getCurrentDouble() {
        return mCurrent / 100.0;
    }

    private void setMaxCurrent(int value) {
        mMaxCurrent = Math.max(mMaxCurrent, value);
    }

    public void setCurrent(int value) {
        mCurrent = value;
        setMaxCurrent(value);
    }

    public void calculateCurrent() {
        setCurrent((int) Math.round(mCalculatedPwm * mPhaseCurrent));
    }

    public int getCurrent() {
        return mCurrent;
    }

    public double getPhaseCurrentDouble() {
        return mPhaseCurrent / 100.0;
    }

    public void setPhaseCurrent(int value) {
        mPhaseCurrent = value;
    }

    public int getPhaseCurrent() {
        return mPhaseCurrent;
    }

    public double getCalculatedPwm() {
        return mCalculatedPwm * 100.0;
    }

    public double getMaxPwm() {
        return mMaxPwm * 100.0;
    }

    public int getTopSpeed() {
        return mTopSpeed;
    }

    public double getTopSpeedDouble() {
        return mTopSpeed / 100.0;
    }

    int getDistance() {
        return (int) (mTotalDistance - mStartTotalDistance);
    }

    long getWheelDistance() {
        return mDistance;
    }

    public double getWheelDistanceDouble() {
        return mDistance / 1000.0;
    }


    public double getUserDistanceDouble() {
        if (mUserDistance == 0 && mTotalDistance != 0) {
            mUserDistance = appConfig.getUserDistance();
            if (mUserDistance == 0) {
                appConfig.setUserDistance(mTotalDistance);
                mUserDistance = mTotalDistance;
            }
        }
        return (mTotalDistance - mUserDistance) / 1000.0;
    }

    public String getMac() {
        return getBluetoothService() != null
                ? getBluetoothService().getWheelAddress()
                : "default";
    }

    public long getTimeStamp() {
        return timestamp_last;
    }

    public void resetUserDistance() {
        if (mTotalDistance != 0) {
            appConfig.setUserDistance(mTotalDistance);
            mUserDistance = mTotalDistance;
        }
    }

    public void resetMaxValues() {
        mTopSpeed = 0;
        mMaxPwm = 0;
        mMaxCurrent = 0;
        mMaxPower = 0;
    }

    public void resetExtremumValues() {
        resetMaxValues();
        mBatteryLowest = 101;
    }

    public void resetVoltageSag() {
        Timber.i("Sag WD");
        mVoltageSag = 20000;
        if (getBluetoothService() != null) {
            getBluetoothService().getApplicationContext().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_RESET));
        }
    }

    public double getDistanceDouble() {
        return (mTotalDistance - mStartTotalDistance) / 1000.0;
    }

    public double getTotalDistanceDouble() {
        return mTotalDistance / 1000.0;
    }

    public long getTotalDistance() {
        return mTotalDistance;
    }

    public SmartBms getBms1() {
        return mSmartBms1;
    }

    public SmartBms getBms2() {
        return mSmartBms2;
    }

    public void setBmsView(boolean bmsView) {
        if (mBmsView != bmsView) resetBmsData();
        mBmsView = bmsView;
    }

    public boolean getBmsView() {
        return mBmsView;
    }

    public void resetBmsData() {
        mSmartBms1.reset();
        mSmartBms2.reset();
    }

    ArrayList<String> getXAxis() {
        return xAxis;
    }

    ArrayList<Float> getCurrentAxis() {
        return currentAxis;
    }

    ArrayList<Float> getSpeedAxis() {
        return speedAxis;
    }

    void setConnected(boolean connected) {
        mConnectionState = connected;
        Timber.i("State %b", connected);
    }

    public void setWheelDistance(long distance) {
        mDistance = distance;
    }

    public void setTotalDistance(long totalDistance) {
        if (mStartTotalDistance == 0 && mTotalDistance != 0)
            mStartTotalDistance = mTotalDistance;
        mTotalDistance = totalDistance;
    }

    public void setCurrentTime(int currentTime) {
        if (mRideTime > (currentTime + TIME_BUFFER))
            mLastRideTime = mRideTime;
        mRideTime = currentTime;
    }

    public void setTopSpeed(int topSpeed) {
        if (topSpeed > mTopSpeed)
            mTopSpeed = topSpeed;
    }

    public void updateVoltageSag() {
        if ((mVoltage < mVoltageSag) && (mVoltage > 0))
            mVoltageSag = mVoltage;
    }

    public int getVoltageSag() {
        return mVoltageSag;
    }

    public void setMaxPwm(double currentPwm) {
        if ((currentPwm > mMaxPwm) && (currentPwm > 0))
            mMaxPwm = currentPwm;

    }

    public void setMaxTemp(int temp) {
        if ((temp > mMaxTemp) && (temp > 0))
            mMaxTemp = temp;

    }

    public void updatePwm() {
        mCalculatedPwm = (double) mOutput / 10000.0;
        setMaxPwm(mCalculatedPwm);
    }

    public void calculatePwm() {
        double rotationSpeed = appConfig.getRotationSpeed() / 10d;
        double rotationVoltage = appConfig.getRotationVoltage() / 10d;
        double powerFactor = appConfig.getPowerFactor() / 100d;
        mCalculatedPwm = mSpeed / (rotationSpeed / rotationVoltage * mVoltage * powerFactor);
        setMaxPwm(mCalculatedPwm);
    }

    public void setBatteryLevel(int battery) {
        if (appConfig.getCustomPercents()) {
            double maxVoltage = getMaxVoltageForWheel();
            double minVoltage = getVoltageTiltbackForWheel();
            double voltagePercentStep = (maxVoltage - minVoltage) / 100.0;
            if (voltagePercentStep != 0) {
                battery = MathsUtil.clamp((int) ((getVoltageDouble() - minVoltage) / voltagePercentStep), 0, 100);
            }
        }
        mBatteryLowest = Math.min(mBatteryLowest, battery);

        if (mBatteryStart == -1) {
            mBatteryStart = battery;
        }
        mBattery = battery;
    }

    void decodeResponse(byte[] data, Context mContext) {
        timestamp_raw = System.currentTimeMillis();//new Date(); //sdf.format(new Date());

        StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte aData : data)
            stringBuilder.append(String.format(Locale.US, "%02X", aData));
        Timber.i("Received: %s", stringBuilder);
        if (protoVer != "") {
            Timber.i("Decode, proto: %s", protoVer);
        }
        boolean new_data = getAdapter().decode(data);

        if (!new_data)
            return;
        mLastLifeData = System.currentTimeMillis();
        resetRideTime();
        updateRideTime();
        setTopSpeed(mSpeed);
        updateVoltageSag();
        setMaxTemp(mTemperature);

        Intent intent = new Intent(Constants.ACTION_WHEEL_DATA_AVAILABLE);

        if (graph_last_update_time + GRAPH_UPDATE_INTERVAL < Calendar.getInstance().getTimeInMillis()) {
            graph_last_update_time = Calendar.getInstance().getTimeInMillis();
            intent.putExtra(Constants.INTENT_EXTRA_GRAPH_UPDATE_AVAILABLE, true);
            currentAxis.add((float) getCurrentDouble());
            speedAxis.add((float) getSpeedDouble());
            xAxis.add(new SimpleDateFormat("HH:mm:ss", Locale.US).format(Calendar.getInstance().getTime()));
            if (speedAxis.size() > (3600000 / GRAPH_UPDATE_INTERVAL)) {
                speedAxis.remove(0);
                currentAxis.remove(0);
                xAxis.remove(0);
            }
        }

        timestamp_last = timestamp_raw;
        intent.putExtra("Speed", mSpeed);
        mContext.sendBroadcast(intent);
        broadcastData(mContext);

        if (!mWheelIsReady && getAdapter().isReady()) {
            mWheelIsReady = true;
            var isReadyIntent = new Intent(Constants.ACTION_WHEEL_IS_READY);
            mContext.sendBroadcast(isReadyIntent);
        }

        CheckMuteMusic();
    }

    public long getLastLifeData() {
        return mLastLifeData;
    }

    private void CheckMuteMusic() {
        if (!appConfig.getUseStopMusic())
            return;

        final double muteSpeedThreshold = 3.5;
        double speed = getSpeedDouble();
        if (speed <= muteSpeedThreshold) {
            mLowSpeedMusicTime = 0;
            MainActivity.audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        } else {
            if (mLowSpeedMusicTime == 0)
                mLowSpeedMusicTime = System.currentTimeMillis();

            if ((System.currentTimeMillis() - mLowSpeedMusicTime) >= 1500)
                MainActivity.audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
    }

    public void resetRideTime() {
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
            mRidingTime = 0;
        }
    }

    public void incrementRidingTime() {
        mRidingTime++;
    }

    /*
        Only for restore from log
     */
    public void setStartParameters(long rideStartTime, long startTotalDistance) {
        mRidingTime = 0;
        mLastRideTime = 0;
        this.rideStartTime = rideStartTime;
        this.mStartTotalDistance = startTotalDistance;
    }

    public void updateRideTime() {
        int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
        setCurrentTime(currentTime);
    }

    void full_reset() {
        if (mWheelType == WHEEL_TYPE.INMOTION) InMotionAdapter.stopTimer();
        if (mWheelType == WHEEL_TYPE.INMOTION_V2) InmotionAdapterV2.stopTimer();
        if (mWheelType == WHEEL_TYPE.NINEBOT_Z) NinebotZAdapter.stopTimer();
        if (mWheelType == WHEEL_TYPE.NINEBOT) NinebotAdapter.stopTimer();
        mWheelType = WHEEL_TYPE.Unknown;
        //mWheelType = WHEEL_TYPE.GOTWAY; //test
        xAxis.clear();
        speedAxis.clear();
        currentAxis.clear();
        reset();
        resetBmsData();
    }

    void reset() {
        mLowSpeedMusicTime = 0;
        mSpeed = 0;
        mTorque = 0;
        mMotorPower = 0;
        mCpuTemp = 0;
        mImuTemp = 0;
        mSpeedLimit = 0;
        mCurrentLimit = 0;
        mTotalDistance = 0;
        mCurrent = 0;
        mPower = 0;
        mTemperature = 0;
        mTemperature2 = 0;
        mCpuLoad = 0;
        mOutput = 0;
        mAngle = 0;
        mRoll = 0;
        mBattery = 0;
        mBatteryLowest = 101;
        mBatteryStart = -1;
        //mAverageBatteryCount = 0;
        mCalculatedPwm = 0.0;
        mMaxPwm = 0.0;
        mMaxTemp = 0;
        mVoltage = 0;
        mVoltageSag = 20000;
        mRideTime = 0;
        mRidingTime = 0;
        mSleepTimer = 0;
        mTopSpeed = 0;
        mFanStatus = 0;
        mChargingStatus = 0;
        mDistance = 0;
        mUserDistance = 0;
        mName = "";
        mModel = "";
        mModeStr = "";
        mVersion = "";
        mError = "";
        mSerialNumber = "";
        mBtName = "";
        rideStartTime = 0;
        mStartTotalDistance = 0;
        protoVer = "";
        mWheelAlarm = false;
        mWheelIsReady = false;
    }

    boolean detectWheel(String deviceAddress, Context mContext, int servicesResId) {
        appConfig.setLastMac(deviceAddress);
        String advData = appConfig.getAdvDataForWheel();
        String adapterName = "";
        protoVer = "";
        if (StringUtil.inArray(advData, new String[]{"4e421300000000ec", "4e421302000000ea",})) {
            protoVer = "S2";
        } else if (StringUtil.inArray(advData, new String[]{"4e421400000000eb", "4e422000000000df", "4e422200000000dd", "4e4230cf"}) || (advData.startsWith("5600"))) {
            protoVer = "Mini";
        }
        Timber.i("ProtoVer %s, adv: %s", protoVer, advData );
        boolean detected_wheel = false;
        String text = StringUtil.getRawTextResource(mContext, servicesResId);
        if (mBluetoothService == null) {
            Timber.wtf("[error] BluetoothService is null. The wheel could not be detected.");
            return false;
        }
        var wheelServices = mBluetoothService.getWheelServices();
        if (wheelServices == null) {
            return false;
        }
        try {
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length() && !detected_wheel; i++) {
                JSONObject services = arr.getJSONObject(i);
                if (services.length() - 1 != wheelServices.size()) {
                    Timber.i("Services len not corresponds, go to the next");
                    continue;
                }
                adapterName = services.getString("adapter");
                Timber.i("Searching for %s", adapterName);
                Iterator<String> iterator = services.keys();
                // skip adapter key
                iterator.next();
                boolean go_next_adapter = false;
                while (iterator.hasNext()) {
                    String keyName = iterator.next();
                    Timber.i("Key name %s", keyName);
                    UUID s_uuid = UUID.fromString(keyName);
                    BluetoothGattService service = mBluetoothService.getWheelService(s_uuid);
                    if (service == null) {
                        Timber.i("No such service");
                        go_next_adapter = true;
                        break;
                    }

                    JSONArray service_uuid = services.getJSONArray(keyName);
                    if (service_uuid.length() != service.getCharacteristics().size()) {
                        Timber.i("Characteristics len not corresponds, go to the next");
                        go_next_adapter = true;
                        break;
                    }
                    for (int j = 0; j < service_uuid.length(); j++) {
                        UUID c_uuid = UUID.fromString(service_uuid.getString(j));
                        Timber.i("UUid %s", service_uuid.getString(j));
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(c_uuid);
                        if (characteristic == null) {
                            Timber.i("UUid not found");
                            go_next_adapter = true;
                            break;
                        } else {Timber.i("UUid found");}
                    }
                    if (go_next_adapter) {
                        break;
                    }
                }
                if (!go_next_adapter) {
                    Timber.i("Wheel Detected as %s", adapterName);
                    detected_wheel = true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (detected_wheel) {
            Timber.i("Protocol recognized as %s", adapterName);
            if (WHEEL_TYPE.GOTWAY.toString().equalsIgnoreCase(adapterName) && (mBtName.equals("RW") || mName.startsWith("ROCKW"))) {
                Timber.i("It seems to be RochWheel, force to Kingsong proto");
                adapterName = WHEEL_TYPE.KINGSONG.toString();
            }
            if (WHEEL_TYPE.KINGSONG.toString().equalsIgnoreCase(adapterName)) {
                setWheelType(WHEEL_TYPE.KINGSONG);
                var targetService = mBluetoothService.getWheelService(Constants.KINGSONG_SERVICE_UUID);
                var notifyCharacteristic = targetService.getCharacteristic(Constants.KINGSONG_READ_CHARACTER_UUID);
                mBluetoothService.setCharacteristicNotification(notifyCharacteristic, true);
                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(Constants.KINGSONG_DESCRIPTER_UUID);
                mBluetoothService.writeWheelDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                return true;
            } else if (WHEEL_TYPE.GOTWAY.toString().equalsIgnoreCase(adapterName)) {
                setWheelType(WHEEL_TYPE.GOTWAY_VIRTUAL);
                var targetService = mBluetoothService.getWheelService(Constants.GOTWAY_SERVICE_UUID);
                var notifyCharacteristic = targetService.getCharacteristic(Constants.GOTWAY_READ_CHARACTER_UUID);
                mBluetoothService.setCharacteristicNotification(notifyCharacteristic, true);
                // Let the user know it's working by making the wheel beep
                if (appConfig.getConnectBeep())
                    mBluetoothService.writeWheelCharacteristic("b".getBytes());

                return true;
            } else if (WHEEL_TYPE.INMOTION.toString().equalsIgnoreCase(adapterName)) {
                setWheelType(WHEEL_TYPE.INMOTION);
                var targetService = mBluetoothService.getWheelService(Constants.INMOTION_SERVICE_UUID);
                var notifyCharacteristic = targetService.getCharacteristic(Constants.INMOTION_READ_CHARACTER_UUID);
                mBluetoothService.setCharacteristicNotification(notifyCharacteristic, true);
                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(Constants.INMOTION_DESCRIPTER_UUID);
                mBluetoothService.writeWheelDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                String inmotionPassword = appConfig.getPasswordForWheel();
                if (inmotionPassword.length() > 0) {
                    InMotionAdapter.getInstance().startKeepAliveTimer(inmotionPassword);
                    return true;
                }
                return false;

            } else if (WHEEL_TYPE.INMOTION_V2.toString().equalsIgnoreCase(adapterName)) {
                Timber.i("Trying to start Inmotion V2");
                setWheelType(WHEEL_TYPE.INMOTION_V2);
                var targetService = mBluetoothService.getWheelService(Constants.INMOTION_V2_SERVICE_UUID);
                Timber.i("service UUID");
                var notifyCharacteristic = targetService.getCharacteristic(Constants.INMOTION_V2_READ_CHARACTER_UUID);
                Timber.i("read UUID");
                if (notifyCharacteristic == null) {
                    Timber.i("it seems that RX UUID doesn't exist");
                }
                mBluetoothService.setCharacteristicNotification(notifyCharacteristic, true);
                Timber.i("notify UUID");
                var descriptor = notifyCharacteristic.getDescriptor(Constants.INMOTION_V2_DESCRIPTER_UUID);
                Timber.i("descr UUID");
                if (descriptor == null) {
                    Timber.i("it seems that descr UUID doesn't exist");
                } else {
                    Timber.i("enable notify UUID");
                    mBluetoothService.writeWheelDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    Timber.i("write notify");
                }
                InmotionAdapterV2.getInstance().startKeepAliveTimer();
                Timber.i("starting Inmotion V2 adapter");
                return true;

            } else if (WHEEL_TYPE.NINEBOT_Z.toString().equalsIgnoreCase(adapterName)) {
                Timber.i("Trying to start Ninebot Z");
                if (protoVer.compareTo("") == 0) {
                    Timber.i("really Z");
                    setWheelType(WHEEL_TYPE.NINEBOT_Z);
                } else {
                    Timber.i("no, switch to NB");
                    setWheelType(WHEEL_TYPE.NINEBOT);
                }
                var targetService = mBluetoothService.getWheelService(Constants.NINEBOT_Z_SERVICE_UUID);
                Timber.i("service UUID");
                var notifyCharacteristic = targetService.getCharacteristic(Constants.NINEBOT_Z_READ_CHARACTER_UUID);
                Timber.i("read UUID");
                if (notifyCharacteristic == null) {
                    Timber.i("it seems that RX UUID doesn't exist");
                }
                mBluetoothService.setCharacteristicNotification(notifyCharacteristic, true);
                Timber.i("notify UUID");
                var descriptor = notifyCharacteristic.getDescriptor(Constants.NINEBOT_Z_DESCRIPTER_UUID);
                Timber.i("descr UUID");
                if (descriptor == null) {
                    Timber.i("it seems that descr UUID doesn't exist");
                } else {
                    Timber.i("enable notify UUID");
                    mBluetoothService.writeWheelDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                }
                Timber.i("write notify");
                if (protoVer.compareTo("S2") == 0 || protoVer.compareTo("Mini") == 0) {
                    NinebotAdapter.getInstance().startKeepAliveTimer(protoVer);
                    Timber.i("starting ninebot adapter, proto: %s", protoVer);
                } else {
                    NinebotZAdapter.getInstance().startKeepAliveTimer();
                    Timber.i("starting ninebot Z adapter");
                }

                return true;
            } else if (WHEEL_TYPE.NINEBOT.toString().equalsIgnoreCase(adapterName)) {
                Timber.i("Trying to start Ninebot");
                setWheelType(WHEEL_TYPE.NINEBOT);
                var targetService = mBluetoothService.getWheelService(Constants.NINEBOT_SERVICE_UUID);
                Timber.i("service UUID");
                var notifyCharacteristic = targetService.getCharacteristic(Constants.NINEBOT_READ_CHARACTER_UUID);
                Timber.i("read UUID");
                if (notifyCharacteristic == null) {
                    Timber.i("it seems that RX UUID doesn't exist");
                }
                mBluetoothService.setCharacteristicNotification(notifyCharacteristic, true);
                Timber.i("notify UUID");
                var descriptor = notifyCharacteristic.getDescriptor(Constants.NINEBOT_DESCRIPTER_UUID);
                Timber.i("descr UUID");
                if (descriptor == null) {
                    Timber.i("it seems that descr UUID doesn't exist");
                } else {
                    Timber.i("enable notify UUID");
                    mBluetoothService.writeWheelDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    Timber.i("write notify");
                }
                NinebotAdapter.getInstance().startKeepAliveTimer(protoVer);
                Timber.i("starting ninebot adapter");
                return true;
            }
        } else {
            appConfig.setLastMac("");
            Timber.i("Protocol recognized as Unknown");
            for (BluetoothGattService service : wheelServices) {
                Timber.i("Service: %s", service.getUuid().toString());
                for (BluetoothGattCharacteristic characteristics : service.getCharacteristics()) {
                    Timber.i("Characteristics: %s", characteristics.getUuid().toString());
                }
            }
        }
        return false;
    }
}
