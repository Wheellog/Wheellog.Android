package com.cooper.wheellog

import android.app.Application
import android.content.res.Configuration
import com.cooper.wheellog.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WheelLog : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WheelLog)
            modules(listOf(settingModule, notificationsModule, volumeKeyModule, dbModule))
        }

        WheelData.initiate()

        // YandexMetrica.
//        if (BuildConfig.metrica_api.isNotEmpty()) {
//            val config = YandexMetricaConfig
//                .newConfigBuilder(BuildConfig.metrica_api)
//                .withLocationTracking(false)
//                .withStatisticsSending(AppConfig.yandexMetricaAccepted)
//                .build()
//            YandexMetrica.activate(applicationContext, config)
//        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }
}