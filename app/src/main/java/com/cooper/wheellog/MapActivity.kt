package com.cooper.wheellog

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.cooper.wheellog.map.LogGeoPoint
import com.cooper.wheellog.utils.LogHeaderEnum
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import timber.log.Timber
import java.io.*
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.HashMap

class MapActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private val header = HashMap<LogHeaderEnum, Int>()

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
            // TODO: localize me
            Timber.wtf("Failed to create inputStream for %s", extras.get("title"))
            return null
        }

        val reader = BufferedReader(InputStreamReader(inputStream))
        val headerLine = reader.readLine().split(",").toTypedArray()
        for (i in headerLine.indices) {
            try {
                header[LogHeaderEnum.valueOf(headerLine[i].toUpperCase(Locale.US))] = i
            } catch (ignored: IllegalArgumentException) {
            }
        }
        if (!header.containsKey(LogHeaderEnum.LATITUDE) || !header.containsKey(LogHeaderEnum.LONGITUDE)) {
            inputStream.close()
            // TODO: localize me
            Timber.wtf("%s file does not contain geolocation data.", extras.get("title"))
            return null
        }

        // for statistics
        var latitude = 0.0
        var longitude = 0.0
        var maxSpeed = 0.0
        var distance: Int
        var startBattery: Int
        var endBattery: Int

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
                val row = line.split(",")
                val latitudeNew = row[header[LogHeaderEnum.LATITUDE]!!].toDoubleOrNull() ?: 0.0
                val longitudeNew = row[header[LogHeaderEnum.LONGITUDE]!!].toDoubleOrNull() ?: 0.0
                if (latitudeNew != latitude && longitudeNew != longitude) {
                    latitude = latitudeNew
                    longitude = longitudeNew
                    val altitude = row[header[LogHeaderEnum.GPS_ALT]!!].toDoubleOrNull() ?: 0.0

                    // stats
                    val speed = row[header[LogHeaderEnum.GPS_SPEED]!!].toDoubleOrNull() ?: 0.0
                    maxSpeed = maxSpeed.coerceAtLeast(speed)
                    distance = row[header[LogHeaderEnum.DISTANCE]!!].toIntOrNull() ?: 0
                    val batteryLevel = row[header[LogHeaderEnum.BATTERY_LEVEL]!!].toIntOrNull() ?: 0
                    if (i == 1) {
                        startBattery = batteryLevel
                    }
                    endBattery = batteryLevel
                    val voltage = row[header[LogHeaderEnum.VOLTAGE]!!].toDoubleOrNull() ?: 0.0
                    val temperature = row[header[LogHeaderEnum.SYSTEM_TEMP]!!].toIntOrNull() ?: 0

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
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            minZoomLevel = 10.0
            maxZoomLevel = 20.0
            setMultiTouchControls(true)
            overlays.add(RotationGestureOverlay(map)) // enable rotation
            overlays.add(ScaleBarOverlay(map)) // scale bar in top-left corner
            overlays.add(CompassOverlay(context, map).apply { enableCompass() })
            controller.apply {
                setZoom(10.0)
                setCenter(GeoPoint(WheelLog.AppConfig.lastLocationLaltitude, WheelLog.AppConfig.lastLocationLongitude))
            }
        }

        // set dark map tiles, if necessary
        if (WheelLog.AppConfig.dayNightThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
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