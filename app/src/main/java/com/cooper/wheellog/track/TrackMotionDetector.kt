package com.cooper.wheellog.track

import java.util.ArrayDeque
import kotlin.math.max

enum class TrackMotion { MOVING, STATIONARY, UNKNOWN }

/** GPS speed alone must not turn a cluster of noisy fixes into a route on a parked wheel. */
class TrackMotionDetector {
    private data class Fix(val point: TrackPoint, val receivedAt: Long)
    private val fixes = ArrayDeque<Fix>()
    var estimatedSpeedMps = 0f
        private set

    fun reset() { fixes.clear(); estimatedSpeedMps = 0f }

    fun observe(point: TrackPoint, hasSpeed: Boolean, now: Long): TrackMotion {
        val last = fixes.peekLast()
        if (last != null) {
            if (point.time <= last.point.time || now <= last.receivedAt) return TrackMotion.UNKNOWN
            val seconds = (now - last.receivedAt) / 1000.0
            if (seconds > 30) reset()
            else if ((last.point.distanceTo(point) - last.point.accuracy - point.accuracy).coerceAtLeast(0.0) / seconds > 55)
                return TrackMotion.UNKNOWN
        }
        while (fixes.isNotEmpty() && now - fixes.first.receivedAt > 30_000) fixes.removeFirst()
        fixes.addLast(Fix(point, now))
        // Bound memory even if a provider delivers unusually frequent updates.
        while (fixes.size > 120) fixes.removeFirst()
        if (hasSpeed && point.speedMps <= .8f) return TrackMotion.STATIONARY
        val first = fixes.first
        val elapsed = now - first.receivedAt
        val displacement = first.point.distanceTo(point)
        val radius = max(8.0, (first.point.accuracy + point.accuracy).toDouble())
        val preceding = fixes.toList().getOrNull(fixes.size - 2)?.point
        if (fixes.size >= 3 && elapsed >= 2_000 && displacement > radius && preceding != null &&
            first.point.distanceTo(preceding) > radius / 2 && preceding.distanceTo(point) < displacement) {
            estimatedSpeedMps = (displacement / (elapsed / 1000.0)).toFloat().coerceIn(0f, 70f)
            return TrackMotion.MOVING
        }
        if (elapsed >= 10_000 && fixes.all {
                first.point.distanceTo(it.point) <= max(8.0, (first.point.accuracy + it.point.accuracy).toDouble())
            }) return TrackMotion.STATIONARY
        // The first moving fix is a harmless anchor, not a distance or a fabricated segment.
        if (fixes.size == 1 && hasSpeed && point.speedMps > .8f) return TrackMotion.MOVING
        return TrackMotion.UNKNOWN
    }
}
