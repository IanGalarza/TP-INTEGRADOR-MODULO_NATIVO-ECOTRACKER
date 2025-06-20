package com.example.proyectointegrador.ranking.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proyectointegrador.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HeatmapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var mapReady = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_heatmap, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return view
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (menuVisible && !mapReady) {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
            mapReady = true
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        cargarDatosYAgregarHeatmap()
    }

    private fun cargarDatosYAgregarHeatmap() {
        val db = Firebase.firestore
        db.collectionGroup("active_challenges")
            .get()
            .addOnSuccessListener { challengesResult ->
                val locationPoints = mutableMapOf<LatLng, Double>()

                for (challengeDoc in challengesResult) {
                    val tasks = challengeDoc.get("tasks") as? List<Map<String, Any>> ?: continue
                    val extraPoints = (challengeDoc.get("extraPoints") as? Long)?.toDouble() ?: 0.0

                    
                    val validTasks = tasks.filter { task ->
                        (task["completed"] as? Boolean == true) &&
                                (task["location"] as? Map<String, Any>)?.get("lat") != null &&
                                (task["location"] as? Map<String, Any>)?.get("lng") != null
                    }

                    
                    val pointsPerTask = if (validTasks.isNotEmpty()) extraPoints / validTasks.size else 0.0

                    for (task in validTasks) {
                        val location = task["location"] as? Map<String, Any> ?: continue
                        val lat = (location["lat"] as? Double)
                        val lng = (location["lng"] as? Double)
                        if (lat != null && lng != null) {
                            val latLng = LatLng(lat, lng)
                            val points = (task["points"] as? Long)?.toDouble() ?: 0.0
                            locationPoints[latLng] = (locationPoints[latLng] ?: 0.0) + points + pointsPerTask
                        }
                    }
                }

                
                val weightedLatLngs = locationPoints.map { (latLng, points) ->
                    com.google.maps.android.heatmaps.WeightedLatLng(latLng, points)
                }

                val argentina = LatLng(-34.6037, -58.3816)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(argentina, 4f))

                val provider = HeatmapTileProvider.Builder()
                    .weightedData(weightedLatLngs)
                    .radius(40)
                    .build()
                mMap.addTileOverlay(TileOverlayOptions().tileProvider(provider))
            }
    }
}