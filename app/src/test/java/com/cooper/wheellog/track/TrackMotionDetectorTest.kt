package com.cooper.wheellog.track

import org.junit.Assert.*
import org.junit.Test

class TrackMotionDetectorTest {
    private fun point(seconds: Int, meters: Double = 0.0, speed: Float = 0f, accuracy: Float = 5f) =
        TrackPoint(seconds * 1000L, 56.8 + meters / 111_195.0, 60.6, null, accuracy, speed)

    @Test fun `stationary fixes without speed become evidence instead of an endless unknown state`() {
        val detector = TrackMotionDetector()
        for (second in 0..600) {
            val evidence = detector.observe(point(second, (second % 5 - 2).toDouble()), false, second * 1000L)
            if (second >= 10) assertEquals(TrackMotion.STATIONARY, evidence)
            else assertEquals(TrackMotion.UNKNOWN, evidence)
        }
    }

    @Test fun `an isolated GPS jump cannot confirm movement`() {
        val detector = TrackMotionDetector()
        for (second in 0..20) detector.observe(point(second), false, second * 1000L)
        assertEquals(TrackMotion.UNKNOWN, detector.observe(point(21, 40.0), false, 21_000))
        assertNotEquals(TrackMotion.MOVING, detector.observe(point(22, 1.0), false, 22_000))
    }

    @Test fun `position progress across several fixes records GPS movement even without a speed field`() {
        val detector = TrackMotionDetector()
        assertEquals(TrackMotion.UNKNOWN, detector.observe(point(0), false, 0))
        assertEquals(TrackMotion.UNKNOWN, detector.observe(point(1, 10.0), false, 1000))
        assertEquals(TrackMotion.MOVING, detector.observe(point(2, 20.0), false, 2000))
        assertEquals(10f, detector.estimatedSpeedMps, .05f)
    }

    @Test fun `reported zero speed and noisy coordinates are standing immediately`() {
        val detector = TrackMotionDetector()
        for (second in 0..600) assertEquals(TrackMotion.STATIONARY,
            detector.observe(point(second, (second % 9).toDouble()), true, second * 1000L))
    }

    @Test fun `false GPS speed does not keep a stationary cluster moving`() {
        val detector = TrackMotionDetector()
        for (second in 0..60) {
            val evidence = detector.observe(point(second, (second % 4).toDouble(), speed = 4f), true, second * 1000L)
            if (second >= 10) assertEquals(TrackMotion.STATIONARY, evidence)
        }
    }

    @Test fun `duplicate fixes and gaps do not manufacture standing evidence`() {
        val detector = TrackMotionDetector()
        val original = point(0)
        detector.observe(original, false, 0)
        assertEquals(TrackMotion.UNKNOWN, detector.observe(original, false, 300_000))
        assertEquals(TrackMotion.UNKNOWN, detector.observe(point(301), false, 301_000))
        assertEquals(TrackMotion.UNKNOWN, detector.observe(point(302), false, 302_000))
    }
}
