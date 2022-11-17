package com.cooper.wheellog.map

import org.osmdroid.views.overlay.compass.IOrientationConsumer
import org.osmdroid.views.overlay.compass.IOrientationProvider

class CompassOrientationProvider : IOrientationProvider {
    private var mOrientationConsumer: IOrientationConsumer? = null
    private var mAzimuth = 0f

    override fun startOrientationProvider(orientationConsumer: IOrientationConsumer?): Boolean {
        mOrientationConsumer = orientationConsumer
        return true
    }

    override fun stopOrientationProvider() {
        mOrientationConsumer = null
    }

    override fun getLastKnownOrientation(): Float {
        return mAzimuth
    }

    override fun destroy() {
        stopOrientationProvider()
    }

    fun onChanged(azimuth: Float) {
        mAzimuth = azimuth
        mOrientationConsumer?.onOrientationChanged(mAzimuth,this)
    }
}