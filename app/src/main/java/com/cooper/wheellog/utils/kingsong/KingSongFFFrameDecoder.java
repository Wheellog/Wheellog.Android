package com.cooper.wheellog.utils.kingsong;

import static com.cooper.wheellog.utils.kingsong.KingsongUtils.getEmptyRequest;

import com.cooper.wheellog.WheelData;
import com.cooper.wheellog.utils.MathsUtil;
import com.cooper.wheellog.utils.SmartBms;

public class KingSongFFFrameDecoder {

    private final WheelData wd;

    public KingSongFFFrameDecoder(final WheelData wd) {
        this.wd = wd;
    }

    public void decodeFFrames(byte[] data) {
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

    private void requestBms1Serial() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0xe1;
        data[17] = (byte) 0x00;
        data[18] = (byte) 0x00;
        data[19] = (byte) 0x00;
        wd.bluetoothCmd(data);
    }

    private void requestBms2Serial() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0xe2;
        data[17] = (byte) 0x00;
        data[18] = (byte) 0x00;
        data[19] = (byte) 0x00;
        wd.bluetoothCmd(data);
    }

    private void requestBms1Firmware() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0xe5;
        data[17] = (byte) 0x00;
        data[18] = (byte) 0x00;
        data[19] = (byte) 0x00;
        wd.bluetoothCmd(data);
    }

    private void requestBms2Firmware() {
        byte[] data = getEmptyRequest();
        data[16] = (byte) 0xe6;
        data[17] = (byte) 0x00;
        data[18] = (byte) 0x00;
        data[19] = (byte) 0x00;
        wd.bluetoothCmd(data);
    }
}