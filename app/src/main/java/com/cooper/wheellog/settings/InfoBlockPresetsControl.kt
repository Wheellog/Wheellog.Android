package com.cooper.wheellog.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.cooper.wheellog.InfoBlockPreset
import com.cooper.wheellog.InfoBlockPresets
import com.cooper.wheellog.R

@Composable
internal fun InfoBlockPresetsControl(store: InfoBlockPresets, order: List<String>, enabled: Set<String>,
                                     onLoad: (InfoBlockPreset) -> Unit) {
    var presets by remember { mutableStateOf(store.read()) }
    var activeId by remember { mutableStateOf<String?>(null) }
    val active = presets.find { it.id == activeId }
    var expanded by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf<String?>(null) }
    var rename by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val modified = active != null && (active.order.filter { it in active.enabled } != order.filter { it in enabled } || active.enabled != enabled)
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.info_presets_selection, active?.name ?: stringResource(R.string.info_presets_choose)) +
                if (modified) " *" else "")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { preset ->
                DropdownMenuItem(text = { Text(preset.name) }, onClick = {
                    activeId = preset.id; onLoad(preset); expanded = false
                })
            }
            if (presets.isNotEmpty()) HorizontalDivider()
            DropdownMenuItem(text = { Text(stringResource(R.string.info_presets_save_new, presets.size, InfoBlockPresets.LIMIT)) },
                enabled = presets.size < InfoBlockPresets.LIMIT, onClick = {
                    rename = false; editName = ""; expanded = false
                })
            if (active != null) {
                DropdownMenuItem(text = { Text(stringResource(R.string.info_presets_update)) }, enabled = modified, onClick = {
                    store.save(active.name, order, enabled, active.id)
                    presets = store.read(); expanded = false
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.info_presets_rename)) }, onClick = {
                    rename = true; editName = active.name; expanded = false
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.info_presets_delete)) }, onClick = {
                    deleting = true; expanded = false
                })
            }
        }
    }
    if (editName != null) {
        val name = editName.orEmpty().trim()
        val duplicate = presets.any { it.id != (if (rename) activeId else null) && it.name.equals(name, ignoreCase = true) }
        val valid = name.isNotEmpty() && name.length <= InfoBlockPresets.NAME_LIMIT && !duplicate
        AlertDialog(onDismissRequest = { editName = null },
            title = { Text(stringResource(if (rename) R.string.info_presets_rename else R.string.info_presets_new)) },
            text = {
                Column {
                    OutlinedTextField(value = editName.orEmpty(), onValueChange = { if (it.length <= InfoBlockPresets.NAME_LIMIT) editName = it },
                        singleLine = true, label = { Text(stringResource(R.string.info_presets_name)) }, isError = duplicate)
                    if (duplicate) Text(stringResource(R.string.info_presets_duplicate), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { TextButton(enabled = valid, onClick = {
                val result = if (rename && active != null) store.save(name, active.order, active.enabled, active.id)
                    else store.save(name, order, enabled)
                activeId = result.id; presets = store.read(); editName = null
            }) { Text(stringResource(android.R.string.ok)) } },
            dismissButton = { TextButton(onClick = { editName = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
    if (deleting && active != null) AlertDialog(onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.info_presets_delete)) },
        text = { Text(stringResource(R.string.info_presets_delete_confirmation, active.name)) },
        confirmButton = { TextButton(onClick = {
            store.delete(active.id); presets = store.read(); activeId = null; deleting = false
        }) { Text(stringResource(R.string.info_presets_delete)) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(android.R.string.cancel)) } })
}
