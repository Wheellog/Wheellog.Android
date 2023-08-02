package com.cooper.wheellog.preferences

import androidx.annotation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

@Composable
private fun baseSettings(
    @StringRes name: Int,
    @StringRes desc: Int,
    themeIcon: ThemeIconEnum? = null,
    rightContent: @Composable BoxScope.() -> Unit = { },
    bottomContent: @Composable (BoxScope.() -> Unit)? = null
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp,
            )
    ) {
        val (rightControl, bottomControl, icon, title, subtext) = createRefs()
        if (themeIcon != null) {
            Icon(
                painterResource(id = WheelLog.ThemeManager.getId(themeIcon)),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width((24 + 16).dp)
                    .height(24.dp)
                    .padding(end = 16.dp)
                    .constrainAs(icon) {
                        top.linkTo(parent.top)
                        bottom.linkTo(subtext.bottom)
                        start.linkTo(parent.start)
                    }
            )
        } else {
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .constrainAs(icon) {
                        top.linkTo(parent.top)
                        bottom.linkTo(subtext.bottom)
                        start.linkTo(parent.start)
                    }
            )
        }
        Text(
            text = stringResource(id = name),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(title) {
                start.linkTo(icon.end)
                top.linkTo(parent.top)
                end.linkTo(rightControl.start, 8.dp)
                width = Dimension.fillToConstraints
            }
        )
        if (desc != 0) {
            Text(
                text = stringResource(id = desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(subtext) {
                    start.linkTo(title.start)
                    top.linkTo(title.bottom, 8.dp)
                    end.linkTo(title.end)
                    width = Dimension.fillToConstraints
                },
            )
        } else {
            Spacer(
                modifier = Modifier
                    .constrainAs(subtext) {
                        start.linkTo(title.start)
                        top.linkTo(title.bottom)
                        end.linkTo(rightControl.start)
                        width = Dimension.fillToConstraints
                    }
            )
        }
        Box(modifier = Modifier
            .constrainAs(rightControl) {
                top.linkTo(parent.top)
                bottom.linkTo(subtext.bottom)
                end.linkTo(parent.end)
            })
        {
            rightContent()
        }
        if (bottomContent != null) {
            Box(modifier = Modifier
                .constrainAs(bottomControl) {
                    top.linkTo(subtext.bottom, 8.dp)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })
            {
                bottomContent()
            }
        }
    }
}

@Composable
fun SettingsClickableComp(
    @StringRes name: Int,
    modifier: Modifier = Modifier,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    showArrowIcon: Boolean = true,
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

@Preview
@Composable
fun SettingsClickablePreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsClickableComp(
        name = R.string.speed_settings_title,
        themeIcon = ThemeIconEnum.SettingsSpeedometer
    ) { }
}

@Preview
@Composable
fun SettingsClickablePreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsClickableComp(
        name = R.string.donate_title,
        themeIcon = ThemeIconEnum.SettingsDonate,
        showArrowIcon = false
    ) { }
}

@Preview
@Composable
fun SettingsClickablePreview3() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsClickableComp(
        name = R.string.beep_on_volume_up_title,
        desc = R.string.beep_on_volume_up_description,
        themeIcon = ThemeIconEnum.SettingsDonate,
        showArrowIcon = true
    ) { }
}

@Preview
@Composable
fun SettingsClickablePreview4() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsClickableComp(
        name = R.string.beep_on_volume_up_title,
        showArrowIcon = false
    ) { }
}

@Composable
fun SettingsSwitchComp(
    @StringRes name: Int,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    isChecked: Boolean,
    onClick: (checked: Boolean) -> Unit
) {
    var mutableState by remember { mutableStateOf(isChecked) }
    baseSettings(
        name = name,
        desc = desc,
        themeIcon = themeIcon,
        rightContent = {
            Switch(
                checked = mutableState,
                onCheckedChange = {
                    mutableState = it
                    onClick(it)
                },
            )
        }
    )
}

@Preview
@Composable
fun SettingsSwitchPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsSwitchComp(
        name = R.string.use_eng_title,
        desc = R.string.use_eng_description,
        themeIcon = ThemeIconEnum.SettingsLanguage,
        isChecked = true
    ) { }
}

@Preview
@Composable
fun SettingsSwitchPreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsSwitchComp(
        name = R.string.use_eng_title,
        themeIcon = ThemeIconEnum.SettingsLanguage,
        isChecked = false
    ) { }
}

@Composable
fun SettingsSliderComp(
    @StringRes name: Int,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    position: Float = 0f,
    min: Float = 0f,
    max: Float = 100f,
    @StringRes unit: Int = 0,
    format: String = "%.0f",
    onChanged: (newPosition: Float) -> Unit
) {
    // if the dialog is visible
    var isDialogShown by remember { mutableStateOf(false) }
    val state = remember { mutableStateOf("position") }

    if (isDialogShown) {
        Dialog(onDismissRequest = {
            // dismiss the dialog on touch outside
            isDialogShown = false
        }) {
//            TextEditDialog(name = name) {
//                // to dismiss dialog from within
//                isDialogShown = false
//            }
        }
    }
    baseSettings(
        name = name,
        desc = desc,
        themeIcon = themeIcon,
        rightContent = {
            IconButton(
                onClick = { isDialogShown = true }
            ) {
                Icon(
                    Icons.Rounded.Info,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "info"
                )
            }
        },
        bottomContent = {
            Row {
                var sliderPosition by remember { mutableStateOf(position) }
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = min..max,
                    onValueChangeFinished = {
                        onChanged(sliderPosition)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Card(
                    modifier = Modifier
                        .sizeIn(maxWidth = 80.dp)
                        .padding(
                            top = 8.dp,
                            start = 8.dp
                        )
                ) {
                    Text(
                        text = String.format(format, sliderPosition)
                                + if (unit != 0) {
                            stringResource(unit)
                        } else {
                            ""
                        },
                        maxLines = 1,
                        modifier = Modifier.padding(6.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun SettingsSliderPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsSliderComp(
        name = R.string.alarm_1_battery_title,
        themeIcon = ThemeIconEnum.MenuMiBandAlarm,
        desc = R.string.alarm_1_battery_description,
    ) { }
}

@Preview
@Composable
fun SettingsSliderPreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsSliderComp(
        name = R.string.alarm_factor2_title,
        desc = R.string.alarm_factor2_description,
        position = 50f,
        min = 10f,
        max = 60f,
        format = "%.2f"
    ) { }
}

@Preview
@Composable
fun SettingsSliderPreview3() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsSliderComp(
        name = R.string.alarm_factor2_title,
        position = 66.66f,
        min = 60f,
        max = 70f,
        format = "%.2f"
    ) { }
}

@Composable
private fun TextEditDialog(
    @StringRes name: Int,
    storedValue: MutableState<String>,
    onSave: (String) -> Unit,
    onCheck: (String) -> Boolean,
    onDismiss: () -> Unit // internal method to dismiss dialog from within
) {

    // storage for new input
    var currentInput by remember {
        mutableStateOf(TextFieldValue(storedValue.value))
    }

    // if the input is valid - run the method for current value
    var isValid by remember {
        mutableStateOf(onCheck(storedValue.value))
    }

    Surface(
        color = MaterialTheme.colorScheme.surface
    ) {

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(id = name))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(currentInput, onValueChange = {
                // check on change, if the value is valid
                isValid = onCheck(it.text)
                currentInput = it
            })
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    // save and dismiss the dialog
                    onSave(currentInput.text)
                    onDismiss()
                    // disable / enable the button
                }, enabled = isValid) {
                    Text(stringResource(id = R.string.wh)) // TODO R.string.next))
                }
            }
        }
    }
}

@Composable
private fun TextEditNumberDialog(
    @StringRes name: Int,
    storedValue: State<String>,
    inputFilter: (String) -> String, // filters out not needed letters
    onSave: (String) -> Unit,
    onCheck: (String) -> Boolean,
    onDismiss: () -> Unit
) {

    var currentInput by remember {
        mutableStateOf(TextFieldValue(storedValue.value))
    }

    var isValid by remember {
        mutableStateOf(onCheck(storedValue.value))
    }

    Surface(
        color = MaterialTheme.colorScheme.surface
    ) {

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(id = name))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(currentInput,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = {
                    // filters the input and removes redundant numbers
                    val filteredText = inputFilter(it.text)
                    isValid = onCheck(filteredText)
                    currentInput = TextFieldValue(filteredText)
                })
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    onSave(currentInput.text)
                    onDismiss()
                }, enabled = isValid) {
                    Text(stringResource(id = R.string.wh)) //TODO R.string.next))
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    @StringRes name: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(id = name),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4),
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsListComp(
    @StringRes name: Int,
    modifier: Modifier = Modifier,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    entries: Map<String, String> = mapOf(),
    selectedKey: String = "",
    onSelect: (selected: Pair<String, String>) -> Unit = {},
) {
    val title = stringResource(name)
    val keys = entries.keys.toTypedArray()
    var selectedIndex by remember { mutableStateOf(selectedKey) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            shape = RoundedCornerShape(8.dp),
            onDismissRequest = { showDialog = false },
            title = {
                Row {
                    if (themeIcon != null) {
                        Icon(
                            painter = painterResource(id = WheelLog.ThemeManager.getId(themeIcon)),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
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
                                )
                                .padding(horizontal = 16.dp),
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
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
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

@Preview
@Composable
fun SettingsListPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsListComp(
        name = R.string.app_theme_title,
        desc = R.string.app_theme_description,
        entries = ThemeEnum.values().associate { it.value.toString() to it.name },
        selectedKey = ThemeEnum.Original.value.toString(),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsMultiListComp(
    @StringRes name: Int,
    modifier: Modifier = Modifier,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    entries: Map<String, String> = mapOf(),
    selectedKeys: List<String> = listOf(),
    useSort: Boolean = false,
    onChecked: (selectedKeys: List<String>) -> Unit,
) {
    val title = stringResource(name)
    val keys = entries.keys.toList()
    var selectedIndex by remember { mutableStateOf(selectedKeys.distinct()) }
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
                Row {
                    if (themeIcon != null) {
                        Icon(
                            painter = painterResource(id = WheelLog.ThemeManager.getId(themeIcon)),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
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
                            Text(
                                text = entries[key] ?: "",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
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

@Preview
@Composable
fun SettingsMultiListPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsMultiListComp(
        name = R.string.view_blocks_title,
        desc = R.string.view_blocks_description,
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
        selectedKeys = listOf("12", "2"),
    ) { }
}