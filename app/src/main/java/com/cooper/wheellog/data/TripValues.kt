package com.cooper.wheellog.data

data class TripValues(
    val gpsMaximum: Double?, val gpsAverage: Double?, val gpsMovingAverage: Double?,
    val wheelMaximum: Double?, val maxPwm: Double?, val maxPower: Double?,
    val maxCurrent: Double?, val maxPhaseCurrent: Double?, val minVoltage: Double?,
    val movingSeconds: Double?, val maxTemperature: Double?, val maxMotorTemperature: Double?
) {
    companion object {
        fun calculate(ticks: List<LogTick>): TripValues {
            val points = NativeCsv.points(ticks)
            val availableGps = ticks.filter { it.hasLocation && it.gpsSpeedAvailable }.map { it.gpsTimestampMillis }.toSet()
            val speeds = points.filter { it.time in availableGps }.map { it.speedMps * 3.6 }
            var seconds = 0.0
            var speedSeconds = 0.0
            var movingSeconds = 0.0
            var movingSpeedSeconds = 0.0
            points.zipWithNext().forEach { (before, after) ->
                val dt = (after.time - before.time) / 1000.0
                if (after.segmentStart || dt <= 0 || dt > 30 || before.time !in availableGps || after.time !in availableGps) return@forEach
                val speed = (before.speedMps + after.speedMps) * 1.8
                seconds += dt; speedSeconds += speed * dt
                if (speed > 0.1) { movingSeconds += dt; movingSpeedSeconds += speed * dt }
            }
            val wheel = ticks.filter { it.wheelDataAvailable }
            // A filtered route omits stationary coordinates, not stationary time.
            // Older files lack a recording clock: count short recorded intervals,
            // but never interpret an unrecorded morning-to-lunch gap as a stop.
            val totalSeconds = TripRecordingTime.seconds(ticks)
            return TripValues(
                speeds.maxOrNull(), (speedSeconds / totalSeconds).takeIf { seconds > 0 && totalSeconds > 0 },
                if (seconds <= 0) null else if (movingSeconds > 0) movingSpeedSeconds / movingSeconds else 0.0,
                wheel.maxOfOrNull { it.speed }, wheel.filter { it.pwmAvailable }.maxOfOrNull { it.pwm },
                wheel.mapNotNull { it.availablePower }.maxOrNull(), wheel.mapNotNull { it.availableCurrent }.maxOrNull(),
                wheel.mapNotNull { it.phaseCurrent }.maxOrNull(), wheel.mapNotNull { it.availableVoltage?.takeIf { v -> v > 0 } }.minOrNull(),
                movingSeconds.takeIf { seconds > 0 }, wheel.mapNotNull { it.availableTemperature }.maxOrNull(),
                wheel.mapNotNull { it.motorTemperature }.maxOrNull())
        }
    }
}
