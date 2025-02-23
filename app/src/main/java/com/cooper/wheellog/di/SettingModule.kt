package com.cooper.wheellog.di

import com.cooper.wheellog.AppConfig
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val settingModule = module {
    single { AppConfig(androidApplication()) }
}