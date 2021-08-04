package com.cooper.wheellog.services

import android.content.Intent
import com.cooper.wheellog.WearActivity
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService


class StartActivityService: WearableListenerService() {
    private val startPath = "/start/wearos"

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == startPath) {
            val intent = Intent(this, WearActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}