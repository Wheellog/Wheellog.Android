package com.cooper.wheellog;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Vibrator;

import com.cooper.wheellog.utils.*;
import com.cooper.wheellog.utils.Constants.ALARM_TYPE;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class WheelData {
    private static final int TIME_BUFFER = 10;
    private static WheelData mInstance;
    private Timer ridingTimerControl;
    private BluetoothLeService mBluetoothLeService;

    private long graph_last_update_time;
    private static final int GRAPH_UPDATE_INTERVAL = 1000; // milliseconds
    private static final int RIDING_SPEED = 200; // 2km/h
    private final ArrayList<String> xAxis = new ArrayList<>();
    private final ArrayList<Float> currentAxis = new ArrayList<>();
    private final ArrayList<Float> speedAxis = new ArrayList<>();

    // BMS
    private final NinebotBms mNinebotBms1 = new NinebotBms();
    private final NinebotBms mNinebotBms2 = new NinebotBms();

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
    private Integer mPower = null;
    private int mPhaseCurrent;
    private int mTemperature;
    private int mMaxTemp;
    private int mTemperature2;
    private int mCpuLoad;
    private int mOutput;
    private double mAngle;
    private double mRoll;

    private int mBattery;
    private double mAverageBattery;
    //    private double mAverageBatteryCount;
    private int mVoltage;
    private long mDistance;
    private long mUserDistance;
    private int mRideTime;
    private int mRidingTime;
    private int mLastRideTime;
    private int mTopSpeed;
    private int mVoltageSag;
    private int mFanStatus;
    private int mChargingStatus;
    private boolean mConnectionState = false;
    private String mName = "Unknown";
    private String mModel = "Unknown";
    private String mModeStr = "Unknown";
    private String mBtName = "";

    private StringBuilder mAlert = new StringBuilder();

    //    private int mVersion; # sorry King, but INT not good for Inmo
    private String mVersion = "";
    private String mSerialNumber = "Unknown";
    private WHEEL_TYPE mWheelType = WHEEL_TYPE.Unknown;
    private long rideStartTime;
    private long mStartTotalDistance;

    private long mLastPlayWarningSpeedTime = System.currentTimeMillis();
    private double mCalculatedPwm = 0.0;
    private double mMaxPwm = 0.0;
    private long mLowSpeedMusicTime = 0;

    private boolean mSpeedAlarmExecuting = false;
    private boolean mCurrentAlarmExecuting = false;
    private boolean mTemperatureAlarmExecuting = false;
    private boolean mBmsView = false;
    private boolean mDataForLog = true;
    private String protoVer = "";

    private final int duration = 1; // duration of sound
    private final int sampleRate = 44100;//22050; // Hz (maximum frequency is 7902.13Hz (B8))
    private final int numSamples = duration * sampleRate;
    //    private double samples[] = new double[numSamples];
    private final short[] buffer = new short[numSamples];
    private final int sfreq = 440;

    private long timestamp_raw;
    private long timestamp_last;
    private static AudioTrack audioTrack = null;

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
                // Костыль форева
                if (protoVer.compareTo("S2") == 0 || protoVer.compareTo("Mini") == 0) {
                    return NinebotAdapter.getInstance();
                }
                return NinebotZAdapter.getInstance();
            case INMOTION:
                return InMotionAdapter.getInstance();
            case INMOTION_V2:
                return InmotionAdapterV2.getInstance();
            default:
                return null;
        }
    }

    public BluetoothLeService getBluetoothLeService() {
        return mBluetoothLeService;
    }

    public boolean bluetoothCmd(byte[] cmd) {
        if (mBluetoothLeService == null) {
            return false;
        }
        return mBluetoothLeService.writeBluetoothGattCharacteristic(cmd);
    }

    public void setBluetoothLeService(BluetoothLeService value) {
        mBluetoothLeService = value;
    }

    void playBeep(ALARM_TYPE type) {
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, buffer.length,
                AudioTrack.MODE_STATIC);
        if (type.getValue() < 4) {
            audioTrack.write(buffer, sampleRate / 20, ((type.getValue()) * sampleRate) / 20); //50, 100, 150 ms depends on number of speed alarm

        } else if (type == ALARM_TYPE.CURRENT) {
            audioTrack.write(buffer, sampleRate * 3 / 10, (2 * sampleRate) / 20); //100 ms for current

        } else {
            audioTrack.write(buffer, sampleRate * 3 / 10, (6 * sampleRate) / 10); //300 ms temperature


        }

        //Timber.i("Beep: %d",(type.getValue()-1)*10*sampleRate / 50);
        audioTrack.play();

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
        mInstance.prepareTone(mInstance.sfreq);
        mInstance.startRidingTimerControl();
    }

    private void prepareTone(int freq) {

        for (int i = 0; i < numSamples; ++i) {
            double originalWave = Math.sin(2 * Math.PI * freq * i / sampleRate);
            double harmonic1 = 0.5 * Math.sin(2 * Math.PI * 2 * freq * i / sampleRate);
            double harmonic2 = 0.25 * Math.sin(2 * Math.PI * 4 * freq * i / sampleRate);
            double secondWave = Math.sin(2 * Math.PI * freq * 1.34F * i / sampleRate);
            double thirdWave = Math.sin(2 * Math.PI * freq * 2.0F * i / sampleRate);
            double fourthWave = Math.sin(2 * Math.PI * freq * 2.68F * i / sampleRate);
            if (i <= (numSamples * 3) / 10) {
                buffer[i] = (short) ((originalWave + harmonic1 + harmonic2) * (Short.MAX_VALUE)); //+ harmonic1 + harmonic2
            } else if (i < (numSamples * 3) / 5) {
                buffer[i] = (short) ((originalWave + secondWave) * (Short.MAX_VALUE));
            } else {
                buffer[i] = (short) ((thirdWave + fourthWave) * (Short.MAX_VALUE));
            }

        }

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

    public static WheelData getInstance() {
        return mInstance;
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
        mBtName = btName;
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

    public int getCpuLoad() {
        return mCpuLoad;
    }

    public void setCpuLoad(int value) {
        mCpuLoad = value;
    }

    public int getOutput() {
        return mOutput;
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
        return mVersion == "" ? "Unknown" : mVersion;
    }

    public void setVersion(String value) {
        mVersion = value;
    }

    public void setWheelType(WHEEL_TYPE wheelType) {
        boolean isChanged = wheelType != mWheelType;
        mWheelType = wheelType;
        if (isChanged) {
            Context mContext = getBluetoothLeService().getApplicationContext();
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
        mModel = model;
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
        return Constants.MAX_CELL_VOLTAGE * adapter.getCellSForWheel();
    }

    double getVoltageTiltbackForWheel() {
        BaseAdapter adapter = getAdapter();
        if (adapter == null) {
            return 0;
        }
        return WheelLog.AppConfig.getCellVoltageTiltback() / 100d * adapter.getCellSForWheel();
    }

    public boolean isVoltageTiltbackUnsupported() {
        return mWheelType == WHEEL_TYPE.NINEBOT || mWheelType == WHEEL_TYPE.NINEBOT_Z;
    }

    String getChargeTime() {
        double maxVoltage = getMaxVoltageForWheel();
        double minVoltage = getVoltageTiltbackForWheel();
        double whInOneV = WheelLog.AppConfig.getBatteryCapacity() / (maxVoltage - minVoltage);
        double needToMax = maxVoltage - getVoltageDouble();
        double needToMaxInWh = needToMax * whInOneV;
        double chargePower = maxVoltage * WheelLog.AppConfig.getChargingPower() / 10d;
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

    public double getAverageSpeedDouble() {
        if (mTotalDistance != 0 && mRideTime != 0) {
            return (((mTotalDistance - mStartTotalDistance) * 3.6) / (mRideTime + mLastRideTime));
        } else return 0.0;
    }

    public double getAverageRidingSpeedDouble() {
        if (mTotalDistance != 0 && mRidingTime != 0) {
            return (((mTotalDistance - mStartTotalDistance) * 3.6) / mRidingTime);
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

    double getVoltageSagDouble() {
        return mVoltageSag / 100.0;
    }

    public double getPowerDouble() {
        return (mPower != null ? mPower : (mCurrent * mVoltage) / 10000.0);
    }

    public void setPower(int power) {
        mPower = power;
    }

    public double getCurrentDouble() {
        return mCurrent / 100.0;
    }

    public void setCurrent(int value) {
        mCurrent = value;
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

    int getAlarm() {
        int alarm = 0;
        if (mSpeedAlarmExecuting) {
            alarm = alarm | 0x01;
        }
        if (mTemperatureAlarmExecuting) {
            alarm = alarm | 0x04;
        }
        if (mCurrentAlarmExecuting) {
            alarm = alarm | 0x02;
        }
        return alarm;
    }


    long getWheelDistance() {
        return mDistance;
    }

    public double getWheelDistanceDouble() {
        return mDistance / 1000.0;
    }


    public double getUserDistanceDouble() {
        if (mUserDistance == 0 && mTotalDistance != 0) {
            mUserDistance = WheelLog.AppConfig.getUserDistance();
            if (mUserDistance == 0) {
                WheelLog.AppConfig.setUserDistance(mTotalDistance);
                mUserDistance = mTotalDistance;
            }
        }
        return (mTotalDistance - mUserDistance) / 1000.0;
    }

    public String getMac() {
        return getBluetoothLeService() != null
                ? getBluetoothLeService().getBluetoothDeviceAddress()
                : "default";
    }

    public long getTimeStamp() {
        return timestamp_last;
    }

    public void resetUserDistance() {
        if (mTotalDistance != 0) {
            WheelLog.AppConfig.setUserDistance(mTotalDistance);
            mUserDistance = mTotalDistance;
        }
    }

    public void resetTopSpeed() {
        mTopSpeed = 0;
        mMaxPwm = 0;
    }

    public void resetVoltageSag() {
        Timber.i("Sag WD");
        mVoltageSag = 20000;
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

    public NinebotBms getBms1() {
        return mNinebotBms1;
    }

    public NinebotBms getBms2() {
        return mNinebotBms2;
    }

    public void setBmsView(boolean bmsView) {
        if (mBmsView != bmsView) resetBmsData();
        mBmsView = bmsView;
    }

    public boolean getBmsView() {
        return mBmsView;
    }

    public void resetBmsData() {
        mNinebotBms1.reset();
        mNinebotBms2.reset();
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
            mLastRideTime += mRideTime;
        mRideTime = currentTime;
    }

    public void setTopSpeed(int topSpeed) {
        if (topSpeed > mTopSpeed)
            mTopSpeed = topSpeed;
    }

    public void setVoltageSag(int voltSag) {
        if ((voltSag < mVoltageSag) && (voltSag > 0))
            mVoltageSag = voltSag;
    }

    private void setMaxPwm(double currentPwm) {
        if ((currentPwm > mMaxPwm) && (currentPwm > 0))
            mMaxPwm = currentPwm;

    }

    private void setMaxTemp(int temp) {
        if ((temp > mMaxTemp) && (temp > 0))
            mMaxTemp = temp;

    }

    public void setBatteryPercent(int battery) {
        if (WheelLog.AppConfig.getFixedPercents()) {
            double maxVoltage = getMaxVoltageForWheel();
            double minVoltage = getVoltageTiltbackForWheel();
            double voltagePercentStep = (maxVoltage - minVoltage) / 100.0;
            if (voltagePercentStep != 0) {
                battery = (int) ((getVoltageDouble() - minVoltage) / voltagePercentStep);
            }
        }

        mBattery = battery;

//        mAverageBatteryCount = mAverageBatteryCount < MAX_BATTERY_AVERAGE_COUNT ?
//                mAverageBatteryCount + 1 : MAX_BATTERY_AVERAGE_COUNT;
//
//        mAverageBattery += (battery - mAverageBattery) / mAverageBatteryCount;
        mAverageBattery = battery;
    }

    private void startSpeedAlarmCount() {
        mSpeedAlarmExecuting = true;
        TimerTask stopSpeedAlarmExecuring = new TimerTask() {
            @Override
            public void run() {
                mSpeedAlarmExecuting = false;
                Timber.i("Stop Speed <<<<<<<<<");
            }
        };
        Timer timerCurrent = new Timer();
        timerCurrent.schedule(stopSpeedAlarmExecuring, 170);

    }

    private void startTempAlarmCount() {
        mTemperatureAlarmExecuting = true;
        TimerTask stopTempAlarmExecuting = new TimerTask() {
            @Override
            public void run() {
                mTemperatureAlarmExecuting = false;
                Timber.i("Stop Temp <<<<<<<<<");
            }
        };
        Timer timerTemp = new Timer();
        timerTemp.schedule(stopTempAlarmExecuting, 570);
    }

    private void startCurrentAlarmCount() {
        mCurrentAlarmExecuting = true;
        TimerTask stopCurrentAlarmExecuring = new TimerTask() {
            @Override
            public void run() {
                mCurrentAlarmExecuting = false;
                Timber.i("Stop Curr <<<<<<<<<");
            }

        };
        Timer timerCurrent = new Timer();
        timerCurrent.schedule(stopCurrentAlarmExecuring, 170);
    }

    private void checkAlarmStatus(Context mContext) {
        // SPEED ALARM
        if (!mSpeedAlarmExecuting) {
            if (WheelLog.AppConfig.getAlteredAlarms()) {
                if (mCalculatedPwm > WheelLog.AppConfig.getAlarmFactor3() / 100d) {
                    startSpeedAlarmCount();
                    raiseAlarm(ALARM_TYPE.SPEED3, mContext);
                } else if (mCalculatedPwm > WheelLog.AppConfig.getAlarmFactor2() / 100d) {
                    startSpeedAlarmCount();
                    raiseAlarm(ALARM_TYPE.SPEED2, mContext);
                } else if (mCalculatedPwm > WheelLog.AppConfig.getAlarmFactor1() / 100d) {
                    startSpeedAlarmCount();
                    raiseAlarm(ALARM_TYPE.SPEED1, mContext);
                } else {
                    double warningPwm = WheelLog.AppConfig.getWarningPwm() / 100d;
                    int warningSpeedPeriod = WheelLog.AppConfig.getWarningSpeedPeriod() * 1000;
                    if (warningPwm != 0 && warningSpeedPeriod != 0 && mCalculatedPwm >= warningPwm && (System.currentTimeMillis() - mLastPlayWarningSpeedTime) > warningSpeedPeriod) {
                        mLastPlayWarningSpeedTime = System.currentTimeMillis();
                        SomeUtil.playSound(mContext, R.raw.warning_pwm);
                    } else {
                        int warningSpeed = WheelLog.AppConfig.getWarningSpeed();
                        if (warningSpeed != 0 && warningSpeedPeriod != 0 && getSpeedDouble() >= warningSpeed && (System.currentTimeMillis() - mLastPlayWarningSpeedTime) > warningSpeedPeriod) {
                            mLastPlayWarningSpeedTime = System.currentTimeMillis();
                            SomeUtil.playSound(mContext, R.raw.sound_warning_speed);
                        }
                    }
                }
            } else {
                int alarm1Speed = WheelLog.AppConfig.getAlarm1Speed();
                int alarm1Battery = WheelLog.AppConfig.getAlarm1Battery();
                if (alarm1Speed > 0 && alarm1Battery > 0 && mAverageBattery <= alarm1Battery && getSpeedDouble() >= alarm1Speed) {
                    startSpeedAlarmCount();
                    raiseAlarm(ALARM_TYPE.SPEED1, mContext);
                } else {
                    int alarm2Speed = WheelLog.AppConfig.getAlarm2Speed();
                    int alarm2Battery = WheelLog.AppConfig.getAlarm2Battery();
                    if (alarm2Speed > 0 && alarm2Battery > 0 && mAverageBattery <= alarm2Battery && getSpeedDouble() >= alarm2Speed) {
                        startSpeedAlarmCount();
                        raiseAlarm(ALARM_TYPE.SPEED2, mContext);
                    } else {
                        int alarm3Speed = WheelLog.AppConfig.getAlarm3Speed();
                        int alarm3Battery = WheelLog.AppConfig.getAlarm3Battery();
                        if (alarm3Speed > 0 && alarm3Battery > 0 && mAverageBattery <= alarm3Battery && getSpeedDouble() >= alarm3Speed) {
                            startSpeedAlarmCount();
                            raiseAlarm(ALARM_TYPE.SPEED3, mContext);
                        }
                    }
                }
            }
        }

        int alarmCurrent = WheelLog.AppConfig.getAlarmCurrent() * 100;
        if (alarmCurrent > 0 && mCurrent >= alarmCurrent && !mCurrentAlarmExecuting) {
            startCurrentAlarmCount();
            raiseAlarm(ALARM_TYPE.CURRENT, mContext);
        }

        int alarmTemperature = WheelLog.AppConfig.getAlarmTemperature() * 100;
        if (alarmTemperature > 0 && mTemperature >= alarmTemperature && !mTemperatureAlarmExecuting) {
            startTempAlarmCount();
            raiseAlarm(ALARM_TYPE.TEMPERATURE, mContext);
        }
    }

    private void raiseAlarm(ALARM_TYPE alarmType, Context mContext) {
        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0};
        Intent intent = new Intent(Constants.ACTION_ALARM_TRIGGERED);
        intent.putExtra(Constants.INTENT_EXTRA_ALARM_TYPE, alarmType);

        switch (alarmType) {
            case SPEED1:
            case SPEED2:
            case SPEED3:
                pattern = new long[]{0, 100, 100};
                break;

            case CURRENT:
                pattern = new long[]{0, 50, 50, 50, 50};
//                mCurrentAlarmExecuted = true;
                break;
            case TEMPERATURE:
                pattern = new long[]{0, 500, 500};
//                mCurrentAlarmExecuted = true;
                break;
        }
        mContext.sendBroadcast(intent);
        if (v.hasVibrator() && !WheelLog.AppConfig.getDisablePhoneVibrate())
            v.vibrate(pattern, -1);
        if (!WheelLog.AppConfig.getDisablePhoneBeep()) {
            playBeep(alarmType);
        }
    }

    void decodeResponse(byte[] data, Context mContext) {
        mDataForLog = true;
        timestamp_raw = System.currentTimeMillis();//new Date(); //sdf.format(new Date());

        StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte aData : data)
            stringBuilder.append(String.format(Locale.US, "%02X", aData));
        Timber.i("Received: " + stringBuilder.toString());
//        FileUtil.writeLine("bluetoothOutput.txt", stringBuilder.toString());
        Timber.i("Decode, proto: %s", protoVer);
        boolean new_data = getAdapter().setContext(mContext).decode(data);

        if (!new_data)
            return;
        resetRideTime();
        updateRideTime();
        setTopSpeed(mSpeed);
        setVoltageSag(mVoltage);
        setMaxTemp(mTemperature);
        if (mWheelType == WHEEL_TYPE.KINGSONG) {
            mCalculatedPwm = (double) mOutput / 100.0;
        } else {
            double rotationSpeed = WheelLog.AppConfig.getRotationSpeed() / 10d;
            double rotationVoltage = WheelLog.AppConfig.getRotationVoltage() / 10d;
            double powerFactor = WheelLog.AppConfig.getPowerFactor() / 100d;
            mCalculatedPwm = mSpeed / (rotationSpeed / rotationVoltage * mVoltage * powerFactor);
        }
        setMaxPwm(mCalculatedPwm);
        if (mWheelType == WHEEL_TYPE.GOTWAY || mWheelType == WHEEL_TYPE.VETERAN) {
            mCurrent = (int) Math.round(mCalculatedPwm * mPhaseCurrent);
        }

        Intent intent = new Intent(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        if (mDataForLog) {
            intent.putExtra(Constants.INTENT_EXTRA_DATA_TO_LOGS, true);
        }

        if (graph_last_update_time + GRAPH_UPDATE_INTERVAL < Calendar.getInstance().getTimeInMillis()) {
            graph_last_update_time = Calendar.getInstance().getTimeInMillis();
            intent.putExtra(Constants.INTENT_EXTRA_GRAPH_UPDATE_AVILABLE, true);
            currentAxis.add((float) getCurrentDouble());
            speedAxis.add((float) getSpeedDouble());
            xAxis.add(new SimpleDateFormat("HH:mm:ss", Locale.US).format(Calendar.getInstance().getTime()));
            if (speedAxis.size() > (3600000 / GRAPH_UPDATE_INTERVAL)) {
                speedAxis.remove(0);
                currentAxis.remove(0);
                xAxis.remove(0);
            }
        }

        if (WheelLog.AppConfig.getAlarmsEnabled())
            checkAlarmStatus(mContext);

        timestamp_last = timestamp_raw;
        mContext.sendBroadcast(intent);

        CheckMuteMusic();
    }

    private void CheckMuteMusic() {
        if (!WheelLog.AppConfig.getUseStopMusic())
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

    public void updateRideTime() {
        int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
        setCurrentTime(currentTime);
    }

    public void setDataForLog(boolean value) {
        mDataForLog = value;
    } // пока работает, но нужно убрать

    void full_reset() {
        if (mWheelType == WHEEL_TYPE.INMOTION) InMotionAdapter.stopTimer();
        if (mWheelType == WHEEL_TYPE.INMOTION_V2) InmotionAdapterV2.stopTimer();
        if (mWheelType == WHEEL_TYPE.NINEBOT_Z) {
            if (protoVer.compareTo("S2") == 0) {
                Timber.i("Ninebot S2 stop!");
                NinebotAdapter.stopTimer();
            } else if (protoVer.compareTo("Mini") == 0) {
                Timber.i("Ninebot Mini stop!");
                NinebotAdapter.stopTimer();
            } else {
                Timber.i("Ninebot Z stop!");
                NinebotZAdapter.stopTimer();
            }
        }
        if (mWheelType == WHEEL_TYPE.NINEBOT) NinebotAdapter.stopTimer();
        mWheelType = WHEEL_TYPE.Unknown;
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
        mPower = null;
        mTemperature = 0;
        mTemperature2 = 0;
        mCpuLoad = 0;
        mOutput = 0;
        mAngle = 0;
        mRoll = 0;
        mBattery = 0;
        //mAverageBatteryCount = 0;
        mCalculatedPwm = 0.0;
        mMaxPwm = 0.0;
        mMaxTemp = 0;
        mAverageBattery = 0;
        mVoltage = 0;
        mVoltageSag = 20000;
        mRideTime = 0;
        mRidingTime = 0;
        mTopSpeed = 0;
        mFanStatus = 0;
        mChargingStatus = 0;
        mDistance = 0;
        mUserDistance = 0;
        mName = "";
        mModel = "";
        mModeStr = "";
        mVersion = "";
        mSerialNumber = "";
        mBtName = "";
        rideStartTime = 0;
        mStartTotalDistance = 0;
        protoVer = "";

    }

    boolean detectWheel(String deviceAddress) {
        Context mContext = getBluetoothLeService().getApplicationContext();
        WheelLog.AppConfig.setLastMac(deviceAddress);
        String advData = WheelLog.AppConfig.getAdvDataForWheel();
        String adapterName = "";
        protoVer = "";
        if (StringUtil.inArray(advData, new String[]{"4e421300000000ec", "4e421302000000ea",})) {
            protoVer = "S2";
        } else if (StringUtil.inArray(advData, new String[]{"4e421400000000eb", "4e422000000000df", "4e422200000000dd", "4e4230cf", "5600"})) {
            protoVer = "Mini";
        }

        boolean detected_wheel = false;
        String text = StringUtil.Companion.getRawTextResource(mContext, R.raw.bluetooth_services);
        try {
            JSONArray arr = new JSONArray(text);
            adaptersLoop:
            for (int i = 0; i < arr.length(); i++) {
                JSONObject services = arr.getJSONObject(i);
                if (services.length() - 1 != mBluetoothLeService.getSupportedGattServices().size()) {
                    continue;
                }
                adapterName = services.getString("adapter");
                Iterator<String> iterator = services.keys();
                // skip adapter key
                iterator.next();
                adapterServicesLoop:
                while (iterator.hasNext()) {
                    String keyName = iterator.next();
                    UUID s_uuid = UUID.fromString(keyName);
                    BluetoothGattService service = mBluetoothLeService.getGattService(s_uuid);
                    if (service == null) {
                        break;
                    }
                    JSONArray service_uuid = services.getJSONArray(keyName);
                    for (int j = 0; j < service_uuid.length(); j++) {
                        UUID c_uuid = UUID.fromString(service_uuid.getString(j));
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(c_uuid);
                        if (characteristic == null) {
                            break adapterServicesLoop;
                        }
                    }
                    detected_wheel = true;
                    break adaptersLoop;
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
                BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID));
                BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.KINGSONG_READ_CHARACTER_UUID));
                mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.KINGSONG_DESCRIPTER_UUID));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);

                return true;
            } else if (WHEEL_TYPE.GOTWAY.toString().equalsIgnoreCase(adapterName)) {
                setWheelType(WHEEL_TYPE.GOTWAY_VIRTUAL);
                BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.GOTWAY_SERVICE_UUID));
                BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.GOTWAY_READ_CHARACTER_UUID));
                mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                // Let the user know it's working by making the wheel beep
                if (WheelLog.AppConfig.getConnectBeep())
                    mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());

                return true;
            } else if (WHEEL_TYPE.INMOTION.toString().equalsIgnoreCase(adapterName)) {
                setWheelType(WHEEL_TYPE.INMOTION);
                BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.INMOTION_SERVICE_UUID));
                BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.INMOTION_READ_CHARACTER_UUID));
                mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.INMOTION_DESCRIPTER_UUID));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
                String inmotionPassword = WheelLog.AppConfig.getPasswordForWheel();
                if (inmotionPassword.length() > 0) {
                    InMotionAdapter.getInstance().startKeepAliveTimer(inmotionPassword);
                    return true;
                }
                return false;

            } else if (WHEEL_TYPE.INMOTION_V2.toString().equalsIgnoreCase(adapterName)) {
                Timber.i("Trying to start Inmotion V2");
                setWheelType(WHEEL_TYPE.INMOTION_V2);
                BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.INMOTION_V2_SERVICE_UUID));
                Timber.i("service UUID");
                BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.INMOTION_V2_READ_CHARACTER_UUID));
                Timber.i("read UUID");
                if (notifyCharacteristic == null) {
                    Timber.i("it seems that RX UUID doesn't exist");
                }
                mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                Timber.i("notify UUID");
                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.INMOTION_V2_DESCRIPTER_UUID));
                Timber.i("descr UUID");
                if (descriptor == null) {
                    Timber.i("it seems that descr UUID doesn't exist");
                }
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                Timber.i("enable notify UUID");
                mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
                Timber.i("write notify");
                InmotionAdapterV2.getInstance().startKeepAliveTimer();
                Timber.i("starting Inmotion V2 adapter");
                return true;

            } else if (WHEEL_TYPE.NINEBOT_Z.toString().equalsIgnoreCase(adapterName)) {
                Timber.i("Trying to start Ninebot Z");
                setWheelType(WHEEL_TYPE.NINEBOT_Z);
                BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.NINEBOT_Z_SERVICE_UUID));
                Timber.i("service UUID");
                BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.NINEBOT_Z_READ_CHARACTER_UUID));
                Timber.i("read UUID");
                if (notifyCharacteristic == null) {
                    Timber.i("it seems that RX UUID doesn't exist");
                }
                mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                Timber.i("notify UUID");
                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.NINEBOT_Z_DESCRIPTER_UUID));
                Timber.i("descr UUID");
                if (descriptor == null) {
                    Timber.i("it seems that descr UUID doesn't exist");
                }
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                Timber.i("enable notify UUID");
                mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
                Timber.i("write notify");
                if (protoVer.compareTo("S2") == 0 || protoVer.compareTo("Mini") == 0) {
                    NinebotAdapter.getInstance().startKeepAliveTimer(protoVer);
                } else {
                    NinebotZAdapter.getInstance().startKeepAliveTimer();
                }
                Timber.i("starting ninebot adapter");
                return true;
            } else if (WHEEL_TYPE.NINEBOT.toString().equalsIgnoreCase(adapterName)) {
                Timber.i("Trying to start Ninebot");
                setWheelType(WHEEL_TYPE.NINEBOT);
                BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.NINEBOT_SERVICE_UUID));
                Timber.i("service UUID");
                BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.NINEBOT_READ_CHARACTER_UUID));
                Timber.i("read UUID");
                if (notifyCharacteristic == null) {
                    Timber.i("it seems that RX UUID doesn't exist");
                }
                mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                Timber.i("notify UUID");
                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.NINEBOT_DESCRIPTER_UUID));
                Timber.i("descr UUID");
                if (descriptor == null) {
                    Timber.i("it seems that descr UUID doesn't exist");
                }
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                Timber.i("enable notify UUID");
                mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
                Timber.i("write notify");
                NinebotAdapter.getInstance().startKeepAliveTimer(protoVer);
                Timber.i("starting ninebot adapter");
                return true;
            }
        } else {
            WheelLog.AppConfig.setLastMac("");
            Timber.i("Protocol recognized as Unknown");
            for (BluetoothGattService service : mBluetoothLeService.getSupportedGattServices()) {
                Timber.i("Service: %s", service.getUuid().toString());
                for (BluetoothGattCharacteristic characteristics : service.getCharacteristics()) {
                    Timber.i("Characteristics: %s", characteristics.getUuid().toString());
                }
            }
        }
        return false;
    }
}
