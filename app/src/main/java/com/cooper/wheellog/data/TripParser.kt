package com.cooper.wheellog.data

import android.content.Context
import android.net.Uri
import android.os.Build
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.LogHeaderEnum
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale
import org.json.JSONObject

object TripParser: KoinComponent {
    const val STATS_VERSION = 6
    private val dao: TripDao by inject()
    private val errors = ThreadLocal<String?>()
    private var lastErrorValue: String?
        get() = errors.get()
        set(value) { errors.set(value) }

    val lastError: String
        get() {
            val res = lastErrorValue ?: ""
            lastErrorValue = null
            return res
        }

    @Synchronized fun parseFile(context: Context, fileName: String, path: String, uri: Uri): Pair<List<LogTick>, TripDataDbEntry?> {
        lastErrorValue = null
        val inputStream: InputStream?
        try {
            inputStream = if (uri.scheme == "file") {
                FileInputStream(File(requireNotNull(uri.path)))
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && path.isNotBlank() && path != "undefined") {
                // Android 9 or less
                FileInputStream(File(path))
            } else {
                // Android 10+
                context.contentResolver.openInputStream(uri)
            }
        } catch (ex: Exception) {
            lastErrorValue = ex.localizedMessage
            Timber.wtf(lastErrorValue)
            return Pair(emptyList(), null)
        }
        return parseFile(context, fileName, inputStream)
    }

    @Synchronized fun parseFile(context: Context, fileName: String, inputStream: InputStream?): Pair<List<LogTick>, TripDataDbEntry?> {
        lastErrorValue = null
        if (inputStream == null) {
            lastErrorValue = context.getString(R.string.error_inputstream_null)
            Timber.wtf(lastErrorValue)
            return Pair(emptyList(), null)
        }

        val resultList = try { NativeCsv.read(inputStream) } catch (error: Exception) {
            lastErrorValue = error.localizedMessage
            return Pair(emptyList(), null)
        }
        if (resultList.isEmpty()) return Pair(emptyList(), dao.getTripByFileName(fileName))

        var trip = dao.getTripByFileName(fileName)
        if (trip == null) {
            trip = TripDataDbEntry(fileName = fileName)
            dao.insert(trip)
        }
        try {
            val first = resultList.first()
            val last = resultList.last()
            trip.apply {
                // Preserve the stock summaries of historical CSVs. Extended logs
                // use absolute timestamps, including precise midnight crossings.
                duration = if (first.legacyStatistics) {
                    ((last.time - first.time) / 600.0).toInt().let { if (it < 0) it + 1440 else it }
                } else (TripRecordingTime.seconds(resultList) / 60).toInt()
                var timeToExclude = 0f
                var beforeTime = first.time // 1/10 sec
                var firstTotalDistance = 0
                maxSpeedGps = 0f
                maxCurrent = 0f
                maxPwm = 0f

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
                    if (it.gpsSpeedAvailable) maxSpeedGps = maxSpeedGps.coerceAtLeast(it.speedGps.toFloat())
                    if (it.wheelDataAvailable) maxCurrent = maxCurrent.coerceAtLeast(it.current.toFloat())
                    if (it.pwmAvailable) maxPwm = maxPwm.coerceAtLeast(it.pwm.toFloat())
                }

                val maxTotalDistance = if (first.legacyStatistics) {
                    resultList.subList(0.coerceAtLeast(resultList.size - 10), resultList.size).maxOf { it.totalDistance }
                } else {
                    // GPS-only/timing-only rows have no wheel odometer. Keep the last
                    // actual reading even when more than ten such rows end the file.
                    resultList.lastOrNull { it.wheelDataAvailable && it.totalDistance > 0 }?.totalDistance ?: 0
                }
                val logDuration = if (first.legacyStatistics) (last.time - first.time - timeToExclude) / 600
                else resultList.zipWithNext().sumOf { (before, after) ->
                    ((after.timestampMillis - before.timestampMillis) / 1000.0).takeIf { it > 0 && it <= 10 } ?: 0.0
                }.toFloat() / 60f
                distance = if (first.legacyStatistics) maxTotalDistance - firstTotalDistance else
                    maxOf(maxTotalDistance - firstTotalDistance, com.cooper.wheellog.track.summarizeTrack(NativeCsv.points(resultList)).distanceMeters.toInt()).coerceAtLeast(0)
                val powers = resultList.map { it.power }
                val wheelTicks = resultList.filter { it.wheelDataAvailable }
                maxPower = wheelTicks.mapNotNull { it.availablePower }.maxOrNull()?.toFloat() ?: 0f
                maxSpeed = wheelTicks.maxOfOrNull { it.speed }?.toFloat() ?: 0f
                val moving = TripSpeedStats.calculate(wheelTicks.map { SpeedSample(it.timestampMillis, it.speed, it.segmentStart) })
                val values = TripValues.calculate(resultList)
                avgSpeed = if (first.legacyStatistics) resultList.map { it.speed }.average().toFloat()
                    else (values.gpsMovingAverage ?: moving.movingAverage).toFloat()
                var energyWh = 0.0
                var energySeconds = 0.0
                resultList.zipWithNext().forEach { (before, after) ->
                    val a = before.availablePower
                    val b = after.availablePower
                    val seconds = (after.timestampMillis - before.timestampMillis) / 1000.0
                    if (before.wheelDataAvailable && after.wheelDataAvailable && a != null && b != null && seconds > 0 && seconds <= 5) {
                        energyWh += (a + b) / 2 * seconds / 3600.0
                        energySeconds += seconds
                    }
                }
                val hasEnergy = if (first.legacyStatistics) resultList.any { it.availablePower != null } else energySeconds > 0
                additionalJson = (runCatching { JSONObject(additionalJson ?: "{}") }.getOrDefault(JSONObject())).apply {
                    put("tripStatsVersion", STATS_VERSION)
                    put("movingAverageSpeed", moving.movingAverage)
                    put("movingSeconds", moving.movingSeconds)
                    put("hasWheelSpeed", wheelTicks.isNotEmpty())
                    put("hasGpsSpeed", values.gpsMaximum != null)
                    put("hasAverageSpeed", values.gpsMovingAverage != null || wheelTicks.isNotEmpty())
                    put("hasPwm", values.maxPwm != null)
                    put("hasEnergy", hasEnergy)
                    put("activeDurationMinutes", (TripRecordingTime.seconds(resultList) / 60).toInt())
                    put("startedAt", first.timestampMillis)
                    put("endedAt", last.timestampMillis)
                }.toString()
                consumptionTotal = if (first.legacyStatistics) powers.average().toFloat() * logDuration / 60F else energyWh.toFloat()
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

        return Pair(resultList, trip)
    }
}
