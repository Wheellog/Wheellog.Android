package com.cooper.wheellog.utils.kingsong

import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.MathsUtil
import timber.log.Timber
import kotlin.math.roundToInt

class KingSongFFFrameDecoder(private val wd: WheelData) {
    fun decodeFFrames(data: ByteArray) {
        val bmsnum = (data[16].toInt() and 255) - 0xF0
        val bms = if (bmsnum == 1) wd.bms1 else wd.bms2
        when (data[17].toInt() and 255) {
            0x00 -> {
                bms.voltage = MathsUtil.getInt2R(data, 2) / 100.0
                bms.current = MathsUtil.getInt2R(data, 4) / 100.0
                bms.remCap = MathsUtil.getInt2R(data, 6) * 10
                bms.factoryCap = MathsUtil.getInt2R(data, 8) * 10
                bms.fullCycles = MathsUtil.getInt2R(data, 10)
                bms.remPerc = (bms.remCap / (bms.factoryCap / 100.0)).roundToInt()
                if (bms.serialNumber == "") {
                    if (bmsnum == 1) {
                        requestBms1Serial()
                    } else {
                        requestBms2Serial()
                    }
                }
            }
            0x01 -> {
                bms.temp1 = (MathsUtil.getInt2R(data, 2) - 2730) / 10.0
                bms.temp2 = (MathsUtil.getInt2R(data, 4) - 2730) / 10.0
                bms.temp3 = (MathsUtil.getInt2R(data, 6) - 2730) / 10.0
                bms.temp4 = (MathsUtil.getInt2R(data, 8) - 2730) / 10.0
                bms.temp5 = (MathsUtil.getInt2R(data, 10) - 2730) / 10.0
                bms.temp6 = (MathsUtil.getInt2R(data, 12) - 2730) / 10.0
                bms.tempMos = (MathsUtil.getInt2R(data, 14) - 2730) / 10.0
            }
            0x02 -> {
                bms.cells[0] = MathsUtil.getInt2R(data, 2) / 1000.0
                bms.cells[1] = MathsUtil.getInt2R(data, 4) / 1000.0
                bms.cells[2] = MathsUtil.getInt2R(data, 6) / 1000.0
                bms.cells[3] = MathsUtil.getInt2R(data, 8) / 1000.0
                bms.cells[4] = MathsUtil.getInt2R(data, 10) / 1000.0
                bms.cells[5] = MathsUtil.getInt2R(data, 12) / 1000.0
                bms.cells[6] = MathsUtil.getInt2R(data, 14) / 1000.0
            }
            0x03 -> {
                bms.cells[7] = MathsUtil.getInt2R(data, 2) / 1000.0
                bms.cells[8] = MathsUtil.getInt2R(data, 4) / 1000.0
                bms.cells[9] = MathsUtil.getInt2R(data, 6) / 1000.0
                bms.cells[10] = MathsUtil.getInt2R(data, 8) / 1000.0
                bms.cells[11] = MathsUtil.getInt2R(data, 10) / 1000.0
                bms.cells[12] = MathsUtil.getInt2R(data, 12) / 1000.0
                bms.cells[13] = MathsUtil.getInt2R(data, 14) / 1000.0
            }
            0x04 -> {
                bms.cells[14] = MathsUtil.getInt2R(data, 2) / 1000.0
                bms.cells[15] = MathsUtil.getInt2R(data, 4) / 1000.0
                bms.cells[16] = MathsUtil.getInt2R(data, 6) / 1000.0
                bms.cells[17] = MathsUtil.getInt2R(data, 8) / 1000.0
                bms.cells[18] = MathsUtil.getInt2R(data, 10) / 1000.0
                bms.cells[19] = MathsUtil.getInt2R(data, 12) / 1000.0
                bms.cells[20] = MathsUtil.getInt2R(data, 14) / 1000.0
            }
            0x05 -> {
                bms.cells[21] = MathsUtil.getInt2R(data, 2) / 1000.0
                bms.cells[22] = MathsUtil.getInt2R(data, 4) / 1000.0
                bms.cells[23] = MathsUtil.getInt2R(data, 6) / 1000.0
                bms.cells[24] = MathsUtil.getInt2R(data, 8) / 1000.0
                bms.cells[25] = MathsUtil.getInt2R(data, 10) / 1000.0
                bms.cells[26] = MathsUtil.getInt2R(data, 12) / 1000.0
                bms.cells[27] = MathsUtil.getInt2R(data, 14) / 1000.0
            }
            0x06 -> {
                bms.cells[28] = MathsUtil.getInt2R(data, 2) / 1000.0
                bms.cells[29] = MathsUtil.getInt2R(data, 4) / 1000.0
                // bms.getCells()[30] = MathsUtil.getInt2R(data, 6)/1000.0;
                // bms.getCells()[31] = MathsUtil.getInt2R(data, 8)/1000.0;
                bms.tempMosEnv = (MathsUtil.getInt2R(data, 10) - 2730) / 10.0
                // bms.getCells()[5] = MathsUtil.getInt2R(data, 12)/1000.0;
                bms.minCell = bms.cells[29]
                bms.maxCell = bms.cells[29]
                for (i in 0..29) {
                    val cell = bms.cells[i]
                    if (cell > 0.0) {
                        if (bms.maxCell < cell) {
                            bms.maxCell = cell
                        }
                        if (bms.minCell > cell) {
                            bms.minCell = cell
                        }
                    }
                }
                bms.cellDiff = bms.maxCell - bms.minCell
                if (bms.versionNumber == "") {
                    if (bmsnum == 1) {
                        requestBms1Firmware()
                    } else {
                        requestBms2Firmware()
                    }
                }
            }
            else -> {
                Timber.i("Unknown BMS data type: %d", data[17].toInt() and 255)
            }
        }
    }

    private fun requestBms1Serial() {
        val data = KingsongUtils.getEmptyRequest()
        data[16] = 0xe1.toByte()
        data[17] = 0x00.toByte()
        data[18] = 0x00.toByte()
        data[19] = 0x00.toByte()
        wd.bluetoothCmd(data)
    }

    private fun requestBms2Serial() {
        val data = KingsongUtils.getEmptyRequest()
        data[16] = 0xe2.toByte()
        data[17] = 0x00.toByte()
        data[18] = 0x00.toByte()
        data[19] = 0x00.toByte()
        wd.bluetoothCmd(data)
    }

    private fun requestBms1Firmware() {
        val data = KingsongUtils.getEmptyRequest()
        data[16] = 0xe5.toByte()
        data[17] = 0x00.toByte()
        data[18] = 0x00.toByte()
        data[19] = 0x00.toByte()
        wd.bluetoothCmd(data)
    }

    private fun requestBms2Firmware() {
        val data = KingsongUtils.getEmptyRequest()
        data[16] = 0xe6.toByte()
        data[17] = 0x00.toByte()
        data[18] = 0x00.toByte()
        data[19] = 0x00.toByte()
        wd.bluetoothCmd(data)
    }
}
