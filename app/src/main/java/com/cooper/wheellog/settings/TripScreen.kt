package com.cooper.wheellog.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.Constants

@Composable
fun tripScreen( ) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        clickablePref(
            name = stringResource(R.string.reset_max_values_title),
            desc = stringResource(R.string.reset_max_values_description),
            showArrowIcon = false,
        ) {
            WheelData.getInstance().resetMaxValues()
        }
        val context = LocalContext.current
        clickablePref(
            name = stringResource(R.string.reset_lowest_battery_title),
            showArrowIcon = false,
        ) {
            WheelData.getInstance().resetVoltageSag()
            context.sendBroadcast(Intent(Constants.ACTION_PREFERENCE_RESET))
        }
        clickablePref(
            name = stringResource(R.string.reset_user_distance_title),
            showArrowIcon = false,
        ) {
            WheelData.getInstance().resetUserDistance()
        }
    }
}
