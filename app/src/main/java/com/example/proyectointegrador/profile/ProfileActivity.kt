package com.example.proyectointegrador.profile

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectointegrador.R
import android.view.MenuItem
import com.example.proyectointegrador.BottomNavigationBaseActivity
import com.example.proyectointegrador.ChallengeDetailHostActivity
import com.example.proyectointegrador.History.HistoryActivity
import com.example.proyectointegrador.MainActivity
import com.example.proyectointegrador.auth.AuthActivity
import com.example.proyectointegrador.ranking.RankingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Locale


class ProfileActivity : BottomNavigationBaseActivity() {
    override val currentMenuItemId: Int = R.id.action_profile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        setupBottomNavigation()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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