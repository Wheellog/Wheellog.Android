package com.cooper.wheellog.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private NotificationUtil mInstance;
    private Context mContext;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private int notificationMessageId = R.string.disconnected;
    private int mBatteryLevel = 0;
    private double mDistance = 0;
    private int mTemperature = 0;

    private static NotificationCompat.Builder mNotification;
    private static Notification pNotification;

    public NotificationUtil(Context context) {
        if (mInstance == null) {
            mContext = context;
            context.registerReceiver(messageReceiver, makeIntentFilter());
            createNotificationChannel();
            mNotification = new NotificationCompat.Builder(mContext, Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION);
            mInstance = this;
        }
    }

//    public static NotificationUtil getInstance() {
//        return mInstance;
//    }

    public static Notification getNotification() {
        return pNotification;
    }

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                    mConnectionState = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BluetoothLeService.STATE_DISCONNECTED);
                    switch (mConnectionState) {
                        case BluetoothLeService.STATE_CONNECTED:
                            notificationMessageId = R.string.connected;
                            break;
                        case BluetoothLeService.STATE_DISCONNECTED:
                            notificationMessageId = R.string.disconnected;
                            break;
                        case BluetoothLeService.STATE_CONNECTING:
                            if (intent.hasExtra(Constants.INTENT_EXTRA_BLE_AUTO_CONNECT))
                                notificationMessageId = R.string.searching;
                            else
                                notificationMessageId = R.string.connecting;
                            break;
                    }
                    updateNotification();
                    break;
                case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                    int batteryLevel = WheelData.getInstance().getBatteryLevel();
                    int temperature = WheelData.getInstance().getTemperature();
                    double distance = (double) Math.round(WheelData.getInstance().getDistanceDouble() * 10) / 10;

                    if (mBatteryLevel != batteryLevel ||
                            mDistance != distance ||
                            mTemperature != temperature) {
                        mBatteryLevel = batteryLevel;
                        mTemperature = temperature;
                        mDistance = distance;
                        updateNotification();
                    }
                    break;
                case Constants.ACTION_PEBBLE_SERVICE_TOGGLED:
                    updateNotification();
                    break;
                case Constants.ACTION_LOGGING_SERVICE_TOGGLED:
                    updateNotification();
                    break;
            }
        }
    };


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = mContext.getResources().getString(R.string.notification_channel_name);
            String description = mContext.getResources().getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID_NOTIFICATION, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager =  mContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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

        switch (mConnectionState) {
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

        notificationView.setTextViewText(R.id.text_title, mContext.getString(R.string.app_name));

        String title = mContext.getString(notificationMessageId);

        if (mConnectionState == BluetoothLeService.STATE_CONNECTED || (mDistance + mTemperature + mBatteryLevel) > 0) {
            notificationView.setTextViewText(R.id.text_message, mContext.getString(R.string.notification_text, mBatteryLevel, mTemperature, mDistance));
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

        pNotification = mNotification
                .setSmallIcon(R.drawable.ic_stat_wheel)
                .setContentIntent(pendingIntent)
                .setContent(notificationView)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        return pNotification;
    }

    private void updateNotification() {
        Notification notification = buildNotification();
        //pNotification = notification;
        android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Constants.MAIN_NOTIFICATION_ID, notification);
    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(messageReceiver);
    }

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED);
        return intentFilter;
    }

    public static class notificationButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Constants.NOTIFICATION_BUTTON_CONNECTION.equals(action))
                context.sendBroadcast(new Intent(Constants.ACTION_REQUEST_CONNECTION_TOGGLE));
            else if (Constants.NOTIFICATION_BUTTON_WATCH.equals(action)) {
                Intent pebbleServiceIntent = new Intent(context.getApplicationContext(), PebbleService.class);
                if (PebbleService.isInstanceCreated())
                    context.stopService(pebbleServiceIntent);
                else
                    context.startService(pebbleServiceIntent);
            } else if (Constants.NOTIFICATION_BUTTON_LOGGING.equals(action)) {
                Intent loggingServiceIntent = new Intent(context.getApplicationContext(), LoggingService.class);
                if (LoggingService.isInstanceCreated())
                    context.stopService(loggingServiceIntent);
                else
                    context.startService(loggingServiceIntent);
            }
        }
    }
}
