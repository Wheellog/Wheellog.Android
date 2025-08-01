package com.cooper.wheellog.utils;

import android.content.Context;
import android.content.Intent;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.WheelData;

import org.koin.java.KoinJavaComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import timber.log.Timber;

public class InmotionAdapterV2 extends BaseAdapter {
    private final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
    private static InmotionAdapterV2 INSTANCE;
    private Timer keepAliveTimer;
    private boolean settingCommandReady = false;
    private boolean requestSettings = false;
    private boolean turningOff = false;
    private static int updateStep = 0;
    private static int stateCon = 0;
    private static int lightSwitchCounter = 0;
    private byte[] settingCommand;
    private static Model mModel = Model.UNKNOWN;
    private static int protoVer = 0;
    InmotionUnpackerV2 unpacker = new InmotionUnpackerV2();

    @Override
    public boolean decode(byte[] data) {
        for (byte c : data) {
            if (unpacker.addChar(c)) {
                Message result = Message.verify(unpacker.getBuffer());

                if (result != null) {
                    Timber.i("Get new data, command: %02X", result.command);
                    if (result.flags == Message.Flag.Initial.getValue()) {
                        if (result.command == Message.Command.MainInfo.getValue()) {
                            return result.parseMainData();
                        } else if ((result.command == Message.Command.Diagnistic.getValue()) && turningOff) {
                            settingCommand = InmotionAdapterV2.Message.wheelOffSecondStage().writeBuffer();
                            turningOff = false;
                            settingCommandReady = true;
                            return false;
                        }
                    } else if (result.flags == Message.Flag.Default.getValue()) {
                        if (result.command == Message.Command.Settings.getValue()) {
                            requestSettings = false;
                            if (getInstance().getModel() == Model.V12HS || getInstance().getModel() == Model.V12HT ||
                                    getInstance().getModel() == Model.V12PRO) {
                                return result.parseSettingsV12();
                            } else if (getInstance().getModel() == Model.V13 || getInstance().getModel() == Model.V13PRO) {
                                return result.parseSettingsV13();
                            } else if (getInstance().getModel() == Model.V14s || getInstance().getModel() == Model.V14g) {
                                return result.parseSettingsV14();
                            } else if (getInstance().getModel() == Model.V11) {
                                return result.parseSettingsV11();
                            } else if (getInstance().getModel() == Model.V11Y) {
                                return result.parseSettingsV11y();
                            } else if (getInstance().getModel() == Model.V9) {
                                return result.parseSettingsV9();
                            } else if (getInstance().getModel() == Model.V12S) {
                                return result.parseSettingsV12S();
                            } else {
                                return false;
                            }
                        } else if (result.command == Message.Command.Diagnistic.getValue()) {
                            return result.parseDiagnostic();
                        } else if (result.command == Message.Command.BatteryRealTimeInfo.getValue()) {
                            return result.parseBatteryRealTimeInfo();
                        } else if (result.command == Message.Command.TotalStats.getValue()) {
                            return result.parseTotalStats();
                        } else if (result.command == Message.Command.RealTimeInfo.getValue()) {
                            if (getInstance().getModel() == Model.V12HS || getInstance().getModel() == Model.V12HT || getInstance().getModel() == Model.V12PRO) {
                                return result.parseRealTimeInfoV12(getContext());
                            } else if (getInstance().getModel() == Model.V13 || getInstance().getModel() == Model.V13PRO) {
                                return result.parseRealTimeInfoV13(getContext());
                            } else if (getInstance().getModel() == Model.V14s || getInstance().getModel() == Model.V14g) {
                                return result.parseRealTimeInfoV14(getContext());
                            } else if (getInstance().getModel() == Model.V11Y) {
                                return result.parseRealTimeInfoV11y(getContext());
                            } else if (getInstance().getModel() == Model.V9) {
                                return result.parseRealTimeInfoV9(getContext());
                            } else if (getInstance().getModel() == Model.V12S) {
                                return result.parseRealTimeInfoV12S(getContext());
                            } else if (protoVer < 2) {
                                return result.parseRealTimeInfoV11(getContext());
                            } else {
                                return result.parseRealTimeInfoV11_1_4(getContext());
                            }
                        } else {
                            Timber.i("Get unknown command: %02X", result.command);
                        }
                    }
                }
            }
        }
        return false;
    }


    public enum Model {
        V11(61,  "Inmotion V11"),
        V11Y(62,  "Inmotion V11y"),
        V12HS(71, "Inmotion V12 HS"),
        V12HT(72, "Inmotion V12 HT"),
        V12PRO(73, "Inmotion V12 PRO"),
        V13(81, "Inmotion V13"),
        V13PRO(82, "Inmotion V13 PRO"),
        V14g(91, "Inmotion V14 50GB"),
        V14s(92, "Inmotion V14 50S"),
        V12S(111, "Inmotion V12S"),
        V9(121, "Inmotion V9"),
        UNKNOWN(0,"Inmotion Unknown");


        private final int value;
        private final String name;

        Model(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }
        public String getName() {
            return name;
        }

        public static Model findById(int id, int type) {
            Timber.i("Model %d, %d", id, type);
            int id_full = id * 10 + type;
            for (Model m : Model.values()) {
                if (m.getValue() == id_full) return m;
            }
            return Model.UNKNOWN;
        }
    }

    @Override
    public boolean isReady() {
        return mModel != Model.UNKNOWN && protoVer != 0;
    }

    public int getMaxSpeed() {
        switch (mModel) {
            case V11:
                return 60;
            case V11Y:
                return 120;
            case V12HS:
                return 70;
            case V12HT:
                return 70;
            case V12PRO:
                return 70;
            case V13:
                return 120;
            case V13PRO:
                return 120;
            case V14s:
                return 120;
            case V14g:
                return 120;
            case V9:
                return 120;
            case V12S:
                return 120;
        }
        return 100;
    }

    public void setModel(Model value){
        mModel = value;
    }
    public void setProto(int value){ // for tests
        protoVer = value;
    }
    public int getProto(){ // for pwm dialog
        return protoVer;
    }

    public Model getModel(){
        return mModel;
    }

    public static InmotionAdapterV2 getInstance() {
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new InmotionAdapterV2();
        }
        Timber.i("Get instance");
        return INSTANCE;

    }

	public void startKeepAliveTimer() {
        updateStep = 0;
        stateCon = 0;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (updateStep == 0) {
                    if (stateCon == 0) {
                        if (WheelData.getInstance().bluetoothCmd(Message.getCarType().writeBuffer())) {
                            Timber.i("Sent car type message");
                        } else updateStep = 35;

                    } else if (stateCon == 1) {
                        if (WheelData.getInstance().bluetoothCmd(Message.getSerialNumber().writeBuffer())) {
                            Timber.i("Sent s/n message");
                        } else updateStep = 35;

                    } else if (stateCon == 2) {
                        if (WheelData.getInstance().bluetoothCmd(Message.getVersions().writeBuffer())) {
                            stateCon += 1;
                            Timber.i("Sent versions message");
                        } else updateStep = 35;

                    } else if (settingCommandReady) {
    					if (WheelData.getInstance().bluetoothCmd(settingCommand)) {
                            settingCommandReady = false;
                            requestSettings = true;
                            Timber.i("Sent command message");
                        } else updateStep = 35; // after +1 and %10 = 0
    				} else if (stateCon == 3 | requestSettings) {
                        if (WheelData.getInstance().bluetoothCmd(Message.getCurrentSettings().writeBuffer())) {
                            stateCon += 1;
                            Timber.i("Sent get settings data message");
                        } else updateStep = 35;

                    }
                    else if (stateCon == 4) {
                        if (WheelData.getInstance().bluetoothCmd(Message.getUselessData().writeBuffer())) {
                            Timber.i("Sent useless data message");
                            stateCon += 1;
                        } else updateStep = 35;

                    }
                    else if (stateCon == 5) {
                        if (WheelData.getInstance().bluetoothCmd(Message.getStatistics().writeBuffer())) {
                            Timber.i("Sent statistics data message");
                            stateCon += 1;
                        } else updateStep = 35;

                    }
                    else  {
                        if (WheelData.getInstance().bluetoothCmd(InmotionAdapterV2.Message.getRealTimeData().writeBuffer())) {
                            Timber.i("Sent realtime data message");
                            stateCon = 5;
                        } else updateStep = 35;

                    }


				}
                updateStep += 1;
                updateStep %= 10;
                Timber.i("Step: %d", updateStep);
            }
        };
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 100, 25);
    }

    @Override
    public void wheelBeep() {
        if (getModel() == Model.V13 || getModel() == Model.V13PRO || getModel() == Model.V14g || getModel() == Model.V14s || getModel() == Model.V11Y) {
            settingCommand = InmotionAdapterV2.Message.playBeep(0x02).writeBuffer();
        } else {
            settingCommand = InmotionAdapterV2.Message.playSound(0x18).writeBuffer();
        }
        settingCommandReady = true;
    }

    @Override
    public void switchFlashlight() {
        if (getModel() == Model.V12HS || getModel() == Model.V12HT || getModel() == Model.V12PRO) {
            boolean lowBeam = appConfig.getLowBeamEnabled();
            boolean highBeam = appConfig.getHighBeamEnabled();
            if (lowBeam && highBeam) {
                highBeam = !highBeam;
            } else if (lowBeam && !highBeam) {
                lowBeam = !lowBeam;
            } else {
                lowBeam = true;
                highBeam = true;
            }
            appConfig.setLowBeamEnabled(lowBeam);
            appConfig.setHighBeamEnabled(highBeam);
            setBeamState(lowBeam, highBeam);
        } else {
            boolean light = !appConfig.getLightEnabled();
            appConfig.setLightEnabled(light);
            setLightState(light);
        }
    }

    @Override
    public void setLightState(final boolean lightEnable) {
        settingCommand = InmotionAdapterV2.Message.setLight(lightEnable).writeBuffer();
        settingCommandReady = true;
    }


    public void setAutoLight(final boolean autoLightEnable) {
        settingCommand = InmotionAdapterV2.Message.setAutoLight(autoLightEnable).writeBuffer();
        settingCommandReady = true;
    }


    public void setSoundWave(final boolean soundWave) {
        settingCommand = InmotionAdapterV2.Message.setSoundWave(soundWave).writeBuffer();
        settingCommandReady = true;
    }


    public void setBeamState(final boolean lowBeamEnable, final boolean highBeamEnable) {
        settingCommand = InmotionAdapterV2.Message.setLightV12(lowBeamEnable, highBeamEnable).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setHandleButtonState(final boolean handleButtonEnable) {
        settingCommand = InmotionAdapterV2.Message.setHandleButton(handleButtonEnable).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setRideMode(final boolean rideMode) {
        settingCommand = InmotionAdapterV2.Message.setClassicMode(rideMode).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setSpeakerVolume(final int speakerVolume) {
        settingCommand = InmotionAdapterV2.Message.setVolume(speakerVolume).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setPedalTilt(final int angle) {
        settingCommand = InmotionAdapterV2.Message.setPedalTilt(angle).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setPedalSensivity(final int sensivity) {
        settingCommand = InmotionAdapterV2.Message.setPedalSensivity(sensivity).writeBuffer();
        settingCommandReady = true;
    }

    public void setStandbyDelay(final int delay) {
        settingCommand = InmotionAdapterV2.Message.setStanbyDelay(delay).writeBuffer();
        settingCommandReady = true;
    }


    public void setSplitModeConf(final int accel, final int breaksens) {
        settingCommand = InmotionAdapterV2.Message.setSplitAccelBreak(accel, breaksens).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void wheelCalibration() {
        if (getInstance().getModel()==Model.V11 && protoVer < 2) {
            settingCommand = InmotionAdapterV2.Message.wheelCalibration().writeBuffer();
        } else {
            settingCommand = InmotionAdapterV2.Message.wheelCalibrationTurn().writeBuffer();
        }
        settingCommandReady = true;
    }


    public void wheelCalibrationBalance() {
        settingCommand = InmotionAdapterV2.Message.wheelCalibrationBalance().writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setLockMode(final boolean lockMode) {
        settingCommand = InmotionAdapterV2.Message.setLock(lockMode).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setTransportMode(final boolean transportMode) {
        settingCommand = InmotionAdapterV2.Message.setTransportMode(transportMode).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setDrl(final boolean drl) {
        settingCommand = InmotionAdapterV2.Message.setDrl(drl).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setGoHomeMode(final boolean goHomeMode) {
        settingCommand = InmotionAdapterV2.Message.setGoHome(goHomeMode).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setFancierMode(final boolean fancierMode) {
        settingCommand = InmotionAdapterV2.Message.setFancierMode(fancierMode).writeBuffer();
        settingCommandReady = true;
    }

    public void setSplitMode(final boolean splitMode) {
        settingCommand = InmotionAdapterV2.Message.setSplitMode(splitMode).writeBuffer();
        settingCommandReady = true;
    }

    public void setBermAngleMode(final boolean bermAngleMode) {
        settingCommand = InmotionAdapterV2.Message.setBermAngleMode(bermAngleMode).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setMute(final boolean mute) {
        settingCommand = InmotionAdapterV2.Message.setMute(mute).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setFanQuiet(final boolean fanQuiet) {
        settingCommand = InmotionAdapterV2.Message.setQuietMode(fanQuiet).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setFan(final boolean fan) {
        settingCommand = InmotionAdapterV2.Message.setFan(fan).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setLightBrightness(final int lightBrightness) {
        settingCommand = InmotionAdapterV2.Message.setLightBrightness(lightBrightness).writeBuffer();
        settingCommandReady = true;
    }


    public void setBeamBrightness(final int lowBeamBrightness, final int highBeamBrightness) {
        settingCommand = InmotionAdapterV2.Message.setLightBrightnessV12(lowBeamBrightness, highBeamBrightness).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void updateMaxSpeed(final int maxSpeed) {
        if (getModel() == Model.V14g || getModel() == Model.V14s || getModel()== Model.V13 ||
                getModel()== Model.V13PRO || getModel()== Model.V11Y || getModel()== Model.V9
                || getModel()== Model.V12S) {
            settingCommand = InmotionAdapterV2.Message.setMaxSpeedV14(maxSpeed, 0).writeBuffer();
        } else {
            settingCommand = InmotionAdapterV2.Message.setMaxSpeed(maxSpeed).writeBuffer();
        }
        settingCommandReady = true;
    }


    public void updateAlarmSpeed(final int alarm1Speed, final int alarm2Speed, final int maxSpeed) {
        if (getModel() == Model.V11 || getModel() == Model.V12HS || getModel() == Model.V12HT || getModel() == Model.V12PRO) {
            settingCommand = InmotionAdapterV2.Message.setAlarmSpeedV12(alarm1Speed, alarm2Speed).writeBuffer();
        } else if (getModel() == Model.V14g || getModel() == Model.V14s || getModel() == Model.V13 ||
                getModel() == Model.V13PRO || getModel() == Model.V11Y || getModel() == Model.V9 || getModel()== Model.V12S) {
            settingCommand = InmotionAdapterV2.Message.setMaxSpeedV14(maxSpeed, alarm1Speed).writeBuffer();
        } //else { //  not confirmed that such command exists at all
            //settingCommand = InmotionAdapterV2.Message.setAlarmSpeed(alarm1Speed).writeBuffer();
        //}
        settingCommandReady = true;
    }

    @Override
    public void powerOff() {
        settingCommand = InmotionAdapterV2.Message.wheelOffFirstStage().writeBuffer();
        turningOff = true;
        settingCommandReady = true;
    }


    public static class Message {
        private final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);

        enum Flag {
            NoOp(0),
            Initial(0x11),
            Default(0x14);

            private final int value;

            Flag(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum Command {
            NoOp(0),
            MainVersion(0x01),
            MainInfo(0x02),
            Diagnistic(0x03),
            RealTimeInfo(0x04),
            BatteryRealTimeInfo(0x05),
            Something1(0x10),
            TotalStats(0x11),
            Settings(0x20),
            Control(0x60);


            private final int value;

            Command(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        int flags = Flag.NoOp.getValue();
        int len = 0;
        int command = 0;
        byte[] data;

        Message(byte[] bArr) {
            if (bArr.length < 5) return;
            flags = bArr[2];
            len = bArr[3];
            command = bArr[4] & 0x7F;
            if (len > 1) {
                data = Arrays.copyOfRange(bArr, 5, len+4);
            }
        }

        private Message() {

        }

        boolean parseMainData(){
            Timber.i("Parse main data");
            WheelData wd = WheelData.getInstance();
            wd.resetRideTime();
            if ((data[0] == (byte) 0x01) && len >= 6) {
                stateCon += 1;
                Timber.i("Parse car type");
                // 020601010100 -v11
                // 020701010100 -v12
                // 020801010100 -v13
                int mainSeries = data[1]; //02
                int series = data[2];    // 06
                int type = data[3];      // 01
                int batch = data[4];     // 02
                int feature = data[5];   // 01
                int reverse = data[6];   // 00
                getInstance().setModel(Model.findById(series, type));
                wd.setModel(mModel.getName());
                wd.setVersion(String.format(Locale.ENGLISH,"-")); // need to find how to parse
            } else if ((data[0] == (byte) 0x02) && len >= 17) {
                stateCon += 1;
                Timber.i("Parse serial num");
                String serialNumber;
                serialNumber = new String(data, 1, 16);

                wd.setSerial(serialNumber);
            } else if ((data[0] == (byte) 0x06) && len >= 24) {
                Timber.i("Parse versions");
                protoVer = 0;
                int DriverBoard3 = MathsUtil.shortFromBytesLE(data, 2);
                int DriverBoard2 = data[4];
                int DriverBoard1 = data[5];
                String DriverBoard = String.format(Locale.US, "%d.%d.%d",DriverBoard1, DriverBoard2, DriverBoard3);
                int smth13 = MathsUtil.shortFromBytesLE(data, 6);
                int smth12 = data[8];
                int smth11 = data[9];
                String smth1 = String.format(Locale.US, "%d.%d.%d",smth11, smth12, smth13);

                int MainBoard3 = MathsUtil.shortFromBytesLE(data, 11);
                int MainBoard2 = data[13];
                int MainBoard1 = data[14];
                String MainBoard = String.format(Locale.US, "%d.%d.%d",MainBoard1, MainBoard2, MainBoard3);

                int smth23 = MathsUtil.shortFromBytesLE(data, 16);
                int smth22 = data[18];
                int smth21 = data[19];
                String smth2 = String.format(Locale.US, "%d.%d.%d",smth21, smth22, smth23);

                int Ble3 = MathsUtil.shortFromBytesLE(data, 20);
                int Ble2 = data[22];
                int Ble1 = data[23];
                String Ble = String.format(Locale.US, "%d.%d.%d",Ble1, Ble2, Ble3);

                int smth33 = MathsUtil.shortFromBytesLE(data, 16);
                int smth32 = data[18];
                int smth31 = data[19];
                String smth3 = String.format(Locale.US, "%d.%d.%d",smth31, smth32, smth33);

                String vers = String.format(Locale.US, "Main:%s Drv:%s BLE:%s",MainBoard, DriverBoard, Ble);
                wd.setVersion(vers);
                if (mModel == Model.V11) {
                    if ((MainBoard1 < 2) && (MainBoard2 < 4)) { // main board ver before 1.4
                        protoVer = 1;
                    } else protoVer = 2; // main board 1.4+
                }
            }
            return false;
        }

        boolean parseBatteryRealTimeInfo(){
            int bat1Voltage = MathsUtil.shortFromBytesLE(data, 0);
            int bat1Temp = data[4];
            int bat1ValidStatus = data[5] & 1;
            int bat1Enabled = (data[5] >> 1) & 1;
            int bat1WorkStatus1 = data[6] & 1;
            int bat1WorkStatus2 = (data[6] >> 1) & 1;
            int bat2Voltage = MathsUtil.shortFromBytesLE(data, 8);
            int bat2Temp = data[12];
            int bat2ValidStatus = data[13] & 1;
            int bat2Enabled = (data[13] >> 1) & 1;
            int bat2WorkStatus1 = data[14] & 1;
            int bat2WorkStatus2 = (data[14] >> 1) & 1;
            int chargeVoltage = MathsUtil.shortFromBytesLE(data, 16);
            int chargeCurrent = MathsUtil.shortFromBytesLE(data, 18);
            return false;
        }
        
        boolean parseDiagnostic(){
            boolean ok = true;
            if (data.length > 7)
                for (byte c : data) {
                    if (c != 0) {
                        ok = false;
                        break;
                    }
                }
            return false;
        }

        boolean parseSettingsV11(){
            Timber.i("Parse settings data v11");
            int i = 1;
            int mSpeedLim = MathsUtil.shortFromBytesLE(data, i);
            int mPitchAngleZero = MathsUtil.signedShortFromBytesLE(data, i+2);
            int mDriveMode = data[i+4] & 0xF;
            int mRideMode = data[i+4] >> 4;
            int mComfSens = data[i + 5];
            int mClassSens = data[i + 6];
            int mVolume = data[i + 7];
            int mAudioId = MathsUtil.intFromBytesLE(data, i+8);
            int mStandByTime = MathsUtil.shortFromBytesLE(data, i+12);
            int mDecorLightMode = data[i + 14];
            int mAutoLightLowThr = data[i + 15];
            int mAutoLightHighThr = data[i + 16];
            int mLightBr = data[i + 17];
            int mSplitAccel = data[i + 18];
            int mSplitBreak = data[i + 19];
            int mAudioState = data[i + 20] & 3;
            int mDecorState = (data[i + 20]>>2) & 3;
            int mLiftedState = (data[i + 20] >> 4) & 3;
            int mAutoLightState = (data[i + 20] >> 6) & 3;
            int mAutoLightBrState = data[i + 21] & 3;
            int mLockState = (data[i + 21]>>2) & 3;
            int mTranspMode = (data[i + 21] >> 4) & 3;
            int mLoadDetect = (data[i + 21] >> 6) & 3;
            int mNoLoadDetect = data[i + 22] & 3;
            int mLowBat = (data[i + 22]>>2) & 3;
            int mFanQuiet = (data[i + 22] >> 4) & 3;
            int mFan = (data[i + 22] >> 6) & 3; // to test
            int mSome1 = data[i + 23] & 3; // to test
            int mSome2 = (data[i + 23]>>2) & 3; // to test
            int mSome3 = (data[i + 23] >> 4) & 3; // to test
            int mSome4 = (data[i + 23] >> 6) & 3; // to test
            int mSplitMode = 0;
            if (data.length > 25) {
                mSplitMode = data[i + 24] & 1;
            }
            int sens = mComfSens;
            if (mRideMode != 0) sens = mClassSens;
            final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
            appConfig.setPedalsAdjustment(mPitchAngleZero/10);
            appConfig.setWheelMaxSpeed(mSpeedLim/100);
            appConfig.setFancierMode(mRideMode != 0);
            appConfig.setRideMode(mDriveMode != 0);
            appConfig.setPedalSensivity(sens);
            appConfig.setSpeakerVolume(mVolume);
            appConfig.setLightBrightness(mLightBr);
            appConfig.setSpeakerMute(mAudioState == 0);
            appConfig.setDrlEnabled(mDecorState != 0);
            appConfig.setHandleButtonDisabled(mLiftedState == 0);
            appConfig.setLockMode(mLockState != 0);
            appConfig.setTransportMode(mTranspMode != 0);
            appConfig.setFanQuietEnabled(mFanQuiet != 0);
            appConfig.setGoHomeMode(mLowBat != 0);
            appConfig.setSplitMode(mSplitMode != 0);
            appConfig.setSplitModeAccel(mSplitAccel);
            appConfig.setSplitModeBreak(mSplitBreak);
            appConfig.setStandbyDelay(mStandByTime/60);
            return false;
        }

        boolean parseSettingsV12(){
            Timber.i("Parse settings data v12");
            int i = 1;
            /* taken from reverse engineering of InmoApp, seems not to be precise
            int mSpeedLim = MathsUtil.shortFromBytesLE(data, i);
            int mPitchAngleZero = MathsUtil.signedShortFromBytesLE(data, i+2);
            int mDriveMode = data[i+4] & 0xF;
            int mRideMode = data[i+4] >> 4;
            int mComfSens = data[i + 5];
            int mClassSens = data[i + 6];
            int mVolume = data[i + 7];
            int mAudioId = MathsUtil.intFromBytesLE(data, i+8);
            int mStandByTime = MathsUtil.shortFromBytesLE(data, i+12);
            int mDecorLightMode = data[i + 14];
            int mAutoLightLowThr = data[i + 15];
            int mAutoLightHighThr = data[i + 16];
            int mLightBr = data[i + 17];
            int mAudioState = data[i + 20] & 3;
            int mDecorState = (data[i + 20]>>2) & 3;
            int mLiftedState = (data[i + 20] >> 4) & 3;
            int mAutoLightState = (data[i + 20] >> 6) & 3;
            int mAutoLightBrState = data[i + 21] & 3;
            int mLockState = (data[i + 21]>>2) & 3;
            int mTranspMode = (data[i + 21] >> 4) & 3;
            int mLoadDetect = (data[i + 21] >> 6) & 3;
            int mNoLoadDetect = data[i + 22] & 3;
            int mLowBat = (data[i + 22]>>2) & 3;
            int mFanQuiet = (data[i + 22] >> 4) & 3;
            int mFan = (data[i + 22] >> 6) & 3; // to test
            int mSome1 = data[i + 23] & 3; // to test
            int mSome2 = (data[i + 23]>>2) & 3; // to test
            int mSome3 = (data[i + 23] >> 4) & 3; // to test
            int mSome4 = (data[i + 23] >> 6) & 3; // to test

             */
            int mSpeedLim = MathsUtil.shortFromBytesLE(data, 9);
            int mAlarmSpeed1 = MathsUtil.signedShortFromBytesLE(data, 11);
            int mAlarmSpeed2 = MathsUtil.signedShortFromBytesLE(data, 13);
            int mPedalsAdjustment = MathsUtil.signedShortFromBytesLE(data, 15);
            int mStandByDelay = MathsUtil.shortFromBytesLE(data, 17);
            int mClassicMode = data[19] & 1;
            int mFancierMode = (data[19] >> 4) & 1;
            int mPedalsComfSens = data[20];
            int mPedalsClassicSens = data[21];
            int mVolume = data[22];
            int mLowBeamBrightness = data[26];
            int mHighBeamBrightness = data[27];
            int mSplitAccel = data[31];
            int mSplitBreak = data[32];
            int mMute = data[39] & 1;
            int mHandleButton = (data[39] >> 2) & 1;
            int mAutoLight = (data[39] >> 3) & 1;
            int mTransportMode = (data[39] >> 6) & 1;
            int mSoundWave = (data[40] >> 2) & 1;
            int mSplitMode = data[41] & 1;
            int sens = mPedalsComfSens;
            if (mClassicMode != 0) sens = mPedalsClassicSens;
            final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
            appConfig.setPedalsAdjustment(mPedalsAdjustment/10);
            appConfig.setWheelMaxSpeed(mSpeedLim/100);
            appConfig.setFancierMode(mFancierMode != 0);
            appConfig.setRideMode(mClassicMode != 0);
            appConfig.setPedalSensivity(sens);
            appConfig.setSpeakerVolume(mVolume);
            appConfig.setLowBeamBrightness(mLowBeamBrightness);
            appConfig.setHighBeamBrightness(mHighBeamBrightness);
            appConfig.setSpeakerMute(mMute == 0);
            appConfig.setWheelAlarm1Speed(mAlarmSpeed1/100);
            appConfig.setWheelAlarm2Speed(mAlarmSpeed2/100);
            //appConfig.setDrlEnabled(mDecorState != 0);
            appConfig.setHandleButtonDisabled(mHandleButton == 0);
            //appConfig.setLockMode(mLockState != 0);
            appConfig.setTransportMode(mTransportMode != 0);
            appConfig.setAutoLight(mAutoLight != 0);
            appConfig.setSoundWave(mSoundWave != 0);
            appConfig.setSplitMode(mSplitMode != 0);
            appConfig.setSplitModeAccel(mSplitAccel);
            appConfig.setSplitModeBreak(mSplitBreak);
            appConfig.setStandbyDelay(mStandByDelay/60);
            return false;
        }

        boolean parseSettingsV13(){
            Timber.i("Parse settings data v13");
            int i = 1;
            int mMaxSpeedLim = MathsUtil.shortFromBytesLE(data, i);
            int mAlarmSpeed1 = MathsUtil.shortFromBytesLE(data, i+2);
            int mPedalsAdjustment = MathsUtil.signedShortFromBytesLE(data, i+8);
            int mOffroadMode = data[i+10] & 1;
            int mFancierMode = (data[i+10]>>4) & 1; // not found
            int mComfSens = data[i+11];
            int mClassSens = data[i+12];
            int mSplitModeAccel = data[i+13]; // not tested
            int mSplitModeBreak = data[i+14]; // not tested
            int mStandByDelay = MathsUtil.shortFromBytesLE(data, i+20); // not tested
            int mDrl = (data[i+30] >> 2) & 1;
            int mMute = data[i+30] & 1;
            int mTransportMode = (data[i+31]>>4) & 1;
            int mSplitMode = (data[i+34] >> 2) & 1;
            int mBermAngleMode = (data[i+34]>>5) & 1;
            int sens = mComfSens;
            if (mOffroadMode != 0) sens = mClassSens;

            final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
            appConfig.setPedalsAdjustment(mPedalsAdjustment/10);
            appConfig.setWheelMaxSpeed(mMaxSpeedLim/100);
            appConfig.setFancierMode(mFancierMode != 0);
            appConfig.setRideMode(mOffroadMode != 0);
            appConfig.setPedalSensivity(sens);
            appConfig.setSpeakerMute(mMute == 0);
            appConfig.setDrlEnabled(mDrl != 0);
            //appConfig.setLockMode(mLockState != 0);
            appConfig.setTransportMode(mTransportMode != 0);
            appConfig.setSplitMode(mSplitMode != 0);
            appConfig.setSplitModeAccel(mSplitModeAccel);
            appConfig.setSplitModeBreak(mSplitModeBreak);
            appConfig.setBermAngleMode(mBermAngleMode != 0);
            appConfig.setStandbyDelay(mStandByDelay/60);
            appConfig.setWheelAlarm1Speed(mAlarmSpeed1/100);
            return false;
        }

        boolean parseSettingsV14(){
            Timber.i("Parse settings data v14");
            int i = 1;

            int mMaxSpeedLim = MathsUtil.shortFromBytesLE(data, i);
            int mAlarmSpeed1 = MathsUtil.shortFromBytesLE(data, i+2);
            int mPedalsAdjustment = MathsUtil.signedShortFromBytesLE(data, i+8);
            int mOffroadMode = data[i+10] & 1;
            int mFancierMode = (data[i+10]>>4) & 1;
            int mComfSens = data[i+11];
            int mClassSens = data[i+12];
            int mSplitModeAccel = data[i+13];
            int mSplitModeBreak = data[i+14];
            int mStandByDelay = MathsUtil.shortFromBytesLE(data, i+20);
            int mDrl = (data[i+30] >> 2) & 1;
            int mMute = data[i+30] & 1;
            int mTransportMode = (data[i+31]>>4) & 1;
            int mSplitMode = (data[i+34] >> 2) & 1;
            int mBermAngleMode = (data[i+34]>>5) & 1;
            int sens = mComfSens;
            if (mOffroadMode != 0) sens = mClassSens;

            final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
            appConfig.setPedalsAdjustment(mPedalsAdjustment/10);
            appConfig.setWheelMaxSpeed(mMaxSpeedLim/100);
            appConfig.setFancierMode(mFancierMode != 0);
            appConfig.setRideMode(mOffroadMode != 0);
            appConfig.setPedalSensivity(sens);
            appConfig.setSpeakerMute(mMute == 0);
            appConfig.setDrlEnabled(mDrl != 0);
            //appConfig.setLockMode(mLockState != 0);
            appConfig.setTransportMode(mTransportMode != 0);
            appConfig.setSplitMode(mSplitMode != 0);
            appConfig.setSplitModeAccel(mSplitModeAccel);
            appConfig.setSplitModeBreak(mSplitModeBreak);
            appConfig.setBermAngleMode(mBermAngleMode != 0);
            appConfig.setStandbyDelay(mStandByDelay/60);
            appConfig.setWheelAlarm1Speed(mAlarmSpeed1/100);
            return false;
        }

        boolean parseSettingsV11y(){
            Timber.i("Parse settings data v11y");
            int i = 1;

            int mMaxSpeedLim = MathsUtil.shortFromBytesLE(data, i);
            int mAlarmSpeed1 = MathsUtil.shortFromBytesLE(data, i+2);
            int mPedalsAdjustment = MathsUtil.signedShortFromBytesLE(data, i+8);
            int mOffroadMode = data[i+10] & 1;
            int mFancierMode = (data[i+10]>>4) & 1;
            int mComfSens = data[i+11];
            int mClassSens = data[i+12];
            int mSplitModeAccel = data[i+13];
            int mSplitModeBreak = data[i+14];
            int mStandByDelay = MathsUtil.shortFromBytesLE(data, i+20);
            int mDrl = (data[i+30] >> 2) & 1;
            int mMute = data[i+30] & 1;
            int mHandleButton = (data[i+30] >> 4) & 1;
            int mTransportMode = (data[i+31]>>4) & 1;
            int mGoHome = (data[i+32] >> 2) & 1;
            int mSplitMode = (data[i+34] >> 2) & 1;
            int mBermAngleMode = (data[i+34]>>5) & 1;
            int sens = mComfSens;
            if (mOffroadMode != 0) sens = mClassSens;

            final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
            appConfig.setPedalsAdjustment(mPedalsAdjustment/10);
            appConfig.setWheelMaxSpeed(mMaxSpeedLim/100);
            appConfig.setFancierMode(mFancierMode != 0);
            appConfig.setRideMode(mOffroadMode != 0);
            appConfig.setPedalSensivity(sens);
            appConfig.setSpeakerMute(mMute == 0);
            appConfig.setDrlEnabled(mDrl != 0);
            //appConfig.setLockMode(mLockState != 0);
            appConfig.setHandleButtonDisabled(mHandleButton == 0);
            appConfig.setGoHomeMode(mGoHome != 0);
            appConfig.setTransportMode(mTransportMode != 0);
            appConfig.setSplitMode(mSplitMode != 0);
            appConfig.setSplitModeAccel(mSplitModeAccel);
            appConfig.setSplitModeBreak(mSplitModeBreak);
            appConfig.setBermAngleMode(mBermAngleMode != 0);
            appConfig.setStandbyDelay(mStandByDelay/60);
            appConfig.setWheelAlarm1Speed(mAlarmSpeed1/100);
            return false;
        }

        boolean parseSettingsV9(){
            Timber.i("Parse settings data v9");
            int i = 1;

            int mMaxSpeedLim = MathsUtil.shortFromBytesLE(data, i);
            int mAlarmSpeed1 = MathsUtil.shortFromBytesLE(data, i+2);
            int mPedalsAdjustment = MathsUtil.signedShortFromBytesLE(data, i+8);
            int mOffroadMode = data[i+10] & 1;
            int mFancierMode = (data[i+10]>>4) & 1;
            int mComfSens = data[i+11];
            int mClassSens = data[i+12];
            int mSplitModeAccel = data[i+13];
            int mSplitModeBreak = data[i+14];
            int mStandByDelay = MathsUtil.shortFromBytesLE(data, i+20);
            int mDrl = (data[i+30] >> 2) & 1;
            int mMute = data[i+30] & 1;
            int mHandleButton = (data[i+30] >> 4) & 1;
            int mTransportMode = (data[i+31]>>4) & 1;
            int mGoHome = (data[i+32] >> 2) & 1;
            int mSplitMode = (data[i+34] >> 2) & 1;
            int mBermAngleMode = (data[i+34]>>5) & 1;
            int sens = mComfSens;
            if (mOffroadMode != 0) sens = mClassSens;

            final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
            appConfig.setPedalsAdjustment(mPedalsAdjustment/10);
            appConfig.setWheelMaxSpeed(mMaxSpeedLim/100);
            appConfig.setFancierMode(mFancierMode != 0);
            appConfig.setRideMode(mOffroadMode != 0);
            appConfig.setPedalSensivity(sens);
            appConfig.setSpeakerMute(mMute == 0);
            appConfig.setDrlEnabled(mDrl != 0);
            //appConfig.setLockMode(mLockState != 0);
            appConfig.setHandleButtonDisabled(mHandleButton == 0);
            appConfig.setGoHomeMode(mGoHome != 0);
            appConfig.setTransportMode(mTransportMode != 0);
            appConfig.setSplitMode(mSplitMode != 0);
            appConfig.setSplitModeAccel(mSplitModeAccel);
            appConfig.setSplitModeBreak(mSplitModeBreak);
            appConfig.setBermAngleMode(mBermAngleMode != 0);
            appConfig.setStandbyDelay(mStandByDelay/60);
            appConfig.setWheelAlarm1Speed(mAlarmSpeed1/100);
            return false;
        }

        boolean parseSettingsV12S(){
            Timber.i("Parse settings data v12s");
            int i = 1;

            int mMaxSpeedLim = MathsUtil.shortFromBytesLE(data, i);
            int mAlarmSpeed1 = MathsUtil.shortFromBytesLE(data, i+2);
            int mPedalsAdjustment = MathsUtil.signedShortFromBytesLE(data, i+8);
            int mOffroadMode = data[i+10] & 1;
            int mFancierMode = (data[i+10]>>4) & 1;
            int mComfSens = data[i+11];
            int mClassSens = data[i+12];
            int mSplitModeAccel = data[i+13];
            int mSplitModeBreak = data[i+14];
            int mStandByDelay = MathsUtil.shortFromBytesLE(data, i+20);
            int mDrl = (data[i+30] >> 2) & 1;
            int mMute = data[i+30] & 1;
            int mHandleButton = (data[i+30] >> 4) & 1;
            int mTransportMode = (data[i+31]>>4) & 1;
            int mGoHome = (data[i+32] >> 2) & 1;
            int mSplitMode = (data[i+34] >> 2) & 1;
            int mBermAngleMode = (data[i+34]>>5) & 1;
            int sens = mComfSens;
            if (mOffroadMode != 0) sens = mClassSens;

            final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
            appConfig.setPedalsAdjustment(mPedalsAdjustment/10);
            appConfig.setWheelMaxSpeed(mMaxSpeedLim/100);
            appConfig.setFancierMode(mFancierMode != 0);
            appConfig.setRideMode(mOffroadMode != 0);
            appConfig.setPedalSensivity(sens);
            appConfig.setSpeakerMute(mMute == 0);
            appConfig.setDrlEnabled(mDrl != 0);
            //appConfig.setLockMode(mLockState != 0);
            appConfig.setHandleButtonDisabled(mHandleButton == 0);
            appConfig.setGoHomeMode(mGoHome != 0);
            appConfig.setTransportMode(mTransportMode != 0);
            appConfig.setSplitMode(mSplitMode != 0);
            appConfig.setSplitModeAccel(mSplitModeAccel);
            appConfig.setSplitModeBreak(mSplitModeBreak);
            appConfig.setBermAngleMode(mBermAngleMode != 0);
            appConfig.setStandbyDelay(mStandByDelay/60);
            appConfig.setWheelAlarm1Speed(mAlarmSpeed1/100);
            return false;
        }


        boolean parseTotalStats() {
            if (data.length < 20) {
                return false;
            }
            Timber.i("Parse total stats data");
            WheelData wd = WheelData.getInstance();
            long mTotal = MathsUtil.intFromBytesLE(data, 0);
            long mTotal2 = MathsUtil.getInt4(data, 0);
            long mDissipation = MathsUtil.intFromBytesLE(data, 4);
            long mRecovery = MathsUtil.intFromBytesLE(data, 8);
            long mRideTime = MathsUtil.intFromBytesLE(data, 12);
            int sec = (int)(mRideTime % 60);
            int min = (int)((mRideTime / 60) % 60);
            int hour = (int) (mRideTime/ 3600);
            String mRideTimeStr = String.format("%d:%02d:%02d",hour,min,sec);
            long mPowerOnTime = MathsUtil.intFromBytesLE(data, 16);
            sec = (int)(mPowerOnTime % 60);
            min = (int)((mPowerOnTime / 60) % 60);
            hour = (int) (mPowerOnTime/ 3600);
            String mPowerOnTimeStr = String.format("%d:%02d:%02d",hour,min,sec);
            wd.setTotalDistance(mTotal*10);
            return false;
        }

        String getError(int i){
            String inmoError = "";
            if (((data[i])&0x01) == 1) inmoError += "err_iPhaseSensorState ";
            if (((data[i]>>1) & 0x01) == 1) inmoError += "err_iBusSensorState ";
            if (((data[i] >> 2) & 0x01)==1) inmoError += "err_motorHallState ";
            if (((data[i] >> 3) & 0x01)==1) inmoError += "err_batteryState ";
            if (((data[i] >> 4) & 0x01)==1) inmoError += "err_imuSensorState ";
            if (((data[i] >> 5) & 0x01)==1) inmoError += "err_controllerCom1State ";
            if (((data[i] >> 6) & 0x01)==1) inmoError += "err_controllerCom2State ";
            if (((data[i] >> 7) & 0x01)==1) inmoError += "err_bleCom1State ";
            if (((data[i+1]) & 0x01)==1) inmoError += "err_bleCom2State ";
            if (((data[i+1] >> 1) & 0x01)==1) inmoError += "err_mosTempSensorState ";
            if (((data[i+1] >> 2) & 0x01)==1) inmoError += "err_motorTempSensorState ";
            if (((data[i+1] >> 3) & 0x01)==1) inmoError += "err_batteryTempSensorState ";
            if (((data[i+1] >> 4) & 0x01)==1) inmoError += "err_boardTempSensorState ";
            if (((data[i+1] >> 5) & 0x01)==1) inmoError += "err_fanState ";
            if (((data[i+1] >> 6) & 0x01)==1) inmoError += "err_rtcState ";
            if (((data[i+1] >> 7) & 0x01)==1) inmoError += "err_externalRomState ";
            if (((data[i+2]) & 0x01)==1) inmoError += "err_vBusSensorState ";
            if (((data[i+2] >> 1) & 0x01)==1) inmoError += "err_vBatterySensorState ";
            if (((data[i+2] >> 2) & 0x01)==1) inmoError += "err_canNotPowerOffState";
            if (((data[i+2] >> 3) & 0x01)==1) inmoError += "err_notKnown1 ";
            if (((data[i+3]) & 0x01)==1) inmoError += "err_underVoltageState ";
            if (((data[i+3] >> 1) & 0x01)==1) inmoError += "err_overVoltageState ";
            if (((data[i+3] >> 2) & 0x03)>0) inmoError += "err_overBusCurrentState-" + String.valueOf((data[43] >> 2) & 0x03) + " ";
            if (((data[i+3] >> 4) & 0x03)>0) inmoError += "err_lowBatteryState-"+ String.valueOf((data[43] >> 4) & 0x03) + " ";
            if (((data[i+3] >> 6) & 0x01)==1) inmoError += "err_mosTempState ";
            if (((data[i+3] >> 7) & 0x01)==1) inmoError += "err_motorTempState ";
            if (((data[i+4]) & 0x01)==1) inmoError += "err_batteryTempState ";
            if (((data[i+4] >> 1) & 0x01)==1) inmoError += "err_overBoardTempState ";
            if (((data[i+4] >> 2) & 0x01)==1) inmoError += "err_overSpeedState ";
            if (((data[i+4] >> 3) & 0x01)==1) inmoError += "err_outputSaturationState ";
            if (((data[i+4] >> 4) & 0x01)==1) inmoError += "err_motorSpinState ";
            if (((data[i+4] >> 5) & 0x01)==1) inmoError += "err_motorBlockState ";
            if (((data[i+4] >> 6) & 0x01)==1) inmoError += "err_postureState ";
            if (((data[i+4] >> 7) & 0x01)==1) inmoError += "err_riskBehaviourState ";
            if (((data[i+5]) & 0x01)==1) inmoError += "err_motorNoLoadState ";
            if (((data[i+5] >> 1) & 0x01)==1) inmoError += "err_noSelfTestState ";
            if (((data[i+5] >> 2) & 0x01)==1) inmoError += "err_compatibilityState ";
            if (((data[i+5] >> 3) & 0x01)==1) inmoError += "err_powerKeyLongPressState ";
            if (((data[i+5] >> 4) & 0x01)==1) inmoError += "err_forceDfuState ";
            if (((data[i+5] >> 5) & 0x01)==1) inmoError += "err_deviceLockState ";
            if (((data[i+5] >> 6) & 0x01)==1) inmoError += "err_cpuOverTempState ";
            if (((data[i+5] >> 7) & 0x01)==1) inmoError += "err_imuOverTempState ";
            if (((data[i+6] >> 1) & 0x01)==1) inmoError += "err_hwCompatibilityState ";
            if (((data[i+6] >> 2) & 0x01)==1) inmoError += "err_fanLowSpeedState ";
            if (((data[i+6] >> 3) & 0x01)==1) inmoError += "err_notKnown2 ";

            return inmoError;
        }

        boolean parseRealTimeInfoV11(Context sContext) {
            Timber.i("Parse V11 realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 4);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 6);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 8);
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 10);
            int mMileage = MathsUtil.shortFromBytesLE(data, 12) * 10;
            int mRemainMileage = MathsUtil.shortFromBytesLE(data, 14) * 10;
            int mBatLevel = data[16] & 0x7f;
            int mBatMode = (data[16] >> 7)  & 0x1;
            int mMosTemp = (data[17] & 0xff) + 80 - 256;
            int mMotTemp = (data[18] & 0xff) + 80 - 256;
            int mBatTemp = (data[19] & 0xff) + 80 - 256;
            int mBoardTemp = (data[20] & 0xff) + 80 - 256;
            int mLampTemp = (data[21] & 0xff) + 80 - 256;
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 22);
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 24);
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 26);
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 28);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 30);
            int mBrightness = data[32]& 0xff;
            int mLightBrightness = data[33]& 0xff;
            int mCpuTemp = (data[34] & 0xff) + 80 - 256;
            int mImuTemp = (data[35] & 0xff) + 80 - 256;
            int mPwm = MathsUtil.shortFromBytesLE(data, 36);
            wd.setVoltage(mVoltage);
            wd.setTorque((double)mTorque/100.0);
            wd.setMotorPower(mMotPower);
            wd.setCpuTemp(mCpuTemp);
            wd.setImuTemp(mImuTemp);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setCurrentLimit((double)mDynamicCurrentLimit/100.0);
            wd.setSpeedLimit((double)mDynamicSpeedLimit/100.0);
            wd.setBatteryLevel(mBatLevel);
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mBoardTemp * 100);
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.setOutput(mPwm);
            wd.updatePwm();
            wd.setTopSpeed(mSpeed);
            wd.setPower(mBatPower * 100);
            wd.setWheelDistance(mMileage);
            //// state data
            int i = (data.length < 49) ? 36 : 38;

            int mPcMode = data[i] & 0x07; // lock, drive, shutdown, idle
            int mMcMode = (data[i]>>3)&0x07;
            int mMotState = (data[i]>>6)&0x01;
            int chrgState = (data[i]>>7)&0x01;
            int lightState = (data[i+1])&0x01;
            int decorLiState = (data[i+1] >> 1) & 0x01;
            int liftedState = (data[i+1]>>2)&0x01;
            int tailLiState = (data[i+1]>>3)&0x03;
            int fanState = (data[i+1]>>5)&0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + "Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            wd.setModeStr(wmode);
            //appConfig.setFanEnabled(fanState != 0); // bad behaviour

            if (appConfig.getLightEnabled() != (lightState == 1)) {
                if (lightSwitchCounter > 3) {
                    appConfig.setLightEnabled(lightState == 1); // bad behaviour
                    lightSwitchCounter = 0;
                } else lightSwitchCounter += 1;
            } else lightSwitchCounter = 0;
            
            //appConfig.setDrlEnabled(decorLiState != 0); // too fast, bad behaviour

            //// errors data
            String inmoError = getError(i+5);
            wd.setAlert(inmoError);
            if ((inmoError != "") && (sContext != null)) {
                Timber.i("News to send: %s, sending Intent", inmoError);
                Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                intent.putExtra(Constants.INTENT_EXTRA_NEWS, inmoError);
                sContext.sendBroadcast(intent);
            }
//            System.out.println(String.format(Locale.US,"\nVolt: %.2f, Amp: %.2f, Km/h: %.2f, N*m: %.2f, Bat Wt: %d, Mot Wt: %d, PWM: %.2f, PitchAim: %.2f, Pith: %.2f, Roll: %.2f, \nTrip Km: %.2f, Rem Km: %.3f, Bat: %.2f, Lim km/h: %.2f, Lim A: %.2f, \nMos t: %d, Mot t: %d, Bat t: %d, Board t: %d, CPU t: %d, IMU t: %d, Lamp t: %d",
//                    mVoltage/100.0, mCurrent/100.0, mSpeed/100.0, mTorque/100.0, mBatPower,mMotPower, mPwm/100.0, mPitchAimAngle/100.0, mPitchAngle/100.0,  mRollAngle/100.0, mMileage/10.0, mRemainMileage/1000.0, mBatLevel/100.0, mDynamicSpeedLimit/100.0, mDynamicCurrentLimit/100.0, mMosTemp, mMotTemp, mBatTemp, mBoardTemp, mCpuTemp, mImuTemp, mLampTemp));

//            if (!(wmode.equals("Active") || wmode.equals(""))) System.out.println(String.format(Locale.US,"State: %s", wmode));
//            if (!inmoError.equals("")) System.out.println(String.format(Locale.US,"Err: %s", inmoError));
            return true;
        }

        boolean parseRealTimeInfoV11_1_4(Context sContext) {
            Timber.i("Parse V11 1.4+ realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 4);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 6);
            int mPwm = MathsUtil.signedShortFromBytesLE(data, 8);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 10);
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 12);
            int mXz = MathsUtil.signedShortFromBytesLE(data, 14); // always 0
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 16);
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 18);
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 20);
            int mSomething1 = MathsUtil.shortFromBytesLE(data, 22);
            int mSomething2 = MathsUtil.shortFromBytesLE(data, 24);
            int mMileage = MathsUtil.shortFromBytesLE(data, 26) * 10;
            int mBatLevel = MathsUtil.shortFromBytesLE(data, 28);
            int mRemainMileage = MathsUtil.shortFromBytesLE(data, 30) * 10;
            int mSomeThing120 = MathsUtil.shortFromBytesLE(data, 32);
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 34);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 36);
            int mSomething3 = MathsUtil.shortFromBytesLE(data, 38);
            int mSomething4 = MathsUtil.shortFromBytesLE(data, 40);
            int mMosTemp = (data[42] & 0xff) + 80 - 256;
            int mMotTemp = (data[43] & 0xff) + 80 - 256;
            int mBatTemp = (data[44] & 0xff) + 80 - 256; // 0
            int mBoardTemp = (data[45] & 0xff) + 80 - 256;
            int mCpuTemp = (data[46] & 0xff) + 80 - 256;
            int mImuTemp = (data[47] & 0xff) + 80 - 256;
            int mLampTemp = (data[48] & 0xff) + 80 - 256; // 0

            int mBrightness = data[49]& 0xff;
            int mLightBrightness = data[50]& 0xff;
//            System.out.println(String.format(Locale.US,"\nVolt: %.2f, Amp: %.2f, Km/h: %.2f, N*m: %.2f, Bat Wt: %d, Mot Wt: %d, XZ: %d, PWM: %.2f, PitchAim: %.2f, Pith: %.2f, Roll: %.2f, \nTrip Km: %.2f, Rem Km: %.3f, Bat: %.2f, Something: %.2f, Lim km/h: %.2f, Lim A: %.2f, \nMos t: %d, Mot t: %d, Bat t: %d, Board t: %d, CPU t: %d, IMU t: %d, Lamp t: %d",
//                    mVoltage/100.0, mCurrent/100.0, mSpeed/100.0, mTorque/100.0, mBatPower,mMotPower, mXz, mPwm/100.0, mPitchAimAngle/100.0, mPitchAngle/100.0,  mRollAngle/100.0, mMileage/10.0, mRemainMileage/1000.0, mBatLevel/100.0, mSomeThing180/100.0, mDynamicSpeedLimit/100.0, mDynamicCurrentLimit/100.0, mMosTemp, mMotTemp, mBatTemp, mBoardTemp, mCpuTemp, mImuTemp, mLampTemp));
            wd.setVoltage(mVoltage);
            wd.setTorque((double)mTorque/100.0);
            wd.setMotorPower(mMotPower);
            wd.setCpuTemp(mCpuTemp);
            wd.setImuTemp(mImuTemp);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setCurrentLimit((double)mDynamicCurrentLimit/100.0);
            wd.setSpeedLimit((double)mDynamicSpeedLimit/100.0);
            wd.setBatteryLevel((int)Math.round(mBatLevel/100.0));
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mBoardTemp * 100);
            wd.setOutput(mPwm);
            wd.updatePwm();
            //wd.setMotorTemp(mMotTemp * 100); not existed in WD
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.setTopSpeed(mSpeed);
            wd.setPower(mBatPower * 100);
            wd.setWheelDistance(mMileage);
            //// state data
            int mPcMode = data[56] & 0x07; // lock, drive, shutdown, idle
            int mMcMode = (data[56]>>3)&0x07;
            int mMotState = (data[56]>>6)&0x01;
            int chrgState = (data[56]>>7)&0x01;
            int lowLightState = (data[57])&0x01;
            int highLightState = (data[57] >> 1) & 0x01;
            int liftedState = (data[57]>>2)&0x01;
            int tailLiState = (data[57]>>3)&0x03;
            int fwUpdateState = (data[57]>>5)&0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + "Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            //if (!(wmode.equals("Active") || wmode.equals(""))) System.out.println(String.format(Locale.US,"State: %s", wmode));
            wd.setModeStr(wmode);

            if (appConfig.getLightEnabled() != (lowLightState == 1)) {
                if (lightSwitchCounter > 3) {
                    //appConfig.setLightEnabled(lightState == 1); // bad behaviour
                    lightSwitchCounter = 0;
                } else lightSwitchCounter += 1;
            } else lightSwitchCounter = 0;

            //// errors data
            String inmoError = getError(61);
            //if (!inmoError.equals("")) System.out.println(String.format(Locale.US,"Err: %s", inmoError));
            wd.setAlert(inmoError);
            if ((inmoError != "") && (sContext != null)) {
                Timber.i("News to send: %s, sending Intent", inmoError);
                Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                intent.putExtra(Constants.INTENT_EXTRA_NEWS, inmoError);
                sContext.sendBroadcast(intent);
            }
            return true;
        }


        boolean parseRealTimeInfoV12(Context sContext) {
            Timber.i("Parse V12 realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 4);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 6);
            int mPwm = MathsUtil.signedShortFromBytesLE(data, 8);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 10);
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 12);
            int mXz = MathsUtil.signedShortFromBytesLE(data, 14); // always 0
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 16);
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 18);
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 20);
            int mMileage = MathsUtil.shortFromBytesLE(data, 22) * 10;
            int mBatLevel = MathsUtil.shortFromBytesLE(data, 24);
            int mRemainMileage = MathsUtil.shortFromBytesLE(data, 26) * 10;
            int mSomeThing180 = MathsUtil.shortFromBytesLE(data, 28); // always 18000
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 30);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 32);
            int mMosTemp = (data[40] & 0xff) + 80 - 256;
            int mMotTemp = (data[41] & 0xff) + 80 - 256;
            int mBatTemp = (data[42] & 0xff) + 80 - 256; // 0
            int mBoardTemp = (data[43] & 0xff) + 80 - 256;
            int mCpuTemp = (data[44] & 0xff) + 80 - 256;
            int mImuTemp = (data[45] & 0xff) + 80 - 256;
            int mLampTemp = (data[46] & 0xff) + 80 - 256; // 0
// don't remove
//            int mBrightness = data[48]& 0xff;
//            int mLightBrightness = data[49]& 0xff;
//            System.out.println(String.format(Locale.US,"\nVolt: %.2f, Amp: %.2f, Km/h: %.2f, N*m: %.2f, Bat Wt: %d, Mot Wt: %d, XZ: %d, PWM: %.2f, PitchAim: %.2f, Pith: %.2f, Roll: %.2f, \nTrip Km: %.2f, Rem Km: %.3f, Bat: %.2f, Something: %.2f, Lim km/h: %.2f, Lim A: %.2f, \nMos t: %d, Mot t: %d, Bat t: %d, Board t: %d, CPU t: %d, IMU t: %d, Lamp t: %d",
//                    mVoltage/100.0, mCurrent/100.0, mSpeed/100.0, mTorque/100.0, mBatPower,mMotPower, mXz, mPwm/100.0, mPitchAimAngle/100.0, mPitchAngle/100.0,  mRollAngle/100.0, mMileage/10.0, mRemainMileage/1000.0, mBatLevel/100.0, mSomeThing180/100.0, mDynamicSpeedLimit/100.0, mDynamicCurrentLimit/100.0, mMosTemp, mMotTemp, mBatTemp, mBoardTemp, mCpuTemp, mImuTemp, mLampTemp));
            wd.setVoltage(mVoltage);
            wd.setTorque((double)mTorque/100.0);
            wd.setMotorPower(mMotPower);
            wd.setCpuTemp(mCpuTemp);
            wd.setImuTemp(mImuTemp);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setCurrentLimit((double)mDynamicCurrentLimit/100.0);
            wd.setSpeedLimit((double)mDynamicSpeedLimit/100.0);
            wd.setBatteryLevel((int)Math.round(mBatLevel/100.0));
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mMotTemp * 100);
            wd.setOutput(mPwm);
            wd.updatePwm();
            //wd.setMotorTemp(mMotTemp * 100); not existed in WD
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.setTopSpeed(mSpeed);
            wd.setPower(mBatPower * 100);
            wd.setWheelDistance(mMileage);
            //// state data
            int mPcMode = data[54] & 0x07; // lock, drive, shutdown, idle
            int mMcMode = (data[54]>>3)&0x07;
            int mMotState = (data[54]>>6)&0x01;
            int chrgState = (data[54]>>7)&0x01;
            int lowLightState = (data[55])&0x01;
            int highLightState = (data[55] >> 1) & 0x01;
            int liftedState = (data[55]>>2)&0x01;
            int tailLiState = (data[55]>>3)&0x03;
            int fwUpdateState = (data[55]>>5)&0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + "Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            //if (!(wmode.equals("Active") || wmode.equals(""))) System.out.println(String.format(Locale.US,"State: %s", wmode));
            wd.setModeStr(wmode);

            appConfig.setLowBeamEnabled(lowLightState != 0);
            appConfig.setHighBeamEnabled(highLightState != 0);
            //if (appConfig.getLightEnabled() != (lowLightState == 1)) {
            //    if (lightSwitchCounter > 3) {
                    //appConfig.setLightEnabled(lightState == 1); // bad behaviour
            //        lightSwitchCounter = 0;
            //    } else lightSwitchCounter += 1;
            //} else lightSwitchCounter = 0;

            //// errors data
            String inmoError = getError(59);
            //if (!inmoError.equals("")) System.out.println(String.format(Locale.US,"Err: %s", inmoError));
            wd.setAlert(inmoError);
            if ((inmoError != "") && (sContext != null)) {
                Timber.i("News to send: %s, sending Intent", inmoError);
                Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                intent.putExtra(Constants.INTENT_EXTRA_NEWS, inmoError);
                sContext.sendBroadcast(intent);
            }
            return true;
        }

        boolean parseRealTimeInfoV13(Context sContext) {
            Timber.i("Parse V13 realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            //int mSpeed = MathsUtil.signedShortFromBytesLE(data, 4);
            int mSomeThing2 = MathsUtil.signedShortFromBytesLE(data, 4);
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 6); //not sure
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 8);
            //int mSomething0 = MathsUtil.signedShortFromBytesLE(data, 10);
            long mMileage = MathsUtil.intFromBytesRevLE(data, 10); // not sure
            int mPwm = MathsUtil.signedShortFromBytesLE(data, 14);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 16);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 18); // not sure
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 20); // not sure
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 22); // not sure
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 24); // not sure

            //int mRemainMileage = MathsUtil.shortFromBytesLE(data, 26) * 10;
            //int mSomeThing180 = MathsUtil.shortFromBytesLE(data, 28); // always 18000
            //int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 30);
            //int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 32);

            int mBatLevel1 = MathsUtil.shortFromBytesLE(data, 34);
            int mBatLevel2 = MathsUtil.shortFromBytesLE(data, 36);
            int mSomeThing200_1 = MathsUtil.shortFromBytesLE(data, 38);
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 40);
            int x5 = MathsUtil.shortFromBytesLE(data, 42);
            int x6 = MathsUtil.shortFromBytesLE(data, 44);
            int x7 = MathsUtil.shortFromBytesLE(data, 46);
            int mSomeThing200_2 = MathsUtil.shortFromBytesLE(data, 48);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 50);
            int mSomeThing380 = MathsUtil.shortFromBytesLE(data, 52);


            int mMosTemp = (data[58] & 0xff) + 80 - 256;
            int mMotTemp = (data[59] & 0xff) + 80 - 256;
            int mBatTemp = (data[60] & 0xff) + 80 - 256; // 0
            int mBoardTemp = (data[61] & 0xff) + 80 - 256;
            int mCpuTemp = (data[62] & 0xff) + 80 - 256;
            int mImuTemp = (data[63] & 0xff) + 80 - 256;
            int mLampTemp = (data[64] & 0xff) + 80 - 256; // 0

// don't remove
//            int mBrightness = data[48]& 0xff;
//            int mLightBrightness = data[49]& 0xff;
//            System.out.println(String.format(Locale.US,"\nVolt: %.2f, Amp: %.2f, Km/h: %.2f, N*m: %.2f, Bat Wt: %d, Mot Wt: %d, XZ: %d, PWM: %.2f, PitchAim: %.2f, Pith: %.2f, Roll: %.2f, \nTrip Km: %.2f, Rem Km: %.3f, Bat: %.2f, Something: %.2f, Lim km/h: %.2f, Lim A: %.2f, \nMos t: %d, Mot t: %d, Bat t: %d, Board t: %d, CPU t: %d, IMU t: %d, Lamp t: %d",
//                    mVoltage/100.0, mCurrent/100.0, mSpeed/100.0, mTorque/100.0, mBatPower,mMotPower, mXz, mPwm/100.0, mPitchAimAngle/100.0, mPitchAngle/100.0,  mRollAngle/100.0, mMileage/10.0, mRemainMileage/1000.0, mBatLevel/100.0, mSomeThing180/100.0, mDynamicSpeedLimit/100.0, mDynamicCurrentLimit/100.0, mMosTemp, mMotTemp, mBatTemp, mBoardTemp, mCpuTemp, mImuTemp, mLampTemp));
            wd.setVoltage(mVoltage);
            wd.setTorque((double)mTorque/100.0);
            wd.setMotorPower(mMotPower);
            wd.setCpuTemp(mCpuTemp);
            wd.setImuTemp(mImuTemp);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setCurrentLimit((double)mDynamicCurrentLimit/100.0);
            wd.setSpeedLimit((double)mDynamicSpeedLimit/100.0);
            wd.setBatteryLevel((int)Math.round((mBatLevel1 + mBatLevel2)/200.0));
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mMotTemp * 100);
            wd.setOutput(mPwm);
            wd.updatePwm();
            //wd.setMotorTemp(mMotTemp * 100); not existed in WD
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.setTopSpeed(mSpeed);
            wd.setPower(mBatPower * 100);
            wd.setWheelDistance(mMileage);
            //// state data
            int mPcMode = data[74] & 0x07; // lock, drive, shutdown, idle
            int mMcMode = (data[74]>>3)&0x07;
            int mMotState = (data[74]>>6)&0x01;
            int chrgState = (data[74]>>7)&0x01;
            int lowLightState = (data[75])&0x01;
            int highLightState = (data[75] >> 1) & 0x01;
            int liftedState = (data[75]>>2)&0x01;
            int tailLiState = (data[75]>>3)&0x03;
            int fwUpdateState = (data[75]>>5)&0x01;
            int lightState = (data[76] >> 1) & 0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + "Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            //if (!(wmode.equals("Active") || wmode.equals(""))) System.out.println(String.format(Locale.US,"State: %s", wmode));
            wd.setModeStr(wmode);

            if (appConfig.getLightEnabled() != (lightState == 1)) {
                if (lightSwitchCounter > 3) {
                    appConfig.setLightEnabled(lightState == 1); // bad behaviour
                    lightSwitchCounter = 0;
                } else lightSwitchCounter += 1;
            } else lightSwitchCounter = 0;

            //// errors data
            String inmoError = getError(76);
            if (!inmoError.equals("")) System.out.println(String.format(Locale.US,"Err: %s", inmoError));
            wd.setAlert(inmoError);
            /*
            if ((inmoError != "") && (sContext != null)) {
                Timber.i("News to send: %s, sending Intent", inmoError);
                Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                intent.putExtra(Constants.INTENT_EXTRA_NEWS, inmoError);
                sContext.sendBroadcast(intent);
            }
            */
            return true;
        }


        boolean parseRealTimeInfoV14(Context sContext) {
            Timber.i("Parse V14 realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            //int mSpeed = MathsUtil.signedShortFromBytesLE(data, 4);
            //int mSomeThing2 = MathsUtil.signedShortFromBytesLE(data, 4);
            //int mSomeThing183 = MathsUtil.signedShortFromBytesLE(data, 6); //not sure
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 8);
            int mSomeThing180 = MathsUtil.signedShortFromBytesLE(data, 10);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 12); // not sure
            int mPwm = MathsUtil.signedShortFromBytesLE(data, 14);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 16);
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 18); // not sure
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 20); // not sure
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 22); // not sure
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 24); // not sure

            int mSomeThing183 = MathsUtil.signedShortFromBytesLE(data, 26);
            int mMileage = MathsUtil.shortFromBytesLE(data, 28)*10; // always 18000
            int mSomeThing181 = MathsUtil.shortFromBytesLE(data, 30);
            int mSomeThing182 = MathsUtil.shortFromBytesLE(data, 32);

            int mBatLevel1 = MathsUtil.shortFromBytesLE(data, 34);
            int mBatLevel2 = MathsUtil.shortFromBytesLE(data, 36);
            int mSomeThing200_1 = MathsUtil.shortFromBytesLE(data, 38);
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 40);
            int x5 = MathsUtil.shortFromBytesLE(data, 42);
            int x6 = MathsUtil.shortFromBytesLE(data, 44);
            int x7 = MathsUtil.shortFromBytesLE(data, 46);
            int mSomeThing200_2 = MathsUtil.shortFromBytesLE(data, 48);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 50);
            int mSomeThing380 = MathsUtil.shortFromBytesLE(data, 52);


            int mMosTemp = (data[58] & 0xff) + 80 - 256;
            int mMotTemp = (data[59] & 0xff) + 80 - 256;
            int mBatTemp = (data[60] & 0xff) + 80 - 256; // 0
            int mBoardTemp = (data[61] & 0xff) + 80 - 256;
            int mCpuTemp = (data[62] & 0xff) + 80 - 256;
            int mImuTemp = (data[63] & 0xff) + 80 - 256;
            int mLampTemp = (data[64] & 0xff) + 80 - 256; // 0

// don't remove
//            int mBrightness = data[48]& 0xff;
//            int mLightBrightness = data[49]& 0xff;
//            System.out.println(String.format(Locale.US,"\nVolt: %.2f, Amp: %.2f, Km/h: %.2f, N*m: %.2f, Bat Wt: %d, Mot Wt: %d, XZ: %d, PWM: %.2f, PitchAim: %.2f, Pith: %.2f, Roll: %.2f, \nTrip Km: %.2f, Bat1: %.2f, Bat2: %.2f, Something: %.2f, Lim km/h: %.2f, Lim A: %.2f, \nMos t: %d, Mot t: %d, Bat t: %d, Board t: %d, CPU t: %d, IMU t: %d, Lamp t: %d",
//                    mVoltage/100.0, mCurrent/100.0, mSpeed/100.0, mTorque/100.0, mBatPower,mMotPower, x5, mPwm/100.0, mPitchAimAngle/100.0, mPitchAngle/100.0,  mRollAngle/100.0, mMileage/1.0,  mBatLevel1/100.0, mBatLevel2/100.0, mSomeThing180/100.0, mDynamicSpeedLimit/100.0, mDynamicCurrentLimit/100.0, mMosTemp, mMotTemp, mBatTemp, mBoardTemp, mCpuTemp, mImuTemp, mLampTemp));
//            System.out.println(String.format(Locale.US,"mSomeThing183: %.2f, mPitchAngle: %.2f, mRollAngle: %.2f, X5: %d, X6: %d,X7: %d, mSomeThing380: %.2f",
//                     mSomeThing183/100.0, mPitchAngle/100.0,mRollAngle/100.0, x5,x6,x7, mSomeThing380/100.0));


            wd.setVoltage(mVoltage);
            wd.setTorque((double)mTorque/100.0);
            wd.setMotorPower(mMotPower);
            wd.setCpuTemp(mCpuTemp);
            wd.setImuTemp(mImuTemp);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setCurrentLimit((double)mDynamicCurrentLimit/100.0);
            wd.setSpeedLimit((double)mDynamicSpeedLimit/100.0);
            wd.setBatteryLevel((int)Math.round((mBatLevel1 + mBatLevel2)/200.0));
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mMotTemp * 100);
            wd.setOutput(mPwm);
            wd.updatePwm();
            //wd.setMotorTemp(mMotTemp * 100); not existed in WD
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.setTopSpeed(mSpeed);
            wd.setPower(mBatPower * 100);
            wd.setWheelDistance(mMileage);
            //// state data
            int mPcMode = data[74] & 0x07; // lock, drive, shutdown, idle
            int mMcMode = (data[74]>>3)&0x07;
            int mMotState = (data[74]>>6)&0x01;
            int chrgState = (data[74]>>7)&0x01;
            //int lowLightState = (data[76])&0x01;
            int lightState = (data[76] >> 1) & 0x01;
            int liftedState = (data[76]>>2)&0x01;
            int tailLiState = (data[76]>>4)&0x01;
            int fwUpdateState = (data[76]>>5)&0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + "Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            //if (!(wmode.equals("Active") || wmode.equals(""))) System.out.println(String.format(Locale.US,"State: %s", wmode));
            wd.setModeStr(wmode);

            if (appConfig.getLightEnabled() != (lightState == 1)) {
                if (lightSwitchCounter > 3) {
                    appConfig.setLightEnabled(lightState == 1);
                    lightSwitchCounter = 0;
                } else lightSwitchCounter += 1;
            } else lightSwitchCounter = 0;
            if (lightSwitchCounter > 3) lightSwitchCounter = 4;

            //// errors data
            String inmoError = getError(77);
            if (!inmoError.equals("")) System.out.println(String.format(Locale.US,"Err: %s", inmoError));
            wd.setAlert(inmoError);
            /*
            if ((inmoError != "") && (sContext != null)) {
                Timber.i("News to send: %s, sending Intent", inmoError);
                Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                intent.putExtra(Constants.INTENT_EXTRA_NEWS, inmoError);
                sContext.sendBroadcast(intent);
            }
            */
            return true;
        }

        boolean parseRealTimeInfoV11y(Context sContext) {
            Timber.i("Parse V11y realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            int mSomeThing1 = MathsUtil.signedShortFromBytesLE(data, 4);
            int mSomeThing2 = MathsUtil.signedShortFromBytesLE(data, 4);
            int mSomeThing3 = MathsUtil.signedShortFromBytesLE(data, 6); //not sure
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 8);
            int mSomeThing180 = MathsUtil.signedShortFromBytesLE(data, 10);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 12); // not sure
            int mPwm = MathsUtil.signedShortFromBytesLE(data, 14);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 16);
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 18); // not sure
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 20); // not sure
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 22); // not sure
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 24); // not sure

            int mSomeThing183 = MathsUtil.signedShortFromBytesLE(data, 26);
            int mMileage = MathsUtil.shortFromBytesLE(data, 28)*10; // always 18000
            int mSomeThing4 = MathsUtil.shortFromBytesLE(data, 30);
            int mSomeThing5 = MathsUtil.shortFromBytesLE(data, 32);

            int mBatLevel1 = MathsUtil.shortFromBytesLE(data, 34);
            int mBatLevel2 = MathsUtil.shortFromBytesLE(data, 36);
            int mSomeThing6 = MathsUtil.shortFromBytesLE(data, 38);
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 40);
            int x5 = MathsUtil.shortFromBytesLE(data, 42);
            int x6 = MathsUtil.shortFromBytesLE(data, 44);
            int x7 = MathsUtil.shortFromBytesLE(data, 46);
            int mSomeThing7 = MathsUtil.shortFromBytesLE(data, 48);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 50);
            int mSomeThing380 = MathsUtil.shortFromBytesLE(data, 52);


            int mMosTemp = (data[58] & 0xff) + 80 - 256;
            int mMotTemp = (data[59] & 0xff) + 80 - 256;
            int mBatTemp = (data[60] & 0xff) + 80 - 256; // 0
            int mBoardTemp = (data[61] & 0xff) + 80 - 256;
            int mCpuTemp = (data[62] & 0xff) + 80 - 256;
            int mImuTemp = (data[63] & 0xff) + 80 - 256;
            int mLampTemp = (data[64] & 0xff) + 80 - 256; // 0

// don't remove
/*
            System.out.println(String.format(Locale.US,"\nVolt: %.2f, Amp: %.2f, Km/h: %.2f, N*m: %.2f, Bat Wt: %d, Mot Wt: %d, XZ: %d, PWM: %.2f, PitchAim: %.2f, Pith: %.2f, Roll: %.2f, \nTrip Km: %.2f, Bat1: %.2f, Bat2: %.2f, Something: %.2f, Lim km/h: %.2f, Lim A: %.2f, \nMos t: %d, Mot t: %d, Bat t: %d, Board t: %d, CPU t: %d, IMU t: %d, Lamp t: %d",
                    mVoltage/100.0, mCurrent/100.0, mSpeed/100.0, mTorque/100.0, mBatPower,mMotPower, x5, mPwm/100.0, mPitchAimAngle/100.0, mPitchAngle/100.0,  mRollAngle/100.0, mMileage/1.0,  mBatLevel1/100.0, mBatLevel2/100.0, mSomeThing180/100.0, mDynamicSpeedLimit/100.0, mDynamicCurrentLimit/100.0, mMosTemp, mMotTemp, mBatTemp, mBoardTemp, mCpuTemp, mImuTemp, mLampTemp));
            System.out.println(String.format(Locale.US,"mSomeThing183: %.2f, mPitchAngle: %.2f, mRollAngle: %.2f, X5: %d, X6: %d,X7: %d, mSomeThing380: %.2f",
                     mSomeThing183/100.0, mPitchAngle/100.0,mRollAngle/100.0, x5,x6,x7, mSomeThing380/100.0));
            System.out.println(String.format(Locale.US,"m1: %.2f, m2: %.2f, m3: %.2f, m4: %d, m5: %d, m6: %d, m7: %.2f",
                    mSomeThing1/100.0, mSomeThing2/100.0,mSomeThing3/100.0, mSomeThing4,mSomeThing5,mSomeThing6, mSomeThing7/100.0));
*/
            wd.setVoltage(mVoltage);
            wd.setTorque((double)mTorque/100.0);
            wd.setMotorPower(mMotPower);
            wd.setCpuTemp(mCpuTemp);
            wd.setImuTemp(mImuTemp);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setCurrentLimit((double)mDynamicCurrentLimit/100.0);
            wd.setSpeedLimit((double)mDynamicSpeedLimit/100.0);
            wd.setBatteryLevel((int)Math.round((mBatLevel1 + mBatLevel2)/200.0));
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mMotTemp * 100);
            wd.setOutput(mPwm);
            wd.updatePwm();
            //wd.setMotorTemp(mMotTemp * 100); not existed in WD
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.setTopSpeed(mSpeed);
            wd.setPower(mBatPower * 100);
            wd.setWheelDistance(mMileage);
            //// state data
            int mPcMode = data[74] & 0x07; // lock, drive, shutdown, idle
            int mMcMode = (data[74]>>3)&0x07;
            int mMotState = (data[74]>>6)&0x01;
            int chrgState = (data[74]>>7)&0x01;
            int lowLightState = (data[75])&0x01;
            int highLightState = (data[75] >> 1) & 0x01;
            int liftedState = (data[75]>>2)&0x01;
            //int tailLiState = (data[75]>>3)&0x03;
            int fwUpdateState = (data[75]>>5)&0x01;
            int lightState = (data[76] >> 1) & 0x01;
            //int liftedState = (data[76]>>2)&0x01;
            int tailLiState = (data[76]>>4)&0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + "Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            System.out.println(String.format(Locale.US,"State: %s", wmode));
            wd.setModeStr(wmode);

            if (appConfig.getLightEnabled() != (lightState == 1)) {
                if (lightSwitchCounter > 3) {
                    appConfig.setLightEnabled(lightState == 1); // bad behaviour
                    lightSwitchCounter = 0;
                } else lightSwitchCounter += 1;
            } else lightSwitchCounter = 0;

            //// errors data
            String inmoError = getError(77);
            if (!inmoError.equals("")) System.out.println(String.format(Locale.US,"Err: %s", inmoError));
            wd.setAlert(inmoError);
            /*
            if ((inmoError != "") && (sContext != null)) {
                Timber.i("News to send: %s, sending Intent", inmoError);
                Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                intent.putExtra(Constants.INTENT_EXTRA_NEWS, inmoError);
                sContext.sendBroadcast(intent);
            }
            */
            return true;
        }


        boolean parseRealTimeInfoV9(Context sContext) {
            Timber.i("Parse V9 realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            int mSomeThing1 = MathsUtil.signedShortFromBytesLE(data, 4);
            int mSomeThing2 = MathsUtil.signedShortFromBytesLE(data, 4);
            int mSomeThing3 = MathsUtil.signedShortFromBytesLE(data, 6); //not sure
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 8);
            int mSomeThing180 = MathsUtil.signedShortFromBytesLE(data, 10);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 12); // not sure
            int mPwm = MathsUtil.signedShortFromBytesLE(data, 14);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 16);
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 18); // not sure
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 20); // not sure
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 22); // not sure
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 24); // not sure

            int mSomeThing183 = MathsUtil.signedShortFromBytesLE(data, 26);
            int mMileage = MathsUtil.shortFromBytesLE(data, 28)*10; // always 18000
            int mSomeThing4 = MathsUtil.shortFromBytesLE(data, 30);
            int mSomeThing5 = MathsUtil.shortFromBytesLE(data, 32);

            int mBatLevel1 = MathsUtil.shortFromBytesLE(data, 34);
            int mBatLevel2 = MathsUtil.shortFromBytesLE(data, 36);
            int mSomeThing6 = MathsUtil.shortFromBytesLE(data, 38);
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 40);
            int x5 = MathsUtil.shortFromBytesLE(data, 42);
            int x6 = MathsUtil.shortFromBytesLE(data, 44);
            int x7 = MathsUtil.shortFromBytesLE(data, 46);
            int mSomeThing7 = MathsUtil.shortFromBytesLE(data, 48);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 50);
            int mSomeThing380 = MathsUtil.shortFromBytesLE(data, 52);


            int mMosTemp = (data[58] & 0xff) + 80 - 256;
            int mMotTemp = (data[59] & 0xff) + 80 - 256;
            int mBatTemp = (data[60] & 0xff) + 80 - 256; // 0
            int mBoardTemp = (data[61] & 0xff) + 80 - 256;
            int mCpuTemp = (data[62] & 0xff) + 80 - 256;
            int mImuTemp = (data[63] & 0xff) + 80 - 256;
            int mLampTemp = (data[64] & 0xff) + 80 - 256; // 0

// don't remove
/*
            System.out.println(String.format(Locale.US,"\nVolt: %.2f, Amp: %.2f, Km/h: %.2f, N*m: %.2f, Bat Wt: %d, Mot Wt: %d, XZ: %d, PWM: %.2f, PitchAim: %.2f, Pith: %.2f, Roll: %.2f, \nTrip Km: %.2f, Bat1: %.2f, Bat2: %.2f, Something: %.2f, Lim km/h: %.2f, Lim A: %.2f, \nMos t: %d, Mot t: %d, Bat t: %d, Board t: %d, CPU t: %d, IMU t: %d, Lamp t: %d",
                    mVoltage/100.0, mCurrent/100.0, mSpeed/100.0, mTorque/100.0, mBatPower,mMotPower, x5, mPwm/100.0, mPitchAimAngle/100.0, mPitchAngle/100.0,  mRollAngle/100.0, mMileage/1.0,  mBatLevel1/100.0, mBatLevel2/100.0, mSomeThing180/100.0, mDynamicSpeedLimit/100.0, mDynamicCurrentLimit/100.0, mMosTemp, mMotTemp, mBatTemp, mBoardTemp, mCpuTemp, mImuTemp, mLampTemp));
            System.out.println(String.format(Locale.US,"mSomeThing183: %.2f, mPitchAngle: %.2f, mRollAngle: %.2f, X5: %d, X6: %d,X7: %d, mSomeThing380: %.2f",
                     mSomeThing183/100.0, mPitchAngle/100.0,mRollAngle/100.0, x5,x6,x7, mSomeThing380/100.0));
            System.out.println(String.format(Locale.US,"m1: %.2f, m2: %.2f, m3: %.2f, m4: %d, m5: %d, m6: %d, m7: %.2f",
                    mSomeThing1/100.0, mSomeThing2/100.0,mSomeThing3/100.0, mSomeThing4,mSomeThing5,mSomeThing6, mSomeThing7/100.0));
*/
            wd.setVoltage(mVoltage);
            wd.setTorque((double)mTorque/100.0);
            wd.setMotorPower(mMotPower);
            wd.setCpuTemp(mCpuTemp);
            wd.setImuTemp(mImuTemp);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setCurrentLimit((double)mDynamicCurrentLimit/100.0);
            wd.setSpeedLimit((double)mDynamicSpeedLimit/100.0);
            wd.setBatteryLevel((int)Math.round((mBatLevel1 + mBatLevel2)/200.0));
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mMotTemp * 100);
            wd.setOutput(mPwm);
            wd.updatePwm();
            //wd.setMotorTemp(mMotTemp * 100); not existed in WD
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.setTopSpeed(mSpeed);
            wd.setPower(mBatPower * 100);
            wd.setWheelDistance(mMileage);
            //// state data
            int mPcMode = data[74] & 0x07; // lock, drive, shutdown, idle
            int mMcMode = (data[74]>>3)&0x07;
            int mMotState = (data[74]>>6)&0x01;
            int chrgState = (data[74]>>7)&0x01;
            int lowLightState = (data[75])&0x01;
            int highLightState = (data[75] >> 1) & 0x01;
            int liftedState = (data[75]>>2)&0x01;
            //int tailLiState = (data[75]>>3)&0x03;
            int fwUpdateState = (data[75]>>5)&0x01;
            int lightState = (data[76] >> 1) & 0x01;
            //int liftedState = (data[76]>>2)&0x01;
            int tailLiState = (data[76]>>4)&0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + "Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            System.out.println(String.format(Locale.US,"State: %s", wmode));
            wd.setModeStr(wmode);

            if (appConfig.getLightEnabled() != (lightState == 1)) {
                if (lightSwitchCounter > 3) {
                    appConfig.setLightEnabled(lightState == 1); // bad behaviour
                    lightSwitchCounter = 0;
                } else lightSwitchCounter += 1;
            } else lightSwitchCounter = 0;

            //// errors data
            String inmoError = getError(77);
            if (!inmoError.equals("")) System.out.println(String.format(Locale.US,"Err: %s", inmoError));
            wd.setAlert(inmoError);
            /*
            if ((inmoError != "") && (sContext != null)) {
                Timber.i("News to send: %s, sending Intent", inmoError);
                Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                intent.putExtra(Constants.INTENT_EXTRA_NEWS, inmoError);
                sContext.sendBroadcast(intent);
            }
            */
            return true;
        }

        boolean parseRealTimeInfoV12S(Context sContext) {
            Timber.i("Parse V12S realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            int mSomeThing1 = MathsUtil.signedShortFromBytesLE(data, 4);
            int mSomeThing2 = MathsUtil.signedShortFromBytesLE(data, 4);
            int mSomeThing3 = MathsUtil.signedShortFromBytesLE(data, 6); //not sure
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 8);
            int mSomeThing180 = MathsUtil.signedShortFromBytesLE(data, 10);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 12); // not sure
            int mPwm = MathsUtil.signedShortFromBytesLE(data, 14);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 16);
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 18); // not sure
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 20); // not sure
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 22); // not sure
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 24); // not sure

            int mSomeThing183 = MathsUtil.signedShortFromBytesLE(data, 26);
            int mMileage = MathsUtil.shortFromBytesLE(data, 28)*10; // always 18000
            int mSomeThing4 = MathsUtil.shortFromBytesLE(data, 30);
            int mSomeThing5 = MathsUtil.shortFromBytesLE(data, 32);

            int mBatLevel1 = MathsUtil.shortFromBytesLE(data, 34);
            int mBatLevel2 = MathsUtil.shortFromBytesLE(data, 36);
            int mSomeThing6 = MathsUtil.shortFromBytesLE(data, 38);
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 40);
            int x5 = MathsUtil.shortFromBytesLE(data, 42);
            int x6 = MathsUtil.shortFromBytesLE(data, 44);
            int x7 = MathsUtil.shortFromBytesLE(data, 46);
            int mSomeThing7 = MathsUtil.shortFromBytesLE(data, 48);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 50);
            int mSomeThing380 = MathsUtil.shortFromBytesLE(data, 52);


            int mMosTemp = (data[58] & 0xff) + 80 - 256;
            int mMotTemp = (data[59] & 0xff) + 80 - 256;
            int mBatTemp = (data[60] & 0xff) + 80 - 256; // 0
            int mBoardTemp = (data[61] & 0xff) + 80 - 256;
            int mCpuTemp = (data[62] & 0xff) + 80 - 256;
            int mImuTemp = (data[63] & 0xff) + 80 - 256;
            int mLampTemp = (data[64] & 0xff) + 80 - 256; // 0

// don't remove
/*
            System.out.println(String.format(Locale.US,"\nVolt: %.2f, Amp: %.2f, Km/h: %.2f, N*m: %.2f, Bat Wt: %d, Mot Wt: %d, XZ: %d, PWM: %.2f, PitchAim: %.2f, Pith: %.2f, Roll: %.2f, \nTrip Km: %.2f, Bat1: %.2f, Bat2: %.2f, Something: %.2f, Lim km/h: %.2f, Lim A: %.2f, \nMos t: %d, Mot t: %d, Bat t: %d, Board t: %d, CPU t: %d, IMU t: %d, Lamp t: %d",
                    mVoltage/100.0, mCurrent/100.0, mSpeed/100.0, mTorque/100.0, mBatPower,mMotPower, x5, mPwm/100.0, mPitchAimAngle/100.0, mPitchAngle/100.0,  mRollAngle/100.0, mMileage/1.0,  mBatLevel1/100.0, mBatLevel2/100.0, mSomeThing180/100.0, mDynamicSpeedLimit/100.0, mDynamicCurrentLimit/100.0, mMosTemp, mMotTemp, mBatTemp, mBoardTemp, mCpuTemp, mImuTemp, mLampTemp));
            System.out.println(String.format(Locale.US,"mSomeThing183: %.2f, mPitchAngle: %.2f, mRollAngle: %.2f, X5: %d, X6: %d,X7: %d, mSomeThing380: %.2f",
                     mSomeThing183/100.0, mPitchAngle/100.0,mRollAngle/100.0, x5,x6,x7, mSomeThing380/100.0));
            System.out.println(String.format(Locale.US,"m1: %.2f, m2: %.2f, m3: %.2f, m4: %d, m5: %d, m6: %d, m7: %.2f",
                    mSomeThing1/100.0, mSomeThing2/100.0,mSomeThing3/100.0, mSomeThing4,mSomeThing5,mSomeThing6, mSomeThing7/100.0));
*/
            wd.setVoltage(mVoltage);
            wd.setTorque((double)mTorque/100.0);
            wd.setMotorPower(mMotPower);
            wd.setCpuTemp(mCpuTemp);
            wd.setImuTemp(mImuTemp);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setCurrentLimit((double)mDynamicCurrentLimit/100.0);
            wd.setSpeedLimit((double)mDynamicSpeedLimit/100.0);
            wd.setBatteryLevel((int)Math.round((mBatLevel1 + mBatLevel2)/200.0));
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mMotTemp * 100);
            wd.setOutput(mPwm);
            wd.updatePwm();
            //wd.setMotorTemp(mMotTemp * 100); not existed in WD
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.setTopSpeed(mSpeed);
            wd.setPower(mBatPower * 100);
            wd.setWheelDistance(mMileage);
            //// state data
            int mPcMode = data[74] & 0x07; // lock, drive, shutdown, idle
            int mMcMode = (data[74]>>3)&0x07;
            int mMotState = (data[74]>>6)&0x01;
            int chrgState = (data[74]>>7)&0x01;
            int lowLightState = (data[75])&0x01;
            int highLightState = (data[75] >> 1) & 0x01;
            int liftedState = (data[75]>>2)&0x01;
            //int tailLiState = (data[75]>>3)&0x03;
            int fwUpdateState = (data[75]>>5)&0x01;
            int lightState = (data[76] >> 1) & 0x01;
            //int liftedState = (data[76]>>2)&0x01;
            int tailLiState = (data[76]>>4)&0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + "Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            System.out.println(String.format(Locale.US,"State: %s", wmode));
            wd.setModeStr(wmode);

            if (appConfig.getLightEnabled() != (lightState == 1)) {
                if (lightSwitchCounter > 3) {
                    appConfig.setLightEnabled(lightState == 1); // bad behaviour
                    lightSwitchCounter = 0;
                } else lightSwitchCounter += 1;
            } else lightSwitchCounter = 0;

            //// errors data
            String inmoError = getError(77);
            if (!inmoError.equals("")) System.out.println(String.format(Locale.US,"Err: %s", inmoError));
            wd.setAlert(inmoError);
            /*
            if ((inmoError != "") && (sContext != null)) {
                Timber.i("News to send: %s, sending Intent", inmoError);
                Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                intent.putExtra(Constants.INTENT_EXTRA_NEWS, inmoError);
                sContext.sendBroadcast(intent);
            }
            */
            return true;
        }


        public static Message getCarType() {
            Message msg = new Message();
            msg.flags = Flag.Initial.getValue();
            msg.command = Command.MainInfo.getValue();
            msg.data = new byte[]{(byte)0x01};
            return msg;
        }

        public static Message getMainVersion() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.MainVersion.getValue();
            msg.data = new byte[0];
            return msg;
        }

        public static Message wheelOffFirstStage() {
            Message msg = new Message();
            msg.flags = Flag.Initial.getValue();
            msg.command = Command.Diagnistic.getValue();
            msg.data = new byte[]{(byte)0x81, (byte) 0x00};
            return msg;
        }

        public static Message wheelOffSecondStage() {
            Message msg = new Message();
            msg.flags = Flag.Initial.getValue();
            msg.command = Command.Diagnistic.getValue();
            msg.data = new byte[]{(byte)0x82};
            return msg;
        }

        public static Message getSerialNumber() {
            Message msg = new Message();
            msg.flags = Flag.Initial.getValue();
            msg.command = Command.MainInfo.getValue();
            msg.data = new byte[]{(byte)0x02};
            return msg;
        }

        public static Message getVersions() {
            Message msg = new Message();
            msg.flags = Flag.Initial.getValue();
            msg.command = Command.MainInfo.getValue();
            msg.data = new byte[]{(byte)0x06};
            return msg;
        }

        public static Message getCurrentSettings() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Settings.getValue();
            msg.data = new byte[]{(byte)0x20};
            return msg;
        }

        public static Message getUselessData() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Something1.getValue();
            msg.data = new byte[]{(byte)0x00, (byte)0x01};
            return msg;
        }

        public static Message getBatteryData() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.BatteryRealTimeInfo.getValue();
            msg.data = new byte[0];
            return msg;
        }

        public static Message getDiagnostic() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Diagnistic.getValue();
            msg.data = new byte[0];
            return msg;
        }

        public static Message getStatistics() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.TotalStats.getValue();
            msg.data = new byte[0];
            return msg;
        }

        public static Message getRealTimeData() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.RealTimeInfo.getValue();
            msg.data = new byte[0];
            return msg;
        }

        public static Message playSound(int number) {
            byte value = (byte)(number & 0xFF);
            byte commandCode = (byte) 0x51;
            if (getInstance().getModel() == Model.V11 && protoVer < 2) {
                commandCode = (byte) 0x41;
            }
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{commandCode, value, 0x01};
            return msg;
        }

        public static Message playBeep(int number) {
            byte value = (byte)(number & 0xFF);
            byte commandCode = (byte) 0x51;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{commandCode, value, 0x64};
            return msg;
        }

        public static Message wheelCalibration() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x42, 0x01, 0x00, 0x01};
            return msg;
        }

        public static Message setLight(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            byte commandCode = (byte) 0x50;
            if (getInstance().getModel() == Model.V11 && protoVer < 2 ) {
                commandCode = (byte) 0x40;
            }
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{commandCode, enable};
            return msg;
        }

        public static Message setLightV12(boolean lowBeamOn, boolean highBeamOn) {
            byte enableLowBeam = 0;
            byte enableHighBeam = 0;
            if (lowBeamOn) enableLowBeam = 1;
            if (highBeamOn) enableHighBeam = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x50, enableLowBeam, enableHighBeam};
            return msg;
        }

        public static Message setAlarmSpeedV12(int speedLow, int speedHigh) {
            byte[] value1 = MathsUtil.getBytes((short)(speedLow * 100));
            byte[] value2 = MathsUtil.getBytes((short)(speedHigh * 100));
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x3e, value1[1], value1[0], value2[1], value2[0]};
            return msg;
        }

        public static Message setSplitMode(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            byte commandCode = (byte) 0x3e; // in conflict with setAlarmSpeed of v12
            if (getInstance().getModel() == Model.V12HS || getInstance().getModel() == Model.V12HT || getInstance().getModel() == Model.V12PRO) {
                commandCode = (byte) 0x42;
            }
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{commandCode, enable};
            return msg;
        }

        public static Message setSplitAccelBreak(int acceleration, int breaksens) {
            byte value1 = (byte)(acceleration & 0xFF);
            byte value2 = (byte)(breaksens & 0xFF);
            byte commandCode = (byte) 0x3f;
            if (getInstance().getModel() == Model.V12HS || getInstance().getModel() == Model.V12HT || getInstance().getModel() == Model.V12PRO) {
                commandCode = (byte) 0x40;
            }
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{commandCode, value1, value2};
            return msg;
        }

        public static Message wheelCalibrationBalance() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x52, 0x01, 0x01, 0x00};
            return msg;
        }

        public static Message wheelCalibrationTurn() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x52, 0x01, 0x00, 0x01};
            return msg;
        }

        public static Message setSoundWave(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x39, enable};
            return msg;
        }

        public static Message setAutoLight(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x2f, enable};
            return msg;
        }

        public static Message setLightBrightnessV12(int brightnessLowBeam, int brightnessHighBeam) {
            byte value1 = (byte)(brightnessLowBeam & 0xFF);
            byte value2 = (byte)(brightnessHighBeam & 0xFF);
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x2b, value1, value2};
            return msg;
        }

        public static Message setLightBrightness(int brightness) {
            byte value = (byte)(brightness & 0xFF);
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x2b, value};
            return msg;
        }

        public static Message setVolume(int volume) {
            byte value = (byte)(volume & 0xFF);
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x26, value};
            return msg;
        }

        public static Message setDrl(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x2d, enable};
            return msg;
        }

        public static Message setHandleButton(boolean on) {
            byte enable = 0;
            if (!on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x2e, enable};
            return msg;
        }

        public static Message setFan(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x43, enable};
            return msg;
        }

        public static Message setQuietMode(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x38, enable};
            return msg;
        }

        public static Message setFancierMode(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x24, enable};
            return msg;
        }

        public static Message setMaxSpeed(int maxSpeed) {
            byte[] value = MathsUtil.getBytes((short)(maxSpeed * 100));
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x21, value[1], value[0]};
            return msg;
        }

        public static Message setMaxSpeedV14(int maxSpeed, int alarmSpeed) {
            byte[] value1 = MathsUtil.getBytes((short)(maxSpeed * 100));
            byte[] value2 = MathsUtil.getBytes((short)(alarmSpeed * 100));
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x21, value1[1], value1[0], value2[1], value2[0]};
            return msg;
        }

        public static Message setPedalSensivity(int sensivity) {
            byte value = (byte)(sensivity & 0xFF);
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x25, value, value};
            return msg;
        }

        public static Message setStanbyDelay(int delay) {
            byte[] value = MathsUtil.getBytes((short)(delay * 60)); // need to set in sec
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x28, value[1], value[0]};
            return msg;
        }

        public static Message setBermAngleMode(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x45, enable};
            return msg;
        }

        public static Message setClassicMode(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x23, enable};
            return msg;
        }

        public static Message setGoHome(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x37, enable};
            return msg;
        }

        public static Message setTransportMode(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x32, enable};
            return msg;
        }

        public static Message setLock(boolean on) {
            byte enable = 0;
            if (on) enable = 1;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x31, enable};
            return msg;
        }

        public static Message setMute(boolean on) {
            byte enable = 1;
            if (on) enable = 0;
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x2c, enable};
            return msg;
        }

        public static Message setPedalTilt(int angle) {
            byte[] value = MathsUtil.getBytes((short)(angle * 10));
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Control.getValue();
            msg.data = new byte[]{0x22, value[1], value[0]};
            return msg;
        }

        public byte[] writeBuffer() {

            byte[] buffer = getBytes();
            byte check = calcCheck(buffer);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0xAA);
            out.write(0xAA);
            try {
                out.write(escape(buffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.write(check);
            return out.toByteArray();
        }

        private byte[] getBytes() {

            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            buff.write(flags);
            buff.write(data.length+1);
            buff.write(command);
            try {
                buff.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buff.toByteArray();
        }

        private static byte calcCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = (check ^ c) & 0xFF;
            }
            return (byte) check;
        }

        static Message verify(byte[] buffer) {
            Timber.i("Verify: %s", StringUtil.toHexString(buffer));
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 0, buffer.length - 1);
            byte check = calcCheck(dataBuffer);

            byte bufferCheck = buffer[buffer.length - 1];
            if (check == bufferCheck) {
                Timber.i("Check OK");
            } else {
                Timber.i("Check FALSE, calc: %02X, packet: %02X",check, bufferCheck);
            }
            return (check == bufferCheck) ? new Message(dataBuffer) : null;
        }

        private byte[] escape(byte[] buffer) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte c : buffer) {
                if (c == (byte) 0xAA || c == (byte) 0xA5) {
                    out.write(0xA5);
                }
                out.write(c);
            }
            return out.toByteArray();
        }
    }
	
    static class InmotionUnpackerV2 {

        enum UnpackerState {
            unknown,
            flagsearch,
            lensearch,
            collecting,
            done
        }


        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        int len = 0;
        int flags = 0;
        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {
            if (c != (byte)0xA5 || oldc == (byte)0xA5){

                switch (state) {

                    case collecting:

                        buffer.write(c);
                        if (buffer.size() == len + 5) {
                            state = UnpackerState.done;
                            updateStep = 0;
                            oldc = 0;
                            Timber.i("Len %d", len);
                            Timber.i("Step reset");
                            return true;
                        }
                        break;

                    case lensearch:
                        buffer.write(c);
                        len = c & 0xff;
                        state = UnpackerState.collecting;
                        oldc = c;
                        break;

                    case flagsearch:
                        buffer.write(c);
                        flags = c & 0xff;
                        state = UnpackerState.lensearch;
                        oldc = c;
                        break;

                    default:
                        if (c == (byte) 0xAA && oldc == (byte) 0xAA) {
                            buffer = new ByteArrayOutputStream();
                            buffer.write(0xAA);
                            buffer.write(0xAA);
                            state = UnpackerState.flagsearch;
                        }
                        oldc = c;
                }

            } else {
                oldc = c;
            }
            return false;
        }

        void reset() {
            buffer = new ByteArrayOutputStream();
            oldc = 0;
            state = UnpackerState.unknown;

        }
    }

    @Override
    public int getCellsForWheel() {
        if (getInstance().getModel() == Model.V12HS) {
            return 24;
        }
        if (getInstance().getModel() == Model.V12HT) {
            return 24;
        }
        if (getInstance().getModel() == Model.V12PRO) {
            return 24;
        }
        if (getInstance().getModel() == Model.V13) {
            return 30;
        }
        if (getInstance().getModel() == Model.V13PRO) {
            return 30;
        }
        if (getInstance().getModel() == Model.V14g) {
            return 32;
        }
        if (getInstance().getModel() == Model.V14s) {
            return 32;
        }
        return 20;

    }

    public static synchronized void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("New instance");
        INSTANCE = new InmotionAdapterV2();
    }

    public static synchronized void stopTimer() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("Kill instance, stop timer");
        INSTANCE = null;
    }
}


