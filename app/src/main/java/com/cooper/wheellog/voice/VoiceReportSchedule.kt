package com.cooper.wheellog.voice

data class DistanceReports(
    val periodic: Boolean = false,
    val intervalMetres: Long = 5000,
    val after: Boolean = false,
    val afterMetres: Long = 5000,
    val originMetres: Long? = null,
    val target: Boolean = false,
    val targetMetres: Long? = null
)

/** Determines when to read the configured report; never changes its contents. */
class VoiceReportSchedule {
    data class Due(val time: Boolean, val periodic: Boolean, val after: Boolean, val target: Boolean,
                   internal val bucket: Long) {
        val any get() = time || periodic || after || target
    }
    private var mac = ""
    private var timeAt: Long? = null
    private var timeConfig: Pair<Boolean, Long>? = null
    private var distanceConfig: Pair<Boolean, Long>? = null
    private var bucket = 0L
    private var trip = 0L

    fun due(wheel: String, now: Long, totalMetres: Long, tripMetres: Long,
            timed: Boolean, intervalMs: Long, distances: DistanceReports): Due {
        if (wheel != mac) {
            reset(); mac = wheel
        }
        val timeOptions = timed to intervalMs
        if (timeConfig != timeOptions || timeAt == null) {
            timeConfig = timeOptions; timeAt = now
        }
        val distanceOptions = distances.periodic to distances.intervalMetres
        val currentBucket = tripMetres.coerceAtLeast(0) / distances.intervalMetres.coerceAtLeast(1)
        if (distanceConfig != distanceOptions || tripMetres < trip) {
            distanceConfig = distanceOptions; bucket = currentBucket
        }
        trip = tripMetres
        val origin = distances.originMetres
        return Due(timed && now - requireNotNull(timeAt) >= intervalMs,
            distances.periodic && totalMetres > 0 && currentBucket > bucket,
            distances.after && origin != null && totalMetres >= origin &&
                totalMetres - origin >= distances.afterMetres,
            distances.target && distances.targetMetres?.let { totalMetres >= it } == true,
            currentBucket)
    }

    /** Commit only after TTS accepted the report. Coalesce missed intervals. */
    fun spoken(now: Long, due: Due) {
        timeAt = now
        bucket = due.bucket
    }
    fun postponeTime(now: Long) { timeAt = now }
    fun reset() {
        mac = ""; timeAt = null; timeConfig = null; distanceConfig = null; bucket = 0; trip = 0
    }
}
