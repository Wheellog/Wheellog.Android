package com.cooper.wheellog.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import timber.log.Timber

class MapFragment : Fragment() {
    lateinit var map: MapView
    private val viewModel: MapViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.map_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        map = view.findViewById(R.id.mapView)
        map.apply {
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            minZoomLevel = 10.0
            maxZoomLevel = 20.0
            setMultiTouchControls(true)
            overlays.add(RotationGestureOverlay(map)) // enable rotation
            overlays.add(ScaleBarOverlay(map)) // scale bar in top-left corner
            overlays.add(CompassOverlay(context, map).apply { enableCompass() })
            controller.apply {
                setZoom(10.0)
                setCenter(GeoPoint(WheelLog.AppConfig.lastLocationLaltitude, WheelLog.AppConfig.lastLocationLongitude))
            }
        }

        // set dark map tiles, if necessary
        if (WheelLog.AppConfig.dayNightThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }

        viewModel.selectedItem.observe(viewLifecycleOwner, { tripData ->
            if (tripData.geoLine == null) {
                // show error
                AlertDialog.Builder(requireContext())
                    .setTitle("Failed to open map.")
                    .setMessage(tripData.errorMessage)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()

                return@observe
            }

            // show map and line
            map.apply {
                isVisible = true
                val polyLine = Polyline(map, true).apply {
                    outlinePaint.apply {
                        color = context.getColorEx(R.color.accent)
                        isAntiAlias = true
                        strokeWidth = 15f
                    }
                    title = tripData.title
                    setOnClickListener { polyline, mapView, eventPos ->
                        try {
                            val pointOnLine = polyline.getCloseTo(eventPos, MathsUtil.dpToPx(context, 24).toDouble(), mapView)
                            val logGeoPoint = polyline.actualPoints.firstOrNull { p -> p.distanceToAsDouble(pointOnLine) < 1.0 } as LogGeoPoint?
                            if (logGeoPoint != null) {
                                polyline.apply {
                                    title = logGeoPoint.toString()
                                    infoWindowLocation = logGeoPoint
                                    showInfoWindow()
                                }
                            }
                        } catch (ex: java.lang.Exception) {
                            Timber.wtf(ex)
                        }
                        true
                    }
                }
                tripData.geoLine.forEach {
                    polyLine.addPoint(it)
                }
                overlays.add(polyLine)
                zoomToBoundingBox(polyLine.bounds, true, MathsUtil.dpToPx(context, 24))
            }
        })
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDetach()
    }
}