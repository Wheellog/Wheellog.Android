package com.cooper.wheellog.preferences

import android.content.Context
import androidx.annotation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.DialogHelper.setBlackIcon
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager

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
                    .width((24+16).dp)
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
fun SettingsClickablePreview()
{
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsClickableComp(
        name = R.string.speed_settings_title,
        themeIcon = ThemeIconEnum.SettingsSpeedometer
    ) { }
}

@Preview
@Composable
fun SettingsClickablePreview2()
{
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
fun SettingsClickablePreview3()
{
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
fun SettingsClickablePreview4()
{
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
fun SettingsSwitchPreview()
{
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
fun SettingsSwitchPreview2()
{
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
    baseSettings(
        name = name,
        desc = desc,
        themeIcon = themeIcon,
        rightContent = {
            IconButton(
                onClick = {
                    onChanged(max)
                }
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
                        text = String.format(format, sliderPosition) + stringResource(unit),
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
fun SettingsSliderPreview()
{
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
fun SettingsSliderPreview2()
{
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
fun SettingsSliderPreview3()
{
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
fun SettingsTextComp(
    @DrawableRes icon: Int,
    @StringRes iconDesc: Int,
    @StringRes name: Int,
    state: State<String>, // current value
    onSave: (String) -> Unit, // method to save the new value
    onCheck: (String) -> Boolean // check if new value is valid to save
) {

    // if the dialog is visible
    var isDialogShown by remember {
        mutableStateOf(false)
    }

    // conditional visibility in dependence to state
    if (isDialogShown) {
        Dialog(onDismissRequest = {
            // dismiss the dialog on touch outside
            isDialogShown = false
        }) {
            TextEditDialog(name, state, onSave, onCheck) {
                // to dismiss dialog from within
                isDialogShown = false
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = {
            // clicking on the preference, will show the dialog
            isDialogShown = true
        },
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    painterResource(id = icon),
                    contentDescription = stringResource(id = iconDesc),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.padding(8.dp)) {
                    // setting text title
                    Text(
                        text = stringResource(id = name),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // current value shown
                    Text(
                        text = state.value,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                    )
                }
            }
            Divider()
        }
    }
}

@Composable
private fun TextEditDialog(
    @StringRes name: Int,
    storedValue: State<String>,
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
    val context: Context = LocalContext.current
    val title = stringResource(name)
    val keys = entries.keys.toTypedArray()
    val values = entries.values.toTypedArray()
    var selectedIndex by remember { mutableStateOf(keys.indexOf(selectedKey)) }
    val onClick: () -> Unit = {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(values, selectedIndex) { dialog, which ->
                selectedIndex = which
                onSelect(Pair(keys[selectedIndex], values[selectedIndex]))
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
        if (themeIcon != null) {
            dialog.setIcon(WheelLog.ThemeManager.getId(themeIcon))
        }
        dialog.show().setBlackIcon()
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
                if (selectedIndex != -1) {
                    Text(
                        maxLines = 1,
                        text = values[selectedIndex],
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
fun SettingsListPreview()
{
    WheelLog.AppConfig = AppConfig(LocalContext.current)
    WheelLog.ThemeManager = ThemeManager()
    SettingsListComp(
        name = R.string.view_blocks_title,
        desc = R.string.view_blocks_description,
        themeIcon = ThemeIconEnum.SettingsBlocks,
        entries = mapOf(
            "1" to "Just one",
            "2" to "Just two",
            "3" to "Just three",
        ),
        selectedKey = "Just two",
    )
}