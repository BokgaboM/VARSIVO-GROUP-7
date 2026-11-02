package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.api.RetrofitClient
import com.example.varsivo.databinding.FragmentUniversityMapBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * Shows a university on an OpenStreetMap-based MapLibre map. The place name is turned into
 * coordinates with the free Nominatim geocoding REST API; if that fails the map falls back to
 * a view of South Africa.
 *
 * References:
 *  - MapLibre (2025) *MapLibre Native for Android* https://maplibre.org/maplibre-native/android/
 *  - OpenStreetMap Foundation (2025) *Nominatim* https://nominatim.org/
 */
class UniversityMapFragment : BaseFragment<FragmentUniversityMapBinding>() {

    private var map: MapLibreMap? = null
    private var styleLoaded = false
    private var resolvedLocation: LatLng? = null
    private var resolvedLabel: String = ""
    private var geocodingDone = false

    private val universityName: String
        get() = arguments?.getString(ARG_UNIVERSITY).orEmpty()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUniversityMapBinding {
        // MapLibre must be initialised BEFORE a MapView is inflated.
        MapLibre.getInstance(requireContext())
        return FragmentUniversityMapBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.subtitle =
            if (universityName.isNotBlank()) getString(R.string.locating, universityName)
            else getString(R.string.explore_university_locations)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { mapLibreMap ->
            map = mapLibreMap
            mapLibreMap.setStyle(STYLE_URL) {
                styleLoaded = true
                Log.d(TAG, "Map style loaded")
                showOnMap()
            }
        }

        if (universityName.isBlank()) {
            geocodingDone = true
        } else {
            geocode()
        }
    }

    /** Turns the university name into coordinates with the Nominatim REST API. */
    private fun geocode() {
        binding.mapStatusText.text = getString(R.string.finding_place, universityName)
        binding.mapStatusText.show()

        viewLifecycleOwner.lifecycleScope.launch {
            var message: String? = null
            try {
                val results = RetrofitClient.geocodingApi.search(query = "$universityName, South Africa")
                if (results.isNotEmpty()) {
                    val first = results.first()
                    resolvedLocation = LatLng(first.lat.toDouble(), first.lon.toDouble())
                    resolvedLabel = first.display_name
                    Log.i(TAG, "Geocoded $universityName -> ${first.lat}, ${first.lon}")
                } else {
                    message = getString(R.string.map_not_found, universityName)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Geocoding failed", e)
                message = getString(R.string.map_service_unreachable)
            }
            geocodingDone = true
            if (!hasView) return@launch
            if (message == null) binding.mapStatusText.hide() else {
                binding.mapStatusText.text = message
                binding.mapStatusText.show()
            }
            showOnMap()
        }
    }

    /** Moves the camera and drops a marker once BOTH the map style and the location are ready. */
    private fun showOnMap() {
        val mapLibreMap = map ?: return
        if (!styleLoaded || !geocodingDone || !hasView) return

        val target = resolvedLocation ?: DEFAULT_LOCATION
        mapLibreMap.cameraPosition = CameraPosition.Builder()
            .target(target)
            .zoom(if (resolvedLocation != null) 14.0 else 5.0)
            .build()
        mapLibreMap.clear()
        mapLibreMap.addMarker(
            MarkerOptions()
                .position(target)
                .title(resolvedLabel.ifBlank { getString(R.string.south_africa) })
        )
    }

    // ----- MapView needs the Fragment lifecycle forwarded to it -----

    override fun onStart() {
        super.onStart()
        if (hasView) binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (hasView) binding.mapView.onResume()
    }

    override fun onPause() {
        if (hasView) binding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        if (hasView) binding.mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (hasView) binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (hasView) binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        if (hasView) binding.mapView.onDestroy()
        map = null
        styleLoaded = false
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "UniversityMapFragment"
        const val ARG_UNIVERSITY = "universityName"

        private const val STYLE_URL = "https://demotiles.maplibre.org/style.json"

        /** Fallback: central South Africa, so the map still shows something sensible. */
        private val DEFAULT_LOCATION = LatLng(-28.4793, 24.6727)
    }
}
