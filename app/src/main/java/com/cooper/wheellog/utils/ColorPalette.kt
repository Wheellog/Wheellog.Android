package com.cooper.wheellog.utils

import androidx.annotation.ColorRes
import com.cooper.wheellog.R

/** Color-only schemes retain the Original speedometer layout and icon set. */
data class ColorPalette(
    @ColorRes val background: Int,
    @ColorRes val surface: Int,
    @ColorRes val accent: Int,
    @ColorRes val foreground: Int,
    @ColorRes val muted: Int,
    @ColorRes val arc: Int,
    @ColorRes val battery: Int,
    @ColorRes val temperature: Int,
) {
    fun resourceFor(@ColorRes original: Int): Int = when (original) {
        R.color.background, R.color.navigation_bar, R.color.primary_dark -> background
        R.color.primary, R.color.settings_bg -> surface
        R.color.accent, R.color.wheelview_main_positive_dial -> accent
        R.color.wheelview_speed_text, R.color.wheelview_textblocks,
        R.color.wheelview_text, R.color.icons -> foreground
        R.color.wheelview_versiontext -> if (this == Contrast || this == Inferno) R.color.palette_version_muted else muted
        R.color.secondary_text, R.color.primary_light -> muted
        R.color.wheelview_arc_dim -> arc
        R.color.wheelview_battery_dial -> battery
        R.color.wheelview_battery_low_dial -> R.color.palette_battery_recovery
        R.color.wheelview_main_negative_dial -> R.color.palette_negative_current
        R.color.wheelview_temperature_dial -> temperature
        else -> original
    }

    companion object {
        val Contrast = ColorPalette(
            R.color.contrast_background, R.color.contrast_surface, R.color.contrast_accent,
            R.color.contrast_foreground, R.color.contrast_muted, R.color.contrast_arc,
            R.color.inferno_battery, R.color.contrast_temperature,
        )
        val Inferno = ColorPalette(
            R.color.inferno_background, R.color.inferno_surface, R.color.inferno_accent,
            R.color.inferno_foreground, R.color.inferno_muted, R.color.inferno_arc,
            R.color.inferno_battery, R.color.inferno_temperature,
        )

        val Porcelain = ColorPalette(
            R.color.porcelain_background, R.color.porcelain_surface, R.color.porcelain_accent,
            R.color.porcelain_foreground, R.color.porcelain_muted, R.color.porcelain_arc,
            R.color.porcelain_battery, R.color.porcelain_temperature
        )
        val Polar = ColorPalette(
            R.color.polar_background, R.color.polar_surface, R.color.polar_accent,
            R.color.polar_foreground, R.color.polar_muted, R.color.polar_arc,
            R.color.polar_battery, R.color.polar_temperature
        )
        val Jade = ColorPalette(
            R.color.jade_background, R.color.jade_surface, R.color.jade_accent,
            R.color.jade_foreground, R.color.jade_muted, R.color.jade_arc,
            R.color.jade_battery, R.color.jade_temperature
        )
        val Steel = ColorPalette(
            R.color.steel_background, R.color.steel_surface, R.color.steel_accent,
            R.color.steel_foreground, R.color.steel_muted, R.color.steel_arc,
            R.color.steel_battery, R.color.steel_temperature
        )
        val Heather = ColorPalette(
            R.color.heather_background, R.color.heather_surface, R.color.heather_accent,
            R.color.heather_foreground, R.color.heather_muted, R.color.heather_arc,
            R.color.heather_battery, R.color.heather_temperature
        )

        fun forTheme(theme: Int): ColorPalette? = when (theme) {
            R.style.ContrastTheme -> Contrast
            R.style.InfernoTheme -> Inferno
            R.style.PorcelainTheme -> Porcelain
            R.style.PolarTheme -> Polar
            R.style.JadeTheme -> Jade
            R.style.SteelTheme -> Steel
            R.style.HeatherTheme -> Heather
            else -> null
        }
    }
}
