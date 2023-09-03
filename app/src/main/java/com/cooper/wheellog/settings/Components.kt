package com.cooper.wheellog.settings

import androidx.annotation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.*
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
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager

@Composable
fun clickablePref(
    @StringRes name: Int,
    modifier: Modifier = Modifier,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
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
    @StringRes name: Int,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    default: Boolean,
    showDiv: Boolean = true,
    onClick: (checked: Boolean) -> Unit
) {
    var mutableState by remember { mutableStateOf(default) }
    baseSettings(
        name = name,
        desc = desc,
        themeIcon = themeIcon,
        showDiv = showDiv,
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

@Composable
fun sliderPref(
    @StringRes name: Int,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
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

    val unitStr = if (unit != 0) {
        " " + stringResource(unit)
    } else {
        ""
    }

    var sliderPosition by remember { mutableStateOf(position * visualMultiple) }
    var prevPosition by remember { mutableStateOf(position * visualMultiple) }
    val showSlider = !(disableSwitchAtMin && sliderPosition == min * visualMultiple)

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
                            )
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
    @StringRes name: Int,
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
                text = if (contentVisible) { "" } else { "➖   " } + stringResource(id = name),
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
    @StringRes name: Int,
    modifier: Modifier = Modifier,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    entries: Map<String, String> = mapOf(),
    defaultKey: String = "",
    showDiv: Boolean = true,
    onSelect: (selected: Pair<String, String>) -> Unit = {},
) {
    val title = stringResource(name)
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
                        text = title,
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
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(id = android.R.string.cancel))
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
    @StringRes name: Int,
    modifier: Modifier = Modifier,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    entries: Map<String, String> = mapOf(),
    defaultKeys: List<String> = listOf(),
    useSort: Boolean = false,
    showDiv: Boolean = true,
    onChecked: (selectedKeys: List<String>) -> Unit,
) {
    val title = stringResource(name)
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
                        text = title,
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
fun baseSettings(
    @StringRes name: Int,
    @StringRes desc: Int = 0,
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
                        text = stringResource(id = name),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (desc != 0) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(id = desc),
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

/*@Composable
fun baseSettingsConstrant(
    @StringRes name: Int,
    @StringRes desc: Int = 0,
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
                painterResource(id = ThemeManager.getId(themeIcon)),
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
}*/

//@Composable
//private fun TextEditDialog(
//    @StringRes name: Int,
//    storedValue: MutableState<String>,
//    onSave: (String) -> Unit,
//    onCheck: (String) -> Boolean,
//    onDismiss: () -> Unit // internal method to dismiss dialog from within
//) {
//
//    // storage for new input
//    var currentInput by remember {
//        mutableStateOf(TextFieldValue(storedValue.value))
//    }
//
//    // if the input is valid - run the method for current value
//    var isValid by remember {
//        mutableStateOf(onCheck(storedValue.value))
//    }
//
//    Surface(
//        color = MaterialTheme.colorScheme.surface
//    ) {
//
//        Column(
//            modifier = Modifier
//                .wrapContentHeight()
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            Text(stringResource(id = name))
//            Spacer(modifier = Modifier.height(8.dp))
//            TextField(currentInput, onValueChange = {
//                // check on change, if the value is valid
//                isValid = onCheck(it.text)
//                currentInput = it
//            })
//            Row {
//                Spacer(modifier = Modifier.weight(1f))
//                Button(onClick = {
//                    // save and dismiss the dialog
//                    onSave(currentInput.text)
//                    onDismiss()
//                    // disable / enable the button
//                }, enabled = isValid) {
//                    Text(stringResource(id = R.string.wh)) // TODO R.string.next))
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun TextEditNumberDialog(
//    @StringRes name: Int,
//    storedValue: State<String>,
//    inputFilter: (String) -> String, // filters out not needed letters
//    onSave: (String) -> Unit,
//    onCheck: (String) -> Boolean,
//    onDismiss: () -> Unit
//) {
//
//    var currentInput by remember {
//        mutableStateOf(TextFieldValue(storedValue.value))
//    }
//
//    var isValid by remember {
//        mutableStateOf(onCheck(storedValue.value))
//    }
//
//    Surface(
//        color = MaterialTheme.colorScheme.surface
//    ) {
//
//        Column(
//            modifier = Modifier
//                .wrapContentHeight()
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            Text(stringResource(id = name))
//            Spacer(modifier = Modifier.height(8.dp))
//            TextField(currentInput,
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                onValueChange = {
//                    // filters the input and removes redundant numbers
//                    val filteredText = inputFilter(it.text)
//                    isValid = onCheck(filteredText)
//                    currentInput = TextFieldValue(filteredText)
//                })
//            Row {
//                Spacer(modifier = Modifier.weight(1f))
//                Button(onClick = {
//                    onSave(currentInput.text)
//                    onDismiss()
//                }, enabled = isValid) {
//                    Text(stringResource(id = R.string.wh)) //TODO R.string.next))
//                }
//            }
//        }
//    }
//}

@Preview
@Composable
private fun baseSettingsPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    baseSettings(
        name = R.string.auto_log_title,
        desc = R.string.auto_log_description,
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
        name = R.string.speed_settings_title,
        themeIcon = ThemeIconEnum.SettingsSpeedometer
    ) { }
}

@Preview
@Composable
private fun clickablePreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    clickablePref(
        name = R.string.donate_title,
        themeIcon = ThemeIconEnum.SettingsDonate,
        showArrowIcon = false
    ) { }
}

@Preview
@Composable
private fun clickablePreview3() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    clickablePref(
        name = R.string.beep_on_volume_up_title,
        desc = R.string.beep_on_volume_up_description,
        themeIcon = ThemeIconEnum.SettingsDonate,
        showArrowIcon = true
    ) { }
}

@Preview
@Composable
private fun clickablePreview4() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    clickablePref(
        name = R.string.beep_on_volume_up_title,
        showArrowIcon = false
    ) { }
}

@Preview
@Composable
private fun switchPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    switchPref(
        name = R.string.use_eng_title,
        desc = R.string.use_eng_description,
        themeIcon = ThemeIconEnum.SettingsLanguage,
        default = true
    ) { }
}

@Preview
@Composable
private fun switchPreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    switchPref(
        name = R.string.use_eng_title,
        themeIcon = ThemeIconEnum.SettingsLanguage,
        default = false
    ) { }
}

@Preview
@Composable
private fun sliderPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    sliderPref(
        name = R.string.alarm_1_battery_title,
        themeIcon = ThemeIconEnum.MenuMiBandAlarm,
        desc = R.string.alarm_1_battery_description,
    ) { }
}

@Preview
@Composable
private fun sliderPreview2() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    sliderPref(
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
private fun sliderPreview3() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    sliderPref(
        name = R.string.alarm_factor2_title,
        position = 66.66f,
        min = 60f,
        max = 70f,
        format = "%.2f"
    ) { }
}

@Preview
@Composable
private fun sliderPreview4() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    sliderPref(
        name = R.string.warning_speed_period_title,
        desc = R.string.warning_speed_period_description,
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
        name = R.string.app_theme_title,
        desc = R.string.app_theme_description,
        entries = ThemeEnum.values().associate { it.value.toString() to it.name },
        defaultKey = ThemeEnum.Original.value.toString(),
    )
}

@Preview
@Composable
private fun multiListPreview() {
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    multiList(
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
        defaultKeys = listOf("12", "2"),
    ) { }
}