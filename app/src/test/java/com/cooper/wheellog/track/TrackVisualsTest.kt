package com.cooper.wheellog.track

import org.junit.Assert.*
import org.junit.Test

class TrackVisualsTest {
    private fun point(time: Long, segment: Boolean = false, pwm: Double? = null) =
        TrackPoint(time, 56.8, 60.6, null, 5f, 10f, segment,
            pwm?.let { TrackTelemetry(36.0, 80, 140.0, 30, it) })

    @Test fun `unknown or absent color setting is always no gradient`() {
        assertEquals(TrackColorMode.NONE, TrackColorMode.fromKey(null))
        assertEquals(TrackColorMode.NONE, TrackColorMode.fromKey("invalid"))
        assertEquals(TrackColorMode.SPEED, TrackColorMode.fromKey("speed"))
        assertEquals(TrackColorMode.NONE, TrackColorMode.fromKey("pwm"))
    }
    @Test fun `solid color remains unchanged and speed works without wheel telemetry`() {
        val solid = 0xff123d
        for (speed in listOf(0f, 10f, 30f)) {
            assertEquals(solid, TrackVisuals.color(point(1_000).copy(speedMps = speed), TrackColorMode.NONE, solid, 100.0))
        }
        assertEquals(0xffff0000.toInt(), TrackVisuals.color(point(1_000).copy(speedMps = 10f), TrackColorMode.SPEED, solid, 36.0))
    }
    @Test fun `playback uses real elapsed time at fixed sixty times speed`() {
        val playback = TrackPlayback()
        assertEquals(60, TrackPlayback.SPEED)
        playback.start(1_000, 500)
        assertEquals(8_380L, playback.advance(623, 1_000_000))
        assertEquals(67_000L, playback.advance(1_600, 1_000_000))
    }
    @Test fun `sixty times playback resumes without counting paused time and stops at the end`() {
        val playback = TrackPlayback()
        playback.start(1_000, 0)
        assertEquals(61_000L, playback.advance(1_000, 100_000))
        playback.start(61_000, 30_000) // pause time must not be replayed
        assertEquals(91_000L, playback.advance(30_500, 100_000))
        assertEquals(100_000L, playback.advance(31_000, 100_000))
        assertEquals(100_000L, playback.advance(40_000, 100_000))
    }
    @Test fun `timeline skips missing hours and follows real timing within each recorded segment`() {
        val points = listOf(point(1_000, true), point(11_000), point(601_000, true), point(611_000))
        val timeline = TrackTimeline(points)
        assertEquals(20_000L, timeline.durationMillis)
        val middle = timeline.select(5_000)!!
        assertEquals(601_000L, middle.requestedTime)
        assertEquals(2, middle.index)
        assertFalse(middle.inGap)
        assertEquals(0, timeline.select(0)!!.index)
        assertEquals(6_000L, timeline.select(2_500)!!.requestedTime)
        val end = timeline.select(10_000)!!
        assertEquals(3, end.index); assertFalse(end.inGap)
        assertEquals(611_000L, end.requestedTime)
        assertNull(TrackTimeline(emptyList()).select(50))
    }
    @Test fun `single position is selectable without replaying the GPS acquisition delay`() {
        val timeline = TrackTimeline(listOf(point(60_000)))
        val selected = timeline.select(0)!!
        assertFalse(selected.inGap)
        assertEquals(60_000L, selected.requestedTime)
        assertEquals(0L, timeline.durationMillis)
    }
    @Test fun `playback at sixty times crosses the morning gap without spending hours on missing samples`() {
        val timeline = TrackTimeline(listOf(point(0, true), point(30_000),
            point(4 * 3_600_000L, true), point(4 * 3_600_000L + 30_000)))
        val playback = TrackPlayback()
        playback.start(0, 0)
        val elapsed = playback.advance(750, timeline.durationMillis)
        val selection = timeline.select((elapsed * TrackTimeline.STEPS / timeline.durationMillis).toInt())!!
        assertEquals(4 * 3_600_000L + 15_000, selection.requestedTime)
        assertEquals(60_000L, playback.advance(1_000, timeline.durationMillis))
    }
    @Test fun `render reduction keeps both sides of every segment boundary`() {
        val points = (0..10_000).map { point(it * 1_000L, it == 0 || it == 5_001) }
        val reduced = TrackVisuals.displayPoints(points, 100)
        assertTrue(reduced.any { it.time == 5_000_000L })
        assertTrue(reduced.any { it.time == 5_001_000L && it.segmentStart })
        assertEquals(points.first(), reduced.first())
        assertEquals(points.last(), reduced.last())
        assertTrue(reduced.size < 110)
    }
    @Test fun `thumbnail projection takes the short route across the date line`() {
        val projected = TrackProjection.project(listOf(point(1_000).copy(latitude = 0.0, longitude = 179.9),
            point(2_000).copy(latitude = 0.0, longitude = -179.9)))
        assertTrue(kotlin.math.abs(projected[1].x - projected[0].x) < .001)
        assertEquals(.5, projected.first().y, .00001)
        assertTrue(TrackProjection.project(listOf(point(1_000).copy(latitude = 90.0))).single().y.isFinite())
    }
}
