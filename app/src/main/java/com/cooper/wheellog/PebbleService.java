package com.cooper.wheellog;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.PEBBLE_APP_SCREEN;
import com.cooper.wheellog.utils.NotificationUtil;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Calendar;
import java.util.UUID;

import timber.log.Timber;

import static com.cooper.wheellog.utils.Constants.PEBBLE_APP_SCREEN.DETAILS;
import static com.cooper.wheellog.utils.Constants.PEBBLE_APP_SCREEN.GUI;
import static com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_ACK;
import static com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_NACK;

import androidx.core.content.ContextCompat;

import org.koin.java.KoinJavaComponent;

public class PebbleService extends Service {
    private final AppConfig appConfig = KoinJavaComponent.get(AppConfig.class);
    private static final UUID APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d");
    private static final int MESSAGE_TIMEOUT = 500; // milliseconds

    static final int KEY_SPEED = 0;
    static final int KEY_BATTERY = 1;
    static final int KEY_TEMPERATURE = 2;
    static final int KEY_FAN_STATE = 3;
    static final int KEY_BT_STATE = 4;
    static final int KEY_VIBE_ALERT = 5;
    static final int KEY_USE_MPH = 6;
    static final int KEY_MAX_SPEED = 7;
    static final int KEY_RIDE_TIME = 8;
    static final int KEY_DISTANCE = 9;
    static final int KEY_TOP_SPEED = 10;
    static final int KEY_READY = 11;
    static final int KEY_VOLTAGE  = 12;
    static final int KEY_CURRENT = 13;
    static final int KEY_PWM = 20;
    private final Handler mHandler = new Handler();
    private static PebbleService instance = null;
    private long last_message_send_time;
    PebbleDictionary outgoingDictionary = new PebbleDictionary();

    int lastSpeed = 0;
    int lastBattery = 0;
    int lastTemperature = 0;
    int lastFanStatus = 0;
    int lastRideTime = 0;
    int lastDistance = 0;
    int lastTopSpeed = 0;
    int lastVoltage = 0;
    int lastCurrent = 0;
    int lastPWM = 0;

    boolean lastConnectionState = false;
    int vibe_alarm = -1;
    boolean refreshAll = true;
    PEBBLE_APP_SCREEN displayedScreen = GUI;
    boolean ready = false;

    boolean message_pending = false;
    boolean data_available = false;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private final Runnable mSendPebbleData = new Runnable() {
        @Override
        public void run() {

            if (!ready) {
                outgoingDictionary.addInt32(KEY_READY, 0);
                ready = true;
            }

            if (refreshAll) {
                outgoingDictionary.addInt32(KEY_USE_MPH, appConfig.getUseMph() ? 1 : 0);
                outgoingDictionary.addInt32(KEY_MAX_SPEED, appConfig.getMaxSpeed());
            }

            WheelData data = WheelData.getInstance();
            if (data == null) {
                return;
            }

            switch (displayedScreen) {
                case GUI:
                    if (refreshAll || lastSpeed != data.getSpeed())
                    {
                        lastSpeed = data.getSpeed();
                        outgoingDictionary.addInt32(KEY_SPEED, lastSpeed);
                    }

                    if (refreshAll || lastBattery != data.getBatteryLevel())
                    {
                        lastBattery = data.getBatteryLevel();
                        outgoingDictionary.addInt32(KEY_BATTERY, lastBattery);
                    }

                    if (refreshAll || lastTemperature != data.getTemperature())
                    {
                        lastTemperature = data.getTemperature();
                        outgoingDictionary.addInt32(KEY_TEMPERATURE, lastTemperature);
                    }

                    if (refreshAll || lastFanStatus != data.getFanStatus())
                    {
                        lastFanStatus = data.getFanStatus();
                        outgoingDictionary.addInt32(KEY_FAN_STATE, lastFanStatus);
                    }

                    if (refreshAll || lastConnectionState != data.isConnected())
                    {
                        lastConnectionState = data.isConnected();
                        outgoingDictionary.addInt32(KEY_BT_STATE, lastConnectionState ? 1 : 0);
                    }

                    if (refreshAll || lastVoltage != data.getVoltage())
                    {
                        lastVoltage = data.getVoltage();
                        outgoingDictionary.addInt32(KEY_VOLTAGE, lastVoltage);
                    }

                    if (refreshAll || lastCurrent != data.getCurrent())
                    {
                        lastCurrent = data.getCurrent();
                        outgoingDictionary.addInt32(KEY_CURRENT, lastCurrent);
                    }

                    if (refreshAll || lastPWM != (int) data.getCalculatedPwm())
                    {
                        lastPWM = (int) data.getCalculatedPwm();
                        outgoingDictionary.addInt32(KEY_PWM, lastCurrent);
                    }
                    break;
                case DETAILS:
                    if (refreshAll || lastRideTime != data.getRideTime())
                    {
                        lastRideTime = data.getRideTime();
                        outgoingDictionary.addInt32(KEY_RIDE_TIME, lastRideTime);
                    }

                    if (refreshAll || lastDistance != data.getDistance())
                    {
                        lastDistance = data.getDistance();
                        outgoingDictionary.addInt32(KEY_DISTANCE, lastDistance/100);
                    }

                    if (refreshAll || lastTopSpeed != data.getTopSpeed())
                    {
                        lastTopSpeed = data.getTopSpeed();
                        outgoingDictionary.addInt32(KEY_TOP_SPEED, lastTopSpeed/10);
                    }
                    break;
            }

            if (vibe_alarm >= 0) {
                outgoingDictionary.addInt32(KEY_VIBE_ALERT, vibe_alarm);
                vibe_alarm = -1;
            }

            if (outgoingDictionary.size() > 0) {
                message_pending = true;
                PebbleKit.sendDataToPebble(getApplicationContext(), APP_UUID, outgoingDictionary);
            }
            last_message_send_time = Calendar.getInstance().getTimeInMillis();
            data_available = false;
            refreshAll = false;
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            switch (intent.getAction()) {
                case Constants.ACTION_ALARM_TRIGGERED:
                    if (intent.hasExtra(Constants.INTENT_EXTRA_ALARM_TYPE)) {
                        // crutch to legacy pebble app, which does't know alarms other than 0 (speed) and 1 (current)
                        vibe_alarm = 0;
                        switch ((Constants.ALARM_TYPE) intent.getSerializableExtra(Constants.INTENT_EXTRA_ALARM_TYPE)) {
                            case CURRENT:
                            case TEMPERATURE:
                                vibe_alarm = 1;
                                break;
                        }
                    }
                    break;
                case Constants.ACTION_PEBBLE_APP_READY:
                    displayedScreen = GUI;
                    refreshAll = true;
                    break;
                case Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED:
                    refreshAll = true;
                    break;
                case Constants.ACTION_PEBBLE_APP_SCREEN:
                    if (intent.hasExtra(Constants.INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN)) {
                        int screen = intent.getIntExtra(Constants.INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN, 0);

                        if (screen == 0)
                            displayedScreen = GUI;
                        else if (screen == 1)
                            displayedScreen = DETAILS;

                        refreshAll = true;
                    }
                    break;
            }

            // There's something new to send, start the check
            if (message_pending &&
                    last_message_send_time + MESSAGE_TIMEOUT >= Calendar.getInstance().getTimeInMillis())
                data_available = true;
            else
                mHandler.post(mSendPebbleData);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        ContextCompat.registerReceiver(
                this,
                ackReceiver,
                new IntentFilter(INTENT_APP_RECEIVE_ACK),
                ContextCompat.RECEIVER_EXPORTED
        );
        ContextCompat.registerReceiver(
                this,
                nackReceiver,
                new IntentFilter(INTENT_APP_RECEIVE_NACK),
                ContextCompat.RECEIVER_EXPORTED
        );

        PebbleKit.startAppOnPebble(this, APP_UUID);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_ALARM_TRIGGERED);
        intentFilter.addAction(Constants.ACTION_PEBBLE_APP_READY);
        intentFilter.addAction(Constants.ACTION_PEBBLE_APP_SCREEN);
        intentFilter.addAction(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED);

        ContextCompat.registerReceiver(
                this,
                mBroadcastReceiver,
                intentFilter,
                ContextCompat.RECEIVER_EXPORTED
        );

        Intent serviceStartedIntent = new Intent(Constants.ACTION_PEBBLE_SERVICE_TOGGLED)
                .putExtra(Constants.INTENT_EXTRA_IS_RUNNING, true);
        sendBroadcast(serviceStartedIntent);
        mHandler.post(mSendPebbleData);

        final NotificationUtil notifications = KoinJavaComponent.get(NotificationUtil.class);
        notifications.update();
        startForeground(Constants.MAIN_NOTIFICATION_ID, notifications.getNotification());
        Timber.d("PebbleConnectivity Started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(mBroadcastReceiver);
            unregisterReceiver(ackReceiver);
            unregisterReceiver(nackReceiver);
        } catch (Exception exception) {
            // ignored
        }
        mHandler.removeCallbacksAndMessages(null);

        instance = null;
        PebbleKit.closeAppOnPebble(this, APP_UUID);

        Intent serviceStartedIntent = new Intent(Constants.ACTION_PEBBLE_SERVICE_TOGGLED);
        serviceStartedIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
        sendBroadcast(serviceStartedIntent);
        stopForeground(false);
        Timber.i("PebbleConnectivity Stopped");
    }

    private final PebbleKit.PebbleAckReceiver ackReceiver = new PebbleKit.PebbleAckReceiver(APP_UUID) {
        @Override
        public void receiveAck(Context context, int transactionId) {
            outgoingDictionary = new PebbleDictionary();

            if (data_available)
                mHandler.post(mSendPebbleData);
            else
                message_pending = false;}
    };

    private final PebbleKit.PebbleNackReceiver nackReceiver = new PebbleKit.PebbleNackReceiver(APP_UUID) {
        @Override
        public void receiveNack(Context context, int transactionId) {
            mHandler.post(mSendPebbleData);
        }
    };
}
