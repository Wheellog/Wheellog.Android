package com.cooper.wheellog.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.wearable.Wearable

class CommonUtils {
    companion object {
        const val messagePath = "/messages"

        fun vibrate(context: Context, vibrationPattern: LongArray) {
            val vibrator = context.getSystemService(FragmentActivity.VIBRATOR_SERVICE) as Vibrator
            val indexInPatternToRepeat = -1  //-1 - don't repeat
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat)
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
            }
        }

        fun sendMessage(context: Context, message: String) {
            Wearable.getNodeClient(context).connectedNodes
                .addOnSuccessListener {
                    it.forEach { node ->
                        if (node.isNearby) {
                            Wearable.getMessageClient(context)
                                .sendMessage(node.id, messagePath, message.toByteArray(Charsets.UTF_8))
                        }
                    }
                }
        }
    }
}