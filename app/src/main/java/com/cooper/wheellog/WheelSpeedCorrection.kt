package com.cooper.wheellog

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Percentages stay fractional at the API boundary and exact half-steps in preferences. */
internal object WheelSpeedCorrection {
    const val MIN_PERCENT = -10.0
    const val MAX_PERCENT = 10.0
    const val STEP_PERCENT = 0.5
    const val SLIDER_STEPS = 39

    fun toHalfSteps(percent: Double): Int =
        ((if (percent.isFinite()) percent else 0.0).coerceIn(MIN_PERCENT, MAX_PERCENT) / STEP_PERCENT).roundToInt()

    fun fromHalfSteps(steps: Int): Double = steps.coerceIn(-20, 20) * STEP_PERCENT

    fun normalize(percent: Double): Double = fromHalfSteps(toHalfSteps(percent))

    fun formatPercent(percent: Double, locale: Locale): String {
        val number = NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }.format(percent)
        return (if (percent > 0) "+" else "") + number + "%"
    }
}
