package com.cooper.wheellog.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cooper.wheellog.R

class StatsTextFragment : Fragment() {

    private val viewModel: MapViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val tripData by viewModel.selectedItem.observeAsState(initial = null)

                // Отображаем таблицу, если данные есть
                tripData?.let { data ->
                    StatsTable(data)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_chart_data), color = Color(LocalContext.current.tripBodyTextColor()))
                }
            }
        }
    }
}

@Composable
fun StatsTable(tripData: TripData) {
    val db = tripData.tripDb ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.no_chart_data), color = Color(LocalContext.current.tripBodyMutedColor()))
        }
        return
    }

    val context = LocalContext.current // Получаем контекст

    val values = tripData.values
    val locale = com.cooper.wheellog.LocaleManager.getLocale(context.resources)
    fun number(value: Double?, unit: String): String = value?.let { String.format(locale, "%.2f %s", it, unit) } ?: "—"
    val speedUnit = stringResource(R.string.kmh)
    val time = values?.movingSeconds?.toLong()?.let {
        String.format(locale, "%02d:%02d:%02d", it / 3600, it / 60 % 60, it % 60)
    } ?: "—"
    val statsList = listOf(
        context.getString(R.string.trip_gps_maximum) to number(values?.gpsMaximum, speedUnit),
        context.getString(R.string.trip_gps_average) to number(values?.gpsAverage, speedUnit),
        context.getString(R.string.trip_gps_moving_average) to number(values?.gpsMovingAverage, speedUnit),
        context.getString(R.string.trip_wheel_maximum) to number(values?.wheelMaximum, speedUnit),
        context.getString(R.string.max_pwm) to number(values?.maxPwm, "%"),
        context.getString(R.string.maxpower) to number(values?.maxPower, context.getString(R.string.watt)),
        context.getString(R.string.maxcurrent) to number(values?.maxCurrent, context.getString(R.string.amp)),
        context.getString(R.string.maxphasecurrent) to number(values?.maxPhaseCurrent, context.getString(R.string.amp)),
        context.getString(R.string.trip_min_voltage) to number(values?.minVoltage, context.getString(R.string.volt)),
        context.getString(R.string.riding_time) to time,
        context.getString(R.string.distance) to String.format(locale, "%.2f %s", db.distance / 1000.0, context.getString(R.string.km)),
        context.getString(R.string.maxtemperature) to number(values?.maxTemperature, "°C"),
        context.getString(R.string.trip_max_motor_temperature) to number(values?.maxMotorTemperature, "°C"),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(statsList) { (label, value) ->
            StatRow(label, value)
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = Color.Gray.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Color(LocalContext.current.tripBodyMutedColor()),
            fontSize = 16.sp
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 12.dp),
            color = Color(LocalContext.current.tripBodyTextColor()),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
internal fun formatTripDistanceKm(meters: Int, unit: String = "км"): String = String.format(java.util.Locale.getDefault(), "%.2f %s", meters / 1_000.0, unit)
