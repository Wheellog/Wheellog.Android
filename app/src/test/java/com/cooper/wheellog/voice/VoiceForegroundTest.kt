package com.cooper.wheellog.voice

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.cooper.wheellog.utils.Constants
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class VoiceForegroundTest {
    class Host : Service() {
        override fun onBind(intent: Intent?): IBinder? = null
    }

    @Test fun `speech and Bluetooth use the existing notification and declare both types`() {
        val controller = Robolectric.buildService(Host::class.java).create()
        val service = controller.get()
        val manager = service.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION, "WheelLog", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(service, Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION)
            .setSmallIcon(android.R.drawable.ic_dialog_info).build()
        VoiceForeground.start(service, notification)
        val types = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        assertEquals(types, service.foregroundServiceType)
        assertEquals(Constants.MAIN_NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
        assertSame(notification, shadowOf(service).lastForegroundNotification)
        assertEquals(1, shadowOf(manager).allNotifications.size)
        assertNull(manager.getNotificationChannel("voice_playback"))
        val info = service.packageManager.getServiceInfo(
            ComponentName(service.packageName, "com.cooper.wheellog.BluetoothService"), 0)
        assertEquals(types, info.foregroundServiceType)
        assertFalse(requireNotNull(service.packageManager.getPackageInfo(service.packageName,
            android.content.pm.PackageManager.GET_SERVICES).services).any {
            it.name == "com.cooper.wheellog.voice.VoicePlaybackService"
        })
        controller.destroy()
    }

    @Test fun `updating the foreground host replaces rather than duplicates its card`() {
        val controller = Robolectric.buildService(Host::class.java).create()
        val service = controller.get()
        val manager = service.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION, "WheelLog", NotificationManager.IMPORTANCE_LOW))
        val builder = Notification.Builder(service, Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
        VoiceForeground.start(service, builder.setContentText("Connecting").build())
        val notification = builder.setContentText("Connected").build()
        VoiceForeground.start(service, notification)
        assertEquals(Constants.MAIN_NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
        assertSame(notification, shadowOf(service).lastForegroundNotification)
        assertEquals(1, shadowOf(manager).allNotifications.size)
        controller.destroy()
    }
}
