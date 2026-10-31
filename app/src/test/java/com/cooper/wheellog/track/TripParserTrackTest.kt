package com.cooper.wheellog.track

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.data.TripDao
import com.cooper.wheellog.data.TripParser
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class TripParserTrackTest {
    @Test fun `last wheel odometer survives long GPS only and timing only file endings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = mockk<TripDao>(relaxed = true)
        every { dao.getTripByFileName(any()) } returns null
        startKoin { modules(module { single { dao } }) }
        try {
            val time = 1_700_000_000_000L
            fun sample(total: Int) = com.cooper.wheellog.data.NativeWheelSample(
                listOf("20", "140", "0", "1", "140", "0", "50", "80", "0", "$total", "30", "30", "0", "0", "", ""),
                TrackTelemetry(20.0, 80, 140.0, 30, 50.0))
            fun point(offset: Int) = TrackPoint(time + offset * 1_000, 50.0 + offset.coerceAtMost(1) * .001,
                10.0, null, 5f, 0f)
            for (gpsTail in listOf(false, true)) {
                val rows = listOf(
                    com.cooper.wheellog.data.NativeCsv.row(time, point(0), sample(10_000), 0.0, true),
                    com.cooper.wheellog.data.NativeCsv.row(time + 1_000, point(1), sample(20_000), 100.0, false)
                ) + (2..22).map { offset ->
                    com.cooper.wheellog.data.NativeCsv.row(time + offset * 1_000, point(offset).takeIf { gpsTail }, null, 100.0, false)
                }
                val csv = com.cooper.wheellog.data.NativeCsv.HEADER + "\n" + rows.joinToString("\n")
                val parsed = TripParser.parseFile(context, "tail-$gpsTail.csv", csv.byteInputStream())
                assertEquals("", TripParser.lastError)
                assertEquals(10_000, parsed.second!!.distance)
                assertTrue(parsed.first.takeLast(10).none { it.wheelDataAvailable })
            }
        } finally { stopKoin() }
    }

    @Test fun `GPS only CSV never labels GPS maximum as wheel speed or missing energy as measured zero`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = mockk<TripDao>(relaxed = true)
        val cached = com.cooper.wheellog.data.TripDataDbEntry(fileName = "gps-only.csv", maxSpeed = 99f, maxPwm = 90f)
        every { dao.getTripByFileName(any()) } returns cached
        startKoin { modules(module { single { dao } }) }
        try {
            val time = 1_700_000_000_000L
            fun row(t: Long) = com.cooper.wheellog.data.NativeCsv.row(t,
                TrackPoint(t, 50.0, 10.0, null, 5f, 20f), null, 1000.0, false)
            val csv = com.cooper.wheellog.data.NativeCsv.HEADER + "\n" + row(time) + "\n" + row(time + 1_000)
            val trip = TripParser.parseFile(context, cached.fileName, csv.byteInputStream()).second!!
            assertEquals(0f, trip.maxSpeed)
            assertEquals(72f, trip.maxSpeedGps, .001f)
            val availability = JSONObject(trip.additionalJson!!)
            assertFalse(availability.getBoolean("hasWheelSpeed"))
            assertFalse(availability.getBoolean("hasPwm"))
            assertFalse(availability.getBoolean("hasEnergy"))
            assertTrue(availability.getBoolean("hasGpsSpeed"))
        } finally { stopKoin() }
    }

    @Test fun `trip tiles get maximum moving average and real times across midnight even without GPS`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = mockk<TripDao>(relaxed = true)
        every { dao.getTripByFileName(any()) } returns null
        startKoin { modules(module { single { dao } }) }
        try {
            val header = "date,time,speed,voltage,phase_current,current,power,torque,pwm,battery_level,distance,totaldistance,system_temp,temp2,tilt,roll,mode,alert"
            fun row(date: String, time: String, speed: Int, total: Int) =
                "$date,$time,$speed,140,0,1,140,0,50,80,0,$total,30,30,0,0,,"
            val csv = listOf(header, row("2026-10-23", "23:59:58.000", 20, 10_000),
                row("2026-10-23", "23:59:59.000", 20, 10_006), row("2026-10-24", "00:00:00.000", 40, 10_017),
                row("2026-10-24", "00:05:00.000", 0, 10_017), row("2026-10-24", "00:05:01.000", 0, 10_017)).joinToString("\n")
            val parsed = TripParser.parseFile(context, "test.csv", csv.byteInputStream())
            assertEquals("", TripParser.lastError)
            assertEquals(5, parsed.first.size)
            val trip = parsed.second!!
            assertEquals(40f, trip.maxSpeed, 0.001f)
            val data = JSONObject(trip.additionalJson!!)
            assertEquals(25.0, data.getDouble("movingAverageSpeed"), 0.001)
            assertEquals(303_000L, data.getLong("endedAt") - data.getLong("startedAt"))
            assertEquals(0, trip.duration) // three recorded seconds, not the five-minute gap
            assertEquals(0, data.getInt("activeDurationMinutes"))
            assertEquals(TripParser.STATS_VERSION, data.getInt("tripStatsVersion"))
        } finally { stopKoin() }
    }
}
