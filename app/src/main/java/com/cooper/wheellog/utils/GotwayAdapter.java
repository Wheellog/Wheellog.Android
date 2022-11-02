package com.cooper.wheellog.utils;

import android.content.Intent;
import android.os.Handler;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;

import java.io.ByteArrayOutputStream;

import timber.log.Timber;

public class GotwayAdapter extends BaseAdapter {
    private static GotwayAdapter INSTANCE;
    gotwayUnpacker unpacker = new gotwayUnpacker();
    private static final double RATIO_GW = 0.875;
    private String model = "";
    private String imu = "";
    private String fw = "";
    private int attempt = 0;
    private int lock_Changes = 0;

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode Gotway/Begode");

        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();
        boolean newDataFound = false;
        if ((model.length() == 0) || (fw.length() == 0)) { // IMU sent at the begining, so there is no sense to check it, we can't request it
            String dataS = new String(data, 0, data.length - 1).trim();
            if (dataS.startsWith("NAME")) {
                model = dataS.substring(5).trim();
                wd.setModel(model);
            } else if (dataS.startsWith("GW")) {
                fw = dataS.substring(2).trim();
                wd.setVersion(fw);
            } else if (dataS.startsWith("MPU")) {
                imu = dataS.substring(1, 7).trim();
            }
        }
        for (byte c : data) {
            if (unpacker.addChar(c)) {

                byte[] buff = unpacker.getBuffer();
                Boolean useRatio = WheelLog.AppConfig.getUseRatio();
                Boolean useBetterPercents = WheelLog.AppConfig.getUseBetterPercents();
                int gotwayNegative = Integer.parseInt(WheelLog.AppConfig.getGotwayNegative());

                if (buff[18] == (byte) 0x00) {
                    Timber.i("Begode frame A found (live data)");

                    int voltage = MathsUtil.shortFromBytesBE(buff, 2);
                    int speed = (int) Math.round(MathsUtil.signedShortFromBytesBE(buff, 4) * 3.6);
                    int distance = MathsUtil.shortFromBytesBE(buff, 8);
                    int phaseCurrent = MathsUtil.signedShortFromBytesBE(buff, 10);
                    int temperature = (int) Math.round((((float) MathsUtil.signedShortFromBytesBE(buff, 12) / 340.0) + 36.53) * 100);  // mpu6050
                    //int temperature = (int) Math.round((((float) MathsUtil.signedShortFromBytesBE(buff, 12) / 333.87) + 21.00) * 100); // mpu6500

                    if (gotwayNegative == 0) {
                        speed = Math.abs(speed);
                        phaseCurrent = Math.abs(phaseCurrent);
                    } else {
                        speed = speed * gotwayNegative;
                        phaseCurrent = phaseCurrent * gotwayNegative;
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
                    wd.setVoltage(voltage);
                    wd.setVoltageSag(voltage);
                    wd.setBatteryLevel(battery);
                    wd.updateRideTime();

                    newDataFound = true;

                } else if (buff[18] == (byte) 0x04) {
                    Timber.i("Begode frame B found (total distance and flags)");

                    int totalDistance = (int) MathsUtil.getInt4(buff, 2);
                    if (useRatio) {
                        wd.setTotalDistance(Math.round(totalDistance * RATIO_GW));
                    } else {
                        wd.setTotalDistance(totalDistance);
                    }
                    int settings = MathsUtil.shortFromBytesBE(buff, 6);
                    int pedalsMode = (settings >> 13) & 0x03;
                    int speedAlarms = (settings >> 10) & 0x03;
                    int rollAngle = (settings >> 7) & 0x03;
                    int inMiles = settings & 0x01;
                    int powerOffTime = MathsUtil.shortFromBytesBE(buff, 8);
                    int tiltBackSpeed = MathsUtil.shortFromBytesBE(buff, 10);
                    if (tiltBackSpeed >= 100) tiltBackSpeed = 0;
                    int alert = buff[12] & 0xFF;
                    int ledMode = buff[13] & 0xFF;
                    int lightMode = buff[15] & 0x03;
                    if (lock_Changes == 0) {
                        WheelLog.AppConfig.setPedalsMode(String.valueOf(2 - pedalsMode));
                        WheelLog.AppConfig.setAlarmMode(String.valueOf(speedAlarms)); //CheckMe
                        WheelLog.AppConfig.setLightMode(String.valueOf(lightMode));
                        WheelLog.AppConfig.setLedMode(String.valueOf(ledMode));
                        if (model.length() != 0) {
                            WheelLog.AppConfig.setGwInMiles(inMiles == 1);
                            WheelLog.AppConfig.setWheelMaxSpeed(tiltBackSpeed);
                            WheelLog.AppConfig.setRollAngle(String.valueOf(rollAngle));
                        }
                    } else {
                        lock_Changes -= 1;
                    }

                    String alertLine = "";
                    if ((alert & 0x01) == 1) alertLine += "HighPower ";
                    if (((alert>>1) & 0x01) == 1) alertLine += "Speed2 ";
                    if (((alert>>2) & 0x01) == 1) alertLine += "Speed1 ";
                    if (((alert>>3) & 0x01) == 1) alertLine += "LowVoltage ";
                    if (((alert>>4) & 0x01) == 1) alertLine += "OverVoltage ";
                    if (((alert>>5) & 0x01) == 1) alertLine += "OverTemperature ";
                    if (((alert>>6) & 0x01) == 1) alertLine += "errHallSensors ";
                    if (((alert>>7) & 0x01) == 1) alertLine += "TransportMode";
                    wd.setAlert(alertLine);

                    if ((alertLine != "") && (getContext() != null)) {
                        Timber.i("News to send: %s, sending Intent", alertLine);
                        Intent intent = new Intent(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
                        intent.putExtra(Constants.INTENT_EXTRA_NEWS, alertLine);
                        getContext().sendBroadcast(intent);
                    }
                }
                if (attempt < 10) {
                    if (model.equals("")) {
                        sendCommand("N", "", 0);
                    } else if (fw.equals("")) {
                        sendCommand("V", "", 0);
                    }
                    attempt += 1;
                } else {
                    if (model.equals("")) {
                        model = "Begode";
                        wd.setVersion(model);
                    } else if (fw.equals("")) {
                        fw = "-";
                        wd.setVersion(fw);
                    }
                }
            }
        }

        return newDataFound;
    }

    @Override
    public boolean isReady() {
        return WheelData.getInstance().getVoltage() != 0;
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
        }
        lock_Changes = 2;
        sendCommand(command);

    }

    @Override
    public void switchFlashlight() {
        int lightMode = Integer.parseInt(WheelLog.AppConfig.getLightMode()) + 1;
        if (lightMode > 2) {
            lightMode = 0;
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
            case 0: command = "E"; break;
            case 1: command = "Q"; break;
            case 2: command = "T"; break;
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
            case 0: command = "o"; break; // alertTwo (1) // 30 + 35 (45) km/h + 80% PWM
            case 1: command = "u"; break; // AlertOff (2) // 80% PWM only
            case 2: command = "i"; break; //alertOne (0) // 35 (45) km/h + 80% PWM
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
                if ((size == 20 && c != (byte) 0x18) || (size > 20 && size <= 24 && c != (byte) 0x5A)) {
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