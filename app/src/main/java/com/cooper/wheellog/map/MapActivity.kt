package com.cooper.wheellog.map

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.LogHeaderEnum
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MapActivity : AppCompatActivity() {
    private lateinit var adapter: MapActivityAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var tabs: TabLayout
    private val tabNames: Array<String> = arrayOf(
        "Map",
        "Stats",
    )
    private val viewModel: MapViewModel by viewModels()

    private val header = HashMap<LogHeaderEnum, Int>()
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = applicationContext

        // load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))

        setContentView(R.layout.activity_map)

        viewPager = findViewById(R.id.pager)
        viewPager.offscreenPageLimit = 10
        viewPager.isUserInputEnabled = false
        adapter = MapActivityAdapter(this)
        viewPager.adapter = adapter
        tabs = findViewById(R.id.tabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()

        if (intent.extras == null) {
            this.finish()
            return
        }
        val extras = intent.extras!!

        // async
        backgroundScope.launch {
            val tripData = parseFile(extras)
            if (isDestroyed) {
                return@launch
            }

            MainScope().launch {
                viewModel.selectItem(tripData)
            }
        }
    }

    private fun parseFile(extras: Bundle): TripData {
        val title = extras.get("title") as String
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
            return TripData(title, null, null, "Could not open the file " + ex.localizedMessage)
        }
        if (inputStream == null) {
            // TODO: localize me
            Timber.wtf("Failed to create inputStream for %s", extras.get("title"))
            return TripData(title, null, null, "Failed to create inputStream.")
        }

        val reader = BufferedReader(InputStreamReader(inputStream))
        val headerLine = reader.readLine().split(",").toTypedArray()
        for (i in headerLine.indices) {
            try {
                header[LogHeaderEnum.valueOf(headerLine[i].uppercase())] = i
            } catch (ignored: IllegalArgumentException) {
            }
        }
        if (!header.containsKey(LogHeaderEnum.LATITUDE) || !header.containsKey(LogHeaderEnum.LONGITUDE)) {
            inputStream.close()
            // TODO: localize me
            Timber.wtf("%s file does not contain geolocation data.", extras.get("title"))
            return TripData(title, null, null, "File does not contain GPS data.")
        }

        // for statistics
        var latitude = 0.0
        var longitude = 0.0
        var maxSpeed = 0.0
        var distance: Int
        var startBattery: Int
        var endBattery: Int
        val sdf_time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

        val geoLine = ArrayList<LogGeoPoint>()
        val lineDataSets = ArrayList<LineDataSet>()
        val entriesVoltage = ArrayList<Entry>()
        val entriesCurrent = ArrayList<Entry>()
        val entriesPower = ArrayList<Entry>()
        val entriesSpeed = ArrayList<Entry>()

        try {
            var i = 0
            reader.forEachLine { line ->
                val row = line.split(",")
                val latitudeNew = row[header[LogHeaderEnum.LATITUDE]!!].toDoubleOrNull() ?: 0.0
                val longitudeNew = row[header[LogHeaderEnum.LONGITUDE]!!].toDoubleOrNull() ?: 0.0
                // stats
                val batteryLevel = row[header[LogHeaderEnum.BATTERY_LEVEL]!!].toIntOrNull() ?: 0
                if (i == 1) {
                    startBattery = batteryLevel
                }
                endBattery = batteryLevel
                val voltage = row[header[LogHeaderEnum.VOLTAGE]!!].toDoubleOrNull() ?: 0.0
                val current = row[header[LogHeaderEnum.CURRENT]!!].toDoubleOrNull() ?: 0.0
                val power = row[header[LogHeaderEnum.POWER]!!].toDoubleOrNull() ?: 0.0
                var speed = row[header[LogHeaderEnum.SPEED]!!].toDoubleOrNull() ?: 0.0
                val temperature = row[header[LogHeaderEnum.SYSTEM_TEMP]!!].toIntOrNull() ?: 0
                val time = sdf_time.parse(row[header[LogHeaderEnum.TIME]!!])!!.time / 100f
                entriesVoltage.add(Entry(time, voltage.toFloat()))
                entriesCurrent.add(Entry(time, current.toFloat()))
                entriesPower.add(Entry(time, power.toFloat()))
                entriesSpeed.add(Entry(time, speed.toFloat()))
                // map
                if (latitudeNew != latitude && longitudeNew != longitude) {
                    latitude = latitudeNew
                    longitude = longitudeNew
                    val altitude = row[header[LogHeaderEnum.GPS_ALT]!!].toDoubleOrNull() ?: 0.0

                    // stats
                    speed = row[header[LogHeaderEnum.GPS_SPEED]!!].toDoubleOrNull() ?: 0.0
                    maxSpeed = maxSpeed.coerceAtLeast(speed)
                    distance = row[header[LogHeaderEnum.DISTANCE]!!].toIntOrNull() ?: 0

                    geoLine.add(LogGeoPoint(latitude, longitude, altitude).also {
                        it.speed = speed
                        it.voltage = voltage
                        it.battery = endBattery
                        it.distance = distance
                        it.temperature = temperature
                    })
                    i++
                }
            }
        } catch (ex: Exception) {
            Timber.wtf(ex.localizedMessage)
            return TripData(title, null, null, "Unexpected exception when parsing file: " + ex.localizedMessage)
        } finally {
            inputStream.close()
        }

        lineDataSets.apply {
            add(LineDataSet(entriesVoltage, "Voltage (V)").apply {
                color = Color.GREEN
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            })
            add(LineDataSet(entriesCurrent, "Current (A)").apply {
                color = Color.YELLOW
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                lineWidth = 2f
            })
            add(LineDataSet(entriesPower, "Power (W)").apply {
                color = Color.GRAY
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                lineWidth = 1f
            })
            add(LineDataSet(entriesSpeed, "Speed (km/h)").apply {
                color = Color.CYAN
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            })
        }

        return TripData(title, geoLine, lineDataSets)
    }
}