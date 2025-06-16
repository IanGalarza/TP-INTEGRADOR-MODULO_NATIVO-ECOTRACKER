package com.example.proyectointegrador.Detail

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectointegrador.BottomNavigationBaseActivity
import com.example.proyectointegrador.ChallengeDetailFragment
import com.example.proyectointegrador.MainActivity
import com.example.proyectointegrador.ChallengeDetailHostActivity
import com.example.proyectointegrador.History.HistoryActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.auth.AuthActivity
import com.example.proyectointegrador.profile.ProfileActivity
import com.example.proyectointegrador.ranking.RankingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Locale

class DetailActivity : BottomNavigationBaseActivity() {
    override val currentMenuItemId: Int = R.id.action_challenges
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail)
        setupBottomNavigation()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            val challengeId = intent.getStringExtra("challengeId")

            val fragment = ChallengeDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ChallengeDetailFragment.ARG_ITEM_ID, challengeId)
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.detail_fragment_container, fragment)
                .commit()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_logout -> {
                Firebase.auth.signOut()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val langCode = prefs.getString("preferred_language", "en") ?: "en"
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}