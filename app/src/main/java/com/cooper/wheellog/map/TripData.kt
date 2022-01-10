package com.cooper.wheellog.map

import com.github.mikephil.charting.data.LineDataSet

data class TripData(val title: String)
{
    var geoLine: List<LogGeoPoint>? = null
    var stats: List<LineDataSet>? = null
    var errorMessage: String = ""
}