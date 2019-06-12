package com.cooper.wheellog;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Vibrator;
import android.media.ToneGenerator;
import android.media.AudioManager;

import android.text.InputType;
import android.widget.EditText;
import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.ALARM_TYPE;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.InMotionAdapter;
import com.cooper.wheellog.utils.NinebotZAdapter;
import com.cooper.wheellog.utils.SettingsUtil;


import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class WheelData {
    private static final int TIME_BUFFER = 10;
    private static WheelData mInstance;
	private Timer ridingTimerControl;

    private BluetoothLeService mBluetoothLeService;

    private long graph_last_update_time;
    private static final int GRAPH_UPDATE_INTERVAL = 1000; // milliseconds
    private static final int MAX_BATTERY_AVERAGE_COUNT = 150;
	private static final int RIDING_SPEED = 200; // 2km/h
	private static final double RATIO_GW = 0.875;
    private ArrayList<String> xAxis = new ArrayList<>();
    private ArrayList<Float> currentAxis = new ArrayList<>();
    private ArrayList<Float> speedAxis = new ArrayList<>();

    private int mSpeed;
    private long mTotalDistance;
    private int mCurrent;
    private int mTemperature;
	private int mTemperature2;
	private double mAngle;
	private double mRoll;

    private int mMode;
    private int mBattery;
    private double mAverageBattery;
    private double mAverageBatteryCount;
    private int mVoltage;
    private long mDistance;
	private long mUserDistance;
    private int mRideTime;
	private int mRidingTime;
    private int mLastRideTime;
    private int mTopSpeed;
    private int mFanStatus;
    private boolean mConnectionState = false;
	private boolean mNewWheelSettings = false;
    private boolean mKSAlertsAndSpeedupdated = false;
    private String mName = "Unknown";
    private String mModel = "Unknown";
	private String mModeStr = "Unknown";
	private String mBtName = "";
	
	private String mAlert = "";

//    private int mVersion; # sorry King, but INT not good for Inmo
	private String mVersion = "";
    private String mSerialNumber = "Unknown";
    private WHEEL_TYPE mWheelType = WHEEL_TYPE.Unknown;
    private long rideStartTime;
    private long mStartTotalDistance;
	/// Wheel Settings
	private boolean mWheelLightEnabled = false;
	private boolean mWheelLedEnabled = false;
	private boolean mWheelButtonDisabled = false;
	private int mWheelMaxSpeed = 0;
	private int mWheelSpeakerVolume = 50;
	private int mWheelTiltHorizon = 0;
	
    private boolean mAlarmsEnabled = false;
    private boolean mDisablePhoneVibrate = false;
    private int mAlarm1Speed = 0;
    private int mAlarm2Speed = 0;
    private int mAlarm3Speed = 0;
    private int mKSAlarm1Speed = 0;
    private int mKSAlarm2Speed = 0;
    private int mKSAlarm3Speed = 0;
    private int mAlarm1Battery = 0;
    private int mAlarm2Battery = 0;
    private int mAlarm3Battery = 0;
    private int mAlarmCurrent = 0;
	private int mAlarmTemperature = 0;
    private int mGotwayVoltageScaler = 0;


	private boolean mUseRatio = false;
	//private boolean mGotway84V = false;
	private boolean mSpeedAlarmExecuted = false;
    private boolean mCurrentAlarmExecuted = false;
	private boolean mTemperatureAlarmExecuted = false;

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

    int getSpeed() {
        return mSpeed / 10;
    }
	
	boolean getWheelLight() {
        return mWheelLightEnabled;
    }
	
	boolean getWheelLed() {
        return mWheelLedEnabled;
    }
	
	boolean getWheelHandleButton() {
        return mWheelButtonDisabled;
    }
	
    int getWheelMaxSpeed() {

        return mWheelMaxSpeed;
    }

    int getKSAlarm1Speed() {

        return mKSAlarm1Speed;
    }

    int getKSAlarm2Speed() {

        return mKSAlarm2Speed;
    }

    int getKSAlarm3Speed() {

        return mKSAlarm3Speed;
    }

	int getSpeakerVolume() {
        return mWheelSpeakerVolume;
    }
	
	int getPedalsPosition() {
        return mWheelTiltHorizon;
    }

    public boolean is_pref_received(){
        return mKSAlertsAndSpeedupdated;
    }

    public void setBtName(String btName) {
        mBtName = btName;
    }

    public void updateLight(boolean enabledLight) {
		if (mWheelLightEnabled != enabledLight) {
			mWheelLightEnabled = enabledLight;
			InMotionAdapter.getInstance().setLightState(enabledLight);
		}
    }
	
	public void updateLed(boolean enabledLed) {
		if (mWheelLedEnabled != enabledLed) {
			mWheelLedEnabled = enabledLed;
			InMotionAdapter.getInstance().setLedState(enabledLed);
		}
    }
	
	public void updatePedalsMode(int pedalsMode) {
		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			switch (pedalsMode) {
				case 0:
					mBluetoothLeService.writeBluetoothGattCharacteristic("h".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;
				case 1: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("f".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;
				case 2: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("s".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;	
			}			
		}
		
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[2] = (byte) pedalsMode;
			data[3] = (byte) 0xE0;
            data[16] = (byte) 0x87;
            data[17] = (byte) 0x15;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
	
    }
	
	public void updateLightMode(int lightMode) {
		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			switch (lightMode) {
				case 0:
					mBluetoothLeService.writeBluetoothGattCharacteristic("E".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;
				case 1: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("Q".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;
				case 2: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("T".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;	
			}			
		}
		
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[2] = (byte) (lightMode + 0x12);
			data[3] = (byte) 0x01;
            data[16] = (byte) 0x73;
            data[17] = (byte) 0x14;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
	
    }

	public void updateStrobe(int strobeMode) {
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[2] = (byte) strobeMode;
            data[16] = (byte) 0x53;
            data[17] = (byte) 0x14;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
		
    }
	
	public void updateLedMode(int ledMode) {
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            byte[] data = new byte[20];
            data[0] = (byte) 0xAA;
            data[1] = (byte) 0x55;
			data[2] = (byte) ledMode;
            data[16] = (byte) 0x6C;
            data[17] = (byte) 0x14;
            data[18] = (byte) 0x5A;
            data[19] = (byte) 0x5A;
            mBluetoothLeService.writeBluetoothGattCharacteristic(data);
		}
		
    }
	
	
	public void updateAlarmMode(int alarmMode) {
		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			switch (alarmMode) {
				case 0:
					mBluetoothLeService.writeBluetoothGattCharacteristic("u".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;
				case 1: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("i".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;
				case 2: 
					mBluetoothLeService.writeBluetoothGattCharacteristic("o".getBytes());
					mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
					break;	
			}			
		}
		
    }
	
	public void updateCalibration() {
		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("c".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("y".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("c".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("y".getBytes());	
			mBluetoothLeService.writeBluetoothGattCharacteristic("c".getBytes());
			mBluetoothLeService.writeBluetoothGattCharacteristic("y".getBytes());	
		}
		
		
    }


	public void updateHandleButton(boolean enabledButton) {
		if (mWheelButtonDisabled != enabledButton) {
			mWheelButtonDisabled = enabledButton;
			InMotionAdapter.getInstance().setHandleButtonState(enabledButton);
		}
    }

	public void updateMaxSpeed(int wheelMaxSpeed) {
		if (mWheelType == WHEEL_TYPE.INMOTION) {
			if (mWheelMaxSpeed != wheelMaxSpeed) {
				mWheelMaxSpeed = wheelMaxSpeed;
				InMotionAdapter.getInstance().setMaxSpeedState(wheelMaxSpeed);
			}
		}

		if (mWheelType == WHEEL_TYPE.GOTWAY) {
			byte[] data = new byte[1];
			if (wheelMaxSpeed != 0) {
				int wheelMaxSpeed2 = wheelMaxSpeed;
				if (mUseRatio) wheelMaxSpeed2 = (int)Math.round(wheelMaxSpeed2/RATIO_GW);
				mBluetoothLeService.writeBluetoothGattCharacteristic("W".getBytes());
				mBluetoothLeService.writeBluetoothGattCharacteristic("Y".getBytes());
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
				
				data[0] = (byte)((wheelMaxSpeed2/10)+0x30);
				mBluetoothLeService.writeBluetoothGattCharacteristic(data);
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
				data[0] = (byte)((wheelMaxSpeed2%10)+0x30);
				mBluetoothLeService.writeBluetoothGattCharacteristic(data);
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
			} else {
				data[0] = 0x22;
				mBluetoothLeService.writeBluetoothGattCharacteristic(data); // "
				mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());			
			}
		}
		if (mWheelType == WHEEL_TYPE.KINGSONG) {
            if (mWheelMaxSpeed != wheelMaxSpeed) {
                mWheelMaxSpeed = wheelMaxSpeed;
                updateKSAlarmAndSpeed();
            }
		}
		
	}

    public void updateKSAlarmAndSpeed() {
        byte[] data = new byte[20];
        data[0] = (byte) 0xAA;
        data[1] = (byte) 0x55;
        data[2] = (byte) mKSAlarm1Speed;
        data[4] = (byte) mKSAlarm2Speed;
        data[6] = (byte) mKSAlarm3Speed;
        data[8] = (byte) mWheelMaxSpeed;
        data[16] = (byte) 0x85;

        if((mWheelMaxSpeed | mKSAlarm3Speed | mKSAlarm2Speed | mKSAlarm1Speed) == 0){
            data[16] = (byte) 0x98; // request speed & alarm values from wheel
        }

        data[17] = (byte) 0x14;
        data[18] = (byte) 0x5A;
        data[19] = (byte) 0x5A;
        mBluetoothLeService.writeBluetoothGattCharacteristic(data);

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
	
	public void updateSpeakerVolume(int speakerVolume) {
        if (mWheelSpeakerVolume != speakerVolume) {
			mWheelSpeakerVolume = speakerVolume;
			InMotionAdapter.getInstance().setSpeakerVolumeState(speakerVolume);
		}
    }
	
	public void updatePedals(int pedalAdjustment) {
        if (mWheelTiltHorizon != pedalAdjustment) {
			mWheelTiltHorizon = pedalAdjustment;
			InMotionAdapter.getInstance().setTiltHorizon(pedalAdjustment);
		}
    }
	
    public int getTemperature() {
        return mTemperature / 100;
    }
	
    public int getTemperature2() {
        return mTemperature2 / 100;
    }
	
	public double getAngle() {
        return mAngle;
    }
	
	public double getRoll() {
        return mRoll;
    }
	
    public int getBatteryLevel() {
        return mBattery;
    }

    int getFanStatus() {
        return mFanStatus;
    }

    boolean isConnected() {
        return mConnectionState;
    }

    //    int getTopSpeed() { return mTopSpeed; }
    String getVersion() {
        return mVersion;
    }

    //    int getCurrentTime() { return mCurrentTime+mLastCurrentTime; }
    int getMode() {
        return mMode;
    }

    WHEEL_TYPE getWheelType() {
        return mWheelType;
    }

    String getName() {
        return mName;
    }

    String getModel() {
        return mModel;
    }
	
	String getModeStr() {
        return mModeStr;
    }
	
	String getAlert() {
		String nAlert = mAlert;
		mAlert = "";
        return nAlert;
    }
	
    String getSerial() {
        return mSerialNumber;
    }

    int getRideTime() { return mRideTime; }

    double getAverageSpeedDouble() {
		if (mTotalDistance!=0 && mRideTime !=0) {
			return (((mTotalDistance - mStartTotalDistance)*3.6)/(mRideTime + mLastRideTime));
		}
		else return 0.0;
	}
	
	double getAverageRidingSpeedDouble() {
		if (mTotalDistance!=0 && mRidingTime !=0) {
			return (((mTotalDistance - mStartTotalDistance)*3.6)/mRidingTime);
		}
		else return 0.0;
	}
	
    String getRideTimeString() {
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
	
    double getSpeedDouble() {
        return mSpeed / 100.0;
    }

    double getVoltageDouble() {
        return mVoltage / 100.0;
    }

    double getPowerDouble() {
        return (mCurrent * mVoltage) / 10000.0;
    }

    double getCurrentDouble() {
        return mCurrent / 100.0;
    }

    int getTopSpeed() { return mTopSpeed; }

    double getTopSpeedDouble() {
        return mTopSpeed / 100.0;
    }

    int getDistance() { return (int) (mTotalDistance - mStartTotalDistance); }
	
	long getWheelDistance() { 
		return mDistance; 
	}
	
	public double getWheelDistanceDouble() {
        return mDistance / 1000.0;
    }
	
	
	public double getUserDistanceDouble() {
		if (mUserDistance == 0 && mTotalDistance != 0 )  {
			Context mContext = mBluetoothLeService.getApplicationContext();
			mUserDistance = SettingsUtil.getUserDistance(mContext, mBluetoothLeService.getBluetoothDeviceAddress());
			if (mUserDistance == 0) {
				SettingsUtil.setUserDistance(mContext, mBluetoothLeService.getBluetoothDeviceAddress(),mTotalDistance);
				mUserDistance = mTotalDistance;
			}
		}
		return (mTotalDistance - mUserDistance)/1000.0; 
    }
	
	public void resetUserDistance() {		
		if (mTotalDistance != 0)  {
			Context mContext = mBluetoothLeService.getApplicationContext();
			SettingsUtil.setUserDistance(mContext, mBluetoothLeService.getBluetoothDeviceAddress(), mTotalDistance);		
			mUserDistance = mTotalDistance;
		}

    }
	
	public void resetTopSpeed() {
		mTopSpeed = 0;
    }
	

    public double getDistanceDouble() {
        return (mTotalDistance - mStartTotalDistance) / 1000.0;
    }

    double getTotalDistanceDouble() {
        return mTotalDistance / 1000.0;
    }
	
	long getTotalDistance() {
        return mTotalDistance;
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

		//if (mWheelType == WHEEL_TYPE.INMOTION || !mConnectionState) InMotionAdapter.getInstance().stopTimer();
        //if (mWheelType == WHEEL_TYPE.NINEBOT_Z) NinebotZAdapter.getInstance().resetConnection();
    }
	
//	void setUserDistance(long userDistance) {
//        mUserDistance = userDistance;
//    }

    void setAlarmsEnabled(boolean enabled) {
        mAlarmsEnabled = enabled;
    }
	
	void setUseRatio(boolean enabled) {
        mUseRatio = enabled;
		reset();
    }
	
	void setGotwayVoltage(int voltage) {
        mGotwayVoltageScaler = voltage;
    }

    void setPreferences(int alarm1Speed, int alarm1Battery,
                                   int alarm2Speed, int alarm2Battery,
                                   int alarm3Speed, int alarm3Battery,
                                   int alarmCurrent,int alarmTemperature, boolean disablePhoneVibrate) {
        mAlarm1Speed = alarm1Speed * 100;
        mAlarm2Speed = alarm2Speed * 100;
        mAlarm3Speed = alarm3Speed * 100;
        mAlarm1Battery = alarm1Battery;
        mAlarm2Battery = alarm2Battery;
        mAlarm3Battery = alarm3Battery;
        mAlarmCurrent = alarmCurrent*100;
		mAlarmTemperature = alarmTemperature*100;
        mDisablePhoneVibrate = disablePhoneVibrate;
    }

    private int byteArrayInt2(byte low, byte high) {
        return (low & 255) + ((high & 255) * 256);
    }

    private long byteArrayInt4(byte value1, byte value2, byte value3, byte value4) {
        return (((((long) ((value1 & 255) << 16))) | ((long) ((value2 & 255) << 24))) | ((long) (value3 & 255))) | ((long) ((value4 & 255) << 8));
    }

    private void setDistance(long distance) {
        if (mStartTotalDistance == 0 && mTotalDistance != 0)
            mStartTotalDistance = mTotalDistance;

        mDistance = distance;
    }

    private void setCurrentTime(int currentTime) {
        if (mRideTime > (currentTime + TIME_BUFFER))
            mLastRideTime += mRideTime;
        mRideTime = currentTime;
    }

    private void setTopSpeed(int topSpeed) {
        if (topSpeed > mTopSpeed)
            mTopSpeed = topSpeed;
    }

    private void setBatteryPercent(int battery) {
        mBattery = battery;

        mAverageBatteryCount = mAverageBatteryCount < MAX_BATTERY_AVERAGE_COUNT ?
                mAverageBatteryCount + 1 : MAX_BATTERY_AVERAGE_COUNT;

        mAverageBattery += (battery - mAverageBattery) / mAverageBatteryCount;
    }

    private void checkAlarmStatus(Context mContext) {
        // SPEED ALARM
        if (!mSpeedAlarmExecuted) {
            if (mAlarm1Speed > 0 && mAlarm1Battery > 0 &&
                    mAverageBattery <= mAlarm1Battery && mSpeed >= mAlarm1Speed)
                raiseAlarm(ALARM_TYPE.SPEED, mContext);
            else if (mAlarm2Speed > 0 && mAlarm2Battery > 0 &&
                    mAverageBattery <= mAlarm2Battery && mSpeed >= mAlarm2Speed)
                raiseAlarm(ALARM_TYPE.SPEED, mContext);
            else if (mAlarm3Speed > 0 && mAlarm3Battery > 0 &&
                    mAverageBattery <= mAlarm3Battery && mSpeed >= mAlarm3Speed)
                raiseAlarm(ALARM_TYPE.SPEED, mContext);
        } else {
            boolean alarm_finished = false;
            if (mAlarm1Speed > 0 && mAlarm1Battery > 0 &&
                    mAverageBattery > mAlarm1Battery && mSpeed < mAlarm1Speed)
                alarm_finished = true;
            else if (mAlarm2Speed > 0 && mAlarm2Battery > 0 &&
                    mAverageBattery <= mAlarm2Battery && mSpeed >= mAlarm2Speed)
                alarm_finished = true;
            else if (mAlarm3Speed > 0 && mAlarm3Battery > 0 &&
                    mAverageBattery <= mAlarm3Battery && mSpeed >= mAlarm3Speed)
                alarm_finished = true;

            mSpeedAlarmExecuted = alarm_finished;
        }

        // CURRENT
        if (!mCurrentAlarmExecuted) {
            if (mAlarmCurrent > 0 &&
                    mCurrent >= mAlarmCurrent) {
                raiseAlarm(ALARM_TYPE.CURRENT, mContext);
            }
        } else {
            if (mCurrent < mAlarmCurrent)
                mCurrentAlarmExecuted = false;
        }
		
		// TEMP
		if (!mTemperatureAlarmExecuted) {
            if (mAlarmTemperature > 0 && mTemperature >= mAlarmTemperature) {
                raiseAlarm(ALARM_TYPE.TEMPERATURE, mContext);
            }
        } else {
            if (mTemperature < mAlarmTemperature)
                mTemperatureAlarmExecuted = false;
        }
		
    }

    private void raiseAlarm(ALARM_TYPE alarmType, Context mContext) {
        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0};
        Intent intent = new Intent(Constants.ACTION_ALARM_TRIGGERED);
        intent.putExtra(Constants.INTENT_EXTRA_ALARM_TYPE, alarmType);

        switch (alarmType) {
            case SPEED:
                pattern = new long[]{0, 100, 100};
                mSpeedAlarmExecuted = true;
                break;
            case CURRENT:
                pattern = new long[]{0, 50, 50, 50, 50};
                mCurrentAlarmExecuted = true;
                break;
			case TEMPERATURE:
                pattern = new long[]{0, 500, 500};
                mCurrentAlarmExecuted = true;
                break;
        }
        mContext.sendBroadcast(intent);
        if (v.hasVibrator() && !mDisablePhoneVibrate)
            v.vibrate(pattern, -1);
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    void decodeResponse(byte[] data, Context mContext) {

        StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte aData : data)
            stringBuilder.append(String.format(Locale.US, "%02X", aData));
        Timber.i("Received: " + stringBuilder.toString());
//        FileUtil.writeLine("bluetoothOutput.txt", stringBuilder.toString());

        boolean new_data = false;
        if (mWheelType == WHEEL_TYPE.KINGSONG)
            new_data = decodeKingSong(data);
        else if (mWheelType == WHEEL_TYPE.GOTWAY)
            new_data = decodeGotway(data);
        else if (mWheelType == WHEEL_TYPE.INMOTION)
            new_data = decodeInmotion(data);
        else if (mWheelType == WHEEL_TYPE.NINEBOT_Z) {
            Timber.i("Ninebot_z decoding");
            new_data = decodeNinebot(data);

        }


        if (!new_data)
			return;

		Intent intent = new Intent(Constants.ACTION_WHEEL_DATA_AVAILABLE);       
		
		if (mNewWheelSettings) {
			intent.putExtra(Constants.INTENT_EXTRA_WHEEL_SETTINGS, true);
			mNewWheelSettings = false;
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

		if (mAlarmsEnabled) 
			checkAlarmStatus(mContext);
		mContext.sendBroadcast(intent);
        
       

        
    }

    private boolean decodeKingSong(byte[] data) {
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
			mRidingTime = 0;
		}
        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            if (a1 != 170 || a2 != 85) {
                return false;
            }
            if ((data[16] & 255) == 169) { // Live data
                mVoltage = byteArrayInt2(data[2], data[3]);
                mSpeed = byteArrayInt2(data[4], data[5]);
                mTotalDistance = byteArrayInt4(data[6], data[7], data[8], data[9]);
                mCurrent = ((data[10]&0xFF) + (data[11]<<8));
 
				mTemperature = byteArrayInt2(data[12], data[13]);

                if ((data[15] & 255) == 224) {
                    mMode = data[14];
					mModeStr = String.format(Locale.US, "%d", mMode);
                }

                int battery;


                if ((mModel.compareTo("KS-18L") == 0) || (mModel.compareTo("KS-16X") == 0) ||(mBtName.compareTo("RW") == 0) || (mName.startsWith("ROCKW"))) {

                    if (mVoltage > 8350) {
                        battery = 100;
                    } else if (mVoltage > 6800) {
                        battery = (mVoltage - 6650) / 17;
                    } else if (mVoltage > 6400){
                        battery = (mVoltage - 6400) / 45;
                    } else {
                        battery = 0;
                    }

                } else {
//                    if (mVoltage > 6680) {
//                        battery = 100;
//                    } else if (mVoltage > 5440) {
//                        battery = (int)Math.round((mVoltage - 5320) / 13.6);
//                    } else if (mVoltage > 5120){
//                        battery = (mVoltage - 5120) / 36;
//                    } else {
//                        battery = 0;
//                    }
                    if (mVoltage < 5000) {
                        battery = 0;
                    } else if (mVoltage >= 6600) {
                        battery = 100;
                    } else {
                        battery = (mVoltage - 5000) / 16;
                    }

                }

                setBatteryPercent(battery);

                return true;
            } else if ((data[16] & 255) == 185) { // Distance/Time/Fan Data
                long distance = byteArrayInt4(data[2], data[3], data[4], data[5]);
                setDistance(distance);
                //int currentTime = byteArrayInt2(data[6], data[7]);
	            int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
                setCurrentTime(currentTime);
                setTopSpeed(byteArrayInt2(data[8], data[9]));
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
                    mVersion = String.format(Locale.US, "%.2f", ((double)(Integer.parseInt(ss[ss.length - 1])/100.0)));
                } catch (Exception ignored) {
                }

            } else if ((data[16] & 255) == 179) { // Serial Number
                byte[] sndata = new byte[18];
                System.arraycopy(data, 2, sndata, 0, 14);
                System.arraycopy(data, 17, sndata, 14, 3);
                sndata[17] = (byte) 0;
                mSerialNumber = new String(sndata);
                updateKSAlarmAndSpeed();
            }
            else if ((data[16] & 255) == 164 || (data[16] & 255) == 181) { //0xa4 || 0xb5 max speed and alerts
                mWheelMaxSpeed = (data[10] & 255);
                mKSAlarm3Speed = (data[8] & 255);
                mKSAlarm2Speed = (data[6] & 255);
                mKSAlarm1Speed = (data[4] & 255);
                mKSAlertsAndSpeedupdated = true;
                // after received 0xa4 send same repeat data[2] =0x01 data[16] = 0x98
                if((data[16] & 255) == 164)
                {
                    data[16] = (byte)0x98;
                    mBluetoothLeService.writeBluetoothGattCharacteristic(data);
                }

            }
        }
        return false;
    }

    private boolean decodeGotway(byte[] data) {
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
			mRidingTime = 0;
		}
        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            int a19 = data[18] & 255;
            if (a1 != 85 || a2 != 170 || a19 != 0) {
                return false;
            }

            if (data[5] >= 0)
                mSpeed = (int) Math.abs(((data[4] * 256.0) + data[5]) * 3.6);
            else
                mSpeed = (int) Math.abs((((data[4] * 256.0) + 256.0) + data[5]) * 3.6);
			if (mUseRatio) mSpeed = (int)Math.round(mSpeed * RATIO_GW);
            setTopSpeed(mSpeed);

            mTemperature = (int) Math.round(((((data[12] * 256) + data[13]) / 340.0) + 35) * 100);
			mTemperature2 = mTemperature;

            long distance = byteArrayInt2(data[9], data[8]);
			if (mUseRatio) distance = Math.round(distance * RATIO_GW);
            setDistance(distance);

            mVoltage = (data[2] * 256) + (data[3] & 255);

            mCurrent = Math.abs((data[10] * 256) + data[11]);

            int battery;

//            if (mVoltage > 6680) {
//                battery = 100;
//            } else if (mVoltage > 5440) {
//                battery = (mVoltage - 5380) / 13;
//            } else if (mVoltage > 5290){
//                battery = (int)Math.round((mVoltage - 5290) / 32.5);
//            } else {
//                battery = 0;
//            }
            if (mVoltage <= 5290) {
                battery = 0;
            } else if (mVoltage >= 6580) {
                battery = 100;
            } else {
                battery = (mVoltage - 5290) / 13;
            }
          setBatteryPercent(battery);
//			if (mGotway84V) {
//				mVoltage = (int)Math.round(mVoltage / 0.8);
//			}
            mVoltage = mVoltage + (int)Math.round(mVoltage*0.25*mGotwayVoltageScaler);
            int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
            setCurrentTime(currentTime);

            return true;
        } else if (data.length >= 10) {
            int a1 = data[0];
            int a5 = data[4] & 255;
            int a6 = data[5] & 255;
            if (a1 != 90 || a5 != 85 || a6 != 170) {
                return false;
            }
            mTotalDistance = ((data[6]&0xFF) <<24) + ((data[7]&0xFF) << 16) + ((data[8] & 0xFF) <<8) + (data[9] & 0xFF);
			if (mUseRatio) mTotalDistance = Math.round(mTotalDistance * RATIO_GW);
        }
        return false;
    }

    private boolean decodeNinebot(byte[] data) {
        ArrayList<NinebotZAdapter.Status> statuses = NinebotZAdapter.getInstance().charUpdated(data);
        if (statuses.size() < 1) return false;
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
            mRidingTime = 0;
        }
        for (NinebotZAdapter.Status status: statuses) {
            Timber.i(status.toString());
            if (status instanceof NinebotZAdapter.serialNumberStatus) {
                mSerialNumber = ((NinebotZAdapter.serialNumberStatus) status).getSerialNumber();
                mModel = "Ninebot Z";
            } else if (status instanceof NinebotZAdapter.versionStatus){
                mVersion = ((NinebotZAdapter.versionStatus) status).getVersion();
            } else {
                mSpeed = (int) (status.getSpeed());
                mVoltage = (int) (status.getVoltage());
                mBattery = (int) (status.getBatt());
                mCurrent = (int) (status.getCurrent());
                mTotalDistance = (long) (status.getDistance());
                mTemperature = (int) (status.getTemperature()*10);


                setDistance((long) status.getDistance());
                int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
                setCurrentTime(currentTime);
                setTopSpeed(mSpeed);
            }


        }
        return true;
    }

    private boolean decodeInmotion(byte[] data) {
        ArrayList<InMotionAdapter.Status> statuses = InMotionAdapter.getInstance().charUpdated(data);
		if (statuses.size() < 1) return false;
        if (rideStartTime == 0) {
            rideStartTime = Calendar.getInstance().getTimeInMillis();
			mRidingTime = 0;
		}		
        for (InMotionAdapter.Status status: statuses) {
            Timber.i(status.toString());
            if (status instanceof InMotionAdapter.Infos) {
				mWheelLightEnabled = ((InMotionAdapter.Infos) status).getLightState();
				mWheelLedEnabled = ((InMotionAdapter.Infos) status).getLedState();
				mWheelButtonDisabled = ((InMotionAdapter.Infos) status).getHandleButtonState();
				mWheelMaxSpeed = ((InMotionAdapter.Infos) status).getMaxSpeedState();
				mWheelSpeakerVolume = ((InMotionAdapter.Infos) status).getSpeakerVolumeState();
				mWheelTiltHorizon = ((InMotionAdapter.Infos) status).getTiltHorizon(); 
                mSerialNumber = ((InMotionAdapter.Infos) status).getSerialNumber();
                mModel = ((InMotionAdapter.Infos) status).getModelString();
                mVersion = ((InMotionAdapter.Infos) status).getVersion();
				mNewWheelSettings = true;
            } else if (status instanceof InMotionAdapter.Alert){
				if (mAlert == "") {
					mAlert = ((InMotionAdapter.Alert) status).getfullText();
				} else {
					mAlert = mAlert + " | " + ((InMotionAdapter.Alert) status).getfullText();
				}
			} else {
                mSpeed = (int) (status.getSpeed() * 360d);
                mVoltage = (int) (status.getVoltage() * 100d);
                mCurrent = (int) (status.getCurrent() * 100d);
				mTemperature = (int) (status.getTemperature() * 100d);
				mTemperature2 = (int) (status.getTemperature2() * 100d);
				mTotalDistance = (long) (status.getDistance()*1000d);
				mAngle = (double) (status.getAngle()); 
				mRoll = (double) (status.getRoll()); 
				
				mModeStr = status.getWorkModeString();
                setBatteryPercent((int) status.getBatt());
                setDistance((long) status.getDistance());
				
                int currentTime = (int) (Calendar.getInstance().getTimeInMillis() - rideStartTime) / 1000;
                setCurrentTime(currentTime);
                setTopSpeed(mSpeed);
            }
        }
        return true;
    }

    void full_reset() {
        if (mWheelType == WHEEL_TYPE.INMOTION) InMotionAdapter.getInstance().stopTimer();
        if (mWheelType == WHEEL_TYPE.NINEBOT_Z) NinebotZAdapter.getInstance().stopTimer();
        mBluetoothLeService = null;
        mWheelType = WHEEL_TYPE.Unknown;
        xAxis.clear();
        speedAxis.clear();
        currentAxis.clear();
        reset();
    }

    void reset() {
        mSpeed = 0;
        mTotalDistance = 0;
        mCurrent = 0;
        mTemperature = 0;
		mTemperature2 = 0;
		mAngle = 0;
		mRoll = 0;
        mMode = 0;
        mBattery = 0;
        mAverageBatteryCount = 0;
        mAverageBattery = 0;
        mVoltage = 0;
        mRideTime = 0;
		mRidingTime = 0;
        mTopSpeed = 0;
        mFanStatus = 0;
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
		mWheelTiltHorizon = 0;
		mWheelLightEnabled = false;
		mWheelLedEnabled = false;
		mWheelButtonDisabled = false;
		mWheelMaxSpeed = 0;
		mWheelSpeakerVolume = 50;
	
    }

    boolean detectWheel(BluetoothLeService bluetoothService) {
        mBluetoothLeService = bluetoothService;
        Context mContext = bluetoothService.getApplicationContext();

        Class<R.array> res = R.array.class;
        String wheel_types[] = mContext.getResources().getStringArray(R.array.wheel_types);
        for (String wheel_Type : wheel_types) {
            boolean detected_wheel = true;
            java.lang.reflect.Field services_res = null;
            try {
                services_res = res.getField(wheel_Type + "_services");
            } catch (Exception ignored) {
            }
            int services_res_id = 0;
            if (services_res != null)
                try {
                    services_res_id = services_res.getInt(null);
                } catch (Exception ignored) {
                }

            String services[] = mContext.getResources().getStringArray(services_res_id);

            if (services.length != mBluetoothLeService.getSupportedGattServices().size())
                continue;

            for (String service_uuid : services) {
                UUID s_uuid = UUID.fromString(service_uuid.replace("_", "-"));
                BluetoothGattService service = mBluetoothLeService.getGattService(s_uuid);
                if (service != null) {
                    java.lang.reflect.Field characteristic_res = null;
                    try {
                        characteristic_res = res.getField(wheel_Type + "_" + service_uuid);
                    } catch (Exception ignored) {
                    }
                    int characteristic_res_id = 0;
                    if (characteristic_res != null)
                        try {
                            characteristic_res_id = characteristic_res.getInt(null);
                        } catch (Exception ignored) {
                        }
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
				final Intent intent = new Intent(Constants.ACTION_WHEEL_TYPE_RECOGNIZED); // update preferences
                intent.putExtra(Constants.INTENT_EXTRA_WHEEL_TYPE, wheel_Type);
				mContext.sendBroadcast(intent);
				Timber.i("Protocol recognized as %s", wheel_Type);
				//System.out.println("WheelRecognizedWD");
                if (mContext.getResources().getString(R.string.gotway).equals(wheel_Type) && (mBtName.equals("RW") || mName.startsWith("ROCKW"))) {
                    Timber.i("It seems to be RochWheel, force to Kingsong proto");
                    wheel_Type = mContext.getResources().getString(R.string.kingsong);
                }
                if (mContext.getResources().getString(R.string.kingsong).equals(wheel_Type)) {
                    mWheelType = WHEEL_TYPE.KINGSONG;
                    BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.KINGSONG_SERVICE_UUID));
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.KINGSONG_READ_CHARACTER_UUID));
                    mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.KINGSONG_DESCRIPTER_UUID));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
					
                    return true;
                } else if (mContext.getResources().getString(R.string.gotway).equals(wheel_Type)) {
                    mWheelType = WHEEL_TYPE.GOTWAY;
                    BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.GOTWAY_SERVICE_UUID));
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.GOTWAY_READ_CHARACTER_UUID));
                    mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                    // Let the user know it's working by making the wheel beep
                    mBluetoothLeService.writeBluetoothGattCharacteristic("b".getBytes());
                    return true;
                } else if (mContext.getResources().getString(R.string.inmotion).equals(wheel_Type)) {
                    mWheelType = WHEEL_TYPE.INMOTION;
                    BluetoothGattService targetService = mBluetoothLeService.getGattService(UUID.fromString(Constants.INMOTION_SERVICE_UUID));
                    BluetoothGattCharacteristic notifyCharacteristic = targetService.getCharacteristic(UUID.fromString(Constants.INMOTION_READ_CHARACTER_UUID));
                    mBluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(Constants.INMOTION_DESCRIPTER_UUID));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothLeService.writeBluetoothGattDescriptor(descriptor);
                    if (SettingsUtil.hasPasswordForWheel(mContext, mBluetoothLeService.getBluetoothDeviceAddress())) {
                        String inmotionPassword = SettingsUtil.getPasswordForWheel(mBluetoothLeService.getApplicationContext(), mBluetoothLeService.getBluetoothDeviceAddress());
                        InMotionAdapter.getInstance().startKeepAliveTimer(mBluetoothLeService, inmotionPassword);
                        return true;
                    }
                    return false;
                } else if (mContext.getResources().getString(R.string.ninebot_z).equals(wheel_Type)) {
                    Timber.i("Trying to start Ninebot");
                    mWheelType = WHEEL_TYPE.NINEBOT_Z;
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
                    NinebotZAdapter.getInstance().startKeepAliveTimer(mBluetoothLeService,"");
                    Timber.i("starting ninebot adapter");
                    return true;
                }
            }
        }
        return false;
    }
}
