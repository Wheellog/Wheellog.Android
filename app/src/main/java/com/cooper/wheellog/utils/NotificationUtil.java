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

import java.util.Timer;
import java.util.TimerTask;


public class NotificationUtil {
    private NotificationUtil mInstance;
    private Context mContext;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private int notificationMessageId = R.string.disconnected;
    private double mSpeed = 0;
    private int mBatteryLevel = 0;
    private double mDistance = 0.0;
    private double mPower = 0.0;
    private double mCurrent = 0.0;
    private int mTemperature = 0;
    private double mMaxSpeed = 0.0;
    private double mAvgSpeed = 0.0;
    public static double MaxCurrent = 0.0;
    public static double MaxPower = 0.0;
    private double mControlPower = 0.0;
    private double mControlPowerTw = 0.0;
    private int mControlPowerTwTest = 0;
    private double mControlDistance = 0.0;
    private int alarmExec = 0;
    private double mVoltage = 0.0;
    private boolean alarmTriggered = false;

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

    new Timer().scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run()
        {
            if (mPower == mControlPower && mPower == mControlPowerTw && mPower > 0 && mDistance > 0 && mDistance == mControlDistance && mSpeed > 0) {
                toggleReconnectWheel();
            }
            mControlPower = mPower;
            mControlDistance = mDistance;

            if (mControlPowerTwTest > 1)
            {
                mControlPowerTwTest = 0;
                mControlPowerTw = mPower;
            }
            mControlPowerTwTest++;
        }
    }, 12000, 12000);
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
                    double power = WheelData.getInstance().getPowerDouble();
                    double current = WheelData.getInstance().getCurrentDouble();
                    double voltage = WheelData.getInstance().getVoltageDouble();
                    double distance = (double) WheelData.getInstance().getDistanceDouble();
                    double speed = (double) Math.round(WheelData.getInstance().getSpeedDouble() * 10) / 10;
                    double maxspeed = (double) Math.round(WheelData.getInstance().getTopSpeedDouble() * 10) / 10;
                    double avgspeed = (double) Math.round(WheelData.getInstance().getAverageRidingSpeedDouble() * 10) / 10;


                    if (mBatteryLevel != batteryLevel ||
                            mDistance != distance ||
                            mSpeed != speed ||
                            mTemperature != temperature || mPower != power || mMaxSpeed != maxspeed || mAvgSpeed != avgspeed) {
                        mSpeed = speed;
                        mBatteryLevel = batteryLevel;
                        mTemperature = temperature;
                        mDistance = distance;
                        mPower = power;
                        mCurrent = current;
                        mVoltage = voltage;
                        mMaxSpeed = maxspeed;
                        mAvgSpeed = avgspeed;
                             if (MainActivity.ButtonMiBand > 0)
                                updateNotification();
                             if (mCurrent > MaxCurrent)
                                 MaxCurrent = mCurrent;
                             if (mPower > MaxPower)
                                 MaxPower = mPower;
                    }
                    break;
         	case Constants.ACTION_PEBBLE_SERVICE_TOGGLED:
                    updateNotification();
                    break;
                case Constants.ACTION_LOGGING_SERVICE_TOGGLED:
                    updateNotification();
                    break;

               case Constants.ACTION_REQUEST_SWMIBAND:
                    updateNotification();
                    break;

                case Constants.ACTION_ALARM_TRIGGERED:
                    int alarmType = ((Constants.ALARM_TYPE) intent.getSerializableExtra(Constants.INTENT_EXTRA_ALARM_TYPE)).getValue();
                    alarmExec = alarmType;
                    alarmTriggered = true; // Чтобы перерисовать шторку
                    updateNotification();
                    alarmExec = 0; // Сброс значения, чтобы при другом апдейте показал не аларм
                    break;
            }
        }
    };
    public void toggleReconnectWheel() {
        mContext.sendBroadcast(new Intent(Constants.ACTION_REQUEST_RECONNECT));
    }

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
     //   notificationView.setOnClickPendingIntent(R.id.ib_watch,
     //           pendingWatchIntent);
     //   notificationView.setOnClickPendingIntent(R.id.ib_logging,
     //           pendingLoggingIntent);

        switch (mConnectionState) {
            case BluetoothLeService.STATE_CONNECTING:
                notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_wheel_new_r);
                break;
            case BluetoothLeService.STATE_CONNECTED:
                if (LoggingService.isInstanceCreated()) {
                    notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_log_new_g);
                } else
                    notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_log_new_r);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
           //     if (WheelLog.AppConfig.getUseRec())
          //      {
         //       notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_wheel_c);
         //       }
         //       else
         //           {
                        notificationView.setImageViewResource(R.id.ib_connection, R.drawable.ic_wheel_new);
        //            }
                break;
        }

        notificationView.setTextViewText(R.id.text_title, mContext.getString(R.string.app_name));

        String title = mContext.getString(notificationMessageId);

        if (MainActivity.ButtonMiBand > 0 && (mDistance + mTemperature + mBatteryLevel + mSpeed + mPower) > 0) {
            notificationView.setTextViewText(R.id.text_message, mContext.getString(R.string.notification_text_not, mSpeed, mBatteryLevel, mPower, mTemperature, mDistance));
        }
        if (MainActivity.ButtonMiBand == 0 && mConnectionState == BluetoothLeService.STATE_CONNECTED) {
            if (alarmTriggered) {
                notificationView.setTextViewText(R.id.text_message, mContext.getString(R.string.notification_text_alarm_not, mSpeed, mCurrent, mVoltage, mBatteryLevel, mTemperature));
            } else {
                notificationView.setTextViewText(R.id.text_message, "Alarm mode on MiBand");
            }
        }
        String titlenot = title;
        if (mConnectionState == BluetoothLeService.STATE_CONNECTED && (mDistance + mTemperature + mBatteryLevel + mSpeed + mPower) > 0 && MainActivity.ButtonMiBand > 0)
            notificationView.setTextViewText(R.id.text_title, title);

        if (mConnectionState == BluetoothLeService.STATE_CONNECTED && (mDistance + mTemperature + mBatteryLevel + mSpeed + mPower) > 0 && MainActivity.ButtonMiBand > 0)
            titlenot = WheelData.getInstance().getRideTimeString();
        if (mConnectionState == BluetoothLeService.STATE_CONNECTED && (mDistance + mTemperature + mBatteryLevel + mSpeed + mPower) > 0 && MainActivity.ButtonMiBand == 0)
            titlenot = mContext.getString(R.string.titlealarm_text);
        if (mConnectionState == BluetoothLeService.STATE_CONNECTING || mConnectionState == BluetoothLeService.STATE_CONNECTING)
            titlenot = title;
        //     if (PebbleService.isInstanceCreated())
        //         notificationView.setImageViewResource(R.id.ib_watch, R.drawable.ic_action_watch_orange);
        //      else
        //         notificationView.setImageViewResource(R.id.ib_watch, R.drawable.ic_action_watch_grey);

        //    if (LoggingService.isInstanceCreated())
        //       notificationView.setImageViewResource(R.id.ib_logging, R.drawable.ic_log_new_g);
        //   else
        //       notificationView.setImageViewResource(R.id.ib_logging, R.drawable.ic_log_new);


        switch (MainActivity.ButtonMiBand) {
            case 0: {
                pNotification = mNotification
                        .setSmallIcon(R.drawable.ic_wheel4)
                        .setContentIntent(pendingIntent)
                        .setContent(notificationView)
                        .setContentTitle(titlenot)
                        .setContentText(mContext.getString(R.string.notification_text_alarm, mSpeed, mCurrent, mVoltage, mBatteryLevel, mTemperature))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            }
            break;
            case 1: {
                pNotification = mNotification
                        .setSmallIcon(R.drawable.ic_wheel4)
                        .setContentIntent(pendingIntent)
                        .setContent(notificationView)
                        .setContentTitle(titlenot)
                        .setContentText(mContext.getString(R.string.notification_text_min, mSpeed, mBatteryLevel, mDistance))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            }
            break;
            case 2: {
                pNotification = mNotification
                        .setSmallIcon(R.drawable.ic_wheel4)
                        .setContentIntent(pendingIntent)
                        .setContent(notificationView)
                        .setContentTitle(titlenot)
                        .setContentText(mContext.getString(R.string.notification_text_med, mSpeed, mAvgSpeed, mBatteryLevel, mTemperature, mDistance))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            }
            break;
            case 3: {
                pNotification = mNotification
                        .setSmallIcon(R.drawable.ic_wheel4)
                        .setContentIntent(pendingIntent)
                        .setContent(notificationView)
                        .setContentTitle(titlenot)
                        .setContentText(mContext.getString(R.string.notification_text_max, mSpeed, mMaxSpeed, MaxPower, mBatteryLevel, mPower, mTemperature, mDistance))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            }
            break;
        }
        switch (alarmExec) {
            case 1:
                    {
                        pNotification = mNotification
                                .setSmallIcon(R.drawable.ic_wheel4)
                                .setContentIntent(pendingIntent)
                                .setContent(notificationView)
                                .setContentTitle(mContext.getString(R.string.titlealarm))
                                .setContentText(mContext.getString(R.string.alarm_text_speed_v, mSpeed))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .build();
                    }
            break;
            case 2:
            {
                pNotification = mNotification
                        .setSmallIcon(R.drawable.ic_wheel4)
                        .setContentIntent(pendingIntent)
                        .setContent(notificationView)
                        .setContentTitle(mContext.getString(R.string.titlealarm))
                        .setContentText(mContext.getString(R.string.alarm_text_speed_v, mSpeed))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            }
            break;
            case 3:
            {
                pNotification = mNotification
                        .setSmallIcon(R.drawable.ic_wheel4)
                        .setContentIntent(pendingIntent)
                        .setContent(notificationView)
                        .setContentTitle(mContext.getString(R.string.titlealarm))
                        .setContentText(mContext.getString(R.string.alarm_text_speed_v, mSpeed))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            }
            break;
            case 4:
            {
                pNotification = mNotification
                        .setSmallIcon(R.drawable.ic_wheel4)
                        .setContentIntent(pendingIntent)
                        .setContent(notificationView)
                        .setContentTitle(mContext.getString(R.string.titlealarm))
                        .setContentText(mContext.getString(R.string.alarm_text_current_v, mCurrent))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            }
            break;
            case 5:
            {
                pNotification = mNotification
                        .setSmallIcon(R.drawable.ic_wheel4)
                        .setContentIntent(pendingIntent)
                        .setContent(notificationView)
                        .setContentTitle(mContext.getString(R.string.titlealarm))
                        .setContentText(mContext.getString(R.string.alarm_text_temperature_v, mTemperature))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            }
            break;
        }
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
        intentFilter.addAction(Constants.ACTION_REQUEST_SWMIBAND);
        intentFilter.addAction(Constants.ACTION_REQUEST_RECONNECT);
        intentFilter.addAction(Constants.ACTION_ALARM_TRIGGERED);
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
