package com.cooper.wheellog.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooper.wheellog.WheelData

@Composable
fun SmartBmsScreen() {
    val data = remember { WheelData.getInstance() }

    val bms1 = data.bms1
    val bms2 = data.bms2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("BMS 1", fontSize = 16.sp)
        BmsBlock(bms1)

        Spacer(Modifier.height(12.dp))

        Text("BMS 2", fontSize = 16.sp)
        BmsBlock(bms2)
    }
}

@Composable
private fun BmsBlock(bms: com.cooper.wheellog.utils.SmartBms) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Voltage: ${String.format("%.2f V", bms.voltage)}")
        Text("Current: ${String.format("%.2f A", bms.current)}")
        Text("Temp 1: ${String.format("%.1f°C", bms.temp1)}")
        Text("Temp 2: ${String.format("%.1f°C", bms.temp2)}")
        Text("Max Cell: ${String.format("%.3f V", bms.maxCell)}")
        Text("Min Cell: ${String.format("%.3f V", bms.minCell)}")
        Text("Cell Diff: ${String.format("%.3f V", bms.cellDiff)}")
    }
}