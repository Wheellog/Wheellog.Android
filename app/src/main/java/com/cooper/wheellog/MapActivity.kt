package com.cooper.wheellog

import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.cooper.wheellog.utils.MathsUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import timber.log.Timber
import java.io.*

class MapActivity : AppCompatActivity() {
    lateinit var map: MapView
    var isDestroed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.mapView)
        map.apply {
            title = "map test"
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            minZoomLevel = 11.0
            maxZoomLevel = 20.0
            setMultiTouchControls(true)
        }

        if (intent.extras == null) {
            this.finish()
            return
        }
        val extras = intent.extras!!
        val context = applicationContext

        // load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))

        // async
        GlobalScope.launch {
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
                this@MapActivity.finish()
                return@launch
            }
            if (inputStream == null) {
                Timber.wtf("Failed to create inputStream for %s", extras.get("title"))
                this@MapActivity.finish()
                return@launch
            }

            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine().split(",").toTypedArray()
            val latIndex = header.indexOf("latitude")
            val longIndex = header.indexOf("longitude")
            if (latIndex == -1 || longIndex == -1) {
                inputStream.close()
                Timber.wtf("%s file does not contain geolocation data.", extras.get("title"))
                this@MapActivity.finish()
                return@launch
            }
            val geoPoints = ArrayList<GeoPoint>()
            var i = 0
            try {
                reader.forEachLine { line ->
                    val row = line.split(",").toTypedArray()
                    val latitude = row[latIndex].toDouble()
                    val longitude = row[longIndex].toDouble()
                    geoPoints.add(i++, GeoPoint(latitude, longitude))
                }
            } catch (ex: Exception) {
                Timber.wtf(ex.localizedMessage)
                inputStream.close()
                this@MapActivity.finish()
                return@launch
            }
            inputStream.close()

            val line = Polyline().apply {
                setPoints(geoPoints)
                outlinePaint.color = Color.BLACK // TODO: change color
            }

            // in UI thread
            if (!isDestroed) {
                MainScope().launch {
                    map.apply {
                        overlays.add(line)
                        map.zoomToBoundingBox(line.bounds, true, MathsUtil.dpToPx(context, 24))
                    }
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
        isDestroed = true
    }
}