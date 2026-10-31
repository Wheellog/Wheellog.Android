package com.cooper.wheellog.track

import com.cooper.wheellog.data.NativeCsv
import com.cooper.wheellog.data.NativeWheelSample
import com.cooper.wheellog.data.TripValues
import org.junit.Assert.*
import org.junit.Test

class TripValuesTest {
    private val start = 1_700_000_000_000L
    private fun wheel(speed: Double = 100.0, voltage: String = "149", phase: String = "25", motor: String = "60") =
        NativeWheelSample(listOf("$speed", voltage, phase, "15", "2000", "0", "58.92", "97", "100", "10000", "33", motor, "0", "0", "", ""),
            TrackTelemetry(speed, 97, 149.0, 33, 58.92))
    private fun row(offset: Long, speedMps: Float, sample: NativeWheelSample? = wheel(), segment: Boolean = false, elapsed: Long? = null) =
        NativeCsv.row(start + offset, TrackPoint(start + offset, 56.8 + offset / 1e9, 60.6, 200.0, 5f, speedMps, segment), sample, 0.0, segment, elapsed)
    private fun read(vararg rows: String) = NativeCsv.read((NativeCsv.HEADER + "\n" + rows.joinToString("\n")).byteInputStream())

    @Test fun `GPS averages are time weighted exclude standing only for moving average and never use wheel speed`() {
        val values = TripValues.calculate(read(row(0, 0f), row(1_000, 0f), row(2_000, 10f), row(3_000, 10f)))
        assertEquals(36.0, values.gpsMaximum!!, .001)
        assertEquals(18.0, values.gpsAverage!!, .001)
        assertEquals(27.0, values.gpsMovingAverage!!, .001)
        assertEquals(2.0, values.movingSeconds!!, .001)
        assertEquals(100.0, values.wheelMaximum!!, .001)
    }

    @Test fun `older files count recorded intervals without adding the gap between sessions`() {
        val csv = read(row(0, 10f), row(1_000, 10f), row(1_000, 10f), row(600_000, 20f, segment = true), row(601_000, 20f))
        val values = TripValues.calculate(csv)
        assertEquals(54.0, values.gpsAverage!!, .001)
        assertEquals(54.0, values.gpsMovingAverage!!, .001)
        assertEquals(2.0, values.movingSeconds!!, .001)
    }

    @Test fun `daily GPS and corrected wheel maxima cover all sessions while real recorded stops still count`() {
        val corrected = wheel(69.372).copy(rawSpeedKmh = 73.8, correctionPercent = -6.0)
        val ticks = read(row(0, 21.5f, null), row(1_000, 21.5f, null),
            row(4 * 3_600_000, 19f, corrected, segment = true),
            row(4 * 3_600_000 + 1_000, 19f, corrected),
            row(4 * 3_600_000 + 2_000, 0f, wheel(0.0)),
            row(4 * 3_600_000 + 3_000, 0f, wheel(0.0)))
        val values = TripValues.calculate(ticks)
        assertEquals(77.4, values.gpsMaximum!!, .001)
        assertEquals(69.372, values.wheelMaximum!!, .001)
        assertEquals(44.999999, values.gpsAverage!!, .001)
        assertEquals(60.0, values.gpsMovingAverage!!, .001)
        assertEquals(4.0, com.cooper.wheellog.data.TripRecordingTime.seconds(ticks), .001)
    }

    @Test fun `active recording clock excludes pauses but includes short stops without stationary coordinates`() {
        val values = TripValues.calculate(read(row(0, 10f, elapsed = 0), row(1_000, 10f, elapsed = 1_000),
            row(600_000, 20f, segment = true, elapsed = 11_000), row(601_000, 20f, elapsed = 12_000)))
        assertEquals(9.0, values.gpsAverage!!, .001)
        assertEquals(54.0, values.gpsMovingAverage!!, .001)
        assertEquals(2.0, values.movingSeconds!!, .001)
    }

    @Test fun `timing only final row counts a final stop without creating GPS points or sensor values`() {
        val ending = NativeCsv.row(start + 11_000, null, null, 0.0, false, 11_000)
        val ticks = read(row(0, 10f, sample = null, elapsed = 0), row(1_000, 10f, sample = null, elapsed = 1_000), ending)
        assertEquals(2, NativeCsv.points(ticks).size)
        assertFalse(ticks.last().wheelDataAvailable)
        assertFalse(ticks.last().gpsSpeedAvailable)
        val values = TripValues.calculate(ticks)
        assertEquals(36.0 / 11, values.gpsAverage!!, .001)
        assertEquals(36.0, values.gpsMovingAverage!!, .001)
        assertNull(values.maxPower)
    }

    @Test fun `continued older CSV keeps its column layout without changing recorded values`() {
        val headers = NativeCsv.HEADER.split(',').dropLast(1)
        val aligned = NativeCsv.alignRow(row(0, 10f, elapsed = 500), headers)
        assertEquals(headers.size, aligned.split(',').size)
        val ticks = NativeCsv.read((headers.joinToString(",") + "\n" + aligned).byteInputStream())
        assertNull(ticks.single().recordingElapsedMillis)
        assertEquals(36.0, ticks.single().speedGps, .001)
        assertEquals(58.92, ticks.single().pwm, .001)
    }

    @Test fun `phase motor and voltage summaries come from recorded telemetry`() {
        val values = TripValues.calculate(read(row(0, 10f, wheel()), row(1_000, 10f, wheel(voltage = "144", phase = "30", motor = "65"))))
        assertEquals(30.0, values.maxPhaseCurrent!!, .001)
        assertEquals(65.0, values.maxMotorTemperature!!, .001)
        assertEquals(144.0, values.minVoltage!!, .001)
        assertEquals(58.92, values.maxPwm!!, .001)
    }

    @Test fun `missing optional columns stay unavailable rather than fabricated zeros`() {
        val ticks = NativeCsv.read("date,time,speed\n2026-10-24,12:00:00.000,20\n2026-10-24,12:00:01.000,30".byteInputStream())
        val values = TripValues.calculate(ticks)
        assertNull(values.gpsMaximum); assertNull(values.gpsAverage); assertNull(values.gpsMovingAverage)
        assertNull(values.maxPhaseCurrent); assertNull(values.maxMotorTemperature)
        assertNull(values.maxPower); assertNull(values.maxCurrent); assertNull(values.maxPwm)
        assertNull(values.minVoltage); assertNull(values.maxTemperature)
        assertEquals(30.0, values.wheelMaximum!!, .001)
    }

    @Test fun `GPS only rows never create wheel maxima and real zero values remain valid`() {
        val onlyGps = TripValues.calculate(read(row(0, 0f, null), row(1_000, 0f, null)))
        assertNull(onlyGps.wheelMaximum); assertNull(onlyGps.maxPwm)
        assertEquals(0.0, onlyGps.gpsAverage!!, .001)
        val zero = wheel().copy(columns = wheel().columns.toMutableList().apply { this[6] = "0"; this[10] = "0" })
        val values = TripValues.calculate(read(row(0, 0f, zero)))
        assertEquals(0.0, values.maxPwm!!, .001)
        assertEquals(0.0, values.maxTemperature!!, .001)
    }

    @Test fun `corrected CSV stores raw wheel speed and correction without altering GPS`() {
        val sample = wheel(90.0).copy(rawSpeedKmh = 100.0, correctionPercent = -10.0)
        val line = row(0, 25f, sample)
        val cells = NativeCsv.HEADER.split(',').zip(line.split(',')).toMap()
        assertEquals("100.0", cells["wheel_speed_raw"])
        assertEquals("-10", cells["speed_correction_percent"])
        assertEquals(90.0, cells.getValue("speed").toDouble(), .001)
        assertEquals(90.0, cells.getValue("gps_speed").toDouble(), .001)
        assertEquals(NativeCsv.HEADER.split(',').size, line.split(',').size)
    }
    @Test fun `fractional correction round trips in CSV while GPS and old metadata stay compatible`() {
        val sample = wheel(93.5).copy(rawSpeedKmh = 100.0, correctionPercent = -6.5)
        val line = row(0, 25f, sample)
        val cells = NativeCsv.HEADER.split(',').zip(line.split(',')).toMap()
        assertEquals("-6.5", cells["speed_correction_percent"])
        val values = TripValues.calculate(read(line))
        assertEquals(93.5, values.wheelMaximum!!, .00001)
        assertEquals(90.0, values.gpsMaximum!!, .00001)
    }

}
