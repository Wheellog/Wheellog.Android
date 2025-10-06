package com.cooper.wheellog.utils;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.R;

import org.koin.java.KoinJavaComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import timber.log.Timber;

/**
 * Created by palachzzz on 08/2018.
 */
public class NinebotZAdapter extends BaseAdapter {
    private final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
    private static NinebotZAdapter INSTANCE;
    private Timer keepAliveTimer;
    protected boolean settingCommandReady = false;
    private boolean settingRequestReady = false;
    private static int updateStep = 0;
    protected byte[] settingCommand;
    private byte[] settingRequest;
    private static byte[] gamma = new byte[16];
    private static int stateCon = 0;
    private static boolean bmsMode = false;

    ///// wheel settings

    private int lockMode = 0;
    private int limitedMode = 0;
    private int limitModeSpeed = 0;
    private int limitModeSpeed1Km = 0; // not sure (?)
    private int LimitModeSpeed = 0;
    private int speakerVolume = 0;
    private int alarms = 0;
    private int alarm1Speed = 0;
    private int alarm2Speed = 0;
    private int alarm3Speed = 0;
    private int ledMode = 0;
    private int ledColor1 = 0;
    private int ledColor2 = 0;
    private int ledColor3 = 0;
    private int ledColor4 = 0;
    private int errorCode1 = 0;
    private int errorCode2 = 0;
    private int pedalSensivity = 0;
    private int driveFlags = 0;

    ///// end of wheel settings


    NinebotZUnpacker unpacker = new NinebotZUnpacker();

    public void startKeepAliveTimer() {
        Timber.i("Ninebot Z timer starting");
        updateStep = 0;
        stateCon = 0;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (updateStep == 0) {
                    Timber.i("State connection %d", stateCon);
                    if (stateCon == 0) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getBleVersion().writeBuffer())) {
                            Timber.i("Sent start message");
                        } else Timber.i("Unable to send start message");

                    } else if (stateCon == 1) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getKey().writeBuffer())) {
                            Timber.i("Sent getkey message");
                        } else Timber.i("Unable to send getkey message");

                    } else if (stateCon == 2) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getSerialNumber().writeBuffer())) {
                            Timber.i("Sent serial number message");
                        } else Timber.i("Unable to send serial number message");

                    } else if (stateCon == 3) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getVersion().writeBuffer())) {
                            Timber.i("Sent version message");
                        } else Timber.i("Unable to send version message");

                    } else if (stateCon == 4) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getParams1().writeBuffer())) {
                            Timber.i("Sent getParams1 message");
                        } else Timber.i("Unable to send getParams1 message");

                    } else if (stateCon == 5) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getParams2().writeBuffer())) {
                            Timber.i("Sent getParams2 message");
                        } else Timber.i("Unable to send getParams2 message");

                    } else if (stateCon == 6) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getParams3().writeBuffer())) {
                            Timber.i("Sent getParams3 message");
                        } else Timber.i("Unable to send getParams2 message");

                    } else if (stateCon == 7) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getBms1Sn().writeBuffer())) {
                            Timber.i("Sent BMS1 SN message");
                        } else Timber.i("Unable to send BMS1 SN message");
                    } else if (stateCon == 8) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getBms1Life().writeBuffer())) {
                            Timber.i("Sent BMS1 life message");
                        } else Timber.i("Unable to send BMS1 life message");
                    } else if (stateCon == 9) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getBms1Cells().writeBuffer())) {
                            Timber.i("Sent BMS1 cells message");
                        } else Timber.i("Unable to send BMS1 cells message");
                    } else if (stateCon == 10) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getBms2Sn().writeBuffer())) {
                            Timber.i("Sent BMS2 SN message");
                        } else Timber.i("Unable to send BMS2 SN message");
                    } else if (stateCon == 11) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getBms2Life().writeBuffer())) {
                            Timber.i("Sent BMS2 life message");
                        } else Timber.i("Unable to send BMS2 life message");
                    } else if (stateCon == 12) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getBms2Cells().writeBuffer())) {
                            Timber.i("Sent BMS2 cells message");
                        } else Timber.i("Unable to send BMS2 cells message");

                    } else if (settingCommandReady) {
                        if (WheelData.getInstance().bluetoothCmd(settingCommand)) {
                            settingCommandReady = false;
                            Timber.i("Sent command message");
                        } else Timber.i("Unable to send command message");

                    } else if (settingRequestReady) {
                        if (WheelData.getInstance().bluetoothCmd(settingRequest)) {
                            settingRequestReady = false;
                            Timber.i("Sent settings request message");
                        } else Timber.i("Unable to send settings request message");

                    } else {
                        if (!WheelData.getInstance().bluetoothCmd(NinebotZAdapter.CANMessage.getLiveData().writeBuffer())) {
                            Timber.i("Unable to send keep-alive message");
                        } else {
                            Timber.i("Sent keep-alive message");
                        }
                    }

                }
                updateStep += 1;

                if ((updateStep == 5) && (stateCon > 6) && (stateCon < 13)) {
                    stateCon += 1;
                    Timber.i("Change state to %d 1", stateCon);
                    if (stateCon > 12) stateCon = 7;
                }
                if (bmsMode && (stateCon == 13)) {
                    stateCon = 7;
                    Timber.i("Change state to %d 2", stateCon);
                }
                if (!bmsMode && (stateCon > 6) && (stateCon < 13)) {
                    stateCon = 13;
                    Timber.i("Change state to %d 3", stateCon);
                }
                updateStep %= 5;
                Timber.i("Step: %d", updateStep);
            }
        };
        Timber.i("Ninebot Z timer started");
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 200, 25);
    }

    public void resetConnection() {
        stateCon = 0;
        updateStep = 0;
        gamma = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        stopTimer();
    }
//// mocks
    public int getWheelAlarmMax(){
        return 100;
    }

    public int getWheelLimitedSpeed(){
        return 500;
    }

    public int getPedalSensivity(){ return pedalSensivity; }

    public String getLedMode(){
        return Integer.toString(ledMode);
    }

    public int getLedColor1(){
        return ledColor1;
    }

    public int getLedColor2(){
        return ledColor2;
    }

    public int getLedColor3(){
        return ledColor3;
    }

    public int getLedColor4(){
        return ledColor4;
    }

    public int getSpeakerVolume(){
        return speakerVolume;
    }
/// end of mocks
    public void setBmsReadingMode(boolean mode) {
        bmsMode = mode;
    }

    @Override
    public int getCellsForWheel() {
        return 14;
    }

    @Override
    public String getLedModeString() {
        switch (appConfig.getLedMode()) {
            case "0": return getContext().getString(R.string.off);
            case "1": return getContext().getString(R.string.led_type1);
            case "2": return getContext().getString(R.string.led_type2);
            case "3": return getContext().getString(R.string.led_type3);
            case "4": return getContext().getString(R.string.led_type4);
            case "5": return getContext().getString(R.string.led_type5);
            case "6": return getContext().getString(R.string.led_type6);
            case "7": return getContext().getString(R.string.led_type7);
            default: return getContext().getString(R.string.led_mode_nb_description);
        }
    }

    private static String getErrorString(int errorCode) {
        String err_text1;
        switch (errorCode) {
            case 0: err_text1 = ""; break;
            case 1: err_text1 = "Motor hall sensor error"; break;
            case 6: err_text1 = "Initial S/N"; break;
            case 8: err_text1 = "Error Bat input 1"; break;
            case 9: err_text1 = "Error Bat input 2"; break;
            case 10: err_text1 = "Abnormal communication Bat#1"; break; //error and Bat1Volt>48V
            case 11: err_text1 = "Abnormal communication Bat#2"; break; //error and Bat2Volt>48V
            case 12: err_text1 = "Failure of Gyroscope initialization"; break;
            case 24: err_text1 = "General voltage > 65V or < 40V"; break;
            case 25: err_text1 = "VGM - Voltage < 10V"; break;
            case 28: err_text1 = "Abnormal power supply Bat#1"; break;
            case 29: err_text1 = "Abnormal power supply Bat#2"; break;
            case 34: err_text1 = "Battery cell of Bat#1 in big differential voltage"; break;
            case 35: err_text1 = "Battery cell of Bat#2 in big differential voltage"; break;
            case 36: err_text1 = "Bat#1 input error 0x800"; break;
            case 37: err_text1 = "Bat#2 input error 0x800"; break;
            case 38: err_text1 = "3c1e8 != 0x5A"; break;
            case 46: err_text1 = "Unknown error"; break;
            default: err_text1 = "Error"; break;
        }
        return String.format("Err:%d %s", errorCode, err_text1);
    }

    @Override
    public boolean getLedIsAvailable(int ledNum) {
        switch (appConfig.getLedMode()) {
            case "1":
            case "4":
            case "5":
                return ledNum == 1;
            case "2":
                return ledNum < 3;
            case "3":
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Ninebot_z decoding");
        WheelData wd = WheelData.getInstance();
        setBmsReadingMode(wd.getBmsView());
        boolean retResult = false;
        for (byte c : data) {
            if (unpacker.addChar(c)) {
                Timber.i("Starting verification");
                CANMessage result = CANMessage.verify(unpacker.getBuffer());

                if (result != null) { // data OK
                    Timber.i("Verification successful, command %02X", result.parameter);
                    if ((result.parameter == CANMessage.Param.BleVersion.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())) {
                        Timber.i("Get start answer");
                        stateCon = 2;

                    } else if ((result.parameter == CANMessage.Param.GetKey.getValue()) && (result.source == CANMessage.Addr.KeyGenerator.getValue())) {
                        Timber.i("Get encryption key");
                        gamma = result.parseKey();
                        stateCon = 2;
                        retResult = false;

                    } else if ((result.parameter == CANMessage.Param.SerialNumber.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())) {
                        Timber.i("Get serial number");
                        result.parseSerialNumber();
                        stateCon = 3;

                    } else if ((result.parameter == CANMessage.Param.LockMode.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())) {
                        Timber.i("Get param1 number");
                        result.parseParams1();
                        stateCon = 5;

                    } else if ((result.parameter == CANMessage.Param.LedMode.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())) {
                        Timber.i("Get param2 number");
                        result.parseParams2();
                        stateCon = 6;

                    } else if ((result.parameter == CANMessage.Param.SpeakerVolume.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())) {
                        Timber.i("Get param3 number");
                        result.parseParams3();
                        stateCon = 13;

                    } else if ((result.parameter == CANMessage.Param.Firmware.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())) {
                        Timber.i("Get version number");
                        result.parseVersionNumber();
                        stateCon = 4;

                    } else if ((result.parameter == CANMessage.Param.LiveData.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())) {
                        Timber.i("Get life data");
                        result.parseLiveData();
                        retResult = true;

                    } else if (result.source == CANMessage.Addr.BMS1.getValue()) {
                        Timber.i("Get info from BMS1");
                        if (result.parameter == 0x10) {
                            result.parseBmsSn(1);
                            stateCon = 8;
                        }
                        if (result.parameter == 0x30) {
                            result.parseBmsLife(1);
                            stateCon = 9;
                        }
                        if (result.parameter == 0x40) {
                            result.parseBmsCells(1);
                            stateCon = 10;
                        }

                    } else if (result.source == CANMessage.Addr.BMS2.getValue()) {
                        Timber.i("Get info from BMS2");
                        if (result.parameter == 0x10) {
                            result.parseBmsSn(2);
                            stateCon = 11;
                        }
                        if (result.parameter == 0x30) {
                            result.parseBmsLife(2);
                            stateCon = 12;
                        }
                        if (result.parameter == 0x40) {
                            result.parseBmsCells(2);
                            stateCon = 13;
                        }
                    }
                }
            }
        }
        wd.resetRideTime();
        return retResult;
    }

    @Override
    public boolean isReady() {
        return !Objects.equals(WheelData.getInstance().getSerial(), "")
                && !Objects.equals(WheelData.getInstance().getVersion(), "")
                && WheelData.getInstance().getVoltage() != 0;
    }

    @Override
    public void setDrl(final boolean drl) {
        // ToDo check if it is the same as old value
        driveFlags = (driveFlags & 0xFFFE) | (drl ? 1 : 0); // need to have driveflags before
        settingRequest = NinebotZAdapter.CANMessage.getParams2().writeBuffer();
        settingRequestReady = true;
        settingCommand = NinebotZAdapter.CANMessage.setDriveFlags(driveFlags).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setLightState(final boolean lightEnable) { //not working yet, need more tests
        // ToDo check if it is the same as old value
        driveFlags = (driveFlags & 0xFFFB) | ((lightEnable ? 1 : 0) << 2) ; // need to have driveflags before
        //driveFlags = (driveFlags & 0xFF7F) | ((lightEnable ? 1 : 0) << 7) ; // need to have driveflags before
        settingRequest = NinebotZAdapter.CANMessage.getParams2().writeBuffer();
        settingRequestReady = true;
        settingCommand = NinebotZAdapter.CANMessage.setDriveFlags(driveFlags).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setTailLightState(final boolean drl) {
        // ToDo check if it is the same as old value
        driveFlags = (driveFlags & 0xFFFD) | ((drl ? 1 : 0) << 1) ; // need to have driveflags before
        settingRequest = NinebotZAdapter.CANMessage.getParams2().writeBuffer();
        settingRequestReady = true;
        settingCommand = NinebotZAdapter.CANMessage.setDriveFlags(driveFlags).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setHandleButtonState(final boolean handleButtonEnable) {
        // ToDo check if it is the same as old value
        driveFlags = (driveFlags & 0xFFF7) | ((handleButtonEnable ? 0 : 1) << 3) ; // need to have driveflags before
        settingRequest = NinebotZAdapter.CANMessage.getParams2().writeBuffer();
        settingRequestReady = true;
        settingCommand = NinebotZAdapter.CANMessage.setDriveFlags(driveFlags).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setBrakeAssist(final boolean brakeAssist) {
        // ToDo check if it is the same as old value
        driveFlags = (driveFlags & 0xFFEF) | ((brakeAssist ? 0 : 1) << 4) ; // need to have driveflags before
        settingRequest = NinebotZAdapter.CANMessage.getParams2().writeBuffer();
        settingRequestReady = true;
        settingCommand = NinebotZAdapter.CANMessage.setDriveFlags(driveFlags).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setLedColor(final int value, final int ledNum) {
        settingRequest = NinebotZAdapter.CANMessage.getParams2().writeBuffer();
        settingRequestReady = true;
        settingCommand = NinebotZAdapter.CANMessage.setLedColor(value, ledNum).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setAlarmEnabled(final boolean value, final int num) {
        if (num == 1) alarms = (alarms & 0xFFFE) | (value ? 1 : 0);
        else if (num == 2) alarms = (alarms & 0xFFFD) | ((value ? 1 : 0) << 1);
        else alarms = (alarms & 0xFFFB) | ((value ? 1 : 0) << 2);
        settingRequest = NinebotZAdapter.CANMessage.getParams1().writeBuffer();
        settingRequestReady = true;
        settingCommand = NinebotZAdapter.CANMessage.setAlarms(alarms).writeBuffer();
        settingCommandReady = true;
    }

    @Override
    public void setAlarmSpeed(final int value, final int num) {
        if (alarm1Speed != value) {
            settingRequest = NinebotZAdapter.CANMessage.getParams1().writeBuffer();
            settingRequestReady = true;
            settingCommand = NinebotZAdapter.CANMessage.setAlarmSpeed(value, num).writeBuffer();
            settingCommandReady = true;
        }
    }

    @Override
    public void setLimitedModeEnabled(final boolean value) {
        if ((limitedMode == 1) != value) {
            settingRequest = NinebotZAdapter.CANMessage.getParams1().writeBuffer();
            settingRequestReady = true;
            settingCommand = NinebotZAdapter.CANMessage.setLimitedMode(value).writeBuffer();
            settingCommandReady = true;
        }
    }

    @Override
    public void setLimitedSpeed(final int value) {
        if (limitModeSpeed != value) {
            settingRequest = NinebotZAdapter.CANMessage.getParams1().writeBuffer();
            settingRequestReady = true;
            settingCommand = NinebotZAdapter.CANMessage.setLimitedSpeed(value).writeBuffer();
            settingCommandReady = true;
        }
    }

    @Override
    public void setPedalSensivity(final int value) {
        if (pedalSensivity != value) {
            settingRequest = NinebotZAdapter.CANMessage.getParams2().writeBuffer();
            settingRequestReady = true;
            settingCommand = NinebotZAdapter.CANMessage.setPedalSensivity(value).writeBuffer();
            settingCommandReady = true;
        }
    }

    @Override
    public void updateLedMode(final int value) {
        if (ledMode != value) {
            settingRequest = NinebotZAdapter.CANMessage.getParams2().writeBuffer();
            settingRequestReady = true;
            settingCommand = NinebotZAdapter.CANMessage.setLedMode(value).writeBuffer();
            settingCommandReady = true;
        }
    }

    @Override
    public void setSpeakerVolume(final int value) {
        if (speakerVolume != value) {
            settingRequest = NinebotZAdapter.CANMessage.getParams1().writeBuffer();
            settingRequestReady = true;
            settingCommand = NinebotZAdapter.CANMessage.setSpeakerVolume(value << 3).writeBuffer();
            settingCommandReady = true;
        }
    }

    @Override
    public void setLockMode(final boolean value) {
        if ((lockMode == 1) != value) {
            settingRequest = NinebotZAdapter.CANMessage.getParams1().writeBuffer();
            settingRequestReady = true;
            settingCommand = NinebotZAdapter.CANMessage.setLockMode(value).writeBuffer();
            settingCommandReady = true;
        }
    }

    @Override
    public void wheelCalibration() {
        settingCommand = NinebotZAdapter.CANMessage.runCalibration(true).writeBuffer();
        settingCommandReady = true;
    }

    public static class CANMessage {
        private final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);

        enum Addr {
            BMS1(0x11),
            BMS2(0x12),
            Controller(0x14),
            KeyGenerator(0x16),
            App(0x3e);

            private final int value;

            Addr(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum Comm {
            Read(0x01),
            Write(0x03),
            Get(0x04),
            GetKey(0x5b);

            private final int value;

            Comm(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum Param {
            GetKey(0x00),
            SerialNumber(0x10),
            Firmware(0x1a),
            BatteryLevel(0x22),
            Angles(0x61),
            Bat1Fw(0x66),
            Bat2Fw(0x67),
            BleVersion(0x68),
            ActivationDate(0x69),
            LockMode(0x70),
            LimitedMode(0x72),
            LimitModeSpeed1Km(0x73), // not sure (?)
            LimitModeSpeed(0x74),
            Calibration(0x75),
            Alarms(0x7c),
            Alarm1Speed(0x7d),
            Alarm2Speed(0x7e),
            Alarm3Speed(0x7f),
            LiveData(0xb0),
            LedMode(0xc6),
            LedColor1(0xc8),
            LedColor2(0xca),
            LedColor3(0xcc),
            LedColor4(0xce),
            PedalSensivity(0xd2),
            DriveFlags(0xd3), // 1bit - Light(DRL?), 2bit - Taillight, 3bit- Light(???), 4bit - StrainGuage, 5bit - BrakeAssist, ,
            SpeakerVolume(0xf5);
            private final int value;

            Param(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        int len = 0;
        int source = 0;
        int destination = 0;
        int command = 0;
        int parameter = 0;
        byte[] data;
        int crc = 0;


        CANMessage(byte[] bArr) {
            if (bArr.length < 7) return;
            len = bArr[0] & 0xff;
            source = bArr[1] & 0xff;
            destination = bArr[2] & 0xff;
            command = bArr[3] & 0xff;
            parameter = bArr[4] & 0xff;
            data = Arrays.copyOfRange(bArr, 5, bArr.length - 2);
            crc = bArr[bArr.length - 1] << 8 + bArr[bArr.length - 2];

        }

        private CANMessage() {

        }

        public byte[] writeBuffer() {

            byte[] canBuffer = getBytes();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0x5A);
            out.write(0xA5);

            try {
                out.write(canBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return out.toByteArray();
        }


        private byte[] getBytes() {

            ByteArrayOutputStream buff = new ByteArrayOutputStream();

            buff.write(len);
            buff.write(source);
            buff.write(destination);
            buff.write(command);
            buff.write(parameter);
            try {
                buff.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            crc = computeCheck(buff.toByteArray());
            buff.write(crc & 0xff);
            buff.write((crc >> 8) & 0xff);
            return crypto(buff.toByteArray());
        }

        private static int computeCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = check + ((int) c & 0xff);
            }
            check ^= 0xFFFF;
            check &= 0xFFFF;

            return check;
        }

        static CANMessage verify(byte[] buffer) {

            Timber.i("Verifying");
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 2, buffer.length);
            dataBuffer = crypto(dataBuffer);

            int check = (dataBuffer[dataBuffer.length - 1] << 8 | ((dataBuffer[dataBuffer.length - 2]) & 0xff)) & 0xffff;
            byte[] dataBufferCheck = Arrays.copyOfRange(dataBuffer, 0, dataBuffer.length - 2);
            int checkBuffer = computeCheck(dataBufferCheck);
            if (check == checkBuffer) {
                Timber.i("Check OK");
            } else {
                Timber.i("Check FALSE, packet: %02X, calc: %02X", check, checkBuffer);
            }
            return (check == checkBuffer) ? new CANMessage(dataBuffer) : null;
        }

        static byte[] crypto(byte[] buffer) {

            byte[] dataBuffer = Arrays.copyOfRange(buffer, 0, buffer.length);
            Timber.i("Initial packet: %s", StringUtil.toHexString(dataBuffer));
            for (int j = 1; j < dataBuffer.length; j++) {
                dataBuffer[j] ^= gamma[(j - 1) % 16];
            }
            Timber.i("En/Decrypted packet: %s", StringUtil.toHexString(dataBuffer));
            return dataBuffer;
        }

        public static CANMessage getBleVersion() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.BleVersion.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getKey() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.KeyGenerator.getValue();
            msg.command = Comm.GetKey.getValue();
            msg.parameter = Param.GetKey.getValue();
            msg.data = new byte[]{};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getSerialNumber() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.SerialNumber.getValue();
            msg.data = new byte[]{0x0e};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getVersion() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.Firmware.getValue();
            msg.data = new byte[]{0x06};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getActivationDate() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.ActivationDate.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getLiveData() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.LiveData.getValue();
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getParams1() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.LockMode.getValue();
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getParams2() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.LedMode.getValue();
            msg.data = new byte[]{0x1c};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getParams3() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.SpeakerVolume.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setLimitedMode(Boolean on) {
            byte value = on ? (byte) 1 : 0;
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.LimitedMode.getValue();
            msg.data = new byte[]{value};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms1Sn() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS1.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x10;
            msg.data = new byte[]{0x22};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms1Life() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS1.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x30;
            msg.data = new byte[]{0x18};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms1Cells() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS1.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x40;
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms2Sn() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS2.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x10;
            msg.data = new byte[]{0x22};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms2Life() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS2.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x30;
            msg.data = new byte[]{0x18};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms2Cells() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS2.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x40;
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setDriveFlags(int drFl) {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.DriveFlags.getValue();
            msg.data = new byte[]{(byte)(drFl & 0xFF), (byte)((drFl >> 8)  & 0xFF)};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setLedColor(int value, int ledNum) {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.LedColor1.getValue() + (ledNum-1) * 2;
            if (value < 256) {
                msg.data = new byte[]{(byte)(0xF0), (byte)(value & 0xFF), 0x00, 0x00};
            } else {
                msg.data = new byte[]{(byte) (0x00), (byte) (0x00), 0x00, 0x00};
            }
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setAlarms(int value) {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.Alarms.getValue();
            msg.data = new byte[]{(byte)(value & 0xFF), (byte)((value >> 8)  & 0xFF)};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setAlarmSpeed(int value, int alarmNum) {
            int speed = value * 100;
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            switch (alarmNum) {
                case 1: msg.parameter = Param.Alarm1Speed.getValue(); break;
                case 2: msg.parameter = Param.Alarm2Speed.getValue(); break;
                case 3: msg.parameter = Param.Alarm3Speed.getValue(); break;
            }
            msg.data = new byte[]{(byte)(speed & 0xFF), (byte)((speed >> 8)  & 0xFF)};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setLimitedSpeed(int value) {
            int speed = value * 100;
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.LimitModeSpeed.getValue();
            msg.data = new byte[]{(byte)(speed & 0xFF), (byte)((speed >> 8)  & 0xFF)};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setPedalSensivity(int value) {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.PedalSensivity.getValue();
            msg.data = new byte[]{(byte)(value & 0xFF), (byte)((value >> 8)  & 0xFF)};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setLedMode(int value) {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.LedMode.getValue();
            msg.data = new byte[]{(byte)(value & 0xFF), (byte)((value >> 8)  & 0xFF)};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setSpeakerVolume(int value) {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.SpeakerVolume.getValue();
            msg.data = new byte[]{(byte)(value & 0xFF), (byte)((value >> 8)  & 0xFF)};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage setLockMode(Boolean on) {
            byte value = on ? (byte) 1 : 0;
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.LockMode.getValue();
            msg.data = new byte[]{value, 0x00};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage runCalibration(Boolean on) {
            byte value = on ? (byte) 1 : 0;
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Write.getValue();
            msg.parameter = Param.Calibration.getValue();
            msg.data = new byte[]{value, 0x00};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        private byte[] parseKey() {
            byte[] gammaTemp = Arrays.copyOfRange(data, 0, data.length);
            StringBuilder gamma_text = new StringBuilder();
            for (byte datum : data) {
                gamma_text.append(String.format("%02X", datum));
            }
            Timber.i("New key: %s", gamma_text.toString());
            return gammaTemp;
        }

        void parseSerialNumber() {
            String serialNumber = new String(data);
            WheelData wd = WheelData.getInstance();
            wd.setSerial(serialNumber);
            wd.setModel("Ninebot Z");
        }

        void parseParams1() {
            getInstance().lockMode = MathsUtil.shortFromBytesLE(data, 0);
            getInstance().limitedMode = MathsUtil.shortFromBytesLE(data, 4);
            getInstance().limitModeSpeed1Km = MathsUtil.shortFromBytesLE(data, 6)/100;
            getInstance().limitModeSpeed = MathsUtil.shortFromBytesLE(data, 8)/100;
            getInstance().alarms = MathsUtil.shortFromBytesLE(data, 24);
            getInstance().alarm1Speed = MathsUtil.shortFromBytesLE(data, 26)/100;
            getInstance().alarm2Speed = MathsUtil.shortFromBytesLE(data, 28)/100;
            getInstance().alarm3Speed = MathsUtil.shortFromBytesLE(data, 30)/100;
            appConfig.setLockMode(getInstance().lockMode==1);
            appConfig.setWheelLimitedModeEnabled(getInstance().limitedMode == 1);
            appConfig.setWheelLimitedModeSpeed(getInstance().limitModeSpeed);
            appConfig.setWheelAlarm1Speed(getInstance().alarm1Speed);
            appConfig.setWheelAlarm2Speed(getInstance().alarm2Speed);
            appConfig.setWheelAlarm3Speed(getInstance().alarm3Speed);
            appConfig.setWheelAlarm1Enabled((getInstance().alarms & 0x0001) == 1);
            appConfig.setWheelAlarm2Enabled(((getInstance().alarms >> 1) & 0x0001) == 1);
            appConfig.setWheelAlarm3Enabled(((getInstance().alarms >> 2) & 0x0001) == 1);
        }

        void parseParams2() {
            getInstance().ledMode = MathsUtil.shortFromBytesLE(data, 0);
            getInstance().ledColor1 = (MathsUtil.intFromBytesLE(data, 4) >> 16) & 0xFF;
            getInstance().ledColor2 = (MathsUtil.intFromBytesLE(data, 8) >> 16) & 0xFF;
            getInstance().ledColor3 = (MathsUtil.intFromBytesLE(data, 12) >> 16) & 0xFF;
            getInstance().ledColor4 = (MathsUtil.intFromBytesLE(data, 16) >> 16) & 0xFF;
            getInstance().pedalSensivity = MathsUtil.shortFromBytesLE(data, 24);
            getInstance().driveFlags = MathsUtil.shortFromBytesLE(data, 26);
            appConfig.setLedMode(Integer.toString(getInstance().ledMode));
            appConfig.setPedalSensivity(getInstance().pedalSensivity);
            appConfig.setLightEnabled(((getInstance().driveFlags >> 2) & 0x0001) == 1);
            appConfig.setTaillightEnabled(((getInstance().driveFlags >> 1) & 0x0001) == 1);
            appConfig.setDrlEnabled((getInstance().driveFlags & 0x0001) == 1);
            appConfig.setHandleButtonDisabled(((getInstance().driveFlags >> 3) & 0x0001) == 0);
            appConfig.setBrakeAssistantEnabled(((getInstance().driveFlags >> 4) & 0x0001) == 1);
        }

        void parseParams3() {
            getInstance().speakerVolume = MathsUtil.shortFromBytesLE(data, 0) >> 3;
            appConfig.setSpeakerVolume(getInstance().speakerVolume);
        }

        void parseVersionNumber() {
            String versionNumber = "";
            WheelData wd = WheelData.getInstance();
            versionNumber += String.format("%X.", (data[1] & 0x0f));
            versionNumber += String.format("%1X.", (data[0] >> 4) & 0x0f);
            versionNumber += String.format("%1X", (data[0]) & 0x0f);
            int error1 = data[2];
            int error2 = data[3];
            int warn1 = data[4];
            int warn2 = data[5];
            wd.setVersion(versionNumber);
            String error = "No";
            if (error1 != 0) {
                error = getErrorString(error1);
                if (error2 != 0){
                    error = error + "\n" + getErrorString(error2);
                }
            }
            wd.setError(error);

        }

        void parseActivationDate() { ////// ToDo: add to wheeldata
            WheelData wd = WheelData.getInstance();
            int activationDate = MathsUtil.shortFromBytesLE(data, 0);
            int year = activationDate>>9;
            int mounth = (activationDate>>5) & 0x0f;
            int day = activationDate & 0x1f;
            String activationDateStr = String.format("%02d.%02d.20%02d", day, mounth,year);
            //wd.setActivationDate(activationDateStr); fixme
        }

        void parseLiveData() {
            WheelData wd = WheelData.getInstance();
            int errorcode = MathsUtil.shortFromBytesLE(data, 0);
            int alarmcode = MathsUtil.shortFromBytesLE(data, 2);
            int escstatus = MathsUtil.shortFromBytesLE(data, 4);
            int batt = MathsUtil.shortFromBytesLE(data, 8);
            int speed = MathsUtil.shortFromBytesLE(data, 10);
            int avgspeed = MathsUtil.shortFromBytesLE(data, 12);
            int distance = MathsUtil.intFromBytesLE(data, 14);
            int tripdistance = MathsUtil.shortFromBytesLE(data, 18) * 10;
            int operatingtime = MathsUtil.shortFromBytesLE(data, 20);
            int temperature = MathsUtil.signedShortFromBytesLE(data, 22);
            int voltage = MathsUtil.shortFromBytesLE(data, 24);
            int current = MathsUtil.signedShortFromBytesLE(data, 26);
            //int speed = MathsUtil.shortFromBytesLE(data, 28); //the same as speed
            //int avgspeed = MathsUtil.shortFromBytesLE(data, 30); //the same as avgspeed

            String alert;
            //alert = String.format(Locale.ENGLISH, "error: %04X, warn: %04X, status: %04X", errorcode, alarmcode, escstatus);

            wd.setSpeed(speed);
            wd.setVoltage(voltage);
            wd.setCurrent(current);
            wd.setTotalDistance(distance);
            wd.setTemperature(temperature * 10);
            //wd.setAlert(alert);
            wd.setBatteryLevel(batt);
            wd.calculatePwm();
            wd.calculatePower();
        }

        void parseBmsSn(int bmsnum) {
            WheelData wd = WheelData.getInstance();
            String serialNumber = new String(data, 0, 14);
            String versionNumber = "";
            versionNumber += String.format("%X.", (data[15]));
            versionNumber += String.format("%1X.", (data[14] >> 4) & 0x0f);
            versionNumber += String.format("%1X", (data[14]) & 0x0f);
            int factoryCap = MathsUtil.shortFromBytesLE(data, 16);
            int actualCap = MathsUtil.shortFromBytesLE(data, 18);
            int fullCycles = MathsUtil.shortFromBytesLE(data, 22);
            int chargeCount = MathsUtil.shortFromBytesLE(data, 24);
            int mfgDate = MathsUtil.shortFromBytesLE(data, 32);
            int year = mfgDate >> 9;
            int mounth = (mfgDate >> 5) & 0x0f;
            int day = mfgDate & 0x1f;
            String mfgDateStr = String.format("%02d.%02d.20%02d", day, mounth, year);
            SmartBms bms = bmsnum == 1 ? wd.getBms1() : wd.getBms2();
            bms.setSerialNumber(serialNumber);
            bms.setVersionNumber(versionNumber);
            bms.setFactoryCap(factoryCap);
            bms.setActualCap(actualCap);
            bms.setFullCycles(fullCycles);
            bms.setChargeCount(chargeCount);
            bms.setMfgDateStr(mfgDateStr);
        }

        void parseBmsLife(int bmsnum) {
            WheelData wd = WheelData.getInstance();
            int bmsStatus = MathsUtil.shortFromBytesLE(data, 0);
            int remCap = MathsUtil.shortFromBytesLE(data, 2);
            int remPerc = MathsUtil.shortFromBytesLE(data, 4);
            int current = MathsUtil.signedShortFromBytesLE(data, 6);
            int voltage = MathsUtil.shortFromBytesLE(data, 8);
            int temp1 = data[10] - 20;
            int temp2 = data[11] - 20;
            int balanceMap = MathsUtil.shortFromBytesLE(data, 12);
            int health = MathsUtil.shortFromBytesLE(data, 22);
            SmartBms bms = bmsnum == 1 ? wd.getBms1() : wd.getBms2();
            bms.setStatus(bmsStatus);
            bms.setRemCap(remCap);
            bms.setRemPerc(remPerc);
            bms.setCurrent(current / 100.0);
            bms.setVoltage(voltage / 100.0);
            bms.setTemp1(temp1);
            bms.setTemp2(temp2);
            bms.setBalanceMap(balanceMap);
            bms.setHealth(health);
        }

        void parseBmsCells(int bmsnum) {
            WheelData wd = WheelData.getInstance();
            int cell1 = MathsUtil.shortFromBytesLE(data, 0);
            int cell2 = MathsUtil.shortFromBytesLE(data, 2);
            int cell3 = MathsUtil.shortFromBytesLE(data, 4);
            int cell4 = MathsUtil.shortFromBytesLE(data, 6);
            int cell5 = MathsUtil.shortFromBytesLE(data, 8);
            int cell6 = MathsUtil.shortFromBytesLE(data, 10);
            int cell7 = MathsUtil.shortFromBytesLE(data, 12);
            int cell8 = MathsUtil.shortFromBytesLE(data, 14);
            int cell9 = MathsUtil.shortFromBytesLE(data, 16);
            int cell10 = MathsUtil.shortFromBytesLE(data, 18);
            int cell11 = MathsUtil.shortFromBytesLE(data, 20);
            int cell12 = MathsUtil.shortFromBytesLE(data, 22);
            int cell13 = MathsUtil.shortFromBytesLE(data, 24);
            int cell14 = MathsUtil.shortFromBytesLE(data, 26);
            int cell15 = MathsUtil.shortFromBytesLE(data, 28);
            int cell16 = MathsUtil.shortFromBytesLE(data, 30);
            int cellNum = 14;
            SmartBms bms = bmsnum == 1 ? wd.getBms1() : wd.getBms2();
            bms.getCells()[0] = cell1 / 1000.0;
            bms.getCells()[1] = cell2 / 1000.0;
            bms.getCells()[2] = cell3 / 1000.0;
            bms.getCells()[3] = cell4 / 1000.0;
            bms.getCells()[4] = cell5 / 1000.0;
            bms.getCells()[5] = cell6 / 1000.0;
            bms.getCells()[6] = cell7 / 1000.0;
            bms.getCells()[7] = cell8 / 1000.0;
            bms.getCells()[8] = cell9 / 1000.0;
            bms.getCells()[9] = cell10 / 1000.0;
            bms.getCells()[10] = cell11 / 1000.0;
            bms.getCells()[11] = cell12 / 1000.0;
            bms.getCells()[12] = cell13 / 1000.0;
            bms.getCells()[13] = cell14 / 1000.0;
            bms.getCells()[14] = cell15 / 1000.0;
            bms.getCells()[15] = cell16 / 1000.0;
            if (cell15 > 0) cellNum = 15;
            if (cell16 > 0) cellNum = 16;
            if (bms.getCellNum() != cellNum) {
                bms.setCellNum(cellNum);
                wd.reconfigureBMSPage();
            }
            bms.setMinCell(bms.getCells()[0]);
            bms.setMaxCell(bms.getCells()[0]);
            bms.setMaxCellNum(1);
            bms.setMinCellNum(1);
            double totalVolt = 0.0;
            for (int i =0; i < bms.getCellNum(); i++) {
                double cell = bms.getCells()[i];
                if (cell > 0.0) {
                    totalVolt += cell;
                    if (bms.getMaxCell() < cell) {
                        bms.setMaxCell(cell);
                        bms.setMaxCellNum(i+1);
                    }
                    if (bms.getMinCell() > cell) {
                        bms.setMinCell(cell);
                        bms.setMinCellNum(i+1);
                    }
                }
            }
            bms.setCellDiff(bms.getMaxCell()-bms.getMinCell());
            bms.setAvgCell(totalVolt/bms.getCellNum());
        }

        public byte[] getData() {
            return data;
        }
    }

    static class NinebotZUnpacker {

        enum UnpackerState {
            unknown,
            started,
            collecting,
            done
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        int len = 0;
        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {

            switch (state) {
                case collecting:
                    buffer.write(c);
                    if (buffer.size() == len + 9) {
                        state = UnpackerState.done;
                        updateStep = 0;
                        Timber.i("Len %d", len);
                        Timber.i("Step reset");
                        return true;
                    }
                    break;
                case started:
                    buffer.write(c);
                    len = c & 0xff;
                    state = UnpackerState.collecting;
                    break;
                default:
                    if (c == (byte) 0xA5 && oldc == (byte) 0x5A) {
                        Timber.i("Find start");
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0x5A);
                        buffer.write(0xA5);
                        state = UnpackerState.started;
                    }
                    oldc = c;
            }
            return false;
        }
    }

    public static NinebotZAdapter getInstance() {
        Timber.i("Get instance");
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new NinebotZAdapter();
        }
        return INSTANCE;
    }

    public static synchronized void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("New instance");
        INSTANCE = new NinebotZAdapter();
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
