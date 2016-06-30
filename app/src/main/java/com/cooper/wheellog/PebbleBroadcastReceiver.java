package com.cooper.wheellog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;

import java.util.UUID;

public class PebbleBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.INTENT_APP_RECEIVE)) {
            final UUID receivedUuid = (UUID) intent.getSerializableExtra(Constants.APP_UUID);
            // Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
            if (!com.cooper.wheellog.Constants.PEBBLE_APP_UUID.equals(receivedUuid)) {
                return;
            }

            final int transactionId = intent.getIntExtra(Constants.TRANSACTION_ID, -1);
//            final String jsonData = intent.getStringExtra(Constants.MSG_DATA);
//            try {
//                final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
//                 do what you need with the data
//                PebbleKit.sendAckToPebble(context, transactionId);
//            } catch (JSONException e) {
//                Log.i("KEVTEST", "failed reived -> dict" + e);
//                return;
//            }

            PebbleKit.sendAckToPebble(context, transactionId);

            if (!PebbleConnectivity.isInstanceCreated()) {
                Intent pebbleServiceIntent = new Intent(context.getApplicationContext(), PebbleConnectivity.class);
                context.startService(pebbleServiceIntent);
            }

            Intent mainActivityIntent = new Intent(context.getApplicationContext(), MainActivity.class);
            mainActivityIntent.putExtra(com.cooper.wheellog.Constants.LAUNCHED_FROM_PEBBLE, true);
            context.startActivity(mainActivityIntent);



        }
    }

}