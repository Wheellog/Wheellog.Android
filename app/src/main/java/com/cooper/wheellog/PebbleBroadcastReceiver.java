package com.cooper.wheellog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cooper.wheellog.utils.KingsongAdapter;
import com.cooper.wheellog.utils.SomeUtil;
import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.cooper.wheellog.utils.Constants.ACTION_PEBBLE_APP_READY;
import static com.cooper.wheellog.utils.Constants.ACTION_PEBBLE_APP_SCREEN;
import static com.cooper.wheellog.utils.Constants.INTENT_EXTRA_LAUNCHED_FROM_PEBBLE;
import static com.cooper.wheellog.utils.Constants.INTENT_EXTRA_PEBBLE_APP_VERSION;
import static com.cooper.wheellog.utils.Constants.INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN;
import static com.cooper.wheellog.utils.Constants.PEBBLE_APP_UUID;
import static com.cooper.wheellog.utils.Constants.PEBBLE_KEY_DISPLAYED_SCREEN;
import static com.cooper.wheellog.utils.Constants.PEBBLE_KEY_LAUNCH_APP;
import static com.cooper.wheellog.utils.Constants.PEBBLE_KEY_PLAY_HORN;
import static com.cooper.wheellog.utils.Constants.PEBBLE_KEY_READY;

public class PebbleBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.INTENT_APP_RECEIVE)) {
            final UUID receivedUuid = (UUID) intent.getSerializableExtra(Constants.APP_UUID);
            // Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
            if (!PEBBLE_APP_UUID.equals(receivedUuid))
                return;

            final int transactionId = intent.getIntExtra(Constants.TRANSACTION_ID, -1);
            PebbleKit.sendAckToPebble(context, transactionId);

            final String jsonData = intent.getStringExtra(Constants.MSG_DATA);
            final PebbleDictionary data;

            try {
                data = PebbleDictionary.fromJson(jsonData);
            } catch (JSONException ex) {
                return;
            }

//            Toast.makeText(context,jsonData, Toast.LENGTH_SHORT).show();

            if (data.contains(PEBBLE_KEY_LAUNCH_APP) && !PebbleService.isInstanceCreated()) {
                Intent mainActivityIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mainActivityIntent.putExtra(INTENT_EXTRA_LAUNCHED_FROM_PEBBLE, true);
                context.getApplicationContext().startActivity(mainActivityIntent);
                Intent pebbleServiceIntent = new Intent(context.getApplicationContext(), PebbleService.class);
                context.startService(pebbleServiceIntent);
            } else if (data.contains(PEBBLE_KEY_READY)) {
                int watch_app_version = data.getInteger(PEBBLE_KEY_READY).intValue();
                if (watch_app_version < com.cooper.wheellog.utils.Constants.PEBBLE_APP_VERSION)
                    sendPebbleAlert(context, "A newer version of the app is available. Please upgrade to make sure the app works as expected.");
                Intent pebbleReadyIntent = new Intent(ACTION_PEBBLE_APP_READY);
                pebbleReadyIntent.putExtra(INTENT_EXTRA_PEBBLE_APP_VERSION, watch_app_version);
                context.sendBroadcast(pebbleReadyIntent);
            } else if (data.contains(PEBBLE_KEY_DISPLAYED_SCREEN)) {
                int displayed_screen = data.getInteger(PEBBLE_KEY_DISPLAYED_SCREEN).intValue();
                Intent pebbleScreenIntent = new Intent(ACTION_PEBBLE_APP_SCREEN);
                pebbleScreenIntent.putExtra(INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN, displayed_screen);
                context.sendBroadcast(pebbleScreenIntent);
            } else if (data.contains(PEBBLE_KEY_PLAY_HORN)) {
                int horn_mode = WheelLog.AppConfig.getHornMode();
                if (horn_mode == 1 && WheelData.getInstance().getWheelType() == com.cooper.wheellog.utils.Constants.WHEEL_TYPE.KINGSONG) {
                    KingsongAdapter.getInstance().horn();
                } else if (horn_mode == 2) {
                    SomeUtil.playSound(context, R.raw.bicycle_bell);
                }
            }
        }
    }


    private void sendPebbleAlert(Context context, final String text) {
        // Push a notification
        final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");
        final Map<String, String> data = new HashMap<>();
        data.put("title", "WheelLog");
        data.put("body", text);
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();
        i.putExtra("messageType", "PEBBLE_ALERT");
        i.putExtra("sender", "PebbleKit Android");
        i.putExtra("notificationData", notificationData);
        context.sendBroadcast(i);
    }

}