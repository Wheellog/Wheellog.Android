package com.cooper.wheellog.map

import com.cooper.wheellog.data.TripDataDbEntry
import com.github.mikephil.charting.data.LineDataSet

data class TripData(
    val title: String,
    val geoLine: List<LogGeoPoint> = emptyList(),
    val stats1: List<LineDataSet> = emptyList(),
    val stats2: List<LineDataSet> = emptyList(),
    val tripDb: TripDataDbEntry? = null,
    val routePoints: List<com.cooper.wheellog.track.TrackPoint> = emptyList(),
    val startedAt: Long = routePoints.firstOrNull()?.time ?: 0,
    val endedAt: Long? = routePoints.lastOrNull()?.time,
    val distanceMeters: Double = 0.0,
    val fileName: String = "",
    val values: com.cooper.wheellog.data.TripValues? = null,
) {
    var errorMessage: String = ""
}
