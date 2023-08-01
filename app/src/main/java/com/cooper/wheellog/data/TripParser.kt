package com.cooper.wheellog.data

import android.content.Context
import android.net.Uri
import android.os.Build
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.LogHeaderEnum
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale

object TripParser {
    private val header = HashMap<LogHeaderEnum, Int>()
    private var lastErrorValue: String? = null

    val lastError: String
        get() {
            val res = lastErrorValue ?: ""
            lastErrorValue = null
            return res
        }

    fun parseFile(context: Context, fileName: String, path: String, uri: Uri): List<LogTick> {
        lastErrorValue = null
        val inputStream: InputStream?
        try {
            inputStream = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 9 or less
                FileInputStream(File(path))
            } else {
                // Android 10+
                context.contentResolver.openInputStream(uri)
            }
        } catch (ex: Exception) {
            lastErrorValue = ex.localizedMessage
            Timber.wtf(lastErrorValue)
            return emptyList()
        }
        return parseFile(context, fileName, inputStream)
    }

    fun parseFile(context: Context, fileName: String, inputStream: InputStream?): List<LogTick> {
        lastErrorValue = null
        if (inputStream == null) {
            lastErrorValue = context.getString(R.string.error_inputstream_null)
            Timber.wtf(lastErrorValue)
            return emptyList()
        }

        // read header
        val reader = BufferedReader(InputStreamReader(inputStream))
        val headerLine = reader.readLine().split(",").toTypedArray()
        for (i in headerLine.indices) {
            try {
                header[LogHeaderEnum.valueOf(headerLine[i].uppercase())] = i
            } catch (ignored: IllegalArgumentException) {
            }
        }
        if (!header.containsKey(LogHeaderEnum.LATITUDE) || !header.containsKey(LogHeaderEnum.LONGITUDE)) {
            inputStream.close()
            lastErrorValue = context.getString(R.string.error_this_file_without_gps, fileName)
            Timber.wtf(lastErrorValue)
            return emptyList()
        }

        val sdfTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val resultList = ArrayList<LogTick>()

        try {
            reader.forEachLine { line ->
                val row = line.split(",")
                val timeString = row[header[LogHeaderEnum.TIME]!!]
                val logTick = LogTick(
                    timeString = timeString,
                    time = sdfTime.parse(timeString)!!.time / 100f,
                    latitude = row[header[LogHeaderEnum.LATITUDE]!!].toDoubleOrNull() ?: 0.0,
                    longitude = row[header[LogHeaderEnum.LONGITUDE]!!].toDoubleOrNull() ?: 0.0,
                    altitude = row[header[LogHeaderEnum.GPS_ALT]!!].toDoubleOrNull() ?: 0.0,
                    batteryLevel = row[header[LogHeaderEnum.BATTERY_LEVEL]!!].toIntOrNull() ?: 0,
                    voltage = row[header[LogHeaderEnum.VOLTAGE]!!].toDoubleOrNull() ?: 0.0,
                    current = row[header[LogHeaderEnum.CURRENT]!!].toDoubleOrNull() ?: 0.0,
                    power = row[header[LogHeaderEnum.POWER]!!].toDoubleOrNull() ?: 0.0,
                    speed = row[header[LogHeaderEnum.SPEED]!!].toDoubleOrNull() ?: 0.0,
                    speedGps = row[header[LogHeaderEnum.GPS_SPEED]!!].toDoubleOrNull() ?: 0.0,
                    temperature = row[header[LogHeaderEnum.SYSTEM_TEMP]!!].toIntOrNull() ?: 0,
                    pwm = row[header[LogHeaderEnum.PWM]!!].toDoubleOrNull() ?: 0.0,
                    distance = row[header[LogHeaderEnum.DISTANCE]!!].toIntOrNull() ?: 0,
                    totalDistance = row[header[LogHeaderEnum.TOTALDISTANCE]!!].toIntOrNull() ?: 0
                )
                resultList.add(logTick)
            }
        } catch (ex: Exception) {
            lastErrorValue = ex.localizedMessage
            Timber.wtf(lastErrorValue)
            return resultList
        } finally {
            inputStream.close()
        }

        val dao = ElectroClub.instance.dao ?: return resultList
        var trip = dao.getTripByFileName(fileName)
        if (trip == null) {
            trip = TripDataDbEntry(fileName = fileName)
            dao.insert(trip)
        }
        try {
            val first = resultList.first()
            val last = resultList.last()
            trip.apply {
                duration = ((last.time - first.time) / 600.0).toInt()
                if (duration < 0) {
                    // +24 hours in minutes
                    duration += 1440
                }
                var timeToExclude = 0f
                var beforeTime = first.time // 1/10 sec
                var firstTotalDistance = 0

                resultList.forEach {
                    // If time between ticks more than 1 sec, we need to exclude this time
                    // or if time between ticks more than -1 hour (360 sec) next day ticks will be excluded
                    val timeBetween = it.time - beforeTime
                    if (-3600_0 > timeBetween || timeBetween > 10) {
                        timeToExclude += timeBetween
                    }
                    beforeTime = it.time

                    if (firstTotalDistance == 0 && it.totalDistance > 0) {
                        firstTotalDistance = it.totalDistance
                    }
                    maxSpeedGps = maxSpeedGps.coerceAtLeast(it.speedGps.toFloat())
                    maxCurrent = maxCurrent.coerceAtLeast(it.current.toFloat())
                    maxPwm = maxPwm.coerceAtLeast(it.pwm.toFloat())
                }

                val maxTotalDistance = resultList.subList(resultList.size - 10, resultList.size).maxOf { it.totalDistance }
                val logDuration = (last.time - first.time - timeToExclude) / 600
                distance = maxTotalDistance - firstTotalDistance
                val powers = resultList.map { it.power }
                maxPower = powers.max().toFloat()
                val speeds = resultList.map { it.speed }
                maxSpeed = speeds.max().toFloat()
                avgSpeed = speeds.average().toFloat()
                consumptionTotal = powers.average().toFloat() * logDuration / 60F
                consumptionByKm = if (distance > 0) {
                    consumptionTotal * 1000F / distance
                } else {
                    0F
                }
            }
            dao.update(trip)
        } catch (ex: Exception) {
            lastErrorValue = ex.localizedMessage
            Timber.wtf(lastErrorValue)
        }

        return resultList
    }
}
