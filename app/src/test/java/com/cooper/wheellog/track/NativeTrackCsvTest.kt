package com.cooper.wheellog.track

import com.cooper.wheellog.data.NativeCsv
import com.cooper.wheellog.data.NativeWheelSample
import org.junit.Assert.*
import org.junit.Test

class NativeTrackCsvTest {
    private val start = 1_700_000_000_000L
    private val point = TrackPoint(start, 56.8, 60.6, 200.0, 5f, 10f, true)
    private fun read(vararg rows: String) = NativeCsv.read((NativeCsv.HEADER + "\n" + rows.joinToString("\n") + "\n").byteInputStream())
    @Test fun `GPS-only rows retain real speed and location without pretending wheel PWM is zero`() {
        val ticks = read(NativeCsv.row(start, point, null, 0.0, true))
        assertFalse(ticks.single().wheelDataAvailable)
        assertEquals(36.0, ticks.single().speed, .001)
        val route = NativeCsv.points(ticks)
        assertEquals(start, route.single().time)
        assertNull(route.single().telemetry)
    }
    @Test fun `repeated wheel rows sharing a GPS fix do not duplicate route points or split the segment`() {
        val ticks = read(NativeCsv.row(start, point, null, 0.0, true), NativeCsv.row(start + 100, point, null, 0.0, false),
            NativeCsv.row(start + 1_000, point.copy(time = start + 1_000, latitude = 56.801), null, 100.0, false))
        val route = NativeCsv.points(ticks)
        assertEquals(2, route.size)
        assertTrue(route.first().segmentStart); assertFalse(route.last().segmentStart)
    }
    @Test fun `partial final row is discarded without losing complete earlier data`() {
        val csv = NativeCsv.HEADER + "\n" + NativeCsv.row(start, point, null, 0.0, true) + "\n2026-10-23,07:45"
        assertEquals(1, NativeCsv.read(csv.byteInputStream()).size)
    }
    @Test fun `native column order stays compatible and chart timestamps retain subsecond precision`() {
        assertTrue(NativeCsv.HEADER.startsWith("date,time,latitude,longitude,gps_speed,gps_alt,gps_heading,gps_distance,speed,voltage,"))
        val ticks = read(NativeCsv.row(start, point, null, 0.0, true), NativeCsv.row(start + 250, point, null, 0.0, false))
        assertEquals(2.5f, ticks.last().timePlusDayOfWeek - ticks.first().timePlusDayOfWeek, .001f)
    }
    @Test fun `historical CSV summary compatibility does not affect absolute map timestamps`() {
        val native = NativeCsv.row(start, point, null, 0.0, true)
        val oldHeader = NativeCsv.HEADER.substringBefore(",gps_time")
        val oldRow = native.split(',').take(oldHeader.split(',').size).joinToString(",")
        val historical = NativeCsv.read((oldHeader + "\n" + oldRow).byteInputStream()).single()
        val extended = read(native).single()
        assertTrue(historical.legacyStatistics)
        assertFalse(extended.legacyStatistics)
        assertEquals(extended.timestampMillis, historical.timestampMillis)
        assertEquals(extended.latitude, historical.latitude, 0.0)
        assertEquals(extended.longitude, historical.longitude, 0.0)
    }

    private fun sample(pwm: String = "58.92") = NativeWheelSample(listOf("38", "130.9", "20", "15.79", "2066.91", "0", pwm, "37", "100", "10000", "33", "0", "0", "0", "", ""),
        TrackTelemetry(38.0, 37, 130.9, 33, pwm.toDoubleOrNull() ?: 0.0, pwm.isNotEmpty()))
    @Test fun `later wheel callback enriches a GPS-first point without duplicating it`() {
        val ticks = read(NativeCsv.row(start, point, null, 0.0, true),
            NativeCsv.row(start + 200, point, sample(), 0.0, false))
        val route = NativeCsv.points(ticks)
        assertEquals(1, route.size)
        assertEquals(58.92, route.single().telemetry!!.pwm, .0001)
        assertTrue(route.single().telemetry!!.pwmAvailable)
        assertTrue(route.single().segmentStart)
    }
    @Test fun `GPS callbacks cannot erase a preceding wheel sample and zero PWM is valid`() {
        val route = NativeCsv.points(read(NativeCsv.row(start, point, sample("0"), 0.0, true),
            NativeCsv.row(start + 200, point, null, 0.0, false)))
        assertTrue(route.single().telemetry!!.pwmAvailable)
        assertEquals(0.0, route.single().telemetry!!.pwm, 0.0)
    }
    @Test fun `missing PWM remains missing in a CSV route and GPX export`() {
        val ticks = read(NativeCsv.row(start, point, sample(""), 0.0, true))
        assertFalse(ticks.single().pwmAvailable)
        val route = NativeCsv.points(ticks)
        assertFalse(route.single().telemetry!!.pwmAvailable)
        assertFalse(TrackPoint.fromLine(route.single().toLine())!!.telemetry!!.pwmAvailable)
        val output = java.io.StringWriter()
        GpxFormat.write(output, "test", route.asSequence())
        assertFalse(output.toString().contains("<wl:pwm>"))
    }
    @Test fun `old CSV wheel-only samples align only inside a short live interval`() {
        val ticks = read(NativeCsv.row(start - 500, null, sample(), 0.0, true),
            NativeCsv.row(start, point, null, 0.0, false),
            NativeCsv.row(start + 500, null, sample(), 0.0, false),
            NativeCsv.row(start + 20_000, point.copy(time = start + 20_000), null, 0.0, true))
        val route = NativeCsv.points(ticks)
        assertEquals(58.92, route.first().telemetry!!.pwm, .001)
        assertNull(route.last().telemetry)
    }
}
