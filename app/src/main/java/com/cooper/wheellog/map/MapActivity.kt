package com.cooper.wheellog.map

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.cooper.wheellog.BuildConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.data.TripDao
import com.cooper.wheellog.data.TripDataDbEntry
import com.cooper.wheellog.data.TripDatabase
import com.cooper.wheellog.databinding.ActivityMapBinding
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
    private val viewModel: MapViewModel by viewModels()

    private val header = HashMap<LogHeaderEnum, Int>()
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())

    var dao: TripDao? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = applicationContext

        // load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance().apply {
            load(context, PreferenceManager.getDefaultSharedPreferences(context))
            userAgentValue = BuildConfig.APPLICATION_ID

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                osmdroidBasePath = File(context.filesDir, "osmdroid").apply {
                    mkdirs()
                }
                osmdroidTileCache = File(osmdroidBasePath, "titles").apply {
                    mkdirs()
                }
            }
        }

        val binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewPager = binding.pager
        viewPager.apply {
            offscreenPageLimit = 10
            isUserInputEnabled = false
            adapter = mapAdapter
        }

        val tabNames: Array<String> = arrayOf(
            getString(R.string.map_map_tab_name),
            getString(R.string.map_statistics_tab_name),
        )

        tabs = binding.tabs
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
        val title = extras.getString("title", "undefined")
        val tripData = TripData(title)
        val inputStream: InputStream?
        try {
            inputStream = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 9 or less
                FileInputStream(File(extras.getString("path", "undefined")))
            } else {
                // Android 10+
                applicationContext.contentResolver.openInputStream(Uri.parse(extras.getString("uri")))
            }
        } catch (ex: Exception) {
            Timber.wtf(ex.localizedMessage)
            return tripData.apply {
                errorMessage =
                    "${getString(R.string.error_cannot_open_file)} ${ex.localizedMessage}"
            }
        }
        if (inputStream == null) {
            val message = getString(R.string.error_inputstream_null)
            Timber.wtf(message)
            return tripData.apply { errorMessage = message }
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
            val message = getString(R.string.error_this_file_without_gps, title)
            Timber.wtf(message)
            return tripData.apply { errorMessage = message }
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

        val batteryLabel = getString(R.string.battery)
        val speedLabel = getString(R.string.speed)
        val tempLabel = getString(R.string.temperature)
        val pwmLabel = getString(R.string.pwm)
        val voltageLabel = getString(R.string.voltage)
        val currentLabel = getString(R.string.current)
        val powerLabel = getString(R.string.power)

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
                    && latitudeNew != 0.0 && longitudeNew != 0.0
                ) {
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
            return tripData.apply {
                errorMessage = "${getString(R.string.error_parsing_log)} ${ex.localizedMessage}"
            }
        } finally {
            inputStream.close()
        }

        dao = TripDatabase.getDataBase(this).tripDao()
        var trip = dao?.getTripByFileName(title)
        if (trip == null) {
            trip = TripDataDbEntry(fileName = title)
            dao?.insert(trip)
        }
        trip.apply {
            duration = (entriesSpeed.first().x - entriesSpeed.last().x).toInt()
            maxCurrent = entriesCurrent.maxOf { it.y }
            maxPwm = entriesPWM.maxOf { it.y }
            maxPower = entriesPower.maxOf { it.y }
            maxSpeed = entriesSpeed.maxOf { it.y }
            avgSpeed = entriesSpeed.map { it.y }.average().toFloat()
        }
        dao?.update(trip)

        val chart1DataSets = listOf(
            LineDataSet(entriesBattery, batteryLabel).apply {
                color = getColorEx(R.color.stats_battery)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(entriesSpeed, speedLabel).apply {
                color = getColorEx(R.color.stats_speed)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(entriesTemperature, tempLabel).apply {
                color = getColorEx(R.color.stats_temp)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                lineWidth = 2f
            })
        val chart2DataSets = listOf(
            LineDataSet(entriesPWM, pwmLabel).apply {
                color = getColorEx(R.color.stats_pwm)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(entriesVoltage, voltageLabel).apply {
                color = getColorEx(R.color.stats_voltage)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(entriesPower, powerLabel).apply {
                color = getColorEx(R.color.stats_power)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                lineWidth = 2f
            },
            LineDataSet(entriesCurrent, currentLabel).apply {
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