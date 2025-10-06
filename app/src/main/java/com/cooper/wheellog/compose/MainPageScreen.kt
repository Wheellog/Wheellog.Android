package com.cooper.wheellog.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainPageScreen() {
    val data = remember { WheelDataComposeBridge.data }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Speed: ${data.speedDouble} km/h", fontSize = 20.sp)
        Text("Battery: ${data.batteryLevel}%", fontSize = 20.sp)
        Text("Temperature: ${data.temperature}°C", fontSize = 20.sp)
        Text("Distance: ${data.distanceDouble} km", fontSize = 20.sp)
        Text("Voltage: ${data.voltageDouble} V", fontSize = 20.sp)
        Text("Current: ${data.currentDouble} A", fontSize = 20.sp)
    }
}