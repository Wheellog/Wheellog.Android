package com.cooper.wheellog.preferences

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.preference.PreferenceScreen
import com.cooper.wheellog.WheelLog

abstract class BaseSettingsClass(var context: Context, var ps: PreferenceScreen) {

    fun getString(id: Int): String {
        return context.resources.getString(id)
    }

    @Suppress("DEPRECATION")
    fun getDrawable(id: Int): Drawable? {
        val themedId = WheelLog.ThemeManager.getDrawableId(id)
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            context.resources.getDrawable(WheelLog.ThemeManager.getDrawableId(themedId))
        else context.getDrawable(WheelLog.ThemeManager.getDrawableId(themedId))
    }

    abstract fun fill (mac: String)
}