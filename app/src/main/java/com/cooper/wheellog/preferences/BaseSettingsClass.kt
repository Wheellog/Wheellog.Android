package com.cooper.wheellog.preferences

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseSettingsClass(var context: Context) {

    fun getString(id: Int): String {
        return context.resources.getString(id)
    }

    fun getDrawable(id: Int): Drawable? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            context.resources.getDrawable(id)
        else context.getDrawable(id)
    }

    fun addPreferenceCategory(title: String, key: String?, vararg insidePrefs: Preference): PreferenceCategory {
        return PreferenceCategory(context).apply {
            this.title = title
            this.key = key
            isVisible = true
            GlobalScope.launch {
                // waiting attaching to preferenceScreen
                for (i in 1..100) {
                    if (parent == null) {
                        delay(5)
                    } else {
                        insidePrefs.forEach {
                            addPreference(it)
                        }
                        return@launch
                    }
                }
            }
        }
    }

    fun addDependency(preference: Preference, dependency: String) {
        GlobalScope.launch(Dispatchers.Main) {
            delay(100)
            // waiting attaching to preferenceScreen
            for (i in 1..100) {
                if (preference.parent == null) {
                    delay(10)
                } else {
                    preference.dependency = dependency
                    return@launch
                }
            }
        }
    }
}