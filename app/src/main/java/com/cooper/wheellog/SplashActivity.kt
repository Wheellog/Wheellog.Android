package com.cooper.wheellog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*


class SplashActivity: AppCompatActivity() {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MainActivity::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
        } else {
            null
        }

        lifecycleScope.launch {
            startActivity(intent, options)
            delay(5000)
            finish()
        }
    }
}