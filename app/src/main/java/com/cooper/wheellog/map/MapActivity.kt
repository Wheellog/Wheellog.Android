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
import com.cooper.wheellog.data.TripParser
import com.cooper.wheellog.databinding.ActivityMapBinding
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import java.io.*

class MapActivity : AppCompatActivity() {

    private val mapAdapter by lazy { MapActivityAdapter(this) }
    private lateinit var viewPager: ViewPager2
    private lateinit var tabs: TabLayout
    private val viewModel: MapViewModel by viewModels()

    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())

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
        val path = extras.getString("path", "undefined")
        val uri = Uri.parse(extras.getString("uri", "undefined"))
        val tripData = TripData(title)
        val list = TripParser.parseFile(applicationContext, title, path, uri)
        tripData.errorMessage = TripParser.lastError
        if (tripData.errorMessage != "") {
            return tripData
        }

        val geoLine = list.filter {
            it.latitude != 0.0 && it.longitude != 0.0
        }.map { tick ->
            LogGeoPoint(tick.latitude, tick.longitude, tick.altitude).also {
                it.speed = tick.speed
                it.voltage = tick.voltage
                it.battery = tick.batteryLevel
                it.distance = tick.distance
                it.temperature = tick.temperature
                it.timeString = tick.timeString
            }
        }

        val chart1DataSets = listOf(
            LineDataSet(
                list.map { Entry(it.time, it.batteryLevel.toFloat()) },
                getString(R.string.battery)
            ).apply {
                color = getColorEx(R.color.stats_battery)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(
                list.map { Entry(it.time, it.speed.toFloat()) },
                getString(R.string.speed)
            ).apply {
                color = getColorEx(R.color.stats_speed)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(
                list.map { Entry(it.time, it.temperature.toFloat()) },
                getString(R.string.temperature)
            ).apply {
                color = getColorEx(R.color.stats_temp)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                lineWidth = 2f
            })
        val chart2DataSets = listOf(
            LineDataSet(
                list.map { Entry(it.time, it.pwm.toFloat()) },
                getString(R.string.pwm)
            ).apply {
                color = getColorEx(R.color.stats_pwm)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(
                list.map { Entry(it.time, it.voltage.toFloat()) },
                getString(R.string.voltage)
            ).apply {
                color = getColorEx(R.color.stats_voltage)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                lineWidth = 2f
            },
            LineDataSet(
                list.map { Entry(it.time, it.power.toFloat()) },
                getString(R.string.power)
            ).apply {
                color = getColorEx(R.color.stats_power)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.RIGHT
                lineWidth = 2f
            },
            LineDataSet(
                list.map { Entry(it.time, it.current.toFloat()) },
                getString(R.string.current)
            ).apply {
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