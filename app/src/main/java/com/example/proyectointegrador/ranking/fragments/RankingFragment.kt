package com.example.proyectointegrador.ranking.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.ranking.adapters.RankingAdapter
import com.example.proyectointegrador.ranking.model.UserRanking
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RankingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val userList = mutableListOf<UserRanking>()
    private lateinit var progressBar: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ranking, container, false)

        recyclerView = view.findViewById(R.id.ranking_recycler)
        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RankingAdapter(userList)

        loadRanking()

        return view
    }

    private fun loadRanking() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val db = Firebase.firestore
        db.collection("users")
            .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { result ->
                userList.clear()
                for (doc in result) {
                    val name = doc.getString("name") ?: "Sin nombre"
                    val points = doc.getLong("points")?.toInt() ?: 0
                    userList.add(UserRanking(name, points))
                }
                recyclerView.adapter = RankingAdapter(userList)
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
    }
}
