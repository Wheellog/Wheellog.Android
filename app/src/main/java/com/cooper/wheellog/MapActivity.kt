package com.cooper.wheellog

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.cooper.wheellog.map.LogGeoPoint
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import timber.log.Timber
import java.io.*


class MapActivity : AppCompatActivity() {
    private val latitudeHeader = "latitude"
    private val longitudeHeader = "longitude"
    private val altHeader = "gps_alt"
    private val gpsSpeedHeader = "gps_speed"
    private val distanceHeader = "distance"
    private val batteryHeader = "battery_level"
    private val voltageHeader = "voltage"
    private val tempHeader = "system_temp"
    private lateinit var map: MapView

    private fun parseFileToPolyLine(extras: Bundle): Polyline? {
        val inputStream: InputStream?
        try {
            inputStream = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 9 or less
                FileInputStream(File(extras.get("path") as String))
            } else {
                // Android 10+
                applicationContext.contentResolver.openInputStream(extras.get("uri") as Uri)
            }
        } catch (ex: Exception) {
            Timber.wtf(ex.localizedMessage)
            return null
        }
        if (inputStream == null) {
            Timber.wtf("Failed to create inputStream for %s", extras.get("title"))
            return null
        }

        val reader = BufferedReader(InputStreamReader(inputStream))
        val header = reader.readLine().split(",").toTypedArray()
        val latIndex = header.indexOf(latitudeHeader)
        val longIndex = header.indexOf(longitudeHeader)
        val altIndex = header.indexOf(altHeader)
        if (latIndex == -1 || longIndex == -1) {
            inputStream.close()
            Timber.wtf("%s file does not contain geolocation data.", extras.get("title"))
            return null
        }

        // for statistics
        var latitude = 0.0
        var longitude = 0.0
        val gpsSpeedIndex = header.indexOf(gpsSpeedHeader)
        val distanceIndex = header.indexOf(distanceHeader)
        val batteryIndex = header.indexOf(batteryHeader)
        val voltageIndex = header.indexOf(voltageHeader)
        val tempIndex = header.indexOf(tempHeader)
        var maxSpeed = 0.0
        var distance = 0
        var startBattery = 0
        var endBattery = 0

        val polyLine = Polyline(map, true).apply {
            outlinePaint.apply {
                color = applicationContext.getColorEx(R.color.accent)
                isAntiAlias = true
                strokeWidth = 15f
            }
            title = extras.get("title") as String
            setOnClickListener { polyline, mapView, eventPos ->
                val pointOnLine = polyline.getCloseTo(eventPos, MathsUtil.dpToPx(applicationContext, 24).toDouble(), mapView)
                val logGeoPoint = polyline.actualPoints.firstOrNull { p -> p.distanceToAsDouble(pointOnLine) < 1.0 } as LogGeoPoint?
                if (logGeoPoint != null) {
                    polyline.apply {
                        title = logGeoPoint.toString()
                        infoWindowLocation = logGeoPoint
                        showInfoWindow()
                    }
                }
                true
            }
        }
        try {
            var i = 0
            reader.forEachLine { line ->
                val row = line.split(",").toTypedArray()
                val latitudeNew = row[latIndex].toDoubleOrNull() ?: 0.0
                val longitudeNew = row[longIndex].toDoubleOrNull() ?: 0.0
                if (latitudeNew != latitude && longitudeNew != longitude) {
                    latitude = latitudeNew
                    longitude = longitudeNew
                    val altitude = row[altIndex].toDoubleOrNull() ?: 0.0

                    // stats
                    val speed = row[gpsSpeedIndex].toDoubleOrNull() ?: 0.0
                    maxSpeed = maxSpeed.coerceAtLeast(speed)
                    distance = row[distanceIndex].toIntOrNull() ?: 0
                    val batteryLevel = row[batteryIndex].toIntOrNull() ?: 0
                    if (i == 1) {
                        startBattery = batteryLevel
                    }
                    endBattery = batteryLevel
                    val voltage = row[voltageIndex].toDoubleOrNull() ?: 0.0
                    val temperature = row[tempIndex].toIntOrNull() ?: 0

                    val geoPoint = LogGeoPoint(latitude, longitude, altitude).also {
                        it.speed = speed
                        it.voltage = voltage
                        it.battery = endBattery
                        it.distance = distance
                        it.temperature = temperature
                    }
                    polyLine.addPoint(geoPoint)
                    i++
                }
            }
        } catch (ex: Exception) {
            Timber.wtf(ex.localizedMessage)
            return null
        } finally {
            inputStream.close()
        }

//        val legend =
//            try {
//                String.format(
//                    "%s\nDistance: %.2f km\nMax GPS speed: %.2f km\\h\nBattery: %d%% > %d%%",
//                    extras.get("title"),
//                    distance / 1000.0,
//                    maxSpeed,
//                    startBattery,
//                    endBattery
//                )
//            } catch (ex: Exception) {
//                ""
//            }

        return polyLine
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = applicationContext

        // load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))

        setContentView(R.layout.activity_map)

        map = findViewById(R.id.mapView)
        map.apply {
            title = "map test"
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(10.0)
            minZoomLevel = 10.0
            maxZoomLevel = 20.0
            setMultiTouchControls(true)
            overlays.add(RotationGestureOverlay(map)) // enable rotation
            overlays.add(ScaleBarOverlay(map)) // scale bar in top-left corner
        }

        if (intent.extras == null) {
            this.finish()
            return
        }
        val extras = intent.extras!!

        // async
        GlobalScope.launch {
            val line = parseFileToPolyLine(extras)
            if (isDestroyed) {
                return@launch
            }
            if (line == null) {
                this@MapActivity.finish()
                return@launch
            }

            // UI thread - show map and line
            MainScope().launch {
                map.apply {
                    isVisible = true
                    overlays.add(line)
                    zoomToBoundingBox(line.bounds, true, MathsUtil.dpToPx(context, 24))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDetach()
    }
}