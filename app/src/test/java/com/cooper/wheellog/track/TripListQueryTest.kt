package com.cooper.wheellog.track

import com.cooper.wheellog.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class TripListQueryTest {
    private val day = TripDay(2026,10,29)
    private fun item(id: String, name: String = id, time: Long? = day.millis(),
        distance: Double? = 0.0, gps: Double? = 0.0, minutes: Double? = 0.0) =
        TripListItem(id,name,id+".csv",time,distance,gps,minutes)

    @Test fun substringSearchUsesRenamedTitlesAndIsCaseInsensitive() {
        val list = listOf(item("one","Поездка толпой в Чебаркуль в пятницу"),
            item("two","ЧЕБАРКУЛЬ — возвращение"), item("three","Поездка в парк"))
        assertEquals(listOf("one","two"),TripListQuery(name="чебар").apply(list).map { it.id })
        assertEquals(3,TripListQuery().apply(list).size)
        assertEquals(listOf("two"),TripListQuery(name="two.csv").apply(list).map { it.id })
    }
    @Test fun dayAndRangeIncludeTheEntireLastDayAndExcludeTheNextDay() {
        val period = TripPeriod(day,TripDay(2026,10,31))
        assertTrue(period.contains(day.millis()))
        assertTrue(period.contains(period.last.next()-1))
        assertFalse(period.contains(day.millis()-1))
        assertFalse(period.contains(period.last.next()))
        assertFalse(period.contains(null))
        assertEquals(1,TripListQuery(name="чебар",period=period).apply(listOf(
            item("a","Чебаркуль"),item("b","Чебаркуль",TripDay(2026,11,1).millis()),item("c","Парк"))).size)
    }
    @Test fun monthHandlesLeapYearsAndYearKeepsItsOwnBoundaries() {
        val february = TripPeriod.month(2024,2)
        assertEquals(TripDay(2024,2,29),february.last)
        assertTrue(february.contains(february.last.next()-1))
        assertFalse(february.contains(TripDay(2024,3,1).millis()))
        val year = TripPeriod.year(2026)
        assertTrue(year.contains(TripDay(2026,12,31).next()-1))
        assertFalse(year.contains(TripDay(2027,1,1).millis()))
    }
    @Test fun dateRangesFollowLocalMidnightAcrossDaylightSavingChanges() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val date = TripDay(2026,11,1)
        assertEquals(25*60*60*1000L,date.next(zone)-date.millis(zone))
        assertTrue(TripPeriod(date,date).contains(date.next(zone)-1,zone))
        assertFalse(TripPeriod(date,date).contains(date.next(zone),zone))
    }
    @Test fun everySortHasBothDirectionsAndMissingValuesStayLast() {
        val small = item("small",time=day.millis(),distance=5.0,gps=40.0,minutes=10.0)
        val large = item("large",time=day.next(),distance=20.0,gps=80.0,minutes=60.0)
        val missing = item("missing",time=null,distance=null,gps=null,minutes=null)
        for (type in TripSort.entries) {
            assertEquals(listOf("large","small","missing"),TripListQuery(sort=type).apply(listOf(missing,small,large)).map { it.id })
            assertEquals(listOf("small","large","missing"),TripListQuery(sort=type,descending=false).apply(listOf(missing,large,small)).map { it.id })
        }
    }
    @Test fun zeroGpsIsARealValueAndDoesNotBecomeMissing() {
        assertEquals(listOf("zero","missing"),TripListQuery(sort=TripSort.GPS_SPEED,descending=false)
            .apply(listOf(item("missing",gps=null),item("zero",gps=0.0))).map { it.id })
    }
}
