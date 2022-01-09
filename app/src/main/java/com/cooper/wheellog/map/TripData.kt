package com.cooper.wheellog.map

import com.github.mikephil.charting.data.LineDataSet

data class TripData(
    val title: String,
    val geoLine: List<LogGeoPoint>?,
    val stats: List<LineDataSet>?,
    val errorMessage: String = "")