package com.cooper.wheellog.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cooper.wheellog.*

class NotificationUtil(private val context: Context) {
    private val builder: NotificationCompat.Builder
    var notificationMessageId = R.string.disconnected
    var notification: Notification? = null
        private set

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        with(NotificationManagerCompat.from(context)) {
            createNotificationChannel(channel)
        }
    }

    private fun build(): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        val notificationView = RemoteViews(context.packageName, R.layout.notification_base)
        notificationView.setOnClickPendingIntent(R.id.ib_connection,
                PendingIntent.getBroadcast(context, 0, Intent(Constants.NOTIFICATION_BUTTON_CONNECTION), 0))
        notificationView.setOnClickPendingIntent(R.id.ib_logging,
                PendingIntent.getBroadcast(context, 0, Intent(Constants.NOTIFICATION_BUTTON_LOGGING), 0))
        val wd = WheelData.getInstance()
        val connectionState = wd.bluetoothLeService?.connectionState ?: BluetoothLeService.STATE_DISCONNECTED
        notificationView.setImageViewResource(R.id.ib_connection,
                when (connectionState) {
                    BluetoothLeService.STATE_CONNECTING -> R.drawable.ic_action_wheel_light_orange
                    BluetoothLeService.STATE_CONNECTED -> R.drawable.ic_action_wheel_orange
                    else -> R.drawable.ic_action_wheel_grey
                })
        val batteryLevel = wd.batteryLevel
        val temperature = wd.temperature
        val distance = wd.distanceDouble
        val speed = wd.speedDouble
        notificationView.setTextViewText(R.id.text_title, context.getString(R.string.app_name))
        val title = context.getString(notificationMessageId)
        if (connectionState == BluetoothLeService.STATE_CONNECTED || distance + temperature + batteryLevel + speed > 0) {
            notificationView.setTextViewText(R.id.text_message, context.getString(R.string.notification_text, speed, batteryLevel, temperature, distance))
        }
        notificationView.setTextViewText(R.id.text_title, title)
        notificationView.setImageViewResource(R.id.ib_logging,
                if (LoggingService.isInstanceCreated()) R.drawable.ic_action_logging_orange
                else R.drawable.ic_action_logging_grey)

        return builder
                .setSmallIcon(R.drawable.ic_stat_wheel)
                .setContentIntent(pendingIntent)
                .setContent(notificationView)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
    }

    fun update() {
        notification = build()
        with(NotificationManagerCompat.from(context)) {
            notify(Constants.MAIN_NOTIFICATION_ID, notification!!)
        }
    }

    init {
        createNotificationChannel()
        builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION)
// for test
//        Timer().scheduleAtFixedRate(object : TimerTask() {
//            override fun run() {
//                val wd = WheelData.getInstance() ?: return
//                wd.setBatteryPercent((Math.random() * 100).toInt())
//                wd.temperature = (Math.random() * 10000).toInt()
//                wd.totalDistance = (Math.random() * 10000).toLong()
//                wd.speed = (Math.random() * 5000).toInt()
//                update()
//            }
//        }, 1000, 1000)
    }
}