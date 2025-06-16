package com.example.proyectointegrador

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.history.HistoryActivity
import com.example.proyectointegrador.profile.ProfileActivity
import com.example.proyectointegrador.ranking.RankingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BottomNavigationBaseActivity : AppCompatActivity()  {
    open val currentMenuItemId: Int = R.id.action_home

    fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener(null)

        bottomNav?.selectedItemId = currentMenuItemId 

        bottomNav?.setOnItemSelectedListener { item -> 
            if (item.itemId == currentMenuItemId) {
                return@setOnItemSelectedListener true
            }
            when (item.itemId) {
                R.id.action_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    finish()
                }
                R.id.action_challenges -> {
                    startActivity(Intent(this, ChallengeDetailHostActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    finish()
                }
                R.id.action_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    finish()
                }
                R.id.action_ranking -> {
                    startActivity(Intent(this, RankingActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    finish()
                }
                R.id.action_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                    finish()
                }
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }


}