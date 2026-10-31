package com.cooper.wheellog.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cooper.wheellog.track.TrackMapPanel

/** The original Map tab now uses the agreed viewer, reading the native CSV trip. */
class MapFragment : Fragment() {
    private val model: MapViewModel by activityViewModels()
    private var panel: TrackMapPanel? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        TrackMapPanel(requireContext()).also { panel = it }
    override fun onViewCreated(view: View, state: Bundle?) {
        model.selectedItem.observe(viewLifecycleOwner) { panel?.showTrip(it) }
    }
    override fun onResume() { super.onResume(); panel?.setPageActive(true) }
    override fun onPause() { panel?.setPageActive(false); super.onPause() }
    override fun onDestroyView() { panel?.dispose(); panel = null; super.onDestroyView() }
}
