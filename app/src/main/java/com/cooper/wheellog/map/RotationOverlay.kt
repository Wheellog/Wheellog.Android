package com.cooper.wheellog.map

import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

class RotationOverlay(val map: MapView, val compassProvider: CompassOrientationProvider?): RotationGestureOverlay(map) {
    override fun onRotate(deltaAngle: Float) {
        super.onRotate(deltaAngle)
        compassProvider?.onChanged(-map.mapOrientation)
    }
}