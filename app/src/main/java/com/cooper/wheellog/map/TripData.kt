package com.cooper.wheellog.map

import com.github.mikephil.charting.data.LineDataSet

data class TripData(
    val title: String,
    val geoLine: List<LogGeoPoint> = emptyList(),
    val stats1: List<LineDataSet> = emptyList(),
    val stats2: List<LineDataSet> = emptyList()
) {
    var errorMessage: String = ""
}