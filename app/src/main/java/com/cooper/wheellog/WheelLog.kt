package com.cooper.wheellog

import android.app.Application
// import com.yandex.metrica.YandexMetricaConfig
// import com.yandex.metrica.YandexMetrica
import com.cooper.wheellog.utils.NotificationUtil
import com.cooper.wheellog.utils.VolumeKeyController
import com.cooper.wheellog.utils.ThemeManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import timber.log.Timber

class WheelLog : Application() {
    override fun onCreate() {
        super.onCreate()
        me = this
//        if (BuildConfig.DEBUG) {
//            Timber.plant(Timber.DebugTree(), FileLoggingTree(applicationContext))
//        }

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

    override fun attachBaseContext(base: Context) {
        var mContext = base
        AppConfig = AppConfig(mContext)
        mContext = LocaleManager.setLocale(mContext)
        Notifications = NotificationUtil(mContext)
        VolumeKeyController = VolumeKeyController(mContext)
        super.attachBaseContext(mContext)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }

    override fun onTerminate() {
        VolumeKeyController.destroy()
        super.onTerminate()
    }

    companion object {
        private var me: WheelLog? = null
        lateinit var AppConfig: AppConfig
        lateinit var Notifications: NotificationUtil
        lateinit var VolumeKeyController: VolumeKeyController

        val appContext: Context?
            get() = me?.applicationContext

        fun cResolver(): ContentResolver {
            return me!!.contentResolver
        }
    }
}