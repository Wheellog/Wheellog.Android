package com.cooper.wheellog.data

data class SpeedSample(val timeMillis: Long, val speedKmh: Double, val segmentStart: Boolean = false)

/** Count recorded intervals, never the hours between separate recording sessions. */
object TripRecordingTime {
    fun seconds(ticks: List<LogTick>): Double = ticks.zipWithNext().sumOf { (before, after) ->
        val a = before.recordingElapsedMillis
        val b = after.recordingElapsedMillis
        if (a != null && b != null) (b - a).coerceAtLeast(0) / 1000.0
        else ((after.timestampMillis - before.timestampMillis) / 1000.0).takeIf { it > 0 && it <= 30 } ?: 0.0
    }
}
data class TripSpeedStats(val maximum: Double, val movingAverage: Double, val movingSeconds: Double) {
    companion object {
        fun calculate(samples: List<SpeedSample>): TripSpeedStats {
            var seconds = 0.0
            var speedSeconds = 0.0
            for (index in 1 until samples.size) {
                val before = samples[index - 1]
                val after = samples[index]
                val dt = (after.timeMillis - before.timeMillis) / 1000.0
                if (after.segmentStart || dt <= 0 || dt > 10 || !before.speedKmh.isFinite() || !after.speedKmh.isFinite()) continue
                val speed = (before.speedKmh.coerceAtLeast(0.0) + after.speedKmh.coerceAtLeast(0.0)) / 2
                if (speed > 0.1) { seconds += dt; speedSeconds += speed * dt }
            }
            return TripSpeedStats(samples.map { it.speedKmh }.filter { it.isFinite() }.maxOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                if (seconds > 0) speedSeconds / seconds else 0.0, seconds)
        }
    }
}
