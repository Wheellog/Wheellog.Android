package com.cooper.wheellog.voice

import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build
import com.cooper.wheellog.utils.Constants

/** Bluetooth and speech share WheelLog's existing foreground notification. */
internal object VoiceForeground {
    fun start(service: Service, notification: Notification?) {
        val activeNotification = requireNotNull(notification)
        if (Build.VERSION.SDK_INT >= 29) service.startForeground(
            Constants.MAIN_NOTIFICATION_ID, activeNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else service.startForeground(Constants.MAIN_NOTIFICATION_ID, activeNotification)
    }
}
