package com.cooper.wheellog.data

import com.cooper.wheellog.WheelData
import com.cooper.wheellog.track.TrackPoint
import com.cooper.wheellog.track.TrackTelemetry
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Original CSV columns remain first; optional trailing columns preserve GPS fixes and gaps. */
object NativeCsv {
    const val HEADER = "date,time,latitude,longitude,gps_speed,gps_alt,gps_heading,gps_distance,speed,voltage,phase_current,current,power,torque,pwm,battery_level,distance,totaldistance,system_temp,temp2,tilt,roll,mode,alert,gps_time,gps_accuracy,track_segment,wheel_speed_raw,speed_correction_percent,recording_elapsed_ms"
    fun row(time: Long, gps: TrackPoint?, wheel: NativeWheelSample?, meters: Double, segment: Boolean,
            recordingElapsedMillis: Long? = null): String {
        val date = SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.US).format(Date(time))
        val position = listOf(gps?.latitude, gps?.longitude, gps?.speedMps?.times(3.6), gps?.altitude,
            null, meters.takeIf { gps != null }).map { it?.toString() ?: "" }
        return date + "," + (position + (wheel?.columns ?: List(16) { "" }) + listOf(
            gps?.time?.toString() ?: "", gps?.accuracy?.toString() ?: "", if (segment) "1" else "0",
            wheel?.rawSpeedKmh?.toString() ?: "", wheel?.correctionPercent?.toString()?.removeSuffix(".0") ?: "",
            recordingElapsedMillis?.toString() ?: ""
        )).joinToString(",") { it.replace(',', ';').replace('\n', ' ').replace('\r', ' ') }
    }

    /** Continuing a file after an update must preserve that file's original column shape. */
    fun alignRow(row: String, columns: List<String>): String {
        val values = HEADER.split(',').zip(row.split(',')).toMap()
        return columns.joinToString(",") { values[it] ?: "" }
    }

    fun read(input: InputStream): List<LogTick> = input.bufferedReader().use { reader ->
        val columns = (reader.readLine() ?: "").trimStart('\uFEFF').split(',').withIndex().associate { it.value.lowercase(Locale.US) to it.index }
        require(listOf("date", "time", "speed").all { it in columns }) { "Not a WheelLog CSV file" }
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply { isLenient = false }
        val legacyStatistics = "gps_time" !in columns && "latitude" in columns && "longitude" in columns
        val legacyClock = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val result = ArrayList<LogTick>()
        var firstTime: Long? = null
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            // A process can die midway through its last row. Complete earlier rows remain usable.
            val values = line.split(',')
            if (values.size < columns.size) return@forEachLine
            fun cell(name: String) = columns[name]?.let { values.getOrNull(it) } ?: ""
            fun number(name: String) = cell(name).toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
            val time = runCatching { timestamp.parse(cell("date") + " " + cell("time"))!!.time }.getOrNull() ?: return@forEachLine
            val wheel = cell("speed").toDoubleOrNull()?.isFinite() == true
            val latitude = cell("latitude").toDoubleOrNull()
            val longitude = cell("longitude").toDoubleOrNull()
            val located = latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
                (latitude != 0.0 || longitude != 0.0 || cell("gps_time").toLongOrNull() != null)
            val dayTime = if (legacyStatistics) legacyClock.parse(cell("time"))!!.time / 100f
                else ((time - timestamp.parse(cell("date") + " 00:00:00.000")!!.time) / 100.0).toFloat()
            if (firstTime == null) firstTime = time
            result.add(LogTick(dayTime, cell("time"), ((time - firstTime!!) / 100.0).toFloat(), latitude ?: 0.0, longitude ?: 0.0,
                number("gps_alt"), number("battery_level").toInt(), number("voltage"), number("current"), number("power"),
                if (wheel) number("speed") else number("gps_speed"), number("gps_speed"), number("system_temp").toInt(), number("pwm"),
                number("distance").toInt(), number("totaldistance").toInt(), time, wheel, located,
                cell("gps_time").toLongOrNull() ?: time, number("gps_accuracy").takeIf { it > 0 }?.toFloat() ?: 10f,
                number("gps_distance"), cell("track_segment") == "1", cell("pwm").toDoubleOrNull()?.isFinite() == true,
                legacyStatistics,
                phaseCurrent = cell("phase_current").toDoubleOrNull()?.takeIf { it.isFinite() },
                motorTemperature = cell("temp2").toDoubleOrNull()?.takeIf { it.isFinite() },
                availablePower = cell("power").toDoubleOrNull()?.takeIf { it.isFinite() },
                availableCurrent = cell("current").toDoubleOrNull()?.takeIf { it.isFinite() },
                availableVoltage = cell("voltage").toDoubleOrNull()?.takeIf { it.isFinite() },
                availableTemperature = cell("system_temp").toDoubleOrNull()?.takeIf { it.isFinite() },
                gpsSpeedAvailable = cell("gps_speed").toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true,
                recordingElapsedMillis = cell("recording_elapsed_ms").toLongOrNull()?.takeIf { it >= 0 }))
        }
        result
    }

    fun points(ticks: List<LogTick>): List<TrackPoint> {
        val result = ArrayList<TrackPoint>()
        val groups = ArrayList<Int>()
        val wheelByGroup = mutableMapOf<Int, MutableList<LogTick>>()
        var group = 0
        var pendingSegment = true
        var lastPointGroup = -1
        fun telemetry(tick: LogTick) = TrackTelemetry(tick.speed, tick.batteryLevel, tick.voltage,
            tick.temperature, tick.pwm, tick.pwmAvailable)
        var bestOffset = Long.MAX_VALUE
        ticks.forEach { tick ->
            if (tick.segmentStart) group++
            if (tick.wheelDataAvailable) wheelByGroup.getOrPut(group) { mutableListOf() }.add(tick)
            pendingSegment = pendingSegment || tick.segmentStart
            if (!tick.hasLocation) return@forEach
            val previous = result.lastOrNull()
            val offset = kotlin.math.abs(tick.timestampMillis - tick.gpsTimestampMillis)
            val sample = tick.takeIf { it.wheelDataAvailable && offset <= 5_000 }?.let(::telemetry)
            if (previous != null && tick.gpsTimestampMillis == previous.time && lastPointGroup == group) {
                // A GPS callback may precede the wheel callback carrying this same fix.
                if (sample != null && (previous.telemetry == null || offset < bestOffset ||
                        !previous.telemetry.pwmAvailable && sample.pwmAvailable)) {
                    result[result.lastIndex] = previous.copy(telemetry = sample)
                    bestOffset = offset
                }
                return@forEach
            }
            if (previous != null && tick.gpsTimestampMillis <= previous.time) return@forEach
            result.add(TrackPoint(tick.gpsTimestampMillis, tick.latitude, tick.longitude, tick.altitude, tick.gpsAccuracy,
                (tick.speedGps / 3.6).toFloat(), pendingSegment || previous == null || tick.gpsTimestampMillis - previous.time > 30_000,
                sample))
            groups.add(group)
            lastPointGroup = group
            bestOffset = if (sample == null) Long.MAX_VALUE else offset
            pendingSegment = false
        }
        // Old CSVs can contain separate wheel-only rows. Associate only a bracketed,
        // live interval (at most two seconds), never extrapolate through a disconnect.
        val ordered = wheelByGroup.mapValues { (_, rows) -> rows.sortedBy { it.timestampMillis } }
        result.indices.forEach { index ->
            val point = result[index]
            if (point.telemetry != null) return@forEach
            val rows = ordered[groups[index]] ?: return@forEach
            val found = rows.binarySearchBy(point.time) { it.timestampMillis }
            val after = if (found >= 0) found else -found - 1
            val exact = rows.getOrNull(after)?.takeIf { it.timestampMillis == point.time }
            val before = rows.getOrNull(after - 1)
            val next = rows.getOrNull(after)
            val sample = exact ?: if (before != null && next != null &&
                point.time - before.timestampMillis <= 1_000 && next.timestampMillis - point.time <= 1_000)
                if (point.time - before.timestampMillis <= next.timestampMillis - point.time) before else next
                else null
            if (sample != null) result[index] = point.copy(telemetry = telemetry(sample))
        }
        return result
    }
}

data class NativeWheelSample(val columns: List<String>, val telemetry: TrackTelemetry,
                             val rawSpeedKmh: Double? = null, val correctionPercent: Double = 0.0) {
    companion object {
        fun capture(data: WheelData, distance: Int, alert: String) = NativeWheelSample(listOf(data.speedDouble, data.voltageDouble, data.phaseCurrentDouble,
            data.currentDouble, data.powerDouble, data.torque, data.calculatedPwm, data.batteryLevel, distance,
            data.totalDistance, data.temperature, data.temperature2, data.angle, data.roll, data.modeStr, alert).map { it?.toString() ?: "" },
            TrackTelemetry(data.speedDouble, data.batteryLevel, data.voltageDouble, data.temperature, data.calculatedPwm),
            data.rawSpeedDouble, data.appliedSpeedCorrectionPercent)
    }
}
