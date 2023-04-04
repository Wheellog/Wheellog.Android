package com.cooper.wheellog.utils

import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ParserLogToWheelData {
    private val header = HashMap<LogHeaderEnum, Int>()

    fun parseFile(fileUtil: FileUtil) {
        val inputStream = fileUtil.inputStream
        if (inputStream == null) {
            Timber.wtf(fileUtil.context.getString(R.string.error_inputstream_null) + " " + fileUtil.fileName)
            return
        }

        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine().split(",").toTypedArray()
            for (i in headerLine.indices) {
                try {
                    header[LogHeaderEnum.valueOf(headerLine[i].uppercase(Locale.US))] = i
                } catch (ignored: IllegalArgumentException) {
                }
            }
            if (!header.containsKey(LogHeaderEnum.LATITUDE) || !header.containsKey(LogHeaderEnum.LONGITUDE)) {
                inputStream.close()
                Timber.wtf(fileUtil.context.getString(R.string.error_this_file_without_gps, fileUtil.fileName))
                return
            }

            val firstRow = reader.readLine()!!.split(",")
            var sdf = SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.US)
            val startDate = sdf.parse(firstRow[header[LogHeaderEnum.DATE]!!] + "," + firstRow[header[LogHeaderEnum.TIME]!!])
            val rideStartTime = startDate!!.time
            val wd = WheelData.getInstance()
            wd.setStartParameters(
                rideStartTime,
                firstRow[header[LogHeaderEnum.TOTALDISTANCE]!!].toLongOrNull() ?: 0L
            )

            sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
            var lastTime = sdf.parse(firstRow[header[LogHeaderEnum.TIME]!!])!!.time
            reader.forEachLine { line ->
                val row = line.split(",")
                val speed = (100 * (row[header[LogHeaderEnum.SPEED]!!].toDoubleOrNull() ?: 0.0)).toInt()
                wd.topSpeed = speed
                val voltage = (100 * (row[header[LogHeaderEnum.VOLTAGE]!!].toDoubleOrNull() ?: 0.0)).toInt()
                wd.voltage = voltage
                wd.voltageSag = voltage
                wd.phaseCurrent = (100 * (row[header[LogHeaderEnum.PHASE_CURRENT]!!].toDoubleOrNull() ?: 0.0)).toInt()
                wd.current = (100 * (row[header[LogHeaderEnum.CURRENT]!!].toDoubleOrNull() ?: 0.0)).toInt()
                wd.batteryLevel = row[header[LogHeaderEnum.BATTERY_LEVEL]!!].toIntOrNull() ?: 0
                wd.totalDistance = row[header[LogHeaderEnum.TOTALDISTANCE]!!].toLongOrNull() ?: 0
                wd.maxTemp = row[header[LogHeaderEnum.SYSTEM_TEMP]!!].toIntOrNull() ?: 0
                wd.maxPwm = (row[header[LogHeaderEnum.PWM]!!].toDoubleOrNull() ?: 0.0) / 100

                val time = sdf.parse(row[header[LogHeaderEnum.TIME]!!])!!.time
                if (time >= lastTime + 1000 && speed > 200) {
                    wd.incrementRidingTime()
                    lastTime = time
                }
            }
            wd.updateRideTime()
        } catch (ex: Exception) {
            Timber.wtf(ex.localizedMessage)
        } finally {
            inputStream.close()
        }
    }
}