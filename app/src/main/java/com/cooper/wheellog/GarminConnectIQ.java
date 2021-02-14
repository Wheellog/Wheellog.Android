package com.cooper.wheellog;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.KingsongAdapter;

import com.cooper.wheellog.utils.NotificationUtil;
import com.cooper.wheellog.utils.SomeUtil;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.ConnectIQListener;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationInfoListener;
import com.garmin.android.connectiq.ConnectIQ.IQConnectType;
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQMessageStatus;
import com.garmin.android.connectiq.ConnectIQ.IQSdkErrorStatus;
import com.garmin.android.connectiq.ConnectIQ.IQSendMessageListener;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.garmin.monkeybrains.serialization.MonkeyHash;

import org.json.JSONException;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;

public class GarminConnectIQ extends Service implements IQApplicationInfoListener, IQDeviceEventListener, IQApplicationEventListener, ConnectIQListener {
    public static final String TAG = GarminConnectIQ.class.getSimpleName();
    public static final String APP_ID = "df8bf0ab-1828-4037-a328-ee86d29d0501";
    private Notification mNotification;
    // This will require Garmin Connect V4.22
    // https://forums.garmin.com/developer/connect-iq/i/bug-reports/connect-version-4-20-broke-local-http-access
    public static final boolean FEATURE_FLAG_NANOHTTPD = true;

    public enum MessageType {
        EUC_DATA,
        PLAY_HORN,
        HTTP_READY,
    }

    public static final int MESSAGE_KEY_MSG_TYPE     = -2;
    public static final int MESSAGE_KEY_MSG_DATA     = -1;
    public static final int MESSAGE_KEY_SPEED        = 0;
    public static final int MESSAGE_KEY_BATTERY      = 1;
    public static final int MESSAGE_KEY_TEMPERATURE  = 2;
    public static final int MESSAGE_KEY_FAN_STATE    = 3;
    public static final int MESSAGE_KEY_BT_STATE     = 4;
    public static final int MESSAGE_KEY_VIBE_ALERT   = 5;
    public static final int MESSAGE_KEY_USE_MPH      = 6;
    public static final int MESSAGE_KEY_MAX_SPEED    = 7;
    public static final int MESSAGE_KEY_RIDE_TIME    = 8;
    public static final int MESSAGE_KEY_DISTANCE     = 9;
    public static final int MESSAGE_KEY_TOP_SPEED    = 10;
    public static final int MESSAGE_KEY_READY        = 11;
    public static final int MESSAGE_KEY_POWER        = 12;
    public static final int MESSAGE_KEY_ALARM1_SPEED = 13;
    public static final int MESSAGE_KEY_ALARM2_SPEED = 14;
    public static final int MESSAGE_KEY_ALARM3_SPEED = 15;

    public static final int MESSAGE_KEY_HTTP_PORT    = 99;

    int lastSpeed = 0;
    int lastBattery = 0;
    int lastTemperature = 0;
    int lastFanStatus = 0;
    int lastRideTime = 0;
    int lastDistance = 0;
    int lastTopSpeed = 0;
    boolean lastConnectionState = false;
    int lastPower = 0;

    private Timer keepAliveTimer;
    final Handler handler = new Handler(); // to call toast from the TimerTask

    private static GarminConnectIQ instance = null;

    private boolean mSdkReady;
    private ConnectIQ mConnectIQ;
    private List<IQDevice> mDevices;
    private IQDevice mDevice;
    private IQApp mMyApp;

    private GarminConnectIQWebServer mWebServer;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    public static GarminConnectIQ instance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        super.onStartCommand(intent, flags, startId);

        instance = this;

        // Setup Connect IQ
        mMyApp = new IQApp(APP_ID);
        mConnectIQ = ConnectIQ.getInstance(this, IQConnectType.WIRELESS);
        mConnectIQ.initialize(this, true, this);
        startForeground(Constants.MAIN_NOTIFICATION_ID, NotificationUtil.getNotification());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();

        cancelRefreshTimer();

        try {
            mConnectIQ.unregisterAllForEvents();
            mConnectIQ.shutdown(this);
        } catch (InvalidStateException e) {
            // This is usually because the SDK was already shut down
            // so no worries.
        }

        stopWebServer();
        stopForeground(false);
        instance = null;
    }

    // General METHODS
    private void populateDeviceList() {
        Log.d(TAG,"populateDeviceList");

        try {
            mDevices = mConnectIQ.getKnownDevices();

            if (mDevices != null && !mDevices.isEmpty()) {
                mDevice = mDevices.get(0);
                registerWithDevice();
            }

        } catch (InvalidStateException e) {
            // This generally means you forgot to call initialize(), but since
            // we are in the callback for initialize(), this should never happen
        } catch (ServiceUnavailableException e) {
            // This will happen if for some reason your app was not able to connect
            // to the ConnectIQ service running within Garmin Connect Mobile.  This
            // could be because Garmin Connect Mobile is not installed or needs to
            // be upgraded.
            Toast.makeText(this, R.string.garmin_connectiq_service_unavailable_message, Toast.LENGTH_LONG).show();
        }
    }

    private void registerWithDevice() {
        Log.d(TAG,"registerWithDevice");

        if (mDevice != null && mSdkReady) {
            // Register for device status updates
            try {
                mConnectIQ.registerForDeviceEvents(mDevice, this);
            } catch (InvalidStateException e) {
                Log.wtf(TAG, "InvalidStateException:  We should not be here!");
            }

            // Register for application status updates
            try {
                mConnectIQ.getApplicationInfo(APP_ID, mDevice, this);
            } catch (InvalidStateException e1) {
                Log.d(TAG, "e1: " + e1.getMessage());
            } catch (ServiceUnavailableException e1) {
                Log.d(TAG, "e2: " + e1.getMessage());
            }

            // Register to receive messages from the device
            try {
                mConnectIQ.registerForAppEvents(mDevice, mMyApp, this);
            } catch (InvalidStateException e) {
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void unregisterWithDevice() {
        Log.d(TAG,"unregisterWithDevice");

        if (mDevice != null && mSdkReady) {
            // It is a good idea to unregister everything and shut things down to
            // release resources and prevent unwanted callbacks.
            try {
                mConnectIQ.unregisterForDeviceEvents(mDevice);

                if (mMyApp != null) {
                    mConnectIQ.unregisterForApplicationEvents(mDevice, mMyApp);
                }
            } catch (InvalidStateException e) {
            }
        }
    }

    private void cancelRefreshTimer() {
        if (keepAliveTimer != null) {
            keepAliveTimer.cancel();
            keepAliveTimer = null;
        }
    }

    private void startRefreshTimer() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        refreshData();
                    }
                });
            }
        };

        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 0, 1500); // 1.5cs
    }

    private void refreshData() {
        if (WheelData.getInstance() == null)
            return;

        try {
            HashMap<Object, Object> data = new HashMap<Object, Object>();

            lastSpeed = WheelData.getInstance().getSpeed() / 10;
            data.put(MESSAGE_KEY_SPEED, lastSpeed);

            lastBattery = WheelData.getInstance().getBatteryLevel();
            data.put(MESSAGE_KEY_BATTERY, lastBattery);

            lastTemperature = WheelData.getInstance().getTemperature();
            data.put(MESSAGE_KEY_TEMPERATURE, lastTemperature);

            lastFanStatus = WheelData.getInstance().getFanStatus();
            data.put(MESSAGE_KEY_FAN_STATE, lastFanStatus);

            lastConnectionState = WheelData.getInstance().isConnected();
            data.put(MESSAGE_KEY_BT_STATE, lastConnectionState);

            data.put(MESSAGE_KEY_VIBE_ALERT, false);
            data.put(MESSAGE_KEY_USE_MPH, WheelLog.AppConfig.getUseMph());
            data.put(MESSAGE_KEY_MAX_SPEED, WheelLog.AppConfig.getMaxSpeed());

            lastRideTime = WheelData.getInstance().getRideTime();
            data.put(MESSAGE_KEY_RIDE_TIME, lastRideTime);

            lastDistance = WheelData.getInstance().getDistance();
            data.put(MESSAGE_KEY_DISTANCE, lastDistance/100);

            lastTopSpeed = WheelData.getInstance().getTopSpeed();
            data.put(MESSAGE_KEY_TOP_SPEED, lastTopSpeed/10);

            lastPower = (int)WheelData.getInstance().getPowerDouble();
            data.put(MESSAGE_KEY_POWER, lastPower);

            HashMap<Object, Object> message = new HashMap<Object, Object>();
            message.put(MESSAGE_KEY_MSG_TYPE, MessageType.EUC_DATA.ordinal());
            message.put(MESSAGE_KEY_MSG_DATA, new MonkeyHash(data));

            try {
                mConnectIQ.sendMessage(mDevice, mMyApp, message, new IQSendMessageListener() {

                    @Override
                    public void onMessageStatus(IQDevice device, IQApp app, IQMessageStatus status) {
                        Log.d(TAG, "message status: " + status.name());

                        if (status.name() != "SUCCESS")
                            Toast.makeText(GarminConnectIQ.this, status.name(), Toast.LENGTH_LONG).show();
                    }

                });
            } catch (InvalidStateException e) {
                Log.e(TAG, "ConnectIQ is not in a valid state");
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show();
            } catch (ServiceUnavailableException e) {
                Log.e(TAG, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?");
                Toast.makeText(this, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?", Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            Log.e(TAG, "refreshData", ex);
            Toast.makeText(this, "Error refreshing data", Toast.LENGTH_SHORT).show();
        }
    }

    // IQApplicationInfoListener METHODS
    @Override
    public void onApplicationInfoReceived(IQApp app) {
        Log.d(TAG,"onApplicationInfoReceived");
        Log.d(TAG, app.toString());
    }

    @Override
    public void onApplicationNotInstalled(String arg0) {
        Log.d(TAG,"onApplicationNotInstalled");

        // The WheelLog app is not installed on the device so we have
        // to let the user know to install it.
        cancelRefreshTimer(); // no point in sending data...

        Toast.makeText(this, R.string.garmin_connectiq_missing_app_message, Toast.LENGTH_LONG).show();
        try {
            mConnectIQ.openStore(APP_ID);
        } catch (InvalidStateException e) {

        } catch (ServiceUnavailableException e) {

        }
    }

    // IQDeviceEventListener METHODS
    @Override
    public void onDeviceStatusChanged(IQDevice device, IQDeviceStatus status) {
        Log.d(TAG,"onDeviceStatusChanged");
        Log.d(TAG, "status is:" + status.name());

        switch(status.name()) {
            case "CONNECTED":
                // Disabled the push method for now until a dev from garmin can shed some light on the
                // intermittent FAILURE_DURING_TRANSFER that we have seen. This is documented here:
                // https://forums.garmin.com/developer/connect-iq/f/legacy-bug-reports/5144/failure_during_transfer-issue-again-now-using-comm-sample
                if (!FEATURE_FLAG_NANOHTTPD) {
                    startRefreshTimer();
                }

                // As a workaround, start a nanohttpd server that will listen for data requests from the watch. This is
                // also documented on the link above and is apparently a good workaround for the meantime. In our implementation
                // we instanciate the httpd server on an ephemeral port and send a message to the watch to tell it on which port
                // it can request its data.
                if (FEATURE_FLAG_NANOHTTPD) {
                    startWebServer();
                }
                break;
            case "NOT_PAIRED":
            case "NOT_CONNECTED":
            case "UNKNOWN":
                cancelRefreshTimer();
                stopWebServer();
        }
    }

    // IQApplicationEventListener
    @Override
    public void onMessageReceived(IQDevice device, IQApp app, List<Object> message, IQMessageStatus status) {
        Log.d(TAG,"onMessageReceived");

        // We know from our widget that it will only ever send us strings, but in case
        // we get something else, we are simply going to do a toString() on each object in the
        // message list.
        StringBuilder builder = new StringBuilder();

        if (message.size() > 0) {
            for (Object o : message) {
                if (o == null) {
                    builder.append("<null> received");
                } else if (o instanceof HashMap) {
                    try {
                        HashMap msg = (HashMap)o;
                        int msgType = (int)msg.get(MESSAGE_KEY_MSG_TYPE);
                        if (msgType == MessageType.PLAY_HORN.ordinal()) {
                            playHorn();
                        }
                        builder = null;
                    } catch (Exception ex) {
                        builder.append("MonkeyHash received:\n\n");
                        builder.append(o.toString());
                    }
                } else {
                    builder.append(o.toString());
                    builder.append("\r\n");
                }
            }
        } else {
            builder.append("Received an empty message from the ConnectIQ application");
        }

        if (builder != null) {
            Toast.makeText(getApplicationContext(), builder.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    // ConnectIQListener METHODS
    @Override
    public void onInitializeError(IQSdkErrorStatus errStatus) {
        Log.d(TAG,"sdk initialization error");
        mSdkReady = false;
    }

    @Override
    public void onSdkReady() {
        Log.d(TAG,"sdk is ready");
        mSdkReady = true;
        populateDeviceList();
    }

    @Override
    public void onSdkShutDown() {
        Log.d(TAG,"sdk shut down");
        mSdkReady = false;
    }

    public void playHorn() {
        Context context = getApplicationContext();

        int horn_mode = WheelLog.AppConfig.getHornMode();
        if (horn_mode == 1 && WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE.KINGSONG) {
            KingsongAdapter.getInstance().horn();
        } else if (horn_mode == 2) {
            SomeUtil.playSound(context, R.raw.bicycle_bell);
        }
    }

    public void startWebServer() {
        Log.d(TAG,"startWebServer");

        if (mWebServer != null)
            return;

        try {
            mWebServer = new GarminConnectIQWebServer();
            Log.d(TAG, "port is:" + mWebServer.getListeningPort());

            HashMap<Object, Object> data = new HashMap<Object, Object>();
            data.put(MESSAGE_KEY_HTTP_PORT, mWebServer.getListeningPort());

            HashMap<Object, Object> message = new HashMap<Object, Object>();
            message.put(MESSAGE_KEY_MSG_TYPE, MessageType.HTTP_READY.ordinal());
            message.put(MESSAGE_KEY_MSG_DATA, new MonkeyHash(data));

            try {
                mConnectIQ.sendMessage(mDevice, mMyApp, message, new IQSendMessageListener() {

                    @Override
                    public void onMessageStatus(IQDevice device, IQApp app, IQMessageStatus status) {
                        Log.d(TAG, "message status: " + status.name());

                        if (status.name() != "SUCCESS")
                            Toast.makeText(GarminConnectIQ.this, status.name(), Toast.LENGTH_LONG).show();
                    }

                });
            } catch (InvalidStateException e) {
                Log.e(TAG, "ConnectIQ is not in a valid state");
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_LONG).show();
            } catch (ServiceUnavailableException e) {
                Log.e(TAG, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?");
                Toast.makeText(this, "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?", Toast.LENGTH_LONG).show();
            }

        } catch (IOException e) {
        }
    }

    public void stopWebServer() {
        Log.d(TAG,"stopWebServer");
        if (mWebServer != null) {
            mWebServer.stop();
            mWebServer = null;
        }
    }
}

class GarminConnectIQWebServer extends NanoHTTPD {
    public GarminConnectIQWebServer()throws IOException {
        super("127.0.0.1", 0); // 0 to automatically find an available ephemeral port
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        if (session.getMethod() == Method.GET && session.getUri().equals("/data")) {
            return handleData();
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
    }

    private Response handleData() {
        Log.d("GarminConnectIQWebSe...","handleData");
        JSONObject data = new JSONObject();

        try {
            data.put("" + GarminConnectIQ.MESSAGE_KEY_SPEED, WheelData.getInstance().getSpeed() / 10); // Convert to km/h
            data.put("" + GarminConnectIQ.MESSAGE_KEY_BATTERY, WheelData.getInstance().getBatteryLevel());
            data.put("" + GarminConnectIQ.MESSAGE_KEY_TEMPERATURE, WheelData.getInstance().getTemperature());
            data.put("" + GarminConnectIQ.MESSAGE_KEY_FAN_STATE, WheelData.getInstance().getFanStatus());
            data.put("" + GarminConnectIQ.MESSAGE_KEY_BT_STATE, WheelData.getInstance().isConnected());
            data.put("" + GarminConnectIQ.MESSAGE_KEY_VIBE_ALERT, false);
            data.put("" + GarminConnectIQ.MESSAGE_KEY_USE_MPH, WheelLog.AppConfig.getUseMph());
            data.put("" + GarminConnectIQ.MESSAGE_KEY_MAX_SPEED, WheelLog.AppConfig.getMaxSpeed());
            data.put("" + GarminConnectIQ.MESSAGE_KEY_RIDE_TIME, WheelData.getInstance().getRideTime());
            data.put("" + GarminConnectIQ.MESSAGE_KEY_DISTANCE, WheelData.getInstance().getDistance() / 100);
            data.put("" + GarminConnectIQ.MESSAGE_KEY_TOP_SPEED, WheelData.getInstance().getTopSpeed() / 10);
            data.put("" + GarminConnectIQ.MESSAGE_KEY_POWER, (int) WheelData.getInstance().getPowerDouble());
            if (WheelData.getInstance().getWheelType() == Constants.WHEEL_TYPE.KINGSONG) {
                data.put("" + GarminConnectIQ.MESSAGE_KEY_ALARM1_SPEED, WheelLog.AppConfig.getWheelKsAlarm1());
                data.put("" + GarminConnectIQ.MESSAGE_KEY_ALARM2_SPEED, WheelLog.AppConfig.getWheelKsAlarm2());
                data.put("" + GarminConnectIQ.MESSAGE_KEY_ALARM3_SPEED, WheelLog.AppConfig.getWheelKsAlarm3());
            }

            JSONObject message = new JSONObject();
            message.put("" + GarminConnectIQ.MESSAGE_KEY_MSG_TYPE, GarminConnectIQ.MessageType.EUC_DATA.ordinal());
            message.put("" + GarminConnectIQ.MESSAGE_KEY_MSG_DATA, data);

            return newFixedLengthResponse(Response.Status.OK, "application/json", message.toString());
        } catch (JSONException e) {
            return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, "text/plain", "");
        }
    }
}