package com.cooper.wheellog.data

import android.content.SharedPreferences

class TripListPreferences(private val preferences: SharedPreferences) {
    fun load(): TripListQuery {
        val days = preferences.getString("period", null)?.split(',')?.mapNotNull { it.toIntOrNull() }
        val period = runCatching { days?.takeIf { it.size == 6 }?.let {
            TripPeriod(TripDay(it[0], it[1], it[2]), TripDay(it[3], it[4], it[5]))
        } }.getOrNull()
        return TripListQuery(preferences.getString("name", "") ?: "", period,
            runCatching { TripSort.valueOf(preferences.getString("sort", "DATE")!!) }.getOrDefault(TripSort.DATE),
            preferences.getBoolean("descending", true))
    }
    fun save(query: TripListQuery) {
        val period = query.period?.let { p -> listOf(p.first.year,p.first.month,p.first.day,p.last.year,p.last.month,p.last.day).joinToString(",") }
        preferences.edit().putString("name",query.name).putString("period",period)
            .putString("sort",query.sort.name).putBoolean("descending",query.descending).apply()
    }
}
