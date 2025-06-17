package com.example.proyectointegrador.history.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.Detail.DetailActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.history.adapter.ChallengeHistoryAdapter
import com.example.proyectointegrador.history.model.Challenge
import com.example.proyectointegrador.placeholder.PlaceholderContent
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ChallengeHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noHistoryText: TextView
    private lateinit var adapter: ChallengeHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_challenge_history, container, false)

        recyclerView = rootView.findViewById(R.id.history_recycler_view)
        progressBar = rootView.findViewById(R.id.history_progress_bar)
        noHistoryText = rootView.findViewById(R.id.no_history_text)

        adapter = ChallengeHistoryAdapter(emptyList()) { challenge ->
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("challengeId", challenge.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadChallengesFromFirestore()

        return rootView
    }

    private fun loadChallengesFromFirestore() {
        val db = Firebase.firestore
        val currentUser = Firebase.auth.currentUser

        if (currentUser == null) {
            updateUIWithChallenges(emptyList())
            return
        }

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noHistoryText.visibility = View.GONE

        PlaceholderContent.ITEMS.clear()
        PlaceholderContent.ITEM_MAP.clear()

        db.collection("users")
            .document(currentUser.uid)
            .collection("active_challenges")
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                val challenges = documents.mapNotNull { doc ->

                    // Agregar documento completo a PlaceholderContent

                    PlaceholderContent.addItemFromDocument(doc)

                    val id = doc.id
                    val title = doc.getString("title") ?: "Challenge without Title"
                    Challenge(id = id, title = title)
                }

                updateUIWithChallenges(challenges)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                noHistoryText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }


    private fun updateUIWithChallenges(challenges: List<Challenge>) {
        if (challenges.isEmpty()) {
            noHistoryText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            adapter.updateData(challenges)
            recyclerView.visibility = View.VISIBLE
            noHistoryText.visibility = View.GONE
        }
    }
}
