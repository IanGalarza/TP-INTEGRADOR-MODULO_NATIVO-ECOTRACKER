package com.example.proyectointegrador.ranking.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.ranking.adapters.RankingAdapter
import com.example.proyectointegrador.ranking.model.UserRanking
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RankingZonaFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val zoneList = mutableListOf<UserRanking>()
    private lateinit var progressBar: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ranking, container, false)

        recyclerView = view.findViewById(R.id.ranking_recycler)
        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RankingAdapter(zoneList)

        view.findViewById<TextView>(R.id.ranking_title)?.text = getString(R.string.top_zones)

        loadRankingPorZonas()

        return view
    }

    private fun loadRankingPorZonas() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val db = Firebase.firestore
        db.collectionGroup("active_challenges")
            .get()
            .addOnSuccessListener { challengesResult ->
                val cityPoints = mutableMapOf<String, Int>()
                for (challengeDoc in challengesResult) {
                    val tasks = challengeDoc.get("tasks") as? List<Map<String, Any>> ?: continue
                    for (task in tasks) {
                        val completed = task["completed"] as? Boolean ?: false
                        val points = (task["points"] as? Long)?.toInt() ?: 0
                        val location = task["location"] as? Map<String, Any>
                        val city = location?.get("city") as? String ?: "Unknown zone"
                        if (completed) {
                            cityPoints[city] = (cityPoints[city] ?: 0) + points
                        }
                    }
                }
                zoneList.clear()
                cityPoints.entries
                    .sortedByDescending { it.value }
                    .forEach { zoneList.add(UserRanking(it.key, it.value)) }

                recyclerView.adapter = RankingAdapter(zoneList)
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
    }
}