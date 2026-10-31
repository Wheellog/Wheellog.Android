package com.cooper.wheellog.data

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class TripSort { DATE, DISTANCE, GPS_SPEED, DURATION }
data class TripDay(val year: Int, val month: Int, val day: Int) {
    fun millis(zone: TimeZone = TimeZone.getDefault()): Long = Calendar.getInstance(zone).apply {
        clear(); isLenient = false; set(year, month - 1, day)
    }.timeInMillis
    fun next(zone: TimeZone = TimeZone.getDefault()): Long = Calendar.getInstance(zone).apply {
        timeInMillis = millis(zone); add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
}
data class TripPeriod(val first: TripDay, val last: TripDay) {
    init { require(first.millis() <= last.millis()) }
    fun contains(time: Long?, zone: TimeZone = TimeZone.getDefault()): Boolean =
        time != null && time >= first.millis(zone) && time < last.next(zone)
    companion object {
        fun month(year: Int, month: Int): TripPeriod {
            val c = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
            return TripPeriod(TripDay(year, month, 1), TripDay(year, month, c.getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        fun year(year: Int) = TripPeriod(TripDay(year, 1, 1), TripDay(year, 12, 31))
    }
}
data class TripListItem(val id: String, val name: String, val fileName: String,
    val startedAt: Long?, val distance: Double?, val gpsMaximum: Double?, val durationMinutes: Double?)
data class TripListQuery(val name: String = "", val period: TripPeriod? = null,
    val sort: TripSort = TripSort.DATE, val descending: Boolean = true) {
    fun apply(items: List<TripListItem>): List<TripListItem> {
        val needle = name.trim().lowercase(Locale.ROOT)
        val filtered = items.filter {
            (needle.isEmpty() || it.name.lowercase(Locale.ROOT).contains(needle) ||
                it.fileName.lowercase(Locale.ROOT).contains(needle)) && (period?.contains(it.startedAt) != false)
        }
        fun value(item: TripListItem): Double? = when (sort) {
            TripSort.DATE -> item.startedAt?.toDouble()
            TripSort.DISTANCE -> item.distance
            TripSort.GPS_SPEED -> item.gpsMaximum
            TripSort.DURATION -> item.durationMinutes
        }?.takeIf { it.isFinite() }
        return filtered.sortedWith { left, right ->
            val a = value(left); val b = value(right)
            when {
                a == null && b != null -> 1
                a != null && b == null -> -1
                a != null && b != null && a != b -> if (descending) b.compareTo(a) else a.compareTo(b)
                else -> compareValues(right.startedAt, left.startedAt).takeIf { it != 0 } ?: left.id.compareTo(right.id)
            }
        }
    }
}
