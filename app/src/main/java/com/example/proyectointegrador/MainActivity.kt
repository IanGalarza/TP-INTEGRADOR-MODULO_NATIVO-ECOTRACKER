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
import com.example.proyectointegrador.auth.AuthActivity
import com.example.proyectointegrador.profile.ProfileActivity
import com.example.proyectointegrador.ranking.RankingActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeTextView: TextView
    private var user: FirebaseUser? = null
    private lateinit var challengesCard: CardView
    private lateinit var welcomeSpinner: ProgressBar
    private lateinit var activeChallengesSpinner: ProgressBar
    private lateinit var profileCard : CardView
    private lateinit var rankingCard: CardView

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

        challengesCard.setOnClickListener {
            val intent = Intent(this, ChallengeDetailHostActivity::class.java)
            startActivity(intent)
        }

        rankingCard.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
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
                    welcomeTextView.text = "Welcome, $name!"
                } else {
                    welcomeTextView.text = "Welcome!"
                }
            }
            .addOnFailureListener {
                welcomeSpinner.visibility = View.GONE
                welcomeTextView.text = "Welcome!"
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
                // startActivity(Intent(this, HistoryActivity::class.java))
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
        activeChallengesSpinner.visibility = View.VISIBLE
        val db = Firebase.firestore
        val container = findViewById<LinearLayout>(R.id.active_challenges_container)

        db.collection("users").document(uid).collection("active_challenges")
            .get()
            .addOnSuccessListener { documents ->
                container.removeAllViews()
                activeChallengesSpinner.visibility = View.GONE
                if (documents.isEmpty) {
                    val emptyView = TextView(this).apply {
                        text = "You have no active challenges."
                        setTextColor(resources.getColor(R.color.white, null))
                        textSize = 16f
                    }
                    container.addView(emptyView)
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val challengeTitle = doc.getString("title") ?: "Unnamed Challenge"
                    val challengeId = doc.id

                    val cardView = layoutInflater.inflate(R.layout.item_active_challenge, container, false)

                    val titleTextView = cardView.findViewById<TextView>(R.id.title)
                    titleTextView.text = challengeTitle

                    cardView.setOnClickListener {
                        val intent = Intent(this, ChallengeDetailHostActivity::class.java)
                        intent.putExtra("challengeId", challengeId)
                        startActivity(intent)
                    }
                    container.addView(cardView)
                }
            }
            .addOnFailureListener {
                container.removeAllViews()
                activeChallengesSpinner.visibility = View.GONE
                val errorView = TextView(this).apply {
                    text = "Error loading challenges."
                    setTextColor(resources.getColor(R.color.white, null))
                    textSize = 16f
                }
                container.addView(errorView)
            }
    }
}
