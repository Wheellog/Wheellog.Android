package com.cooper.wheellog;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

import com.cooper.wheellog.utils.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Calendar;
import java.util.UUID;

import timber.log.Timber;

public class PebbleService extends Service {

    private static final UUID APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d");
    private static final int MESSAGE_TIMEOUT = 1000;

    static final int KEY_SPEED = 0;
    static final int KEY_BATTERY = 1;
    static final int KEY_TEMPERATURE = 2;
    static final int KEY_FAN_STATE = 3;
    static final int KEY_BT_STATE = 4;

    private Handler mHandler = new Handler();
    private static PebbleService instance = null;
    private long last_message_send_time;

    int lastSpeed = 0;
    int lastBattery = 0;
    int lastTemperature = 0;
    int lastFanStatus = 0;
    boolean lastConnectionState = false;
    boolean message_pending = false;
    boolean data_available = false;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private Runnable mSendPebbleData = new Runnable() {
        @Override
        public void run() {
            PebbleDictionary outgoing = new PebbleDictionary();

            if (lastSpeed != WheelData.getInstance().getSpeed())
            {
                lastSpeed = WheelData.getInstance().getSpeed();
                outgoing.addInt32(KEY_SPEED, lastSpeed);
            }

            if (lastBattery != WheelData.getInstance().getBatteryLevel())
            {
                lastBattery = WheelData.getInstance().getBatteryLevel();
                outgoing.addInt32(KEY_BATTERY, lastBattery);
            }

            if (lastTemperature != WheelData.getInstance().getTemperature())
            {
                lastTemperature = WheelData.getInstance().getTemperature();
                outgoing.addInt32(KEY_TEMPERATURE, lastTemperature);
            }

            if (lastFanStatus != WheelData.getInstance().getFanStatus())
            {
                lastFanStatus = WheelData.getInstance().getFanStatus();
                outgoing.addInt32(KEY_FAN_STATE, lastFanStatus);
            }

            if (lastConnectionState != WheelData.getInstance().isConnected())
            {
                lastConnectionState = !lastConnectionState;
                outgoing.addInt32(KEY_BT_STATE, lastConnectionState ? 1 : 0);
            }

            if (outgoing.size() > 0)
            {
                message_pending = true;
                PebbleKit.sendDataToPebble(getApplicationContext(), APP_UUID, outgoing);
            }
            last_message_send_time = Calendar.getInstance().getTimeInMillis();
            data_available = false;
        }
    };

    private final BroadcastReceiver mBreadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (message_pending && last_message_send_time + MESSAGE_TIMEOUT >= Calendar.getInstance().getTimeInMillis())
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
        PebbleKit.registerReceivedAckHandler(this, ackReceiver);
        PebbleKit.registerReceivedNackHandler(this, nackReceiver);

        PebbleKit.startAppOnPebble(this, APP_UUID);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        registerReceiver(mBreadcastReceiver, intentFilter);

        Intent serviceStartedIntent = new Intent(Constants.ACTION_PEBBLE_SERVICE_TOGGLED)
                .putExtra(Constants.INTENT_EXTRA_IS_RUNNING, true);
        sendBroadcast(serviceStartedIntent);
        mHandler.post(mSendPebbleData);

        Timber.d("PebbleConnectivity Started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBreadcastReceiver);
        unregisterReceiver(ackReceiver);
        unregisterReceiver(nackReceiver);
        mHandler.removeCallbacksAndMessages(null);

        instance = null;
        PebbleKit.closeAppOnPebble(this, APP_UUID);

        Intent serviceStartedIntent = new Intent(Constants.ACTION_PEBBLE_SERVICE_TOGGLED);
        serviceStartedIntent.putExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
        sendBroadcast(serviceStartedIntent);

        Timber.i("PebbleConnectivity Stopped");
    }

    private PebbleKit.PebbleAckReceiver ackReceiver = new PebbleKit.PebbleAckReceiver(APP_UUID) {
        @Override
        public void receiveAck(Context context, int transactionId) {
            if (data_available)
                mHandler.post(mSendPebbleData);
            else
                message_pending = false;
        }
    };

    private PebbleKit.PebbleNackReceiver nackReceiver = new PebbleKit.PebbleNackReceiver(APP_UUID) {
        @Override
        public void receiveNack(Context context, int transactionId) {
            lastSpeed = -1;
            lastBattery = -1;
            lastTemperature = -1;
            lastConnectionState = false;
            lastFanStatus = -1;
            mHandler.post(mSendPebbleData);
        }
    };
}
