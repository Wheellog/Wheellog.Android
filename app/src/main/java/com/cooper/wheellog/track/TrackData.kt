package com.cooper.wheellog.track

import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

data class TrackTelemetry(val speedKmh: Double, val battery: Int, val voltage: Double,
                          val temperature: Int, val pwm: Double, val pwmAvailable: Boolean = true)

data class TrackPoint(
    val time: Long, val latitude: Double, val longitude: Double,
    val altitude: Double?, val accuracy: Float, val speedMps: Float,
    val segmentStart: Boolean = false, val telemetry: TrackTelemetry? = null,
) {
    fun distanceTo(other: TrackPoint): Double {
        val lat = Math.toRadians(other.latitude - latitude)
        val lon = Math.toRadians(other.longitude - longitude)
        val a = sin(lat / 2).pow(2) + cos(Math.toRadians(latitude)) *
            cos(Math.toRadians(other.latitude)) * sin(lon / 2).pow(2)
        return 6_371_000 * 2 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1 - a).coerceIn(0.0, 1.0)))
    }

    fun toLine(): String = listOf(time, latitude, longitude, altitude ?: "", accuracy, speedMps,
        if (segmentStart) 1 else 0, telemetry?.speedKmh ?: "", telemetry?.battery ?: "",
        telemetry?.voltage ?: "", telemetry?.temperature ?: "", telemetry?.takeIf { it.pwmAvailable }?.pwm ?: "").joinToString("\t")

    companion object {
        fun fromLine(line: String): TrackPoint? = runCatching {
            val cells = line.split('\t')
            require(cells.size == 12)
            TrackPoint(cells[0].toLong(), cells[1].toDouble(), cells[2].toDouble(), cells[3].toDoubleOrNull(),
                cells[4].toFloat(), cells[5].toFloat(), cells[6] == "1",
                if (cells[7].isEmpty()) null else TrackTelemetry(cells[7].toDouble(), cells[8].toInt(),
                    cells[9].toDouble(), cells[10].toInt(), cells[11].toDoubleOrNull() ?: 0.0, cells[11].toDoubleOrNull()?.isFinite() == true))
        }.getOrNull()
    }
}

/** Reject stale fixes, poor accuracy and impossible jumps before persisting any point. */
class TrackPointFilter {
    private var last: TrackPoint? = null
    private var newSegment = true
    fun startSegment() { newSegment = true }
    fun accept(point: TrackPoint, now: Long): TrackPoint? {
        if (!point.latitude.isFinite() || !point.longitude.isFinite() ||
            point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0 ||
            !point.accuracy.isFinite() || point.accuracy !in 0f..50f ||
            !point.speedMps.isFinite() || point.speedMps !in 0f..70f ||
            (point.altitude != null && !point.altitude.isFinite()) ||
            point.time < now - 15_000 || point.time > now + 5_000) return null
        val previous = last
        var segment = newSegment || previous == null
        if (previous != null) {
            val dt = (point.time - previous.time) / 1000.0
            if (dt <= 0) return null
            if (dt > 30) segment = true
            val distance = previous.distanceTo(point)
            if (!segment && (distance - point.accuracy - previous.accuracy).coerceAtLeast(0.0) / dt > 55) return null
            if (!segment && distance < max(3.0, point.accuracy * 0.3) && dt < 15) return null
        }
        val accepted = point.copy(segmentStart = segment)
        last = accepted
        newSegment = false
        return accepted
    }
}

object TrackStartPolicy {
    fun shouldStart(locationEnabled: Boolean, automaticEnabled: Boolean, connected: Boolean,
                    speedKmh: Double, thresholdKmh: Float, alreadyRecording: Boolean, suppressed: Boolean): Boolean =
        locationEnabled && automaticEnabled && connected && !alreadyRecording && !suppressed &&
            speedKmh.isFinite() && thresholdKmh.isFinite() && speedKmh > max(0f, thresholdKmh)
}

data class TrackSummary(val points: Int, val distanceMeters: Double, val maxSpeedKmh: Double,
                        val movingAverageKmh: Double, val movingSeconds: Double)

fun summarizeTrack(points: List<TrackPoint>): TrackSummary {
    var distance = 0.0
    var movingDistance = 0.0
    var movingSeconds = 0.0
    for (index in 1 until points.size) {
        val before = points[index - 1]
        val after = points[index]
        val seconds = (after.time - before.time) / 1000.0
        if (after.segmentStart || seconds <= 0 || seconds > 30) continue
        val meters = before.distanceTo(after)
        distance += meters
        val movementKmh = if (before.telemetry != null && after.telemetry != null)
            (before.telemetry.speedKmh + after.telemetry.speedKmh) / 2 else meters / seconds * 3.6
        if (movementKmh > 0.1) {
            movingDistance += movementKmh / 3.6 * seconds
            movingSeconds += seconds
        }
    }
    return TrackSummary(points.size, distance, points.maxOfOrNull { it.telemetry?.speedKmh ?: it.speedMps * 3.6 } ?: 0.0,
        if (movingSeconds > 0) movingDistance / movingSeconds * 3.6 else 0.0, movingSeconds)
}

object GpxFormat {
    private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;")
    fun write(writer: Writer, title: String, points: Sequence<TrackPoint>) {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write("<gpx version=\"1.1\" creator=\"WheelLog\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:wl=\"https://github.com/Wheellog/Wheellog.Android/gpx/1\">\n")
        writer.write("<trk><name>${escape(title)}</name><trkseg>\n")
        var first = true
        points.forEach { point ->
            if (!first && point.segmentStart) writer.write("</trkseg><trkseg>\n")
            first = false
            writer.write("<trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
            point.altitude?.let { writer.write("<ele>$it</ele>") }
            writer.write("<time>${date.format(Date(point.time))}</time><extensions><wl:gpsSpeed>${point.speedMps * 3.6}</wl:gpsSpeed><wl:accuracy>${point.accuracy}</wl:accuracy>")
            point.telemetry?.let {
                writer.write("<wl:speed>${it.speedKmh}</wl:speed><wl:battery>${it.battery}</wl:battery><wl:voltage>${it.voltage}</wl:voltage><wl:temperature>${it.temperature}</wl:temperature>")
                if (it.pwmAvailable) writer.write("<wl:pwm>${it.pwm}</wl:pwm>")
            }
            writer.write("</extensions></trkpt>\n")
        }
        writer.write("</trkseg></trk></gpx>\n")
        writer.flush()
    }
}
