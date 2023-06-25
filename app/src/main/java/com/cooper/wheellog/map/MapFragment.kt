package com.cooper.wheellog.map

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.databinding.MapFragmentBinding
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.cooper.wheellog.utils.SomeUtil.getDrawableEx
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import timber.log.Timber

class MapFragment : Fragment() {
    lateinit var map: MapView
    private lateinit var binding: MapFragmentBinding
    private val viewModel: MapViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MapFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        map = binding.mapView
        map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            minZoomLevel = 8.0
            maxZoomLevel = 20.0
            setMultiTouchControls(true)
            val compassProvider = CompassOrientationProvider()
            overlays.add(RotationOverlay(map, compassProvider)) // enable rotation
            overlays.add(ScaleBarOverlay(map)) // scale bar in top-left corner
            overlays.add(CompassOverlay(context, compassProvider, map).apply {
                enableCompass()
            })
//            overlays.add(MyLocationNewOverlay(GpsMyLocationProvider(context), map).apply {
//                enableFollowLocation()
//                enableMyLocation()
//            })
//            overlays.add(MinimapOverlay(context, map.tileRequestCompleteHandler).apply {
//                zoomDifference = 3
//            })
            controller.apply {
                setZoom(10.0)
                setCenter(GeoPoint(WheelLog.AppConfig.lastLocationLaltitude, WheelLog.AppConfig.lastLocationLongitude))
            }
        }

        viewModel.selectedItem.observe(viewLifecycleOwner) { tripData ->
            if (tripData.geoLine.isEmpty()) {
                // show error
                AlertDialog.Builder(requireContext())
                    .setTitle("Failed to open map.")
                    .setMessage(tripData.errorMessage)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
            } else {
                drawMap(tripData)
            }
        }
    }

    private fun drawMap(tripData: TripData) {
        val polyLine = Polyline(map, true).apply {
            outlinePaint.apply {
                color = requireContext().getColorEx(R.color.accent)
                isAntiAlias = true
                strokeWidth = 15f
            }
            title = tripData.title
            setOnClickListener { polyline, mapView, eventPos ->
                try {
                    val pointOnLine = polyline.getCloseTo(eventPos, MathsUtil.dpToPx(requireContext(), 24).toDouble(), mapView)
                    if (pointOnLine != null) {
                        val logGeoPoint = polyline.actualPoints.firstOrNull { p ->
                            p.distanceToAsDouble(pointOnLine) < 1.0
                        } as LogGeoPoint?
                        if (logGeoPoint != null) {
                            polyline.apply {
                                snippet = getPointTitle(logGeoPoint)
                                infoWindowLocation = logGeoPoint
                                showInfoWindow()
                            }
                        }
                    }
                } catch (ex: java.lang.Exception) {
                    Timber.wtf(ex)
                }
                true
            }
        }

        if (tripData.geoLine.isEmpty()) {
            return
        }

        map.apply {
            isVisible = true
            tripData.geoLine.forEach {
                polyLine.addPoint(it)
            }
            overlays.add(polyLine)
            val startPoint = tripData.geoLine.first()

            Marker(this).apply {
                title = "Start"
                snippet = getPointTitle( startPoint)
                position = startPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = getDrawableEx(R.drawable.ic_start_marker)
                overlays.add(this)
            }

            val finishPoint = tripData.geoLine.last()
            Marker(this).apply {
                title = "Finish"
                snippet = getPointTitle(finishPoint)
                position = finishPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = getDrawableEx(R.drawable.ic_finish_marker)
                overlays.add(this)
            }
            try {
                if (tripData.geoLine.size > 100) {
                    val maxSpeedPoint = tripData.geoLine.maxByOrNull { it.speed }
                    if (maxSpeedPoint != null && maxSpeedPoint.speed > 20) {
                        Marker(this).apply {
                            title = "Max speed"
                            snippet = getPointTitle(maxSpeedPoint)
                            position = maxSpeedPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = getDrawableEx(R.drawable.ic_maxspeed_marker)
                            overlays.add(this)
                        }
                    }
                }
            } catch (ex: Exception) {
                Timber.wtf(ex.localizedMessage)
            }
            zoomToBoundingBox(polyLine.bounds, true, MathsUtil.dpToPx(context, 24))
        }
    }

    private fun getPointTitle(geoPoint: LogGeoPoint): String {
        return "\uD83D\uDE80 ${getString(R.string.speed)}: <b>${"%.2f".format(geoPoint.speed)}</b> ${getString(R.string.kmh)}<br/>" +
                "\uD83D\uDD0B ${getString(R.string.battery)}: <b>${geoPoint.battery}</b> %<br/>" +
                "⚡ ${getString(R.string.voltage)}: <b>${"%.2f".format(geoPoint.voltage)}</b> ${getString(R.string.volt)}<br/>" +
                "\uD83D\uDCCF ${getString(R.string.distance)}: <b>${"%.2f".format(geoPoint.distance / 1000.0)}</b> ${getString(R.string.km)}<br/>" +
                "\uD83C\uDF21 ${getString(R.string.temperature_title)}: <b>${geoPoint.temperature}</b> °C<br/><br/>" +
                "⌚ <b>${geoPoint.timeDate?.let { DateFormat.getTimeFormat(context).format(it) }}</b>"
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