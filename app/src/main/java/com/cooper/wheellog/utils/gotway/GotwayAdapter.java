package com.cooper.wheellog.utils.gotway;

import android.content.Intent;
import android.os.Handler;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;
import com.cooper.wheellog.utils.BaseAdapter;
import com.cooper.wheellog.utils.Constants;

import timber.log.Timber;

public class GotwayAdapter extends BaseAdapter {
    private static GotwayAdapter INSTANCE;
    private GotwayUnpacker unpacker;
    private GotwayFrameADecoder gotwayFrameADecoder;
    private GotwayFrameBDecoder gotwayFrameBDecoder;
    private AppConfig appConfig;
    private WheelData wd;
    static final double RATIO_GW = 0.875;
    private String model = "";
    private String imu = "";
    private String fw = "";
    private int attempt = 0;
    private int lock_Changes = 0;
    private final int lightModeOff = 0;
    private final int lightModeOn = 1;
    private final int lightModeStrobe = 2;
    private final int alarmModeTwo = 0; // 30 + 35 (45) km/h + 80% PWM
    private final int alarmModeOne = 1; // 35 (45) km/h + 80% PWM
    private final int alarmModeOff = 2; // 80% PWM only
    private final int alarmModeCF = 3; // PWM tiltback for custom firmware

    public GotwayAdapter(final AppConfig appConfig, final WheelData wd, final GotwayUnpacker unpacker, final GotwayFrameADecoder gotwayFrameADecoder, final GotwayFrameBDecoder gotwayFrameBDecoder) {
        this.appConfig = appConfig;
        this.wd = wd;
        this.unpacker = unpacker;
        this.gotwayFrameADecoder = gotwayFrameADecoder;
        this.gotwayFrameBDecoder = gotwayFrameBDecoder;
    }

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode Gotway/Begode");
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
                appConfig.setHwPwm(false);
            } else if (dataS.startsWith("CF")) {
                fw = dataS.substring(2).trim();
                wd.setVersion(fw);
                appConfig.setHwPwm(true);
            } else if (dataS.startsWith("MPU")) {
                imu = dataS.substring(1, 7).trim();
            }
        }
        for (byte c : data) {
            if (unpacker.addChar(c)) {

                byte[] buff = unpacker.getBuffer();
                Boolean useRatio = appConfig.getUseRatio();
                Boolean useBetterPercents = appConfig.getUseBetterPercents();
                int gotwayNegative = Integer.parseInt(appConfig.getGotwayNegative());

                if (buff[18] == (byte) 0x00) {
                    Timber.i("Begode frame A found (live data)");
                    gotwayFrameADecoder.decode(buff, useRatio, useBetterPercents, gotwayNegative);
                    newDataFound = true;
                } else if (buff[18] == (byte) 0x04) {
                    Timber.i("Begode frame B found (total distance and flags)");

                    GotwayFrameBDecoder.AlertResult result = gotwayFrameBDecoder.decode(buff, useRatio, lock_Changes, fw);
                    lock_Changes = result.lock;
                    int alert = result.alert;

                    String alertLine = "";
                    if ((alert & 0x01) == 1) alertLine += "HighPower ";
                    if (((alert >> 1) & 0x01) == 1) alertLine += "Speed2 ";
                    if (((alert >> 2) & 0x01) == 1) alertLine += "Speed1 ";
                    if (((alert >> 3) & 0x01) == 1) alertLine += "LowVoltage ";
                    if (((alert >> 4) & 0x01) == 1) alertLine += "OverVoltage ";
                    if (((alert >> 5) & 0x01) == 1) alertLine += "OverTemperature ";
                    if (((alert >> 6) & 0x01) == 1) alertLine += "errHallSensors ";
                    if (((alert >> 7) & 0x01) == 1) alertLine += "TransportMode";
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
                        appConfig.setHwPwm(false);
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
        String command = switch (pedalsMode) {
            case 0 -> "h";
            case 1 -> "f";
            case 2 -> "s";
            default -> "";
        };
        lock_Changes = 2;
        sendCommand(command);

    }

    @Override
    public void switchFlashlight() {
        int lightMode = Integer.parseInt(appConfig.getLightMode()) + 1;
        if (lightMode > lightModeStrobe) {
            lightMode = lightModeOff;
        }
        // Strobe light not available on Freestyl3r firmware while pwm tiltback mode enabled.
        // For custom firmware with enabled tiltback available only light off and on.
        // Strobe is using for tiltback warning. Detect via specific for this firmware alarm mode.
        if (lightMode > lightModeOn && appConfig.getAlarmMode().equals(String.valueOf(alarmModeCF))) {
            lightMode = lightModeOff;
        }
        appConfig.setLightMode(String.valueOf(lightMode));
        setLightMode(lightMode);
    }

    @Override
    public void setLightMode(int lightMode) {
        lock_Changes = 2;
        String command = switch (lightMode) {
            case lightModeOff -> "E";
            case lightModeOn -> "Q";
            case lightModeStrobe -> "T";
            default -> "E";
        };
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
            case 0 -> command = ">";
            case 1 -> command = "=";
            case 2 -> command = "<";
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
        String command = switch (alarmMode) {
            case alarmModeTwo -> "o";
            case alarmModeOne -> "u";
            case alarmModeOff -> "i";
            case alarmModeCF -> "I";
            default -> "";
        };
        lock_Changes = 2;
        sendCommand(command);
    }

    @Override
    public void wheelCalibration() {
        sendCommand("c", "y", 300);
    }

    @Override
    public int getCellsForWheel() {
        return switch (appConfig.getGotwayVoltage()) {
            case "0" -> 16;
            case "1" -> 20;
            case "2" -> 24;
            case "3" -> 32;
            case "4" -> 32;
            default -> 24;
        };
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


    public static GotwayAdapter getInstance() {
        if (INSTANCE == null) {
            WheelData wd = WheelData.getInstance();
            AppConfig appConfig = WheelLog.AppConfig;
            INSTANCE = new GotwayAdapter(appConfig, wd, new GotwayUnpacker(), new GotwayFrameADecoder(wd, new GotwayScaledVoltageCalculator(appConfig), new GotwayBatteryCalculator()), new GotwayFrameBDecoder(wd, appConfig));
        }
        return INSTANCE;
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