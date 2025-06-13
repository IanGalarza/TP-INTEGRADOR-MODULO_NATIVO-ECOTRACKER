package com.example.proyectointegrador

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.Detail.DetailActivity
import com.example.proyectointegrador.History.HistoryActivity
import com.example.proyectointegrador.adapter.ActiveChallengeAdapter
import com.example.proyectointegrador.auth.AuthActivity
import com.example.proyectointegrador.profile.ProfileActivity
import com.example.proyectointegrador.ranking.RankingActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.proyectointegrador.placeholder.PlaceholderContent


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeTextView: TextView
    private var user: FirebaseUser? = null
    private lateinit var challengesCard: CardView
    private lateinit var welcomeSpinner: ProgressBar
    private lateinit var activeChallengesSpinner: ProgressBar
    private lateinit var profileCard : CardView
    private lateinit var rankingCard: CardView
    private lateinit var historyCard: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        auth = Firebase.auth
        welcomeTextView = findViewById(R.id.user_welcome)
        user = auth.currentUser
        challengesCard = findViewById(R.id.challenges_card)
        welcomeSpinner = findViewById(R.id.welcome_spinner)
        activeChallengesSpinner = findViewById(R.id.active_challenges_spinner)
        profileCard = findViewById(R.id.profile_card)
        rankingCard = findViewById(R.id.ranking_card)
        historyCard = findViewById(R.id.history_card)

        challengesCard.setOnClickListener {
            val intent = Intent(this, ChallengeDetailHostActivity::class.java)
            startActivity(intent)
        }

        rankingCard.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            startActivity(intent)
        }

        historyCard.setOnClickListener{
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        profileCard.setOnClickListener{
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }


        if (user == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        } else {
            loadUserData(user!!.uid)
            loadActiveChallenges(user!!.uid)

        }
    }

    //Se utiliza para cargar el nombre para el mensaje de bienvenida

    private fun loadUserData(uid: String) {
        welcomeSpinner.visibility = View.VISIBLE

        val db = Firebase.firestore
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                welcomeSpinner.visibility = View.GONE
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    welcomeTextView.text = getString(R.string.welcome_user, name)
                } else {
                    welcomeTextView.text = getString(R.string.welcome)
                }
            }
            .addOnFailureListener {
                welcomeSpinner.visibility = View.GONE
                welcomeTextView.text = getString(R.string.welcome)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    //Menu desplegable en el toolbar

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                true
            }
            R.id.action_challenges -> {
                startActivity(Intent(this, ChallengeDetailHostActivity::class.java))
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_ranking -> {
                startActivity(Intent(this, RankingActivity::class.java))
                true
            }
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Se cargan los challenges que el usuario tiene activo

    private fun loadActiveChallenges(uid: String) {
        val db = Firebase.firestore
        val activeChallengesCard = findViewById<View>(R.id.active_challenges_card)
        val recyclerView = findViewById<RecyclerView>(R.id.active_challenges_recycler)
        val noActiveChallengesText = findViewById<TextView>(R.id.no_active_challenges_text)

        activeChallengesSpinner.visibility = View.VISIBLE
        activeChallengesCard.visibility = View.GONE
        noActiveChallengesText.visibility = View.GONE


        db.collection("users").document(uid).collection("active_challenges")
            .get()
            .addOnSuccessListener { documents ->
                activeChallengesSpinner.visibility = View.GONE
                if (documents.isEmpty) {
                    activeChallengesCard.visibility = View.GONE
                    noActiveChallengesText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                activeChallengesCard.visibility = View.VISIBLE
                noActiveChallengesText.visibility = View.GONE

                val challengeList = documents.map {
                    val title = it.getString("title") ?: "Unnamed Challenge"
                    val id = it.id
                    id to title
                }

                recyclerView.layoutManager = LinearLayoutManager(this)

                val adapter = ActiveChallengeAdapter(challengeList) { challengeId ->
                    PlaceholderContent.ITEMS.clear()
                    PlaceholderContent.ITEM_MAP.clear()

                    val doc = documents.firstOrNull { it.id == challengeId }
                    if (doc != null) {
                        PlaceholderContent.addItemFromDocument(doc)
                    }

                    val intent = Intent(this, DetailActivity::class.java)
                    intent.putExtra("challengeId", challengeId)
                    startActivity(intent)
                }

                recyclerView.adapter = adapter
                recyclerView.setHasFixedSize(true)
            }
            .addOnFailureListener {
                activeChallengesSpinner.visibility = View.GONE
                activeChallengesCard.visibility = View.GONE
                noActiveChallengesText.visibility = View.VISIBLE
                Toast.makeText(this, getString(R.string.error_loading_challenges), Toast.LENGTH_SHORT).show()
            }
    }
}

