package com.example.proyectointegrador

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectointegrador.auth.AuthActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var button: Button
    private lateinit var textView : TextView
    private var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        button = findViewById(R.id.logout)
        textView = findViewById(R.id.user_details)

        user = auth.currentUser

        val currentUser = user

        if (currentUser == null){

            val intent = Intent(this, AuthActivity::class.java)

            startActivity(intent)

            finish()
        }

        else {
            textView.text = currentUser.email
        }

        button.setOnClickListener {

            auth.signOut()

            val intent = Intent(this, AuthActivity::class.java)

            startActivity(intent)

            finish()

        }

    }
}