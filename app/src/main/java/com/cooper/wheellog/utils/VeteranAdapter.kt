package com.cooper.wheellog.utils

import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.CRC32
import kotlin.math.abs
import kotlin.math.roundToInt

class VeteranAdapter : BaseAdapter() {
    private var unpacker = VeteranUnpacker()
    private var oldTime: Long = 0
    private var manufacturerVersion = 0
    override fun decode(data: ByteArray?): Boolean {
        Timber.i("Decode Veteran")
        val wd = WheelData.getInstance()
        wd.resetRideTime()
        val newTime = System.currentTimeMillis()
        if (newTime - oldTime > WAITING_TIME) {
            // need to reset state in case of packet loose
            unpacker.reset()
        }
        oldTime = newTime
        var newDataFound = false
        for (c in data!!) {
            if (unpacker.addChar(c.toInt())) {
                val buff = unpacker.getBuffer()
                val useBetterPercents = WheelLog.AppConfig.useBetterPercents
                val veteranNegative = WheelLog.AppConfig.gotwayNegative.toInt()
                val voltage = MathsUtil.shortFromBytesBE(buff, 4)
                var speed = MathsUtil.signedShortFromBytesBE(buff, 6) * 10
                val distance = MathsUtil.intFromBytesRevBE(buff, 8)
                val totalDistance = MathsUtil.intFromBytesRevBE(buff, 12)
                var phaseCurrent = MathsUtil.signedShortFromBytesBE(buff, 16) * 10
                val temperature = MathsUtil.signedShortFromBytesBE(buff, 18)
                val autoOffSec = MathsUtil.shortFromBytesBE(buff, 20)
                val chargeMode = MathsUtil.shortFromBytesBE(buff, 22)
                val speedAlert = MathsUtil.shortFromBytesBE(buff, 24) * 10
                val speedTiltback = MathsUtil.shortFromBytesBE(buff, 26) * 10
                val ver = MathsUtil.shortFromBytesBE(buff, 28)
                manufacturerVersion = ver / 1000
                val version = String.format(
                    Locale.US,
                    "%03d.%01d.%02d",
                    ver / 1000,
                    ver % 1000 / 100,
                    ver % 100,
                )
                val pedalsMode = MathsUtil.shortFromBytesBE(buff, 30)
                val pitchAngle = MathsUtil.signedShortFromBytesBE(buff, 32)
                val hwPwm = MathsUtil.shortFromBytesBE(buff, 34)
                val battery: Int = computeBatteryPercentage(useBetterPercents, voltage)
                if (veteranNegative == 0) {
                    speed = abs(speed)
                    phaseCurrent = abs(phaseCurrent)
                } else {
                    speed *= veteranNegative
                    phaseCurrent *= veteranNegative
                }
                wd.version = version
                wd.speed = speed
                wd.topSpeed = speed
                wd.setWheelDistance(distance.toLong())
                wd.totalDistance = totalDistance.toLong()
                wd.temperature = temperature
                wd.phaseCurrent = phaseCurrent
                wd.current = phaseCurrent
                wd.voltage = voltage
                wd.voltageSag = voltage
                wd.batteryLevel = battery
                wd.chargingStatus = chargeMode
                wd.angle = pitchAngle / 100.0
                wd.output = hwPwm
                wd.updateRideTime()
                newDataFound = true
            }
        }
        return newDataFound
    }

    /**
     * Compute battery percentage from voltage
     * @param useBetterPercents use better percents is a flag set by the user
     * @param voltage voltage in mV
     * TODO correct the voltage values to work for the LeaperKim Lynx and other high voltage wheels
     */
    private fun computeBatteryPercentage(useBetterPercents: Boolean, voltage: Int): Int {
        return if (manufacturerVersion < 4) {
            // For non patton wheels, the voltage is 0-100V. This will change with the LeaperKim Lynx.
            if (useBetterPercents) {
                when {
                    voltage > SHERMAN_S_MAX_VOLTAGE -> 100
                    voltage > 8160 -> ((voltage - 8070) / 19.5).roundToInt()
                    voltage > MIN_VOLTAGE -> ((voltage - 7935) / 48.75).roundToInt()
                    else -> 0
                }
            } else {
                when {
                    voltage <= MIN_VOLTAGE -> 0
                    voltage >= 9870 -> 100
                    else -> ((voltage - 7935) / 19.5).roundToInt()
                }
            }
        } else {
            // For patton wheels, the voltage is 0-126V
            if (useBetterPercents) {
                when {
                    voltage > PATTON_MAX_VOLTAGE_BETTER_PERCENT -> 100
                    voltage > SHERMAN_S_MAX_VOLTAGE -> ((voltage - 9975) / 25.5).roundToInt()
                    voltage > 9600 -> ((voltage - 9600) / 67.5).roundToInt()
                    else -> 0
                }
            } else {
                when {
                    voltage <= 9918 -> 0
                    voltage >= PATTON_MAX_VOLTAGE -> 100
                    else -> ((voltage - 9918) / 24.2).roundToInt()
                }
            }
        }
    }

    override val isReady: Boolean
        get() = WheelData.getInstance().voltage != 0 && manufacturerVersion != 0

    fun resetTrip() {
        WheelData.getInstance().bluetoothCmd("CLEARMETER".toByteArray())
    }

    override fun updatePedalsMode(pedalsMode: Int) {
        when (pedalsMode) {
            0 -> WheelData.getInstance().bluetoothCmd("SETh".toByteArray())
            1 -> WheelData.getInstance().bluetoothCmd("SETm".toByteArray())
            2 -> WheelData.getInstance().bluetoothCmd("SETs".toByteArray())
        }
    }

    val ver: Int
        get() {
            if (manufacturerVersion >= 2) {
                WheelLog.AppConfig.hwPwm = true
            }
            return manufacturerVersion
        }

    override fun switchFlashlight() {
        val light = !WheelLog.AppConfig.lightEnabled
        WheelLog.AppConfig.lightEnabled = light
        setLightState(light)
    }

    override fun setLightState(on: Boolean) {
        var command = ""
        command = if (on) {
            "SetLightON"
        } else {
            "SetLightOFF"
        }
        WheelData.getInstance().bluetoothCmd(command.toByteArray())
    }

    /**
     * TODO Cells for wheel is not necessarily correct for the LeaperKim Lynx
     */
    override val cellsForWheel: Int
        get() = if (manufacturerVersion > 3) {
            30
        } else {
            24
        }

    override fun wheelBeep() {
        WheelData.getInstance().bluetoothCmd("b".toByteArray())
    }

    private class VeteranUnpacker {
        enum class UnpackerState {
            UNKNOWN, COLLECTING, LENS_SEARCH, DONE
        }

        var buffer = ByteArrayOutputStream()
        var old1 = 0
        var old2 = 0
        var len = 0
        var state = UnpackerState.UNKNOWN
        fun getBuffer(): ByteArray {
            return buffer.toByteArray()
        }

        fun addChar(c: Int): Boolean {
            return when (state) {
                UnpackerState.COLLECTING -> handleCollectingState(c)
                UnpackerState.LENS_SEARCH -> handleLensSearchState(c)
                UnpackerState.UNKNOWN -> handleUnknownOrDoneState(c)
                UnpackerState.DONE -> handleUnknownOrDoneState(c)
            }
        }

        private fun handleUnknownOrDoneState(c: Int): Boolean {
            when {
                c == 0x5C.toByte().toInt() && old1 == 0x5A.toByte()
                    .toInt() && old2 == 0xDC.toByte().toInt() -> {
                    buffer = ByteArrayOutputStream()
                    buffer.write(0xDC)
                    buffer.write(0x5A)
                    buffer.write(0x5C)
                    state = UnpackerState.LENS_SEARCH
                }

                c == 0x5A.toByte().toInt() && old1 == 0xDC.toByte().toInt() -> {
                    old2 = old1
                }

                else -> {
                    old2 = 0
                }
            }
            old1 = c
            return false
        }

        private fun handleLensSearchState(c: Int): Boolean {
            buffer.write(c)
            len = c and 0xff
            state = UnpackerState.COLLECTING
            old2 = old1
            old1 = c
            return false
        }

        private fun handleCollectingState(c: Int): Boolean {
            val bufferSize = buffer.size()
            return if (shouldTransitionToDone(bufferSize, c)) {
                state = UnpackerState.DONE
                Timber.i("Data verification failed")
                reset()
                false
            } else {
                buffer.write(c)
                if (bufferSize == len + 3) {
                    state = UnpackerState.DONE
                    Timber.i("Len %d", len)
                    Timber.i("Step reset")
                    reset()
                    if (len > 38) {
                        crcFormatParsing(
                            len,
                            getBuffer(),
                        )
                    } else {
                        true // old format without crc32
                    }
                } else {
                    false
                }
            }
        }

        private fun shouldTransitionToDone(bufferSize: Int, c: Int) =
            (bufferSize == 22 || bufferSize == 30) && c != 0x00 || bufferSize == 23 && c and 0xFE != 0x00 || bufferSize == 31 && c and 0xFC != 0x00

        /**
         * CRC32 format parsing
         * @param length length of the buffer
         * @return true if crc32 is ok
         * @return false if crc32 is wrong
         * TODO send failures to some sort of analytics/tracking service
         */
        private fun crcFormatParsing(length: Int, buffer: ByteArray): Boolean {
            val crc = CRC32()
            try {
                crc.update(buffer, 0, length - 4)
            } catch (e: ArrayIndexOutOfBoundsException) {
                Timber.i("CRC32 fail")
                Timber.e(e)
                return false
            } catch (e: NullPointerException) {
                Timber.i("CRC32 fail")
                Timber.e(e)
                return false
            }
            val calculatedCrc = crc.value
            val providedCrc = MathsUtil.getInt4(buffer, length)
            return if (calculatedCrc == providedCrc) {
                Timber.i("CRC32 ok")
                true
            } else {
                Timber.i("CRC32 fail")
                false
            }
        }

        fun reset() {
            old1 = 0
            old2 = 0
            state = UnpackerState.UNKNOWN
        }
    }

    companion object {
        private var INSTANCE: VeteranAdapter? = null
        private const val WAITING_TIME = 100

        // The minimum value that the wheel will report as a voltage in bytebuffer
        private const val MIN_VOLTAGE = 7935
        private const val SHERMAN_S_MAX_VOLTAGE = 10020

        // The maximum value that the wheel will report as a voltage in bytebuffer
        private const val PATTON_MAX_VOLTAGE_BETTER_PERCENT = 12525
        private const val PATTON_MAX_VOLTAGE = 12337

        @JvmStatic
        val instance: VeteranAdapter
            get() {
                if (INSTANCE == null) {
                    INSTANCE = VeteranAdapter()
                }
                return INSTANCE!!
            }
    }
}
