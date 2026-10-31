package com.cooper.wheellog.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.InfoBlocks
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.ThemeIconEnum

@Composable
fun infoBlocksPreference(config: AppConfig) {
    val context = LocalContext.current
    val blocks = remember(context) { InfoBlocks(context) }
    var open by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(config.viewBlockOrder) }
    var selected by remember { mutableStateOf(config.viewBlockIds.toSet()) }
    clickablePref(name = stringResource(R.string.view_blocks_title),
        desc = stringResource(R.string.info_blocks_order_description),
        themeIcon = ThemeIconEnum.SettingsBlocks) {
        selected = config.viewBlockIds.toSet()
        order = blocks.editorOrder(config.viewBlockOrder, selected)
        open = true
    }
    if (open) AlertDialog(onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.view_blocks_title)) },
        text = {
            Column {
                Text(stringResource(R.string.info_blocks_order_description), style = MaterialTheme.typography.bodySmall)
                InfoBlockPresetsControl(config.infoBlockPresets, order, selected) { preset ->
                    selected = preset.enabled
                    order = blocks.editorOrder(preset.order, selected)
                }
                LazyColumn {
                    itemsIndexed(order, key = { _, key -> key }) { index, key ->
                        val name = stringResource(blocks.titles.getValue(key))
                        if (index == 0 && selected.isNotEmpty())
                            Text(stringResource(R.string.info_blocks_enabled), style = MaterialTheme.typography.titleSmall)
                        if (index == selected.size)
                            Text(stringResource(R.string.info_blocks_disabled), style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = key in selected, onCheckedChange = { checked ->
                                order = blocks.toggleInEditor(order, selected, key, checked)
                                selected = if (checked) selected + key else selected - key
                            })
                            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            fun move(offset: Int) {
                                order = order.toMutableList().apply { java.util.Collections.swap(this, index, index + offset) }
                            }
                            if (key in selected) {
                                IconButton(onClick = { move(-1) }, enabled = index > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.dashboard_move_up, name))
                                }
                                IconButton(onClick = { move(1) }, enabled = index < selected.size - 1) {
                                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.dashboard_move_down, name))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            config.viewBlockIds = order.filter { it in selected }
            config.viewBlockOrder = order
            open = false
        }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(android.R.string.cancel)) } })
}
