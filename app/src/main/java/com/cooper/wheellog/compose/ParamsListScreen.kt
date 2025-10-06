package com.cooper.wheellog.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.AppConfig
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun ParamsListScreen() {
    val data = remember { WheelDataComposeBridge.data }
    val appConfig: AppConfig = koinInject()

    val useMph = appConfig.useMph

    val items = remember(data) {
        listOf(
            "Speed" to formatSpeed(data.speedDouble, useMph),
            "Top Speed" to formatSpeed(data.topSpeedDouble, useMph),
            "Average Speed" to formatSpeed(data.averageSpeedDouble, useMph),
            "Average Riding Speed" to formatSpeed(data.averageRidingSpeedDouble, useMph),
            "Distance" to formatDistance(data.distanceDouble, useMph),
            "Wheel Distance" to formatDistance(data.wheelDistanceDouble, useMph),
            "User Distance" to formatDistance(data.userDistanceDouble, useMph),
            "Total Distance" to formatDistance(data.totalDistanceDouble, useMph),
            "Voltage" to String.format(Locale.US, "%.2f V", data.voltageDouble),
            "Voltage Sag" to String.format(Locale.US, "%.2f V", data.voltageSagDouble),
            "Current" to String.format(Locale.US, "%.2f A", data.currentDouble),
            "Power" to String.format(Locale.US, "%.2f W", data.powerDouble),
            "Motor Power" to String.format(Locale.US, "%.2f W", data.motorPower),
            "Battery" to "${data.batteryLevel}%",
            "Temperature" to "${data.temperature}°C",
            "Temperature 2" to "${data.temperature2}°C",
            "CPU Temp" to "${data.cpuTemp}°C",
            "IMU Temp" to "${data.imuTemp}°C",
            "Angle" to String.format(Locale.US, "%.2f°", data.angle),
            "Roll" to String.format(Locale.US, "%.2f°", data.roll),
            "Ride Time" to data.rideTimeString,
            "Riding Time" to data.ridingTimeString,
            "Mode" to data.modeStr,
            "Model" to data.model,
            "Version" to data.version,
            "Serial" to data.serial
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 16.sp)
                Text(value, fontSize = 18.sp)
            }
        }
    }
}

private fun formatSpeed(kmh: Double, useMph: Boolean): String =
    if (useMph) String.format(Locale.US, "%.1f mph", MathsUtil.kmToMiles(kmh))
    else String.format(Locale.US, "%.1f km/h", kmh)

private fun formatDistance(km: Double, useMph: Boolean): String =
    if (useMph) String.format(Locale.US, "%.2f mi", MathsUtil.kmToMiles(km))
    else String.format(Locale.US, "%.3f km", km)