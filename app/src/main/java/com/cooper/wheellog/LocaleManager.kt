package com.cooper.wheellog

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build.VERSION_CODES
import android.os.Build
import java.util.Locale

object LocaleManager {
    const val LANGUAGE_ENGLISH = "en"

    @JvmStatic
    fun setLocale(context: Context): Context {
        var context = context
        if (WheelLog.AppConfig.useEng) {
            context = updateResources(context, LANGUAGE_ENGLISH)
        }
        return context
    }

    private fun updateResources(context: Context, language: String): Context {
        var context = context
        val locale = Locale(language)
        Locale.setDefault(locale)
        val res = context.resources
        val config = Configuration(res.configuration)
        if (isAtLeastVersion(VERSION_CODES.JELLY_BEAN_MR1)) {
            config.setLocale(locale)
            context = context.createConfigurationContext(config)
        } else {
            config.locale = locale
            res.updateConfiguration(config, res.displayMetrics)
        }
        return context
    }

    fun getLocale(res: Resources): Locale {
        val config = res.configuration
        return if (isAtLeastVersion(VERSION_CODES.N))
            config.locales[0]
        else
            config.locale
    }

    private fun isAtLeastVersion(version: Int): Boolean {
        return Build.VERSION.SDK_INT >= version
    }
}