package com.cooper.wheellog.utils

import com.cooper.wheellog.WheelData
import timber.log.Timber
import java.io.*
import java.lang.IllegalArgumentException
import java.util.*

class ParserLogToWheelData {
    private val header = HashMap<LogHeaderEnum, Int>()

    fun parseFile(fileUtil: FileUtil) {
        val inputStream = fileUtil.inputStream
        if (inputStream == null) {
            // TODO: localize me
            Timber.wtf("Failed to create inputStream for %s", fileUtil.fileName)
            return
        }

        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine().split(",").toTypedArray()
            for (i in headerLine.indices) {
                try {
                    header[LogHeaderEnum.valueOf(headerLine[i].toUpperCase(Locale.US))] = i
                } catch (ignored: IllegalArgumentException) {
                }
            }
            if (!header.containsKey(LogHeaderEnum.LATITUDE) || !header.containsKey(LogHeaderEnum.LONGITUDE)) {
                inputStream.close()
                // TODO: localize me
                Timber.wtf("%s file does not contain geolocation data.", fileUtil.fileName)
                return
            }
            WheelData.getInstance().apply {
                val firstRow = reader.readLine()!!.split(",")
                setStartParameters(
                    firstRow[header[LogHeaderEnum.TIME]!!].toLongOrNull() ?: 0L,
                    firstRow[header[LogHeaderEnum.TOTALDISTANCE]!!].toLongOrNull() ?: 0L
                )

                reader.forEachLine { line ->
                    val row = line.split(",")
                    topSpeed = (100 * (row[header[LogHeaderEnum.SPEED]!!].toDoubleOrNull() ?: 0.0)).toInt()
                    voltage = (100 * (row[header[LogHeaderEnum.VOLTAGE]!!].toDoubleOrNull() ?: 0.0)).toInt()
                    setVoltageSag(voltage)
                    setPhaseCurrent((100 * (row[header[LogHeaderEnum.PHASE_CURRENT]!!].toDoubleOrNull() ?: 0.0)).toInt())
                    current = (100 * (row[header[LogHeaderEnum.CURRENT]!!].toDoubleOrNull() ?: 0.0)).toInt()
                    setBatteryPercent(row[header[LogHeaderEnum.BATTERY_LEVEL]!!].toIntOrNull() ?: 0)
                    totalDistance = row[header[LogHeaderEnum.TOTALDISTANCE]!!].toLongOrNull() ?: 0
                    maxTemp = row[header[LogHeaderEnum.SYSTEM_TEMP]!!].toIntOrNull() ?: 0
                    maxPwm = (row[header[LogHeaderEnum.PWM]!!].toDoubleOrNull() ?: 0.0) / 100
                }
            }
        } catch (ex: Exception) {
            Timber.wtf(ex.localizedMessage)
        } finally {
            inputStream.close()
        }
    }
}