package com.cooper.wheellog.map

import com.cooper.wheellog.data.TripDataDbEntry
import com.github.mikephil.charting.data.LineDataSet

data class TripData(
    val title: String,
    val geoLine: List<LogGeoPoint> = emptyList(),
    val stats1: List<LineDataSet> = emptyList(),
    val stats2: List<LineDataSet> = emptyList(),
    val tripDb: TripDataDbEntry? = null
) {
    var errorMessage: String = ""
}