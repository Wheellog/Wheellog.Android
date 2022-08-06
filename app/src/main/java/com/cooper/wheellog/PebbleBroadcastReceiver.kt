package com.cooper.wheellog

import com.cooper.wheellog.utils.SomeUtil.playBeep
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import org.json.JSONException
import com.cooper.wheellog.utils.ACTION_PEBBLE_APP_READY
import com.cooper.wheellog.utils.ACTION_PEBBLE_APP_SCREEN
import com.cooper.wheellog.utils.INTENT_EXTRA_LAUNCHED_FROM_PEBBLE
import com.cooper.wheellog.utils.INTENT_EXTRA_PEBBLE_APP_VERSION
import com.cooper.wheellog.utils.INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN
import com.cooper.wheellog.utils.PEBBLE_APP_UUID
import com.cooper.wheellog.utils.PEBBLE_APP_VERSION
import com.cooper.wheellog.utils.PEBBLE_KEY_DISPLAYED_SCREEN
import com.cooper.wheellog.utils.PEBBLE_KEY_LAUNCH_APP
import com.cooper.wheellog.utils.PEBBLE_KEY_PLAY_HORN
import com.cooper.wheellog.utils.PEBBLE_KEY_READY
import com.getpebble.android.kit.Constants
import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID

class PebbleBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.INTENT_APP_RECEIVE) {
            val receivedUuid = intent.getSerializableExtra(Constants.APP_UUID) as UUID?
            // Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
            if (PEBBLE_APP_UUID != receivedUuid)
                return

            val transactionId = intent.getIntExtra(Constants.TRANSACTION_ID, -1)
            PebbleKit.sendAckToPebble(context, transactionId)
            val jsonData = intent.getStringExtra(Constants.MSG_DATA)

            val data: PebbleDictionary = try {
                PebbleDictionary.fromJson(jsonData)
            } catch (ex: JSONException) {
                return
            }

//            Toast.makeText(context,jsonData, Toast.LENGTH_SHORT).show();
            if (data.contains(PEBBLE_KEY_LAUNCH_APP) && !PebbleService.isServiceRunning(context)) {
                val mainActivityIntent = Intent(context.applicationContext, MainActivity::class.java)
                mainActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                mainActivityIntent.putExtra(INTENT_EXTRA_LAUNCHED_FROM_PEBBLE, true)
                context.applicationContext.startActivity(mainActivityIntent)
                val pebbleServiceIntent = Intent(context.applicationContext, PebbleService::class.java)
                context.startService(pebbleServiceIntent)
            } else if (data.contains(PEBBLE_KEY_READY)) {
                val watch_app_version = data.getInteger(PEBBLE_KEY_READY).toInt()
                if (watch_app_version < PEBBLE_APP_VERSION) sendPebbleAlert(
                    context,
                    "A newer version of the app is available. Please upgrade to make sure the app works as expected."
                )
                val pebbleReadyIntent = Intent(ACTION_PEBBLE_APP_READY)
                pebbleReadyIntent.putExtra(INTENT_EXTRA_PEBBLE_APP_VERSION, watch_app_version)
                context.sendBroadcast(pebbleReadyIntent)
            } else if (data.contains(PEBBLE_KEY_DISPLAYED_SCREEN)) {
                val displayed_screen = data.getInteger(PEBBLE_KEY_DISPLAYED_SCREEN).toInt()
                val pebbleScreenIntent = Intent(ACTION_PEBBLE_APP_SCREEN)
                pebbleScreenIntent.putExtra(INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN, displayed_screen)
                context.sendBroadcast(pebbleScreenIntent)
            } else if (data.contains(PEBBLE_KEY_PLAY_HORN)) {
                val horn_mode = WheelLog.AppConfig.hornMode
                playBeep(context, horn_mode == 1, false)
            }
        }
    }

    private fun sendPebbleAlert(context: Context, text: String) {
        // Push a notification
        val intent = Intent("com.getpebble.action.SEND_NOTIFICATION")
        val data = mapOf("title" to "WheelLog", "body" to text)
        val jsonData = JSONObject(data)
        val notificationData = JSONArray().put(jsonData).toString()

        intent.apply {
            putExtra("messageType", "PEBBLE_ALERT")
            putExtra("sender", "PebbleKit Android")
            putExtra("notificationData", notificationData)
        }

        context.sendBroadcast(intent)
    }
}