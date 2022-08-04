package com.cooper.wheellog.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cooper.wheellog.*
import java.util.*

class NotificationUtil(private val context: Context) {
    private val builder: NotificationCompat.Builder
    private var kostilTimer: Timer? = null
    var notificationMessageId = R.string.disconnected
    var notification: Notification? = null
        private set
    var alarmText: String = ""

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
        val notificationView = RemoteViews(context.packageName, R.layout.notification_base)
        val buttonSettings = WheelLog.AppConfig.notificationButtons
        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, notificationIntent, 0)
        }


        notificationView.setViewVisibility(R.id.ib_actions_layout,
                if (buttonSettings.any()) View.VISIBLE
                else View.GONE)

        arrayOf(Triple(R.id.ib_connection, R.string.icon_connection, Constants.NOTIFICATION_BUTTON_CONNECTION),
                Triple(R.id.ib_logging, R.string.icon_logging, Constants.NOTIFICATION_BUTTON_LOGGING),
                Triple(R.id.ib_watch, R.string.icon_watch, Constants.NOTIFICATION_BUTTON_WATCH),
                Triple(R.id.ib_beep, R.string.icon_beep, Constants.NOTIFICATION_BUTTON_BEEP),
                Triple(R.id.ib_light, R.string.icon_light, Constants.NOTIFICATION_BUTTON_LIGHT),
                Triple(R.id.ib_mi_band, R.string.icon_miband, Constants.NOTIFICATION_BUTTON_MIBAND)
        ).forEach {
            notificationView.setViewVisibility(it.first,
                    if (buttonSettings.contains(context.getString(it.second))) View.VISIBLE
                    else View.GONE)
            notificationView.setOnClickPendingIntent(it.first,
                    PendingIntent.getBroadcast(context, 0, Intent(it.third), 0))
        }
        val wd = WheelData.getInstance()
        val connectionState = wd.bluetoothLeService?.connectionState
                ?: BluetoothLeService.STATE_DISCONNECTED
        val batteryLevel = wd.batteryLevel
        val temperature = wd.temperature
        val distance = wd.distanceDouble
        val speed = wd.speedDouble
        val title = context.getString(notificationMessageId)
        val title_ride = WheelData.getInstance().rideTimeString
        notificationView.setTextViewText(R.id.text_title, context.getString(R.string.app_name))
        notificationView.setTextViewText(R.id.ib_actions_text, context.getString(R.string.notifications_actions_text))
        if (connectionState == BluetoothLeService.STATE_CONNECTED || distance + temperature + batteryLevel + speed > 0) {
            if (WheelLog.AppConfig.mibandMode == MiBandEnum.Alarm) {
                notificationView.setTextViewText(R.id.text_message, context.getString(R.string.alarmmiband))
            } else {
                val template = when (WheelLog.AppConfig.appTheme) {
                    R.style.AJDMTheme -> R.string.notification_text_ajdm_theme
                    else -> R.string.notification_text
                }
                notificationView.setTextViewText(R.id.text_message, context.getString(template, speed, batteryLevel, temperature, distance))
                notificationView.setTextViewText(R.id.text_title, "$title - $title_ride")
            }
        } else {
            notificationView.setTextViewText(R.id.text_title, title)
        }

        notificationView.setImageViewResource(R.id.ib_mi_band,
                when (WheelLog.AppConfig.mibandMode) {
                    MiBandEnum.Alarm -> WheelLog.ThemeManager.getId(ThemeIconEnum.MenuMiBandAlarm)
                    MiBandEnum.Min -> WheelLog.ThemeManager.getId(ThemeIconEnum.MenuMiBandMin)
                    MiBandEnum.Medium -> WheelLog.ThemeManager.getId(ThemeIconEnum.MenuMiBandMed)
                    MiBandEnum.Max -> WheelLog.ThemeManager.getId(ThemeIconEnum.MenuMiBandMax)
                })
        // Themes
        if (WheelLog.AppConfig.appTheme == R.style.AJDMTheme) {
            notificationView.setImageViewResource(R.id.icon, R.drawable.ajdm_notification_icon)
            notificationView.setInt(R.id.status_bar_latest_event_content, "setBackgroundResource", R.color.ajdm_background)
            val textColor = Color.BLACK
            notificationView.setTextColor(R.id.text_title, textColor)
            notificationView.setTextColor(R.id.text_message, textColor)
            notificationView.setTextColor(R.id.ib_actions_text, textColor)
        }
        notificationView.setImageViewResource(R.id.ib_connection,
                when (connectionState) {
                    BluetoothLeService.STATE_CONNECTING -> WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationConnecting)
                    BluetoothLeService.STATE_CONNECTED -> WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationConnected)
                    else -> WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationDisconnected)
                })
        notificationView.setImageViewResource(R.id.ib_logging,
                if (LoggingService.isInstanceCreated()) WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationLogOn)
                else WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationLogOff))
        notificationView.setImageViewResource(R.id.ib_watch,
                if (PebbleService.isInstanceCreated()) WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationWatchOn)
                else WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationWatchOff))
        notificationView.setImageViewResource(R.id.ib_beep, WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationHorn))
        notificationView.setImageViewResource(R.id.ib_light, WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationLight))

        builder.setSmallIcon(WheelLog.ThemeManager.getId(ThemeIconEnum.NotificationIcon))
                .setContentIntent(pendingIntent)
                .setContent(notificationView)
                .setCustomBigContentView(notificationView)
                .setChannelId(Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION)
                .priority = NotificationCompat.PRIORITY_LOW

        builder.setContentTitle(
                if (connectionState == BluetoothLeService.STATE_CONNECTED && distance + temperature + batteryLevel + speed > 0)
                    title_ride
                else
                    title)

        when (WheelLog.AppConfig.mibandMode) {
            MiBandEnum.Alarm -> {
                builder.setContentTitle(context.getString(R.string.titlealarm))
                    .setContentText(alarmText)
                alarmText = ""
            }
            MiBandEnum.Min -> builder.setContentText(context.getString(R.string.notification_text_min, speed, wd.topSpeedDouble, wd.batteryLevel, wd.distanceDouble))
            MiBandEnum.Medium -> builder.setContentText(context.getString(R.string.notification_text_med, speed, wd.averageSpeedDouble, wd.calculatedPwm, wd.batteryLevel, wd.temperature, wd.distanceDouble))
            MiBandEnum.Max -> builder.setContentText(context.getString(R.string.notification_text_max, speed, wd.topSpeedDouble, wd.averageSpeedDouble, wd.batteryLevel, wd.voltageDouble, wd.powerDouble, wd.temperature, wd.distanceDouble))
        }

        return builder.build()
    }

    fun update() {
        notification = build()
        with(NotificationManagerCompat.from(context)) {
            notify(Constants.MAIN_NOTIFICATION_ID, notification!!)
        }
    }

    // Fix Me
    // https://github.com/Wheellog/Wheellog.Android/pull/249
    fun updateKostilTimer() {
        if (WheelLog.AppConfig.mibandFixRs && kostilTimer == null) {
            kostilTimer = Timer()
            kostilTimer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (WheelLog.AppConfig.mibandMode != MiBandEnum.Alarm && WheelData.getInstance().speedDouble > 0) {
                        update()
                    }
                }
            }, 5000, 1000)
        } else {
            kostilTimer?.cancel()
            kostilTimer = null
        }
    }

    init {
        createNotificationChannel()
        builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION)
        updateKostilTimer()
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