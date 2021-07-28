package com.cooper.wheellog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView


class SplashActivity: Activity() {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.splash_screen)
//        val contentView = layoutInflater.inflate(R.layout.splash_screen, null)
//        val image = when (WheelLog.AppConfig.appTheme) {
//            R.style.AJDMTheme -> R.drawable.ajdm_notification_icon
//            else -> R.mipmap.ic_launcher
//        }
//        contentView.findViewById<ImageView>(R.id.splashImage).setImageResource(image)
//        setContentView(contentView)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}