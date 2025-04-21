package com.cooper.wheellog

import android.content.Context
import android.content.Intent

fun broadcastData(context: Context) {
    if (AppConfig(context).broadcastEnabled) {
        val intent = Intent("com.cooper.wheellog.broadcast")

        val wheelData = WheelData.getInstance()
        // Add data
        intent.putExtra("speed", wheelData.speedDouble)
        intent.putExtra("pwm", wheelData.calculatedPwm)
        intent.putExtra("voltage", wheelData.voltageDouble)
        intent.putExtra("current", wheelData.currentDouble)
        intent.putExtra("phaseCurrent", wheelData.phaseCurrentDouble)

        intent.putExtra("power", wheelData.powerDouble)
        intent.putExtra("battery", wheelData.batteryLevel)

        intent.putExtra("temp1", wheelData.temperature)
        intent.putExtra("temp2", wheelData.temperature2)

        intent.putExtra("maxSpeed", wheelData.topSpeedDouble)
        intent.putExtra("maxPwm", wheelData.maxPwm)
        intent.putExtra("maxPower", wheelData.maxPowerDouble)
        intent.putExtra("maxTemp", wheelData.maxTemp)

        intent.putExtra("distance", wheelData.distanceDouble)
        intent.putExtra("totalDistance", wheelData.totalDistanceDouble)

        context.sendBroadcast(intent)

    }
}
