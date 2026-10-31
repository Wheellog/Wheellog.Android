package com.cooper.wheellog.data

import android.content.Context
import com.cooper.wheellog.views.TripModel
import java.io.File
import java.io.InputStream

object NativeTripFiles {
    fun open(context: Context, trip: TripModel): InputStream =
        trip.pathLegacyAndroid?.takeIf { it.isNotBlank() }?.let { File(it).inputStream() }
            ?: context.contentResolver.openInputStream(trip.uri) ?: error("Cannot open ${trip.fileName}")
    @Synchronized fun points(context: Context, trip: TripModel) = NativeCsv.points(NativeCsv.read(open(context, trip)))
}
