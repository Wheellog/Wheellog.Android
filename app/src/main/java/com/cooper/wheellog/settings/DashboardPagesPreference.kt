package com.cooper.wheellog.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.DashboardPages
import com.cooper.wheellog.R

@Composable
fun dashboardPagesPreference(config: AppConfig) {
    var open by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(config.dashboardPageOrder.filter { it != "main" }) }
    var enabled by remember { mutableStateOf(config.dashboardVisiblePages) }
    clickablePref(name = stringResource(R.string.dashboard_manage), desc = stringResource(R.string.dashboard_fixed_main)) { open = true }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(stringResource(R.string.dashboard_manage)) },
        text = { Column {
            Text(stringResource(R.string.dashboard_fixed_main), style = MaterialTheme.typography.bodySmall)
            LazyColumn { itemsIndexed(order, key = { _, key -> key }) { index, key ->
                val name = stringResource(DashboardPages.titles.getValue(key))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = key in enabled, onCheckedChange = { checked ->
                        enabled = if (checked) enabled + key else enabled - key
                        config.dashboardVisiblePages = enabled
                    })
                    Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    fun move(offset: Int) {
                        val next = order.toMutableList()
                        java.util.Collections.swap(next, index, index + offset)
                        order = next; config.dashboardPageOrder = listOf("main") + next
                    }
                    IconButton(enabled = index > 0, onClick = { move(-1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.dashboard_move_up, name))
                    }
                    IconButton(enabled = index < order.lastIndex, onClick = { move(1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.dashboard_move_down, name))
                    }
                }
            } }
        } }, confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(android.R.string.ok)) } })
}
