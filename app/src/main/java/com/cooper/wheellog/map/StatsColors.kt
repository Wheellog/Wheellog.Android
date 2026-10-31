package com.cooper.wheellog.map

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.SomeUtil.getColorEx

fun readableStatsColor(preferred: Int, background: Int): Int =
    if (ColorUtils.calculateContrast(preferred, background) >= 4.5) preferred
    else listOf(Color.BLACK, Color.WHITE).maxBy { ColorUtils.calculateContrast(it, background) }

/** Raise lightness contrast without discarding the metric's hue. */
fun statsHueColor(preferred: Int, background: Int): Int {
    if (ColorUtils.calculateContrast(preferred, background) >= 3.0) return preferred
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(preferred, hsl)
    val start = hsl[2]
    val target = if (ColorUtils.calculateContrast(Color.WHITE, background) > ColorUtils.calculateContrast(Color.BLACK, background)) 1f else 0f
    for (step in 1..100) {
        hsl[2] = start + (target - start) * step / 100f
        val color = ColorUtils.HSLToColor(hsl)
        if (ColorUtils.calculateContrast(color, background) >= 3.0) return color
    }
    return if (target == 1f) Color.WHITE else Color.BLACK
}

private fun Context.statsThemeColor(attribute: Int): Int {
    val value = TypedValue()
    check(theme.resolveAttribute(attribute, value, true))
    return if (value.resourceId != 0) androidx.core.content.ContextCompat.getColor(this, value.resourceId) else value.data
}

fun Context.statsBackgroundColor() = statsThemeColor(android.R.attr.colorBackground)
fun Context.statsMarkerBackgroundColor() = statsBackgroundColor()
fun Context.statsTextColor(): Int {
    val background = statsBackgroundColor()
    // Small axis labels must stay neutral, even when dashboard digits use a saturated accent.
    val preferred = listOf(0xff202020.toInt(), 0xffececec.toInt())
        .maxBy { ColorUtils.calculateContrast(it, background) }
    return readableStatsColor(preferred, background)
}
fun Context.statsMarkerTextColor() = statsTextColor()

/** Stable metric hues, choosing the readable shade for the actual chart surface. */
fun Context.statSeriesColor(metric: Int): Int {
    val shades = when (metric) {
        R.color.stats_battery -> 0xff145ac4.toInt() to 0xff71a8ff.toInt()
        R.color.stats_speed -> 0xff006b31.toInt() to 0xff00ee78.toInt()
        R.color.stats_temp -> 0xffb4003c.toInt() to 0xffff527e.toInt()
        R.color.stats_pwm -> 0xff66508e.toInt() to 0xffc8b3ef.toInt()
        R.color.stats_voltage -> 0xff006966.toInt() to 0xff41e3c1.toInt()
        R.color.stats_power -> 0xff805500.toInt() to 0xffffd93d.toInt()
        else -> 0xffa31565.toInt() to 0xffff70be.toInt()
    }
    val background = statsBackgroundColor()
    return statsHueColor(listOf(shades.first, shades.second).maxBy { ColorUtils.calculateContrast(it, background) }, background)
}
