package com.cooper.wheellog.map

import com.cooper.wheellog.utils.SomeUtil.getColorEx
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.ThemeManager

/** Matches the selected theme's settings menu, independently of dashboard digit colors. */
fun Context.tripTextColor(): Int = when (ThemeManager.theme) {
    R.style.InfernoTheme -> ContextCompat.getColor(this, R.color.inferno_battery)
    R.style.ContrastTheme -> ContextCompat.getColor(this, R.color.contrast_accent)
    else -> ThemeManager.palette?.let { ContextCompat.getColor(this, it.foreground) } ?: if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        0xffa2c9ff.toInt() else 0xff1560a5.toInt()
}
fun Context.tripMutedColor(): Int = ThemeManager.palette?.let { ContextCompat.getColor(this, it.muted) }
    ?: if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        0xffc3c6cf.toInt() else 0xffd1c4e9.toInt()

/** Main settings icons use MaterialTheme.primary: palette accent, or the day/night primary. */
fun Context.tripIconColor(): Int = ThemeManager.palette?.let { ContextCompat.getColor(this, it.accent) }
    ?: tripTextColor()

fun Context.tripSurfaceColor(): Int = ContextCompat.getColor(this,
    ThemeManager.palette?.surface ?: R.color.trip_popup_background)


/** Style both the strip and its individual tabs: XML tabBackground otherwise masks the strip. */
fun styleTripTabs(context: Context, tabs: com.google.android.material.tabs.TabLayout) {
    val surface = context.tripSurfaceColor()
    tabs.setBackgroundColor(surface)
    tabs.setTabTextColors(readableStatsColor(context.tripMutedColor(), surface),
        readableStatsColor(context.tripTextColor(), surface))
    tabs.setSelectedTabIndicatorColor(statsHueColor(context.tripIconColor(), surface))
}

fun styleTripMessage(context: Context, bar: com.google.android.material.snackbar.Snackbar) {
    val surface = context.tripSurfaceColor()
    bar.setBackgroundTint(surface)
    bar.view.setBackgroundColor(surface)
    bar.setTextColor(readableStatsColor(context.tripTextColor(), surface))
    bar.setActionTextColor(readableStatsColor(context.tripIconColor(), surface))
}


/** Body and popup surfaces can have opposite brightness in the original themes. */
fun Context.tripBodyTextColor(): Int = readableStatsColor(tripTextColor(), getColorEx(R.color.background))
fun Context.tripBodyMutedColor(): Int = readableStatsColor(tripMutedColor(), getColorEx(R.color.background))
