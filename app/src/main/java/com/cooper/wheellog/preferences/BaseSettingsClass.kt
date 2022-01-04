package com.cooper.wheellog.preferences

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.preference.PreferenceScreen
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.ThemeIconEnum

abstract class BaseSettingsClass(var context: Context, var ps: PreferenceScreen) {

    fun getString(id: Int): String {
        return context.resources.getString(id)
    }

    @Suppress("DEPRECATION")
    fun getDrawable(id: ThemeIconEnum): Drawable? {
        val resId = WheelLog.ThemeManager.getId(id)
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            context.resources.getDrawable(resId)
        else context.getDrawable(resId)
    }

    abstract fun fill (mac: String)
}