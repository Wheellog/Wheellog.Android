package com.cooper.wheellog.settings

import android.app.Activity
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager
import java.util.Locale

@Composable
fun clickablePref(
    name: String,
    modifier: Modifier = Modifier,
    themeIcon: ThemeIconEnum? = null,
    desc: String = "",
    showArrowIcon: Boolean = true,
    showDiv: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        baseSettings(
            name = name,
            themeIcon = themeIcon,
            desc = desc,
            showDiv = showDiv,
            rightContent = {
                if (showArrowIcon) {
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "next"
                    )
                }
            }
        )
    }
}

@Composable
fun switchPref(
    name: String,
    themeIcon: ThemeIconEnum? = null,
    desc: String = "",
    default: Boolean = false,
    defaultState: MutableState<Boolean>? = null,
    showDiv: Boolean = true,
    onClick: (checked: Boolean) -> Unit
) {
    val mutableState = defaultState ?: remember { mutableStateOf(default) }
    baseSettings(
        name = name,
        desc = desc,
        themeIcon = themeIcon,
        showDiv = showDiv,
        rightContent = {
            Switch(
                checked = mutableState.value,
                onCheckedChange = {
                    mutableState.value = it
                    onClick(it)
                },
            )
        }
    )
}

@Composable
fun sliderPref(
    name: String,
    themeIcon: ThemeIconEnum? = null,
    desc: String = "",
    position: Float = 0f,
    min: Float = 0f,
    max: Float = 100f,
    @StringRes unit: Int = 0,
    visualMultiple: Float = 1f,
    format: String = "%.0f",
    showSwitch: Boolean = false,
    valueWhenSwitchOff: Float = min,
    disableSwitchAtMin: Boolean = false,
    showDiv: Boolean = true,
    onChanged: (newPosition: Float) -> Unit
) {
    // if the dialog is visible
    var isDialogShown by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(position * visualMultiple) }
    var prevPosition by remember { mutableFloatStateOf(position * visualMultiple) }
    val minV = min * visualMultiple
    val maxV = max * visualMultiple
    val showSlider = !(disableSwitchAtMin && sliderPosition == minV)
    val unitStr = if (unit != 0) {
        " " + stringResource(unit)
    } else {
        ""
    }

    if (isDialogShown) {
        val tv = String.format(Locale.US, format, sliderPosition)
        var textValue by remember { mutableStateOf(TextFieldValue(tv, TextRange(0, tv.length))) }
        val onDone = {
            isDialogShown = false
            sliderPosition = textValue.text.toFloat()
            onChanged(sliderPosition / visualMultiple)
        }
        var isError by remember { mutableStateOf(sliderPosition !in minV..maxV) }
        AlertDialog(
            onDismissRequest = {
                isDialogShown = false
            },
            title = {
                Text(name)
            },
            text = {
                var errorText by remember { mutableStateOf("") }
                val invalidNumberText = stringResource(R.string.invalid_number)
                val focusRequester = remember { FocusRequester() }
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = textValue,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isError) {
                                onDone()
                            }
                        }
                    ),
                    onValueChange = {
                        val f = it.text.toFloatOrNull()
                        if (f != null && f in minV..maxV) {
                            isError = false
                            errorText = ""
                        } else {
                            isError = true
                            errorText = when {
                                f == null -> invalidNumberText
                                f > maxV -> "Max = ${String.format(format, maxV)}"
                                f < minV -> "Min = ${String.format(format, minV)}"
                                else -> ""
                            }
                        }
                        textValue = it
                    },
                    singleLine = true,
                    suffix = {
                        Text(
                            text = unitStr,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    isError = isError,
                    supportingText = {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    textStyle = TextStyle.Default.copy(fontSize = 32.sp),
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            },
            dismissButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    onClick = {
                    isDialogShown = false
                }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    enabled = !isError,
                    onClick = onDone
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
            )
        )
    }

    baseSettings(
        name = name,
        desc = desc,
        themeIcon = themeIcon,
        showDiv = showDiv,
        rightContent = {
            if (showSwitch) {
                val offValue = valueWhenSwitchOff * visualMultiple
                Switch(
                    checked = disableSwitchAtMin && sliderPosition != offValue,
                    onCheckedChange = {
                        if (!it) {
                            prevPosition = sliderPosition
                            sliderPosition = offValue
                        } else {
                            if (prevPosition == offValue) {
                                prevPosition = offValue + 1f
                            }
                            sliderPosition = prevPosition
                        }
                        onChanged(sliderPosition / visualMultiple)
                    },
                )
            }
            /* else {
                IconButton(
                    onClick = { isDialogShown = true }
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "info"
                    )
                }
            } */
        },
        bottomContent = {
            AnimatedVisibility(showSlider) {
                Row {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        valueRange = min * visualMultiple..max * visualMultiple,
                        onValueChangeFinished = {
                            onChanged(sliderPosition / visualMultiple)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    val maxCardWidth =
                        if ((String.format(format, max * visualMultiple)).length < 4) {
                            60.dp
                        } else {
                            100.dp
                        }
                    Card(
                        modifier = Modifier
                            .sizeIn(maxWidth = maxCardWidth)
                            .padding(
                                top = 8.dp,
                                start = 8.dp
                            ).clickable {
                                isDialogShown = true
                            },
                    ) {
                        Text(
                            text = String.format(format, sliderPosition),
                            maxLines = 1,
                            modifier = Modifier.padding(6.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (unitStr.isNotEmpty()) {
                        Text(
                            text = unitStr,
                            maxLines = 1,
                            modifier = Modifier.padding(
                                top = 14.dp,
                                start = 2.dp
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun group(
    name: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        var contentVisible by remember { mutableStateOf(true) }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp),
            onClick = {
                contentVisible = !contentVisible
            },
        ) {
            Text(
                modifier = Modifier
                    .height(48.dp)
                    .wrapContentHeight()
                    .padding(
                        start = 20.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                        end = 20.dp
                    ),
                text = if (contentVisible) { "" } else { "➖   " } + name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        AnimatedVisibility (contentVisible) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 4.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                ),
            ) {
                Column {
                    content()
                }
            }
        }
    }
}

@Composable
fun list(
    name: String,
    modifier: Modifier = Modifier,
    desc: String = "",
    themeIcon: ThemeIconEnum? = null,
    entries: Map<String, String> = mapOf(),
    defaultKey: String = "",
    showDiv: Boolean = true,
    onSelect: (selected: Pair<String, String>) -> Unit = {},
) {
    val keys = entries.keys.toTypedArray()

    var selectedIndex by remember { mutableStateOf(defaultKey) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog && keys.isNotEmpty()) {
        AlertDialog(
            shape = RoundedCornerShape(8.dp),
            onDismissRequest = { showDialog = false },
            title = {
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    if (themeIcon != null) {
                        Icon(
                            painter = painterResource(id = ThemeManager.getId(themeIcon)),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            },
            text = {
                LazyColumn {
                    items(items = keys) { key ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedIndex == key,
                                    onClick = {
                                        selectedIndex = key
                                        onSelect(Pair(key, entries[key] ?: ""))
                                        showDialog = false
                                    },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedIndex == key,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(),
                                modifier = Modifier.padding(8.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entries[key] ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
            )
        )
    }

    val onClick: () -> Unit = {
        showDialog = true
    }
    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        baseSettings(
            name = name,
            desc = desc,
            themeIcon = themeIcon,
            showDiv = showDiv,
            rightContent = {
                if (selectedIndex != "") {
                    Text(
                        maxLines = 1,
                        text = entries[selectedIndex] ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun multiList(
    name: String,
    modifier: Modifier = Modifier,
    desc: String = "",
    themeIcon: ThemeIconEnum? = null,
    entries: Map<String, String> = mapOf(),
    keyIcons: Map<String, Int> = mapOf(),
    defaultKeys: List<String> = listOf(),
    useSort: Boolean = false,
    showDiv: Boolean = true,
    onChecked: (selectedKeys: List<String>) -> Unit,
) {
    val keys = entries.keys.toList()
    var selectedIndex by remember { mutableStateOf(defaultKeys.distinct()) }
    // if selected keys are not in the list, then find them in the values
    if (!keys.containsAll(selectedIndex)) {
        selectedIndex = entries.filter { selectedIndex.contains(it.value) }.keys.toList()
    }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            shape = RoundedCornerShape(8.dp),
            onDismissRequest = { showDialog = false },
            title = {
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    if (themeIcon != null) {
                        Icon(
                            painter = painterResource(id = ThemeManager.getId(themeIcon)),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            },
            text = {
                val getItems = {
                    if (useSort) {
                        selectedIndex + keys.filter { i -> !selectedIndex.contains(i) }
                    } else {
                        keys
                    }
                }
                var items by remember {
                    mutableStateOf(getItems())
                }
                LazyColumn {
                    items(items = items, key = { it }) { key ->
                        val checked = selectedIndex.contains(key)
                        val onCheckedChange: (Boolean) -> Unit = {
                            selectedIndex = if (it) {
                                selectedIndex + key
                            } else {
                                selectedIndex - key
                            }
                            if (useSort) {
                                items = getItems()
                            }
                            onChecked(selectedIndex)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp)
                                .selectable(
                                    selected = checked,
                                    onClick = {
                                        onCheckedChange(!checked)
                                    },
                                    role = Role.Checkbox
                                )
                                .animateItemPlacement(
                                    animationSpec = tween(300)
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = onCheckedChange,
                                colors = CheckboxDefaults.colors(),
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (keyIcons.containsKey(key)) {
                                Icon(
                                    painter = painterResource(id = keyIcons[key]!!),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = entries[key] ?: "",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
            )
        )
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
        onClick = { showDialog = true },
    ) {
        baseSettings(
            name = name,
            desc = desc,
            themeIcon = themeIcon,
            showDiv = showDiv,
            rightContent = {
                if (selectedIndex.isNotEmpty()) {
                    Text(
                        maxLines = 1,
                        text = selectedIndex.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        )
    }
}

@Composable
fun alarmsList(
    name: String,
    desc: String = "",
    themeIcon: ThemeIconEnum? = null,
    default: Uri = Uri.EMPTY,
    showDiv: Boolean = true,
    onSelect: (selected: Pair<String, Uri>) -> Unit = {},
) {
    val context = LocalContext.current
    val manager by lazy {
        RingtoneManager(context as Activity).apply {
            setType(RingtoneManager.TYPE_ALARM)
        }
    }

    val nameTitle = if (default != Uri.EMPTY) {
        manager.getRingtonePosition(default).let { position ->
            manager.getRingtone(position)?.getTitle(context)
        } ?: name
    } else {
        name
    }

    var perfName by remember { mutableStateOf(nameTitle) }
    var showDialog by remember { mutableStateOf(false) }

    clickablePref(
        name = perfName,
        desc = desc,
        themeIcon = themeIcon,
        showDiv = showDiv,
    ) {
        showDialog = true
    }

    if (showDialog) {
        val cursor = manager.cursor
        val ringtones = mutableListOf<Pair<String, Uri>>()
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val ringtoneURI = manager.getRingtoneUri(cursor.position)
            ringtones.add(title to ringtoneURI)
        }

        var currentRingtone: Ringtone? by remember { mutableStateOf(null) }
        var selectedIndex by remember { mutableStateOf(default) }
        val listState = rememberLazyListState()

        AlertDialog(
            onDismissRequest = { },
            title = {
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = stringResource(R.string.use_custom_beep_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            },
            text = {
                LazyColumn(state = listState) {
                    items(items = ringtones) { key ->
                        Row (
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedIndex == key.second,
                                    onClick = {
                                        val needPlay = selectedIndex != key.second ||
                                                currentRingtone == null ||
                                                currentRingtone?.isPlaying == false
                                        selectedIndex = key.second
                                        currentRingtone?.stop()
                                        if (needPlay) {
                                            currentRingtone = manager.getRingtone(manager.getRingtonePosition(key.second))
                                            currentRingtone?.play()
                                        }
                                        perfName = key.first
                                        onSelect(key)
                                    },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedIndex == key.second,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(),
                                modifier = Modifier.padding(8.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = key.first,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                LaunchedEffect(key1 = Unit) {
                    val defaultIndex = ringtones.indexOfFirst { it.second == default }
                    if (defaultIndex != -1) {
                        listState.animateScrollToItem(defaultIndex)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    currentRingtone?.stop()
                }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        )
    }
}

@Composable
fun clickableAndAlert(
    name: String,
    desc: String = "",
    alertDesc: String = "",
    themeIcon: ThemeIconEnum? = null,
    showDiv: Boolean = true,
    condition: () -> Boolean = { true },
    confirmButtonText: String = name,
    onConfirm: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(name) },
            text = {
                Text(alertDesc)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        showDialog = false
                    },
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    clickablePref(
        name = name,
        desc = desc,
        themeIcon = themeIcon,
        showDiv = showDiv,
    ) {
        if (condition()) {
            showDialog = true
        }
    }
}

@Composable
fun baseSettings(
    name: String,
    desc: String = "",
    themeIcon: ThemeIconEnum? = null,
    showDiv: Boolean = true,
    rightContent: @Composable BoxScope.() -> Unit = { },
    bottomContent: @Composable (BoxScope.() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 0.dp)
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Column(
                modifier = Modifier.weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    if (themeIcon != null) {
                        Icon(
                            painterResource(id = ThemeManager.getId(themeIcon)),
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .width((24 + 8).dp)
                                .height(24.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (desc.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box()
            {
                rightContent()
            }
        }
        if (bottomContent != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp) )
            {
                bottomContent()
            }
        }
        if (showDiv) {
            Divider(modifier = Modifier.padding(top = 16.dp))
        } else {
            Spacer(modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Preview
@Composable
private fun baseSettingsPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    baseSettings(
        name = stringResource(R.string.auto_log_title),
        desc = stringResource(R.string.auto_log_description),
        themeIcon = ThemeIconEnum.SettingsAutoLog,
        rightContent = {
            Switch(
                checked = true,
                onCheckedChange = {},
            )
        },
        bottomContent = {
            Text("bottom")
        }
    )
}

@Preview
@Composable
private fun clickablePreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    clickablePref(
        name = stringResource(R.string.speed_settings_title),
        themeIcon = ThemeIconEnum.SettingsSpeedometer
    ) { }
}

@Preview
@Composable
private fun clickablePreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    clickablePref(
        name = stringResource(R.string.donate_title),
        themeIcon = ThemeIconEnum.SettingsDonate,
        showArrowIcon = false
    ) { }
}

@Preview
@Composable
private fun clickablePreview3() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    clickablePref(
        name = stringResource(R.string.beep_on_volume_up_title),
        desc = stringResource(R.string.beep_on_volume_up_description),
        themeIcon = ThemeIconEnum.SettingsDonate,
        showArrowIcon = true
    ) { }
}

@Preview
@Composable
private fun clickablePreview4() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    clickablePref(
        name = stringResource(R.string.beep_on_volume_up_title),
        showArrowIcon = false
    ) { }
}

@Preview
@Composable
private fun switchPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    switchPref(
        name = stringResource(R.string.use_eng_title),
        desc = stringResource(R.string.use_eng_description),
        themeIcon = ThemeIconEnum.SettingsLanguage,
        default = true
    ) { }
}

@Preview
@Composable
private fun switchPreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    switchPref(
        name = stringResource(R.string.use_eng_title),
        themeIcon = ThemeIconEnum.SettingsLanguage,
        default = false
    ) { }
}

@Preview
@Composable
private fun sliderPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    sliderPref(
        name = stringResource(R.string.alarm_1_battery_title),
        themeIcon = ThemeIconEnum.MenuMiBandAlarm,
        desc = stringResource(R.string.alarm_1_battery_description),
    ) { }
}

@Preview
@Composable
private fun sliderPreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    sliderPref(
        name = stringResource(R.string.alarm_factor2_title),
        desc = stringResource(R.string.alarm_factor2_description),
        position = 50f,
        min = 10f,
        max = 60f,
        format = "%.2f"
    ) { }
}

@Preview
@Composable
private fun sliderPreview3() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    sliderPref(
        name = stringResource(R.string.alarm_factor2_title),
        position = 66.66f,
        min = 60f,
        max = 70f,
        visualMultiple = 2f,
        format = "%.2f"
    ) { }
}

@Preview
@Composable
private fun sliderPreview4() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    sliderPref(
        name = stringResource(R.string.warning_speed_period_title),
        desc = stringResource(R.string.warning_speed_period_description),
        position = 10f,
        min = 0f,
        max = 50f,
        showSwitch = true,
        disableSwitchAtMin = true,
        unit = R.string.sec,
        format = "%.0f"
    ) { }
}

@Preview
@Composable
private fun listPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    list(
        name = stringResource(R.string.app_theme_title),
        desc = stringResource(R.string.app_theme_description),
        entries = ThemeEnum.values().associate { it.value.toString() to it.name },
        defaultKey = ThemeEnum.Original.value.toString(),
    )
}

@Preview
@Composable
private fun multiListPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    multiList(
        name = stringResource(R.string.view_blocks_title),
        desc = stringResource(R.string.view_blocks_description),
        themeIcon = ThemeIconEnum.SettingsBlocks,
        entries = mapOf(
            "1" to "Just one",
            "2" to "Just two",
            "3" to "Just three",
            "4" to "Just four",
            "5" to "Just five",
            "6" to "Just six",
            "7" to "Just seven",
            "8" to "Just eight",
            "9" to "Just nine",
            "10" to "Just ten",
            "11" to "Just eleven",
            "12" to "Just twelve",
            "13" to "Just thirteen",
            "14" to "Just fourteen",
        ),
        defaultKeys = listOf("12", "2"),
    ) { }
}