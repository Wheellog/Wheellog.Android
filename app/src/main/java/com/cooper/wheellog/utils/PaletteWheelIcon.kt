package com.cooper.wheellog.utils

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.cooper.wheellog.R

/** Each search frame needs its own tint: tinting the container erases the blink. */
object PaletteWheelIcon {
    fun create(context: Context, palette: ColorPalette, connected: Boolean, searching: Boolean): Drawable {
        fun frame(color: Int): Drawable = ContextCompat.getDrawable(
            context, R.drawable.ic_action_wheel_white
        )!!.mutate().apply { setTint(ContextCompat.getColor(context, color)) }

        val light = if (palette == ColorPalette.Inferno) R.color.inferno_battery else palette.foreground
        return when {
            connected -> frame(palette.accent)
            searching -> AnimationDrawable().apply {
                isOneShot = false
                addFrame(frame(palette.accent), 500)
                addFrame(frame(light), 500)
            }
            else -> frame(light)
        }
    }
}
