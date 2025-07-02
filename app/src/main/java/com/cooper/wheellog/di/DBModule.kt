package com.cooper.wheellog.di

import com.cooper.wheellog.data.TripDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dbModule = module {
    single { TripDatabase.getDataBase(androidApplication()).tripDao() }
}