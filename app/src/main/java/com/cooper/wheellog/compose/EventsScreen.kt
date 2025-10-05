package com.cooper.wheellog.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooper.wheellog.EventsLoggingTree

@Composable
fun EventsScreen() {
    val logs = remember { EventsLoggingTree.logsCache.toString() }

    Text(
        text = logs,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    )
}