package com.cooper.wheellog.utils.kingsong;

import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is100vWheel;
import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is126vWheel;
import static com.cooper.wheellog.utils.kingsong.KingsongUtils.is84vWheel;

import com.cooper.wheellog.AppConfig;
import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.WheelLog;
import com.cooper.wheellog.utils.BaseAdapter;
import com.cooper.wheellog.utils.MathsUtil;
import com.cooper.wheellog.utils.SmartBms;

import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

public class KingsongAdapter extends BaseAdapter {
    private static KingsongAdapter INSTANCE;

    private int mKSAlarm1Speed = 0;
    private int mKSAlarm2Speed = 0;
    private int mKSAlarm3Speed = 0;
    private int mWheelMaxSpeed = 0;
    private boolean m18Lkm = true;
    private int mMode;
    static final double KS18L_SCALER = 0.83;
    private double mSpeedLimit;

    private final WheelData wd;
    private final AppConfig appConfig;
    private final KingSongLiveDataDecoder kingsongLiveDataDecoder;

    public KingsongAdapter(
            final WheelData wd,
            final AppConfig appConfig,
            final KingSongLiveDataDecoder kingsongLiveDataDecoder
    ) {
        this.wd = wd;
        this.appConfig = appConfig;
        this.kingsongLiveDataDecoder = kingsongLiveDataDecoder;
    }

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Decode KingSong");
        wd.resetRideTime();
        if (data.length >= 20) {
            int a1 = data[0] & 255;
            int a2 = data[1] & 255;
            if (a1 != 170 || a2 != 85) {
                return false;
            }
            if ((data[16] & 255) == 0xA9) {
                mMode = kingsongLiveDataDecoder.decode(data, m18Lkm, mMode);
                return true;
            } else if ((data[16] & 255) == 0xB9) { // Distance/Time/Fan Data
                decodeKingsongDistanceTimeFan(data);
                return false;
            } else if ((data[16] & 255) == 187) { // Name and Type data
                decodeKingSongNameAndTypeData(data);
                return false;
            } else if ((data[16] & 255) == 0xB3) { // Serial Number
                decodeKingSongSerialNumber(data);
                return false;
            } else if ((data[16] & 255) == 0xF5) { //cpu load
                decodeKingSongCpuLoad(data);
                return false;
            } else if ((data[16] & 255) == 0xF6) { //speed limit (PWM?)
                decodeKingSongSpeed(data);
                return false;
            } else if ((data[16] & 255) == 0xA4 || (data[16] & 255) == 0xB5) { //max speed and alerts
                decodeKingSongMaxSpeedAndAlerts(data);
                return true;
            } else if ((data[16] & 255) == 0xF1 || (data[16] & 255) == 0xF2) { // F1 - 1st BMS, F2 - 2nd BMS. F3 and F4 are also present but empty
                decodeFFrames(data);
            } else if ((data[16] & 255) == 0xe1 || (data[16] & 255) == 0xe2) { // e1 - 1st BMS, e2 - 2nd BMS.
                decodeE1E2Frames(data);
            } else if ((data[16] & 255) == 0xe5 || (data[16] & 255) == 0xe6) { // e5 - 1st BMS, e6 - 2nd BMS.
                deocdE5E6Frames(data);
            }
        }
        return false;
    }

    private void deocdE5E6Frames(byte[] data) {
        int bmsnum = (data[16] & 255) - 0xE4;
        SmartBms bms = bmsnum == 1 ? wd.getBms1() : wd.getBms2();
        byte[] sndata = new byte[19];
        System.arraycopy(data, 2, sndata, 0, 14);
        System.arraycopy(data, 17, sndata, 14, 3);
        sndata[18] = (byte) 0;
        bms.setVersionNumber(new String(sndata));
    }

    private void decodeE1E2Frames(byte[] data) {
        int bmsnum = (data[16] & 255) - 0xE0;
        SmartBms bms = bmsnum == 1 ? wd.getBms1() : wd.getBms2();
        byte[] sndata = new byte[18];
        System.arraycopy(data, 2, sndata, 0, 14);
        System.arraycopy(data, 17, sndata, 14, 3);
        sndata[17] = (byte) 0;
        bms.setSerialNumber(new String(sndata));
    }

    private void decodeFFrames(byte[] data) {
        int bmsnum = (data[16] & 255) - 0xF0;
        SmartBms bms = bmsnum == 1 ? wd.getBms1() : wd.getBms2();
        int pNum = (data[17] & 255);
        if (pNum == 0x00) {
            bms.setVoltage(MathsUtil.getInt2R(data, 2) / 100.0);
            bms.setCurrent(MathsUtil.getInt2R(data, 4) / 100.0);
            bms.setRemCap(MathsUtil.getInt2R(data, 6) * 10);
            bms.setFactoryCap(MathsUtil.getInt2R(data, 8) * 10);
            bms.setFullCycles(MathsUtil.getInt2R(data, 10));
            bms.setRemPerc((int) Math.round(bms.getRemCap() / (bms.getFactoryCap() / 100.0)));
            if (bms.getSerialNumber().equals("")) {
                if (bmsnum == 1) {
                    requestBms1Serial();
                } else {
                    requestBms2Serial();
                }
            }
        } else if (pNum == 0x01) {
            bms.setTemp1((MathsUtil.getInt2R(data, 2) - 2730) / 10.0);
            bms.setTemp2((MathsUtil.getInt2R(data, 4) - 2730) / 10.0);
            bms.setTemp3((MathsUtil.getInt2R(data, 6) - 2730) / 10.0);
            bms.setTemp4((MathsUtil.getInt2R(data, 8) - 2730) / 10.0);
            bms.setTemp5((MathsUtil.getInt2R(data, 10) - 2730) / 10.0);
            bms.setTemp6((MathsUtil.getInt2R(data, 12) - 2730) / 10.0);
            bms.setTempMos((MathsUtil.getInt2R(data, 14) - 2730) / 10.0);
        } else if (pNum == 0x02) {
            bms.getCells()[0] = MathsUtil.getInt2R(data, 2) / 1000.0;
            bms.getCells()[1] = MathsUtil.getInt2R(data, 4) / 1000.0;
            bms.getCells()[2] = MathsUtil.getInt2R(data, 6) / 1000.0;
            bms.getCells()[3] = MathsUtil.getInt2R(data, 8) / 1000.0;
            bms.getCells()[4] = MathsUtil.getInt2R(data, 10) / 1000.0;
            bms.getCells()[5] = MathsUtil.getInt2R(data, 12) / 1000.0;
            bms.getCells()[6] = MathsUtil.getInt2R(data, 14) / 1000.0;
        } else if (pNum == 0x03) {
            bms.getCells()[7] = MathsUtil.getInt2R(data, 2) / 1000.0;
            bms.getCells()[8] = MathsUtil.getInt2R(data, 4) / 1000.0;
            bms.getCells()[9] = MathsUtil.getInt2R(data, 6) / 1000.0;
            bms.getCells()[10] = MathsUtil.getInt2R(data, 8) / 1000.0;
            bms.getCells()[11] = MathsUtil.getInt2R(data, 10) / 1000.0;
            bms.getCells()[12] = MathsUtil.getInt2R(data, 12) / 1000.0;
            bms.getCells()[13] = MathsUtil.getInt2R(data, 14) / 1000.0;
        } else if (pNum == 0x04) {
            bms.getCells()[14] = MathsUtil.getInt2R(data, 2) / 1000.0;
            bms.getCells()[15] = MathsUtil.getInt2R(data, 4) / 1000.0;
            bms.getCells()[16] = MathsUtil.getInt2R(data, 6) / 1000.0;
            bms.getCells()[17] = MathsUtil.getInt2R(data, 8) / 1000.0;
            bms.getCells()[18] = MathsUtil.getInt2R(data, 10) / 1000.0;
            bms.getCells()[19] = MathsUtil.getInt2R(data, 12) / 1000.0;
            bms.getCells()[20] = MathsUtil.getInt2R(data, 14) / 1000.0;
        } else if (pNum == 0x05) {
            bms.getCells()[21] = MathsUtil.getInt2R(data, 2) / 1000.0;
            bms.getCells()[22] = MathsUtil.getInt2R(data, 4) / 1000.0;
            bms.getCells()[23] = MathsUtil.getInt2R(data, 6) / 1000.0;
            bms.getCells()[24] = MathsUtil.getInt2R(data, 8) / 1000.0;
            bms.getCells()[25] = MathsUtil.getInt2R(data, 10) / 1000.0;
            bms.getCells()[26] = MathsUtil.getInt2R(data, 12) / 1000.0;
            bms.getCells()[27] = MathsUtil.getInt2R(data, 14) / 1000.0;
        } else if (pNum == 0x06) {
            bms.getCells()[28] = MathsUtil.getInt2R(data, 2) / 1000.0;
            bms.getCells()[29] = MathsUtil.getInt2R(data, 4) / 1000.0;
            //bms.getCells()[30] = MathsUtil.getInt2R(data, 6)/1000.0;
            //bms.getCells()[31] = MathsUtil.getInt2R(data, 8)/1000.0;
            bms.setTempMosEnv((MathsUtil.getInt2R(data, 10) - 2730) / 10.0);
            //bms.getCells()[5] = MathsUtil.getInt2R(data, 12)/1000.0;
            bms.setMinCell(bms.getCells()[29]);
            bms.setMaxCell(bms.getCells()[29]);
            for (int i = 0; i < 30; i++) {
                double cell = bms.getCells()[i];
                if (cell > 0.0) {
                    if (bms.getMaxCell() < cell) {
                        bms.setMaxCell(cell);
                    }
                    if (bms.getMinCell() > cell) {
                        bms.setMinCell(cell);
                    }
                }
            }
            bms.setCellDiff(bms.getMaxCell() - bms.getMinCell());
            if (bms.getVersionNumber().equals("")) {
                if (bmsnum == 1) {
                    requestBms1Firmware();
                } else {
                    requestBms2Firmware();
                }
            }
        }
    }

    private void decodeKingSongMaxSpeedAndAlerts(byte[] data) {
        mWheelMaxSpeed = data[10] & 255;
        appConfig.setWheelMaxSpeed(mWheelMaxSpeed);
        mKSAlarm3Speed = (data[8] & 255);
        mKSAlarm2Speed = (data[6] & 255);
        mKSAlarm1Speed = (data[4] & 255);
        appConfig.setWheelKsAlarm3(mKSAlarm3Speed);
        appConfig.setWheelKsAlarm2(mKSAlarm2Speed);
        appConfig.setWheelKsAlarm1(mKSAlarm1Speed);
        // after received 0xa4 send same repeat data[2] =0x01 data[16] = 0x98
        if ((data[16] & 255) == 164) {
            data[16] = (byte) 0x98;
            wd.bluetoothCmd(data);
        }
    }

    private void decodeKingSongSpeed(byte[] data) {
        mSpeedLimit = MathsUtil.getInt2R(data, 2) / 100.0;
        wd.setSpeedLimit(mSpeedLimit);
    }

    private void decodeKingSongCpuLoad(byte[] data) {
        wd.setCpuLoad(data[14]);
        wd.setOutput(data[15] * 100);
    }

    private void decodeKingSongSerialNumber(byte[] data) {
        byte[] sndata = new byte[18];
        System.arraycopy(data, 2, sndata, 0, 14);
        System.arraycopy(data, 17, sndata, 14, 3);
        sndata[17] = (byte) 0;
        wd.setSerial(new String(sndata));
        updateKSAlarmAndSpeed();
    }

    private void decodeKingSongNameAndTypeData(byte[] data) {
        int end = 0;
        int i = 0;
        while (i < 14 && data[i + 2] != 0) {
            end++;
            i++;
        }
        wd.setName(new String(data, 2, end).trim());
        wd.setModel("");
        String[] ss = wd.getName().split("-");
        StringBuilder model = new StringBuilder();
        for (i = 0; i < ss.length - 1; i++) {
            if (i != 0) {
                model.append("-");
            }
            model.append(ss[i]);
        }
        wd.setModel(model.toString());
        try {
            wd.setVersion(String.format(Locale.US, "%.2f", Integer.parseInt(ss[ss.length - 1]) / 100.0));
        } catch (Exception ignored) {
        }
    }

    private void decodeKingsongDistanceTimeFan(byte[] data) {
        long distance = MathsUtil.getInt4R(data, 2);
        wd.setWheelDistance(distance);
        wd.updateRideTime();
        wd.setTopSpeed(MathsUtil.getInt2R(data, 8));
        wd.setFanStatus(data[12]);
        wd.setChargingStatus(data[13]);
        wd.setTemperature2(MathsUtil.getInt2R(data, 14));
    }

    @Override
    public boolean isReady() {
        return !Objects.equals(wd.getModel(), "Unknown")
                && wd.getVoltage() != 0;
    }

    @Override
    public void updatePedalsMode(int pedalsMode) {
        byte[] data = getEmptyRequest();
        data[2] = (byte) pedalsMode;
        data[3] = (byte) 0xE0;
        data[16] = (byte) 0x87;
        data[17] = (byte) 0x15;
        wd.bluetoothCmd(data);
    }

    @Override
    public void wheelCalibration() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0x89;
        wd.bluetoothCmd(data);
    }

    @Override
    public void switchFlashlight() {
        int lightMode = Integer.parseInt(appConfig.getLightMode()) + 1;
        if (lightMode > 2) {
            lightMode = 0;
        }
        appConfig.setLightMode(String.valueOf(lightMode));
        setLightMode(lightMode);
    }

    @Override
    public void setLightMode(int lightMode) {
        byte[] data = getEmptyRequest();
        data[2] = (byte) (lightMode + 0x12);
        data[3] = (byte) 0x01;
        data[16] = (byte) 0x73;
        wd.bluetoothCmd(data);
    }


    @Override
    public int getCellsForWheel() {
        int cells = 16;
        if (is84vWheel(wd)) {
            cells = 20;
        } else if (is126vWheel(wd)) {
            cells = 30;
        } else if (is100vWheel(wd)) {
            cells = 24;
        }
        return cells;
    }

    @Override
    public void updateMaxSpeed(final int maxSpeed) {
        mWheelMaxSpeed = maxSpeed;
        updateKSAlarmAndSpeed();
    }

    public void updateKSAlarmAndSpeed() {
        byte[] data = getEmptyRequest();
        data[2] = (byte) mKSAlarm1Speed;
        data[4] = (byte) mKSAlarm2Speed;
        data[6] = (byte) mKSAlarm3Speed;
        data[8] = (byte) mWheelMaxSpeed;
        data[16] = (byte) 0x85;

        if ((mWheelMaxSpeed | mKSAlarm3Speed | mKSAlarm2Speed | mKSAlarm1Speed) == 0) {
            data[16] = (byte) 0x98; // request speed & alarm values from wheel
        }
        wd.bluetoothCmd(data);
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

    public void set18Lkm(boolean enabled) {
        m18Lkm = enabled;

        if ((wd.getModel().compareTo("KS-18L") == 0) && !m18Lkm) {
            wd.setTotalDistance(Math.round(wd.getTotalDistance() * KS18L_SCALER));
        }
    }

    public int getMode() {
        return mMode;
    }

    public double getSpeedLimit() {
        return mSpeedLimit;
    }

    private byte[] getEmptyRequest() {
        return new byte[]{(byte) 0xAA, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x14, 0x5A, 0x5A};
    }

    @Override
    public void wheelBeep() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0x88;
        wd.bluetoothCmd(data);
    }

    public void requestNameData() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0x9B;
        wd.bluetoothCmd(data);
    }

    public void requestSerialData() {
        byte[] data = getEmptyRequest();
        data[16] = 0x63;
        wd.bluetoothCmd(data);
    }

    public void requestBms1Serial() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0xe1;
        data[17] = (byte) 0x00;
        data[18] = (byte) 0x00;
        data[19] = (byte) 0x00;
        wd.bluetoothCmd(data);
    }

    public void requestBms2Serial() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0xe2;
        data[17] = (byte) 0x00;
        data[18] = (byte) 0x00;
        data[19] = (byte) 0x00;
        wd.bluetoothCmd(data);
    }

    public void requestBms1Firmware() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0xe5;
        data[17] = (byte) 0x00;
        data[18] = (byte) 0x00;
        data[19] = (byte) 0x00;
        wd.bluetoothCmd(data);
    }

    public void requestBms2Firmware() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0xe6;
        data[17] = (byte) 0x00;
        data[18] = (byte) 0x00;
        data[19] = (byte) 0x00;
        wd.bluetoothCmd(data);
    }

    public void requestAlarmSettingsAndMaxSpeed() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0x98;
        wd.bluetoothCmd(data);
    }

    @Override
    public void powerOff() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0x40;
        wd.bluetoothCmd(data);
    }

    @Override
    public void updateLedMode(int ledMode) {
        byte[] data = getEmptyRequest();
        data[2] = (byte) ledMode;
        data[16] = (byte) 0x6C;
        wd.bluetoothCmd(data);
    }

    @Override
    public void updateStrobeMode(int strobeMode) {
        byte[] data = getEmptyRequest();
        data[2] = (byte) strobeMode;
        data[16] = (byte) 0x53;
        wd.bluetoothCmd(data);
    }


    public void setChargingUpTo(int chargeUpTo) { // 100 => 100%
        byte[] data = getEmptyRequest();
        data[2] = (byte) 0x09;
        data[4] = (byte) chargeUpTo;
        data[16] = (byte) 0x8a;
        wd.bluetoothCmd(data);
    }

    public void setStandByDelay(int standByDelay) { //3600 => 60 min => 1 hour
        byte[] data = getEmptyRequest();
        data[2] = (byte) 0x01;
        data[4] = (byte) (standByDelay & 0xFF);
        data[5] = (byte) ((standByDelay >> 8) & 0xFF);
        data[16] = (byte) 0x3f;
        wd.bluetoothCmd(data);
    }

    public void setGyroSwitchOff(int gyroSwitchOff) { //501 => 50.1 degree
        byte[] data = getEmptyRequest();
        data[2] = (byte) 0x03;
        data[4] = (byte) (gyroSwitchOff & 0xFF);
        data[5] = (byte) ((gyroSwitchOff >> 8) & 0xFF);
        data[16] = (byte) 0x8a;
        wd.bluetoothCmd(data);
    }

    public void setGyroFrontAngle(int gyroFrontAngle) { //-32 => -3.2 degree
        byte[] data = getEmptyRequest();
        data[2] = (byte) 0x01;
        data[4] = (byte) (gyroFrontAngle & 0xFF);
        data[5] = (byte) ((gyroFrontAngle >> 8) & 0xFF);
        data[16] = (byte) 0x8a;
        wd.bluetoothCmd(data);
    }

    public static KingsongAdapter getInstance() {
        Timber.i("Get instance");
        if (INSTANCE == null) {
            WheelData wd = WheelData.getInstance();
            AppConfig appConfig = WheelLog.AppConfig;
            Timber.i("New instance");
            INSTANCE = new KingsongAdapter(wd, appConfig, new KingSongLiveDataDecoder(wd, appConfig, new KingSongBatteryCalculator(wd, appConfig)));
        }
        return INSTANCE;
    }
}
