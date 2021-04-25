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
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            context.resources.getDrawable(WheelLog.ThemeManager.getDrawableId(id))
        else context.getDrawable(WheelLog.ThemeManager.getDrawableId(id))
    }

    abstract fun fill (mac: String)
}