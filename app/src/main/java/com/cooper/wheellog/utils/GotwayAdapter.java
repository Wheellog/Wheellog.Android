package com.cooper.wheellog.utils;

import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import timber.log.Timber;

public class GotwayAdapter extends BaseAdapter {
    private static GotwayAdapter INSTANCE;
    gotwayUnpacker unpacker = new gotwayUnpacker();
    private static final double RATIO_GW = 0.875;
    private String model = "";
    private String imu = "";
    private String fw = "";
    private boolean trueVoltage = false;
    private boolean trueCurrent = false;
    private boolean truePWM = false;
    private boolean bIsReady = false;
    private long lastTryTime = 0;
    private long lastFFTime = 0;
    private int frameFFcount = 0;
    private int attempt = 0;
    private int lock_Changes = 0;
    private final int lightModeOff = 0;
    private final int lightModeOn = 1;
    private final int lightModeStrobe = 2;
    private final int alarmModeTwo = 0; // 30 + 35 (45) km/h + 80% PWM
    private final int alarmModeOne = 1; // 35 (45) km/h + 80% PWM
    private final int alarmModeOff = 2; // 80% PWM only
    private final int alarmModeCF = 3; // PWM tiltback for custom firmware

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode Gotway/Begode");

        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();
        boolean newDataFound = false;
        if ((model.length() == 0) || (fw.length() == 0)) { // IMU sent at the begining, so there is no sense to check it, we can't request it
            String dataS = new String(data, 0, data.length).trim();
            if (dataS.startsWith("NAME")) {
                model = dataS.substring(5).trim();
                wd.setModel(model);
            } else if (dataS.startsWith("GW")) {
                fw = dataS.substring(2).trim();
                wd.setVersion(fw);
                WheelLog.AppConfig.setHwPwm(false);
                WheelLog.AppConfig.setIsAlexovikFW(false);
                bIsReady = true;
            } else if (dataS.startsWith("CF")) {
                fw = dataS.substring(2).trim();
                wd.setVersion(fw);
                WheelLog.AppConfig.setHwPwm(true);
                WheelLog.AppConfig.setIsAlexovikFW(false);
                bIsReady = true;
            } else if (dataS.startsWith("BF")) {
                fw = dataS.substring(2).trim();
                wd.setVersion(fw);
                model = "Begode Alexovik";
                wd.setModel(model);
                WheelLog.AppConfig.setHwPwm(true);
                WheelLog.AppConfig.setIsAlexovikFW(true);
                bIsReady = true;
            } else if (dataS.startsWith("MPU")) {
                imu = dataS.substring(1, 7).trim();
            }
        }
        for (byte c : data) {
            if (unpacker.addChar(c)) {

                byte[] buff = unpacker.getBuffer();
                Boolean bIsAlexovikFW = WheelLog.AppConfig.getIsAlexovikFW();
                Boolean useRatio = WheelLog.AppConfig.getUseRatio();
                Boolean useBetterPercents = WheelLog.AppConfig.getUseBetterPercents();
                Boolean autoVoltage = !bIsAlexovikFW ? WheelLog.AppConfig.getAutoVoltage() : false;
                int gotwayNegative = Integer.parseInt(WheelLog.AppConfig.getGotwayNegative());
                if (buff[18] == (byte) 0x07) System.out.println(String.format(Locale.US,StringUtil.toHexString(buff)));
                //System.out.println(String.format(Locale.US, "type: %d", buff[18]));
                if (buff[18] == (byte) 0x00) {
                    Timber.i("Begode frame A found (live data)");
                    int voltage = MathsUtil.shortFromBytesBE(buff, 2);
                    int speed = (int) Math.round(MathsUtil.signedShortFromBytesBE(buff, 4) * 3.6);
                    int distance = MathsUtil.shortFromBytesBE(buff, 8);
                    int phaseCurrent = MathsUtil.signedShortFromBytesBE(buff, 10);
                    int temperature;
                    if (!bIsAlexovikFW)
                        temperature = (int) Math.round((((float) MathsUtil.signedShortFromBytesBE(buff, 12) / 340.0) + 36.53) * 100);  // mpu6050
                    else
                        temperature = (int) Math.round((((float) MathsUtil.signedShortFromBytesBE(buff, 12) / 333.87) + 21.00) * 100); // mpu6500

                    int hwPwm = MathsUtil.signedShortFromBytesBE(buff, 14) * 10;
                    if (gotwayNegative == 0) {
                        speed = Math.abs(speed);
                        phaseCurrent = Math.abs(phaseCurrent);
                        hwPwm = Math.abs(hwPwm);
                    } else {
                        phaseCurrent = phaseCurrent * gotwayNegative;
                        if (!bIsAlexovikFW)
                        {
                            speed = speed * gotwayNegative;
                            hwPwm = hwPwm * gotwayNegative;
                        }
                    }

                    int battery;
                    if (useBetterPercents) {
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

                    if (useRatio) {
                        distance = (int) Math.round(distance * RATIO_GW);
                        speed = (int) Math.round(speed * RATIO_GW);
                    }
                    voltage = (int) Math.round(getScaledVoltage(voltage));

                    wd.setSpeed(speed);
                    wd.setTopSpeed(speed);
                    wd.setWheelDistance(distance);
                    wd.setTemperature(temperature);
                    wd.setPhaseCurrent(phaseCurrent);
                    if (!(trueVoltage && autoVoltage)) {
                        wd.setVoltage(voltage);
                    }
                    wd.setBatteryLevel(battery);
                    if (!truePWM) {
                        wd.setOutput(hwPwm);
                    }
                    if (!trueCurrent) {
                        wd.calculateCurrent();
                    }
                    newDataFound = !((trueVoltage && autoVoltage) || trueCurrent);
                } else if (buff[18] == (byte) 0x01) {
                    if (!bIsAlexovikFW)
                    {
                        newDataFound = !trueCurrent && (trueVoltage && autoVoltage);
                        trueVoltage = true;
                        int pwmlimit = MathsUtil.shortFromBytesBE(buff, 2);
                        int batVoltage = MathsUtil.shortFromBytesBE(buff, 6);
                        int something5000 = MathsUtil.shortFromBytesBE(buff, 12);
                        if (autoVoltage) wd.setVoltage(batVoltage * 10);
                    }
                    else
                    {
                        int pedalsMode = buff[6] & 0x03;
                        if (lock_Changes == 0)
                            WheelLog.AppConfig.setPedalsMode(String.valueOf(pedalsMode));
                        else
                            lock_Changes -= 1;
                    }
                } else if (buff[18] == (byte) 0x03) {
                    int zero = MathsUtil.shortFromBytesBE(buff, 2);
                } else if (buff[18] == (byte) 0x04) {
                    Timber.i("Begode frame B found (total distance and flags)");

                    int totalDistance = (int) MathsUtil.getInt4(buff, 2);
                    if (useRatio) {
                        wd.setTotalDistance(Math.round(totalDistance * RATIO_GW));
                    } else {
                        wd.setTotalDistance(totalDistance);
                    }
                    if (!bIsAlexovikFW)
                    {
                        int settings = MathsUtil.shortFromBytesBE(buff, 6);
                        int pedalsMode = (settings >> 13) & 0x03;
                        int speedAlarms = (settings >> 10) & 0x03;
                        int rollAngle = (settings >> 7) & 0x03;
                        int inMiles = settings & 0x01;
                        int powerOffTime = MathsUtil.shortFromBytesBE(buff, 8);
                        int tiltBackSpeed = MathsUtil.shortFromBytesBE(buff, 10);
                        if (tiltBackSpeed >= 100) tiltBackSpeed = 0;
                        int alert = buff[14] & 0xFF;
                        int ledMode = buff[13] & 0xFF;
                        int lightMode = buff[15] & 0x03;
                        if (lock_Changes == 0) {
                            WheelLog.AppConfig.setPedalsMode(String.valueOf(2 - pedalsMode));
                            WheelLog.AppConfig.setAlarmMode(String.valueOf(speedAlarms)); //CheckMe
                            WheelLog.AppConfig.setLightMode(String.valueOf(lightMode));
                            WheelLog.AppConfig.setLedMode(String.valueOf(ledMode));
                            if (!fw.equals("-"))
                            {
                                WheelLog.AppConfig.setGwInMiles(inMiles == 1);
                                WheelLog.AppConfig.setWheelMaxSpeed(tiltBackSpeed);
                                WheelLog.AppConfig.setRollAngle(String.valueOf(rollAngle));
                            }
                        } else {
                            lock_Changes -= 1;
                        }

                        String alertLine = "";
                        //if ((alert & 0x01) == 1) alertLine += "HighPower ";
                        wd.setWheelAlarm((alert & 0x01) == 1);
                        if (((alert >> 1) & 0x01) == 1) alertLine += "Speed2 ";
                        if (((alert >> 2) & 0x01) == 1) alertLine += "Speed1 ";
                        if (((alert >> 3) & 0x01) == 1) alertLine += "LowVoltage ";
                        if (((alert >> 4) & 0x01) == 1) alertLine += "OverVoltage ";
                        if (((alert >> 5) & 0x01) == 1) alertLine += "OverTemperature ";
                        if (((alert >> 6) & 0x01) == 1) alertLine += "errHallSensors ";
                        if (((alert >> 7) & 0x01) == 1) alertLine += "TransportMode";
                        wd.setAlert(alertLine);

                        if (alertLine != "" && getContext() != null)
                        {
                            Timber.i("News to send: %s, sending Intent", alertLine);
                            Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                            intent.putExtra(Constants.INTENT_EXTRA_NEWS, alertLine);
                            getContext().sendBroadcast(intent);
                        }
                    }
                } else if (buff[18] == (byte) 0x07) {
                    if (!bIsAlexovikFW)
                    {
                        newDataFound = trueCurrent;
                        trueCurrent = true;
                        int batteryCurrent = MathsUtil.signedShortFromBytesBE(buff, 2);
                        int hwPWMb = MathsUtil.signedShortFromBytesBE(buff, 8);
                        if (hwPWMb > 0) {
                            truePWM = true;
                        }
                        if (truePWM) {
                            if (gotwayNegative == 0) {
                                hwPWMb = Math.abs(hwPWMb);
                            } else {
                                hwPWMb = hwPWMb * gotwayNegative * (-1);
                            }
                            wd.setOutput(hwPWMb * 100);
                        }
                        wd.setCurrent((-1) * batteryCurrent);
                    }
                } else if (buff[18] == (byte) 0xFF) {
                    if (!bIsAlexovikFW) {
                        checkFirmware();
                    }
                    if (lock_Changes == 0) {
                        WheelLog.AppConfig.setExtremeMode((buff[2] & 0x01) != (byte) 0);
                        WheelLog.AppConfig.setBrakingCurrent(buff[3] & 0xFF);
                        WheelLog.AppConfig.setRotationControl((buff[4] & 0x01) != (byte) 0);
                        WheelLog.AppConfig.setRotationAngle((buff[5] & 0xFF) + 260);
                        WheelLog.AppConfig.setAdvancedSettings((buff[6] & 0x01)!= (byte) 0);
                        WheelLog.AppConfig.setProportionalFactor(buff[7] & 0xFF);
                        WheelLog.AppConfig.setIntegralFactor(buff[8] & 0xFF);
                        WheelLog.AppConfig.setDifferentialFactor(buff[9] & 0xFF);
                        WheelLog.AppConfig.setDynamicCompensation(buff[10] & 0xFF);
                        WheelLog.AppConfig.setDynamicCompensationFilter(buff[11] & 0xFF);
                        WheelLog.AppConfig.setAccelerationCompensation(buff[12] & 0xFF);
                        WheelLog.AppConfig.setProportionalCurrentFactorQ(buff[14] & 0xFF);
                        WheelLog.AppConfig.setIntegralCurrentFactorQ(buff[15] & 0xFF);
                        WheelLog.AppConfig.setProportionalCurrentFactorD(buff[16] & 0xFF);
                        WheelLog.AppConfig.setIntegralCurrentFactorD(buff[17] & 0xFF);
                    } else {
                        lock_Changes -= 1;
                    }
                }
                if (newDataFound) {
                    Boolean hwPwmEnabled = WheelLog.AppConfig.getHwPwm();
                    wd.calculatePower();
                    if (hwPwmEnabled || truePWM) {
                        wd.updatePwm();
                    } else {
                        wd.calculatePwm();
                    }
                }

                if (attempt < 10)
                {
                    long nowTime = SystemClock.elapsedRealtime();
                    if (nowTime - lastTryTime > 190)
                    {
                        if (fw.equals(""))
                            sendCommand("V", "", 0);
                        else if (model.equals(""))
                            sendCommand("N", "", 0);

                        attempt += 1;
                        lastTryTime = nowTime;
                    }
                }
                else
                {
                    if (model.equals("")) {
                        model = "Begode";
                        wd.setVersion(model);
                    } else if (fw.equals("")) {
                        fw = "-";
                        wd.setVersion(fw);
                        WheelLog.AppConfig.setHwPwm(false);
                        WheelLog.AppConfig.setIsAlexovikFW(false);
                        bIsReady = true;
                    }
                }
            }
        }

        return newDataFound;
    }

    @Override
    public boolean isReady() {
        return bIsReady && (WheelData.getInstance().getVoltage() != 0);
    }

    private void sendCommand(String s) {
        sendCommand(s, "b", 100);
    }

    private void sendCommand(String s, String delayed) {
        sendCommand(s, delayed, 100);
    }

    private void sendCommand(String s, String delayed, int timer) {
        sendCommand(s.getBytes(), delayed.getBytes(), timer);
    }

    private void sendCommand(byte[] s, byte[] delayed, int timer) {
        WheelData.getInstance().bluetoothCmd(s);

        if (timer > 0) {
            new Handler().postDelayed(() -> WheelData.getInstance().bluetoothCmd(delayed), timer);
        }
    }

    @Override
    public void updatePedalsMode(int pedalsMode) {
        String command = "";
        switch (pedalsMode) {
            case 0: command = "h"; break;
            case 1: command = "f"; break;
            case 2: command = "s"; break;
            case 3: command = "i"; break;
        }
        lock_Changes = 2;
        sendCommand(command);
    }

    //begin Alexovik
    private void checkFirmware() {
        long nowTime = SystemClock.elapsedRealtime();
        long interval = nowTime - lastFFTime;
        lastFFTime = nowTime;

        if (interval > 80 && interval < 120)
            frameFFcount++;
        else
            frameFFcount = 0;

        if (frameFFcount > 5) {
            model = "Begode Alexovik";
            WheelData.getInstance().setModel(model);
            WheelLog.AppConfig.setHwPwm(true);
            WheelLog.AppConfig.setIsAlexovikFW(true);
            bIsReady = true;
            frameFFcount = 0;
        }
    }

    public void updateExtremeMode(boolean value) {
        byte[] cmd = { (byte)0x45, (byte)0x4D, (byte)(value ? 1 : 0) };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateBrakingCurrent(int value) {
        byte[] cmd = { (byte)0x42, (byte)0x41, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateRotationControl(boolean value) {
        byte[] cmd = { (byte)0x52, (byte)0x43, (byte)(value ? 1 : 0) };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateRotationAngle(int value) {
        byte[] cmd = { (byte)0x72, (byte)0x73, (byte)(value - 260) };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateAdvancedSettings(boolean value) {
        byte[] cmd = { (byte)0x61, (byte)0x73, (byte)(value ? 1 : 0) };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateProportionalFactor(int value) {
        byte[] cmd = { (byte)0x68, (byte)0x70, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateIntegralFactor(int value) {
        byte[] cmd = { (byte)0x68, (byte)0x69, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateDifferentialFactor(int value) {
        byte[] cmd = { (byte)0x68, (byte)0x64, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateDynamicCompensation(int value) {
        byte[] cmd = { (byte)0x68, (byte)0x63, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateDynamicCompensationFilter(int value) {
        byte[] cmd = { (byte)0x68, (byte)0x66, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateAccelerationCompensation(int value) {
        byte[] cmd = { (byte)0x61, (byte)0x63, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updatePCurrentQ(int value) {
        byte[] cmd = { (byte)0x63, (byte)0x70, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateICurrentQ(int value) {
        byte[] cmd = { (byte)0x63, (byte)0x69, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updatePCurrentD(int value) {
        byte[] cmd = { (byte)0x64, (byte)0x70, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }

    public void updateICurrentD(int value) {
        byte[] cmd = { (byte)0x64, (byte)0x69, (byte)value };
        lock_Changes = 2;
        WheelData.getInstance().bluetoothCmd(cmd);
    }
    //end Alexovik

    @Override
    public void switchFlashlight() {
        int lightMode = Integer.parseInt(WheelLog.AppConfig.getLightMode()) + 1;
        if (lightMode > lightModeStrobe) {
            lightMode = lightModeOff;
        }
        // Strobe light not available on Freestyl3r firmware while pwm tiltback mode enabled.
        // For custom firmware with enabled tiltback available only light off and on.
        // Strobe is using for tiltback warning. Detect via specific for this firmware alarm mode.
        if (lightMode > lightModeOn
                && WheelLog.AppConfig.getAlarmMode().equals(String.valueOf(alarmModeCF))) {
            lightMode = lightModeOff;
        }
        WheelLog.AppConfig.setLightMode(String.valueOf(lightMode));
        setLightMode(lightMode);
    }

    @Override
    public void setLightMode(int lightMode) {
        lock_Changes = 2;
        String command = "";
        switch (lightMode) {
            default:
            case lightModeOff: command = "E"; break;
            case lightModeOn: command = "Q"; break;
            case lightModeStrobe: command = "T"; break;
        }
        sendCommand(command);
    }

    @Override
    public void setMilesMode(boolean milesMode) {
        String command = "";
        lock_Changes = 2;
        if (milesMode) {
            command = "m";
        } else {
            command = "g";
        }
        sendCommand(command);
    }

    @Override
    public void setRollAngleMode(int rollAngle) {
        String command = "";
        lock_Changes = 2;
        switch (rollAngle) {
            case 0: command = ">"; break;
            case 1: command = "="; break;
            case 2: command = "<"; break;
        }
        sendCommand(command);
    }

    @Override
    public void updateLedMode(int ledMode) {
        final byte[] param = new byte[1];
        lock_Changes = 5;
        param[0] = (byte) ((ledMode % 10) + 0x30);
        new Handler().postDelayed(() -> sendCommand("W", "M"), 100);
        new Handler().postDelayed(() -> sendCommand(param, "b".getBytes(), 100), 300);
    }

    @Override
    public void updateBeeperVolume(int beeperVolume) {
        final byte[] param = new byte[1];
        param[0] = (byte) ((beeperVolume % 10) + 0x30);
        new Handler().postDelayed(() -> sendCommand("W", "B"), 100);
        new Handler().postDelayed(() -> sendCommand(param, "b".getBytes(), 100), 300);
    }

    @Override
    public void updateAlarmMode(int alarmMode) {
        String command = "";
        switch (alarmMode) {
            case alarmModeTwo: command = "o"; break;
            case alarmModeOne: command = "u"; break;
            case alarmModeOff: command = "i"; break;
            case alarmModeCF: command = "I"; break;
        }
        lock_Changes = 2;
        sendCommand(command);
    }

    @Override
    public void wheelCalibration() {
        sendCommand("c", "y", 300);
    }

    @Override
    public int getCellsForWheel() {
        switch (WheelLog.AppConfig.getGotwayVoltage()) {
            case "0":
                return 16;
            case "1":
                return 20;
            case "2":
                return 24;
            case "3":
                return 32;
            case "4":
                return 32;
        }
        return 24;
    }

    @Override
    public void wheelBeep() {
        WheelData.getInstance().bluetoothCmd("b".getBytes());
    }

    @Override
    public void updateMaxSpeed(final int maxSpeed) {
        final byte[] hhh = new byte[1];
        final byte[] lll = new byte[1];
        lock_Changes = 5;
        if (maxSpeed != 0) {
            hhh[0] = (byte) ((maxSpeed / 10) + 0x30);
            lll[0] = (byte) ((maxSpeed % 10) + 0x30);
            WheelData.getInstance().bluetoothCmd("b".getBytes());
            new Handler().postDelayed(() -> sendCommand("W", "Y"), 100);
            new Handler().postDelayed(() -> sendCommand(hhh, lll, 100), 300);
            new Handler().postDelayed(() -> sendCommand("b", "b"), 500);
        } else {
            sendCommand("b", "\"");
            new Handler().postDelayed(() -> sendCommand("b", "b"), 200);
        }
    }

    static class gotwayUnpacker {

        enum UnpackerState {
            unknown,
            collecting,
            done
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        gotwayUnpacker.UnpackerState state = UnpackerState.unknown;
        int oldc = -1;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {
            if (state == UnpackerState.collecting) {
                buffer.write(c);
                oldc = c;
                int size = buffer.size();
                if (size > 20 && size <= 24 && c != (byte) 0x5A) {
                    Timber.i("Invalid frame footer (expected 18 5A 5A 5A 5A)");
                    state = UnpackerState.unknown;
                    return false;
                }
                if (size == 24) {
                    state = UnpackerState.done;
                    Timber.i("Valid frame received");
                    return true;
                }
            } else {
                if (c == (byte) 0xAA && oldc == (byte) 0x55) {
                    Timber.i("Frame header found (55 AA), collecting data");
                    buffer = new ByteArrayOutputStream();
                    buffer.write(0x55);
                    buffer.write(0xAA);
                    state = UnpackerState.collecting;
                }
                oldc = c;
            }
            return false;
        }
    }

    public static GotwayAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GotwayAdapter();
        }
        return INSTANCE;
    }

    private double getScaledVoltage(double value) {
        int voltage = 0;
        double scaler = 1.0;
        if (!WheelLog.AppConfig.getGotwayVoltage().equals("")) {
            voltage = Integer.parseInt(WheelLog.AppConfig.getGotwayVoltage());
        }
        switch (voltage) {
            case 0:
                scaler = 1.0;
                break;
            case 1:
                scaler = 1.25;
                break;
            case 2:
                scaler = 1.5;
                break;
            case 3:
                scaler = 1.7380952380952380952380952380952;
                break;
            case 4:
                scaler = 2.0;
                break;
            case 5:
                scaler = 2.5;
                break;
        }
        return value * scaler;
    }
}


/*
    Gotway/Begode reverse-engineered protocol

    Gotway uses byte stream from a serial port via Serial-to-BLE adapter.
    There are two types of frames, A and B. Normally they alternate.
    Most numeric values are encoded as Big Endian (BE) 16 or 32 bit integers.
    The protocol has no checksums.

    Since the BLE adapter has no serial flow control and has limited input buffer,
    data come in variable-size chunks with arbitrary delays between chunks. Some
    bytes may even be lost in case of BLE transmit buffer overflow.

         0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23
        -----------------------------------------------------------------------
     A: 55 AA 19 F0 00 00 00 00 00 00 01 2C FD CA 00 01 FF F8 00 18 5A 5A 5A 5A
     B: 55 AA 00 0A 4A 12 48 00 1C 20 00 2A 00 03 00 07 00 08 04 18 5A 5A 5A 5A
     A: 55 AA 19 F0 00 00 00 00 00 00 00 F0 FD D2 00 01 FF F8 00 18 5A 5A 5A 5A
     B: 55 AA 00 0A 4A 12 48 00 1C 20 00 2A 00 03 00 07 00 08 04 18 5A 5A 5A 5A
        ....

    Frame A:
        Bytes 0-1:   frame header, 55 AA
        Bytes 2-3:   BE voltage, fixed point, 1/100th (assumes 67.2 battery, rescale for other voltages)
        Bytes 4-5:   BE speed, fixed point, 3.6 * value / 100 km/h
        Bytes 6-9:   BE distance, 32bit fixed point, meters
        Bytes 10-11: BE current, signed fixed point, 1/100th amperes
        Bytes 12-13: BE temperature, (value / 340 + 36.53) / 100, Celsius degrees (MPU6050 native data)
        Bytes 14-17: unknown
        Byte  18:    frame type, 00 for frame A
        Byte  19:    18 frame footer
        Bytes 20-23: frame footer, 5A 5A 5A 5A

    Frame B:
        Bytes 0-1:   frame header, 55 AA
        Bytes 2-5:   BE total distance, 32bit fixed point, meters
        Byte  6:     pedals mode (high nibble), speed alarms (low nibble)
        Bytes 7-12:  unknown
        Byte  13:    LED mode
        Bytes 14-17: unknown
        Byte  18:    frame type, 04 for frame B
        Byte  19:    18 frame footer
        Bytes 20-23: frame footer, 5A 5A 5A 5A

    Unknown bytes may carry out other data, but currently not used by the parser.
*/