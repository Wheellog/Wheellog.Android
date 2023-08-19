package com.cooper.wheellog.data

import android.content.Context
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.WheelLog
import com.google.common.collect.Range
import com.google.common.truth.Truth.*
import io.mockk.*
import org.junit.Test
import java.io.File
import java.io.InputStream
import kotlin.system.measureTimeMillis

class TripParserTest {

    @Test
    fun parseFile() {
        // Arrange.
        val context = mockkClass(Context::class, relaxed = true)
        val dao = mockkClass(TripDao::class, relaxed = false)
        val trip = TripDataDbEntry(fileName = "test")
        ElectroClub.instance.dao = dao
        every { dao.getTripByFileName(any()) } returns trip
        justRun { dao.update(any()) }
        val inputStream: InputStream = File("src/test/resources/log_test1.csv").inputStream()

        // Act.
        measureTimeMillis {
            TripParser.parseFile(context, "test", inputStream)
        }.also {
            println("parseFile took $it ms")
        }

        // Assert.
        assertThat(trip.duration).isEqualTo(880)
        assertThat(trip.distance).isEqualTo(19_493)
        assertThat(trip.maxSpeedGps).isIn(Range.closed(66f, 66.2f))
        assertThat(trip.maxCurrent).isIn(Range.closed(86.8f, 86.9f))
        assertThat(trip.maxPwm).isIn(Range.closed(86.7f, 86.8f))
        assertThat(trip.maxPower).isIn(Range.closed(7825f, 7826f))
        assertThat(trip.maxSpeed).isIn(Range.closed(71f, 71.05f))
        assertThat(trip.avgSpeed).isIn(Range.closed(29.7f, 29.8f))
        assertThat(trip.consumptionTotal).isIn(Range.closed(1059f, 1060f))
        assertThat(trip.consumptionByKm).isIn(Range.closed(54.3f, 54.4f))
    }
}