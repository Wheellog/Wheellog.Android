package com.cooper.wheellog.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.cooper.wheellog.BluetoothLeService;
import com.cooper.wheellog.LoggingService;
import com.cooper.wheellog.MainActivity;
import com.cooper.wheellog.PebbleService;
import com.cooper.wheellog.R;
import com.cooper.wheellog.WheelData;

public class NotificationUtil {
    private final Context mContext;
    private static Notification pNotification;
    private int notificationMessageId = R.string.disconnected;
    private final NotificationCompat.Builder mNotification;

    public NotificationUtil(Context context) {
        mContext = context;
        createNotificationChannel();
        mNotification = new NotificationCompat.Builder(mContext, Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION);
    }

    public static Notification getNotification() {
        return pNotification;
    }

    public void setNotificationMessageId(int value) {
        notificationMessageId = value;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        CharSequence name = mContext.getResources().getString(R.string.notification_channel_name);
        String description = mContext.getResources().getString(R.string.notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public Notification buildNotification() {
        Intent notificationIntent = new Intent(mContext, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
        RemoteViews notificationView = new RemoteViews(mContext.getPackageName(), R.layout.notification_base);

        PendingIntent pendingConnectionIntent = PendingIntent.getBroadcast(mContext, 0,
                new Intent(Constants.NOTIFICATION_BUTTON_CONNECTION), 0);
        PendingIntent pendingWatchIntent = PendingIntent.getBroadcast(mContext, 0,
                new Intent(Constants.NOTIFICATION_BUTTON_WATCH), 0);
        PendingIntent pendingLoggingIntent = PendingIntent.getBroadcast(mContext, 0,
                new Intent(Constants.NOTIFICATION_BUTTON_LOGGING), 0);

        notificationView.setOnClickPendingIntent(R.id.ib_connection,
                pendingConnectionIntent);
        notificationView.setOnClickPendingIntent(R.id.ib_watch,
                pendingWatchIntent);
        notificationView.setOnClickPendingIntent(R.id.ib_logging,
                pendingLoggingIntent);

        WheelData wd = WheelData.getInstance();

        int connectionState = BluetoothLeService.STATE_DISCONNECTED;
        BluetoothLeService bs = wd.getBluetoothLeService();
        if (bs != null) {
            connectionState = bs.getConnectionState();
        }

        switch (connectionState) {
            case BluetoothLeService.STATE_CONNECTING:
                notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_action_wheel_light_orange);
                break;
            case BluetoothLeService.STATE_CONNECTED:
                notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_action_wheel_orange);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_action_wheel_grey);
                break;
        }

        int batteryLevel = wd.getBatteryLevel();
        int temperature = wd.getTemperature();
        double distance = (double) Math.round(wd.getDistanceDouble() * 10) / 10;
        double speed = (double) Math.round(wd.getSpeedDouble() * 10) / 10;

        notificationView.setTextViewText(R.id.text_title, mContext.getString(R.string.app_name));

        String title = mContext.getString(notificationMessageId);

        if (connectionState == BluetoothLeService.STATE_CONNECTED || (distance + temperature + batteryLevel + speed) > 0) {
            notificationView.setTextViewText(R.id.text_message, mContext.getString(R.string.notification_text, speed, batteryLevel, temperature, distance));
        }

        notificationView.setTextViewText(R.id.text_title, title);

        if (PebbleService.isInstanceCreated())
            notificationView.setImageViewResource(R.id.ib_watch, R.drawable.ic_action_watch_orange);
        else
            notificationView.setImageViewResource(R.id.ib_watch, R.drawable.ic_action_watch_grey);

        if (LoggingService.isInstanceCreated())
            notificationView.setImageViewResource(R.id.ib_logging, R.drawable.ic_action_logging_orange);
        else
            notificationView.setImageViewResource(R.id.ib_logging, R.drawable.ic_action_logging_grey);

        return pNotification = mNotification
                .setSmallIcon(R.drawable.ic_stat_wheel)
                .setContentIntent(pendingIntent)
                .setContent(notificationView)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public void updateNotification() {
        Notification notification = buildNotification();
        android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Constants.MAIN_NOTIFICATION_ID, notification);
    }
}
