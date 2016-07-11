package com.cooper.wheellog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.widget.Toast;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import org.json.JSONException;
import java.util.UUID;

public class PebbleBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.INTENT_APP_RECEIVE)) {
            final UUID receivedUuid = (UUID) intent.getSerializableExtra(Constants.APP_UUID);
            // Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
            if (!com.cooper.wheellog.Constants.PEBBLE_APP_UUID.equals(receivedUuid))
                return;

            final int transactionId = intent.getIntExtra(Constants.TRANSACTION_ID, -1);
            final String jsonData = intent.getStringExtra(Constants.MSG_DATA);
            try {
                final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
//                Toast.makeText(context,jsonData, Toast.LENGTH_SHORT).show();
                PebbleKit.sendAckToPebble(context, transactionId);
                if (data.contains(com.cooper.wheellog.Constants.PEBBLE_KEY_LAUNCH_APP)) {
                    if (!PebbleConnectivity.isInstanceCreated()) {
                        Intent pebbleServiceIntent = new Intent(context.getApplicationContext(), PebbleConnectivity.class);
                        context.startService(pebbleServiceIntent);
                    }

                    Intent mainActivityIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                    mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mainActivityIntent.putExtra(com.cooper.wheellog.Constants.LAUNCHED_FROM_PEBBLE, true);
                    context.getApplicationContext().startActivity(mainActivityIntent);
                } else if (data.contains(com.cooper.wheellog.Constants.PEBBLE_KEY_PLAY_HORN)) {
                    final Intent hornIntent = new Intent(com.cooper.wheellog.Constants.ACTION_REQUEST_HORN);
                    context.sendBroadcast(hornIntent);
                } else if (data.contains(com.cooper.wheellog.Constants.PEBBLE_KEY_PLAY_HORN_MP3)) {
                    MediaPlayer mp = MediaPlayer.create(context, R.raw.bicycle_bell);
                    mp.start();
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();
                        }
                    });
                }
            } catch (JSONException ignored) { }
        }
    }

}