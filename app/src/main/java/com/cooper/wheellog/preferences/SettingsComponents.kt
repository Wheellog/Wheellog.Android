package com.cooper.wheellog.preferences

import androidx.annotation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeIconEnum
import com.cooper.wheellog.utils.ThemeManager
import kotlin.math.roundToInt

@Composable
private fun IconWithName(
    @StringRes name: Int,
    icon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            var offsetX by remember { mutableStateOf(0f) }
            Icon(
                painterResource(id = WheelLog.ThemeManager.getId(icon)),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
                    .padding(start = 0.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = 0.dp,
                    )
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            offsetX += delta
                        }
                    )
            )
        }
        Row (
            verticalAlignment = Alignment.CenterVertically
                ) {
            Text(
                text = stringResource(id = name),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
            )
            if (desc != 0) {
                Text(
                    text = stringResource(id = desc),
                    modifier = Modifier.padding(top = 0.dp).weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ShowDescription(
    @StringRes desc: Int = 0,
) {
    if (desc != 0) {
        Text(
            text = stringResource(id = desc),
            modifier = Modifier.padding(top = 0.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
fun SettingsClickableComp(
    @StringRes name: Int,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    showArrowIcon: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp
            ),
        onClick = onClick,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconWithName(name, themeIcon, desc)
                if (showArrowIcon) {
                    Spacer(modifier = Modifier.weight(1.0f))
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = ""
                    )
                }
            }
            // ShowDescription(desc)
        }
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

@Composable
fun SettingsSwitchComp(
    @StringRes name: Int,
    themeIcon: ThemeIconEnum? = null,
    @StringRes desc: Int = 0,
    isChecked: Boolean,
    onClick: (checked: Boolean) -> Unit
) {
    var mutableState by remember { mutableStateOf(isChecked) }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
    ) {
        val (icon, title, subtext, control) = createRefs()
        if (themeIcon != null) {
            Icon(
                painterResource(id = WheelLog.ThemeManager.getId(themeIcon)),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
                    .constrainAs(icon) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
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
                start.linkTo(icon.end, 16.dp)
                top.linkTo(parent.top)
                end.linkTo(control.start, 8.dp)
                if (desc == 0) {
                    bottom.linkTo(parent.bottom)
                }
                width = Dimension.fillToConstraints
            }
        )
        if (desc != 0) {
            Text(
                text = stringResource(id = desc),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(subtext) {
                    start.linkTo(icon.end, 16.dp)
                    top.linkTo(title.bottom, 8.dp)
                    end.linkTo(control.start, 8.dp)
                    width = Dimension.fillToConstraints
                },
            )
        }
        Switch(
            modifier = Modifier
                .constrainAs(control) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                },
            checked = mutableState,
            onCheckedChange = {
                mutableState = !mutableState
                onClick(mutableState)
            }
        )
    }
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
    minPosition: Float = 0f,
    maxPosition: Float = 100f,
    format: String = "%.0f",
    onChanged: (newPosition: Float) -> Unit
) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            var sliderPosition by remember { mutableStateOf(position) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconWithName(name, themeIcon)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = minPosition..maxPosition,
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
                        .padding(8.dp)
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
            }
            ShowDescription(desc)
        }
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
        minPosition = 10f,
        maxPosition = 60f,
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
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4),
        ) {
            Column {
                content()
            }
        }
    }
}