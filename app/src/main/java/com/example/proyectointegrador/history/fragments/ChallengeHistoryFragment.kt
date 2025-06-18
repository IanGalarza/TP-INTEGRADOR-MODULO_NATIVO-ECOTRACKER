package com.example.proyectointegrador.history.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
    private lateinit var searchEditText: EditText
    private lateinit var statusSpinner: Spinner
    private lateinit var adapter: ChallengeHistoryAdapter

    private var allChallenges: List<Challenge> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_challenge_history, container, false)

        recyclerView = rootView.findViewById(R.id.history_recycler_view)
        progressBar = rootView.findViewById(R.id.history_progress_bar)
        noHistoryText = rootView.findViewById(R.id.no_history_text)
        searchEditText = rootView.findViewById(R.id.challenge_search_edittext)
        statusSpinner = rootView.findViewById(R.id.status_filter_spinner)

        adapter = ChallengeHistoryAdapter(emptyList()) { challenge ->
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("challengeId", challenge.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        setupSearchListener()
        setupStatusFilter()
        loadChallengesFromFirestore()

        return rootView
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    applyFilters()
                }
                handler.postDelayed(searchRunnable!!, 500) // 500ms debounce
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupStatusFilter() {
        val statusOptions = listOf(
            getString(R.string.status_all),
            getString(R.string.status_active),
            getString(R.string.status_completed),
            getString(R.string.status_cancelled)
        )

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_selected_item,
            statusOptions
        ).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        statusSpinner.adapter = adapter

        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
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
                    PlaceholderContent.addItemFromDocument(doc)
                    val id = doc.id
                    val title = doc.getString("title") ?: "Challenge without Title"
                    val status = doc.getString("status") ?: "ACTIVE"
                    Challenge(id = id, title = title, status = status)
                }

                allChallenges = challenges
                applyFilters()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                noHistoryText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }

    private fun applyFilters() {
        val query = searchEditText.text.toString().trim().lowercase()
        val selectedStatus = statusSpinner.selectedItem.toString()

        val statusInEnglish = when (selectedStatus) {
            getString(R.string.status_active) -> "ACTIVE"
            getString(R.string.status_completed) -> "COMPLETED"
            getString(R.string.status_cancelled) -> "CANCELLED"
            else -> "ALL"
        }

        val filtered = allChallenges.filter { challenge ->
            val matchesTitle = challenge.title.lowercase().contains(query)
            val matchesStatus = statusInEnglish == "ALL" || challenge.status == statusInEnglish
            matchesTitle && matchesStatus
        }

        updateUIWithChallenges(filtered)
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
