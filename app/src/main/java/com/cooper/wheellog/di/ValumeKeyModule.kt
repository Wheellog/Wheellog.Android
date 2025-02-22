package com.cooper.wheellog.di

import com.cooper.wheellog.utils.VolumeKeyController
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val volumeKeyModule = module {
    single { VolumeKeyController(androidApplication()) }
}