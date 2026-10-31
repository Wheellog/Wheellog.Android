package com.cooper.wheellog.track

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.data.*
import com.cooper.wheellog.views.TripModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class TripCatalogTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun model(name: String) = TripModel(name, "", Uri.parse("file:///not-readable/$name"))
    private fun summary(name: String, day: Int, meters: Int, gps: Float, minutes: Int, version: Int = TripParser.STATS_VERSION) =
        TripDataDbEntry(fileName = name, distance = meters, maxSpeedGps = gps, duration = minutes,
            additionalJson = JSONObject().put("startedAt", TripDay(2026,10,day).millis())
                .put("tripStatsVersion",version).put("hasGpsSpeed",true)
                .put("activeDurationMinutes",minutes).toString())

    @Test fun cachedMetricsArePublishedBeforeAnyOldCsvIsOpenedInALargeLibrary() = runBlocking {
        // The video contains 264 logs. An obsolete CSV must not hold all cached
        // distance/GPS/duration values hostage until the whole library is parsed.
        val cached = (1..264).map { index -> summary("ride$index.csv",1,index*100,index.toFloat(),index,0) }
        val dao = mockk<TripDao>()
        every { dao.getAll() } returns cached
        var available = emptyList<TripListItem>()
        var cancelled = false
        try {
            TripCatalog.load(context,cached.map { model(it.fileName) },dao) { first ->
                available = first
                throw CancellationException("Stop before CSV reads")
            }
        } catch (_: CancellationException) { cancelled = true }
        assertTrue(cancelled)
        assertEquals(264,available.size)
        for(sort in listOf(TripSort.DISTANCE,TripSort.GPS_SPEED,TripSort.DURATION)) {
            assertEquals("ride264.csv",TripListQuery(sort=sort).apply(available).first().fileName)
            assertEquals("ride1.csv",TripListQuery(sort=sort,descending=false).apply(available).first().fileName)
        }
    }

    @Test fun cachedCardMetricsDriveEverySortInsteadOfFallingBackToTheFilenameDate() = runBlocking {
        val cached = listOf(summary("new.csv",29,1000,20f,15),
            summary("far.csv",28,9000,50f,10),summary("fast.csv",27,3000,90f,30))
        val dao = mockk<TripDao>()
        every { dao.getAll() } returns cached
        val updates = mutableListOf<List<TripListItem>>()
        val items = TripCatalog.load(context,cached.map { model(it.fileName) },dao) { updates.add(it) }
        assertEquals(1,updates.size)
        val expected = mapOf(TripSort.DATE to "new.csv",TripSort.DISTANCE to "far.csv",
            TripSort.GPS_SPEED to "fast.csv",TripSort.DURATION to "fast.csv")
        for((sort,file) in expected) assertEquals(file,TripListQuery(sort=sort).apply(items).first().fileName)
        assertEquals(listOf("new.csv","fast.csv","far.csv"),
            TripListQuery(sort=TripSort.DISTANCE,descending=false).apply(items).map { it.fileName })
    }

    @Test fun unreadableOldCsvKeepsItsCachedMetricsAndDoesNotEraseOtherTrips() = runBlocking {
        val cached = listOf(summary("unavailable.csv",29,9000,70f,40,0),summary("ready.csv",28,2000,30f,10))
        val dao = mockk<TripDao>()
        every { dao.getAll() } returns cached
        val updates = mutableListOf<List<TripListItem>>()
        val result = TripCatalog.load(context,cached.map { model(it.fileName) },dao) { updates.add(it) }
        assertTrue(updates.size >= 2)
        assertEquals(9000.0,result.first().distance!!,0.0)
        assertEquals("unavailable.csv",TripListQuery(sort=TripSort.DISTANCE).apply(result).first().fileName)
        assertEquals(2,result.size)
    }
}
