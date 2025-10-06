package com.cooper.wheellog.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooper.wheellog.utils.FileUtil
import com.cooper.wheellog.views.TripModel

@Composable
fun TripsScreen() {
    val context = LocalContext.current
    val trips = remember { FileUtil.fillTrips(context) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(trips) { trip ->
            TripItem(trip)
        }
    }
}

@Composable
private fun TripItem(trip: TripModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(text = trip.title, fontSize = 18.sp)
        Text(text = trip.description, fontSize = 14.sp)
    }
}