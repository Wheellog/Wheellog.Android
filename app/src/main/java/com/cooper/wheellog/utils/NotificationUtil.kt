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
import com.cooper.wheellog.presentation.preferences.MultiSelectPreference

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
        val buttonsSettingsString = WheelLog.AppConfig.notifivationButtons
        val buttonSettings = buttonsSettingsString?.split(MultiSelectPreference.separator)?.toTypedArray()
                ?: arrayOf(context.getString(R.string.icon_connection),
                        context.getString(R.string.icon_logging),
                        context.getString(R.string.icon_watch))

        notificationView.setViewVisibility(R.id.ib_actions_layout,
                if (buttonsSettingsString != "") View.VISIBLE
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
        notificationView.setTextViewText(R.id.text_title, context.getString(R.string.app_name))
        notificationView.setTextViewText(R.id.ib_actions_text, context.getString(R.string.notifications_actions_text))
        val title = context.getString(notificationMessageId)
        if (connectionState == BluetoothLeService.STATE_CONNECTED || distance + temperature + batteryLevel + speed > 0) {
            val template = when (WheelLog.AppConfig.appTheme) {
                R.style.AJDMTheme -> R.string.notification_text_ajdm_theme
                else -> R.string.notification_text
            }
            notificationView.setTextViewText(R.id.text_message, context.getString(template, speed, batteryLevel, temperature, distance))
        }
        notificationView.setTextViewText(R.id.text_title, title)
        notificationView.setImageViewResource(R.id.ib_mi_band,
                when (WheelLog.AppConfig.mibandMode) {
                    MiBandEnum.Alarm -> R.drawable.ic_mi_alarm
                    MiBandEnum.Min -> R.drawable.ic_mi_min
                    MiBandEnum.Medium -> R.drawable.ic_mi_med
                    MiBandEnum.Max -> R.drawable.ic_mi_max
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
                    BluetoothLeService.STATE_CONNECTING -> WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_wheel_light_orange)
                    BluetoothLeService.STATE_CONNECTED -> WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_wheel_orange)
                    else -> WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_wheel_grey)
                })
        notificationView.setImageViewResource(R.id.ib_logging,
                if (LoggingService.isInstanceCreated()) WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_logging_orange)
                else WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_logging_grey))
        notificationView.setImageViewResource(R.id.ib_watch,
                if (PebbleService.isInstanceCreated()) WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_watch_orange)
                else WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_watch_grey))
        notificationView.setImageViewResource(R.id.ib_beep, WheelLog.ThemeManager.getDrawableId(R.drawable.ic_horn_32_gray))
        notificationView.setImageViewResource(R.id.ib_light, WheelLog.ThemeManager.getDrawableId(R.drawable.ic_sun_32_gray))

        builder.setSmallIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_stat_wheel))
                .setContentIntent(pendingIntent)
                .setContent(notificationView)
                .setCustomBigContentView(notificationView)
                .setChannelId(Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION)
                .priority = NotificationCompat.PRIORITY_LOW
        when (WheelLog.AppConfig.mibandMode) {
            MiBandEnum.Alarm -> builder.setContentTitle(title)
                    .setContentText(context.getString(R.string.notification_text_alarm, speed, wd.currentDouble, wd.voltageDouble, wd.batteryLevel, wd.temperature))
            MiBandEnum.Min -> builder.setContentTitle(title)
                    .setContentText(context.getString(R.string.notification_text_min, speed, wd.topSpeedDouble, wd.batteryLevel, wd.distanceDouble))
            MiBandEnum.Medium -> builder.setContentTitle(title)
                    .setContentText(context.getString(R.string.notification_text_med, speed, wd.averageSpeedDouble, wd.calculatedPwm, wd.batteryLevel, wd.temperature, wd.distanceDouble))
            MiBandEnum.Max -> builder.setContentTitle(title)
                    .setContentText(context.getString(R.string.notification_text_max, speed, wd.topSpeedDouble, wd.averageSpeedDouble, wd.batteryLevel, wd.voltageDouble, wd.powerDouble, wd.temperature, wd.distanceDouble))
        }

        return builder.build()
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