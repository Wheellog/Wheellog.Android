package com.cooper.wheellog.track

import com.cooper.wheellog.data.SpeedSample
import com.cooper.wheellog.data.TripSpeedStats
import org.junit.Assert.*
import org.junit.Test
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory

class TrackCoreTest {
    @Test fun `automatic start requires actual movement and honors all gates`() {
        fun start(speed: Double, threshold: Float = 7f, location: Boolean = true, automatic: Boolean = true,
                  connected: Boolean = true, recording: Boolean = false, suppressed: Boolean = false) =
            TrackStartPolicy.shouldStart(location, automatic, connected, speed, threshold, recording, suppressed)
        assertFalse(start(0.0, 0f))
        assertFalse(start(7.0))
        assertTrue(start(7.1))
        assertTrue(start(0.1, 0f))
        assertFalse(start(20.0, location = false))
        assertFalse(start(20.0, automatic = false))
        assertFalse(start(20.0, connected = false))
        assertFalse(start(20.0, recording = true))
        assertFalse(start(20.0, suppressed = true))
        assertFalse(start(Double.NaN))
    }

    @Test fun `lunch triggers an automatic pause and motion resumes the same session`() {
        val state = TrackPauseState()
        for (time in 0L..299_000L step 1_000) state.motion(false, time)
        assertEquals(TrackPause.NONE, state.pause)
        assertTrue(state.motion(false, 300_000))
        assertEquals(TrackPause.AUTOMATIC, state.pause)
        for (time in 301_000L..3_600_000L step 1_000) state.motion(false, time)
        assertEquals(TrackPause.AUTOMATIC, state.pause)
        assertTrue(state.motion(true, 3_601_000))
        assertEquals(TrackPause.NONE, state.pause)
    }

    @Test fun `manual pause ignores movement until an explicit resume`() {
        val state = TrackPauseState()
        state.manualPause()
        assertFalse(state.motion(true, 1_000))
        assertFalse(state.motion(true, 3_600_000))
        assertEquals(TrackPause.MANUAL, state.pause)
        state.resume()
        assertEquals(TrackPause.NONE, state.pause)
    }

    @Test fun `missing telemetry never counts as five minutes of standing`() {
        val state = TrackPauseState()
        state.motion(false, 0)
        state.motion(false, 600_000)
        assertEquals(TrackPause.NONE, state.pause)
        state.motion(true, 600_001)
        assertEquals(TrackPause.NONE, state.pause)
    }

    @Test fun `GPS filter rejects stale inaccurate duplicate and impossible fixes`() {
        val now = 1_700_000_000_000L
        val base = TrackPoint(now, 56.8, 60.6, 250.0, 4f, 8f)
        val filter = TrackPointFilter()
        assertTrue(filter.accept(base, now)!!.segmentStart)
        assertNull(filter.accept(base, now))
        assertNull(filter.accept(base.copy(time = now + 1_000, latitude = 65.0), now + 1_000))
        assertNull(filter.accept(base.copy(time = now + 1_000, accuracy = 200f), now + 1_000))
        assertNull(filter.accept(base.copy(time = now - 60_000), now))
        assertNull(filter.accept(base.copy(latitude = Double.NaN), now))
        val next = filter.accept(base.copy(time = now + 1_000, latitude = 56.8001), now + 1_000)!!
        assertFalse(next.segmentStart)
        filter.startSegment()
        assertTrue(filter.accept(base.copy(time = now + 600_000), now + 600_000)!!.segmentStart)
    }

    @Test fun `zero latitude or longitude is a valid fix`() {
        val point = TrackPoint(1_000, 0.0, 0.0, null, 5f, 0f)
        assertNotNull(TrackPointFilter().accept(point, 1_000))
    }

    @Test fun `moving average is weighted by time and excludes rests missing data and pauses`() {
        val stats = TripSpeedStats.calculate(listOf(SpeedSample(0, 20.0), SpeedSample(1_000, 20.0),
            SpeedSample(10_000, 40.0), SpeedSample(600_000, 0.0), SpeedSample(601_000, 0.0)))
        assertEquals(40.0, stats.maximum, 0.0001)
        assertEquals(29.0, stats.movingAverage, 0.0001)
        assertEquals(10.0, stats.movingSeconds, 0.0001)
        val paused = TripSpeedStats.calculate(listOf(SpeedSample(0, 20.0), SpeedSample(1_000, 20.0),
            SpeedSample(2_000, 80.0, segmentStart = true), SpeedSample(3_000, 80.0)))
        assertEquals(50.0, paused.movingAverage, 0.0001)
        assertEquals(2.0, paused.movingSeconds, 0.0001)
    }

    @Test fun `GPX exports a renamed trip as one track with segments and escaped text`() {
        val first = TrackPoint(1_700_000_000_000, 56.8, 60.6, 250.0, 5f, 10f, true,
            TrackTelemetry(42.0, 87, 145.2, 35, 55.0))
        val second = first.copy(time = first.time + 600_000, segmentStart = true)
        val writer = StringWriter()
        GpxFormat.write(writer, "Парк & река <вечером>", sequenceOf(first, second))
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(writer.toString().byteInputStream())
        assertEquals(1, document.getElementsByTagNameNS("http://www.topografix.com/GPX/1/1", "trk").length)
        assertEquals(2, document.getElementsByTagNameNS("http://www.topografix.com/GPX/1/1", "trkseg").length)
        assertEquals(2, document.getElementsByTagNameNS("http://www.topografix.com/GPX/1/1", "trkpt").length)
        assertEquals("Парк & река <вечером>", document.getElementsByTagName("name").item(0).textContent)
        assertTrue(writer.toString().contains("Z</time>"))
        assertTrue(writer.toString().contains("<wl:battery>87</wl:battery>"))
        assertEquals(first, TrackPoint.fromLine(first.toLine()))
        assertNull(TrackPoint.fromLine("partial crash line"))
    }
}
