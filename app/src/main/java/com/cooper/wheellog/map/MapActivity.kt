package com.cooper.wheellog.map

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.cooper.wheellog.BuildConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.LogHeaderEnum
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx
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

    private val mapAdapter by lazy { MapActivityAdapter(this) }
    private lateinit var viewPager: ViewPager2
    private lateinit var tabs: TabLayout

    // TODO: localize me
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
        Configuration.getInstance().apply {
            load(context, PreferenceManager.getDefaultSharedPreferences(context))
            userAgentValue = BuildConfig.APPLICATION_ID

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            {
                osmdroidBasePath = File(context.filesDir, "osmdroid").apply {
                    mkdirs()
                }
                osmdroidTileCache = File(osmdroidBasePath, "titles").apply {
                    mkdirs()
                }
            }
        }

        setContentView(R.layout.activity_map)

        viewPager = findViewById(R.id.pager)
        viewPager.apply {
            offscreenPageLimit = 10
            isUserInputEnabled = false
            adapter = mapAdapter
        }

        tabs = findViewById(R.id.tabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()

        val extras = intent.extras
            ?: kotlin.run {
                this.finish()
                return
            }

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
        val tripData = TripData(title)
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
            // TODO: localize me
            return tripData.apply { errorMessage = "Could not open the file " + ex.localizedMessage }
        }
        if (inputStream == null) {
            // TODO: localize me
            Timber.wtf("Failed to create inputStream for %s", extras.get("title"))
            return tripData.apply { errorMessage = "Failed to create inputStream."}
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
            return tripData.apply { errorMessage = "File does not contain GPS data. Check if location logging is enabled in the app settings" }
        }

        // for statistics
        var latitude = 0.0
        var longitude = 0.0
        var distance: Int
        var endBattery: Int
        val sdfTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

        val geoLine = ArrayList<LogGeoPoint>()
        val entriesVoltage = ArrayList<Entry>()
        val entriesCurrent = ArrayList<Entry>()
        val entriesPower = ArrayList<Entry>()
        val entriesPWM = ArrayList<Entry>()
        val entriesSpeed = ArrayList<Entry>()
        val entriesBattery = ArrayList<Entry>()
        val entriesTemperature = ArrayList<Entry>()

        try {
            var i = 0
            reader.forEachLine { line ->
                val row = line.split(",")
                val latitudeNew = row[header[LogHeaderEnum.LATITUDE]!!].toDoubleOrNull() ?: 0.0
                val longitudeNew = row[header[LogHeaderEnum.LONGITUDE]!!].toDoubleOrNull() ?: 0.0
                // stats
                val batteryLevel = row[header[LogHeaderEnum.BATTERY_LEVEL]!!].toIntOrNull() ?: 0
                endBattery = batteryLevel
                val voltage = row[header[LogHeaderEnum.VOLTAGE]!!].toDoubleOrNull() ?: 0.0
                val current = row[header[LogHeaderEnum.CURRENT]!!].toDoubleOrNull() ?: 0.0
                val power = row[header[LogHeaderEnum.POWER]!!].toDoubleOrNull() ?: 0.0
                var speed = row[header[LogHeaderEnum.SPEED]!!].toDoubleOrNull() ?: 0.0
                val temperature = row[header[LogHeaderEnum.SYSTEM_TEMP]!!].toIntOrNull() ?: 0
                val timeString = row[header[LogHeaderEnum.TIME]!!]
                val time = sdfTime.parse(timeString)!!.time / 100f
                val pwm = row[header[LogHeaderEnum.PWM]!!].toDoubleOrNull() ?: 0.0
                entriesVoltage.add(Entry(time, voltage.toFloat()))
                entriesCurrent.add(Entry(time, current.toFloat()))
                entriesPower.add(Entry(time, power.toFloat()))
                entriesSpeed.add(Entry(time, speed.toFloat()))
                entriesBattery.add(Entry(time, batteryLevel.toFloat()))
                entriesTemperature.add(Entry(time, temperature.toFloat()))
                entriesPWM.add(Entry(time, pwm.toFloat()))
                // map
                if (latitudeNew != latitude && longitudeNew != longitude
                    && latitudeNew != 0.0 && longitudeNew != 0.0) {
                    latitude = latitudeNew
                    longitude = longitudeNew
                    val altitude = row[header[LogHeaderEnum.GPS_ALT]!!].toDoubleOrNull() ?: 0.0
                    speed = row[header[LogHeaderEnum.GPS_SPEED]!!].toDoubleOrNull() ?: 0.0
                    distance = row[header[LogHeaderEnum.DISTANCE]!!].toIntOrNull() ?: 0

                    geoLine.add(LogGeoPoint(latitude, longitude, altitude).also {
                        it.speed = speed
                        it.voltage = voltage
                        it.battery = endBattery
                        it.distance = distance
                        it.temperature = temperature
                        it.timeString = timeString
                    })
                    i++
                }
            }
        } catch (ex: Exception) {
            Timber.wtf(ex.localizedMessage)
            // TODO: localize me
            return tripData.apply { errorMessage = "Unexpected exception when parsing file: " + ex.localizedMessage }
        } finally {
            inputStream.close()
        }


        // TODO: localize me
        val chart1DataSets = listOf(
             LineDataSet(entriesBattery, "Battery %").apply {
                color = getColorEx(R.color.stats_battery)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(entriesSpeed, "Speed (km/h)").apply {
                color = getColorEx(R.color.stats_speed)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(entriesTemperature, "Temperature (°C)").apply {
                color = getColorEx(R.color.stats_temp)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                lineWidth = 2f
            })
        val chart2DataSets = listOf(
            LineDataSet(entriesPWM, "PWM").apply {
                color = getColorEx(R.color.stats_pwm)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(entriesVoltage, "Voltage (V)").apply {
                color = getColorEx(R.color.stats_voltage)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(entriesPower, "Power (W)").apply {
                color = getColorEx(R.color.stats_power)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                lineWidth = 2f
            },
            LineDataSet(entriesCurrent, "Current (A)").apply {
                color = getColorEx(R.color.stats_current)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            })

        return tripData.copy(
            geoLine = geoLine,
            stats1 = chart1DataSets,
            stats2 = chart2DataSets
        )
    }
}