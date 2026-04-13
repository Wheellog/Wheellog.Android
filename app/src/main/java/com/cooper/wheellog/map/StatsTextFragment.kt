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
                    Text(text = stringResource(R.string.no_chart_data), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatsTable(tripData: TripData) {
    val db = tripData.tripDb ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No DB data", color = Color.LightGray)
        }
        return
    }

    val context = LocalContext.current // Получаем контекст

    val unitKm = stringResource(R.string.km)
    val unitMin = stringResource(R.string.min)

    val statsList = remember(db) { // Пересчитываем только если изменился объект db
        listOf(
            context.getString(R.string.average_speed) to "${"%.2f".format(db.avgSpeed)} $unitKm",
            context.getString(R.string.max_speed_title) to "${db.maxSpeed} $unitKm",
            "GPS " + context.getString(R.string.max_speed_title) to "${db.maxSpeedGps} $unitKm",
            context.getString(R.string.max_pwm) to "${db.maxPwm} %",
            context.getString(R.string.riding_time) to "${db.duration} $unitMin",
            context.getString(R.string.current) to "${db.maxCurrent} A",
            context.getString(R.string.distance) to db.distance.toString(),
        )
    }

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
            color = Color.LightGray,
            fontSize = 16.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}