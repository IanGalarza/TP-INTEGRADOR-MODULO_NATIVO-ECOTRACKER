package com.example.proyectointegrador

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectointegrador.auth.AuthActivity
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

        challengesCard.setOnClickListener {
            val intent = Intent(this, ChallengeDetailHostActivity::class.java)
            startActivity(intent)
        }

        if (user == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        } else {
            loadUserData(user!!.uid)
        }
    }

    private fun loadUserData(uid: String) {
        val db = Firebase.firestore
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    welcomeTextView.text = "Welcome, $name!"
                } else {
                    welcomeTextView.text = "Welcome!"
                }
            }
            .addOnFailureListener {
                welcomeTextView.text = "Welcome!"
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                true
            }
            R.id.action_challenges -> {
                startActivity(Intent(this, ChallengeDetailHostActivity::class.java))
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
}
