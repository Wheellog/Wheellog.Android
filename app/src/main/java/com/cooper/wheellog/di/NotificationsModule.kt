package com.cooper.wheellog.di

import com.cooper.wheellog.utils.NotificationUtil
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val notificationsModule = module {
    single { NotificationUtil(androidApplication()) }
}