package com.example.proyectointegrador.profile.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyectointegrador.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileDetailFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_profile_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Botones
        view.findViewById<LinearLayout>(R.id.btn_edit_name).setOnClickListener {
            findNavController().navigate(R.id.nameFragment)
        }

        view.findViewById<LinearLayout>(R.id.btn_edit_email).setOnClickListener {
            findNavController().navigate(R.id.emailFragment)
        }

        view.findViewById<LinearLayout>(R.id.btn_edit_password).setOnClickListener {
            findNavController().navigate(R.id.passwordFragment)
        }

        // Obtener y mostrar puntos

        val pointsText = view.findViewById<TextView>(R.id.pointsText)
        val pointsProgress = view.findViewById<ProgressBar>(R.id.pointsProgress)
        val userId = auth.currentUser?.uid

        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val points = document.getLong("points") ?: 0
                    pointsText.text = points.toString()
                    pointsText.visibility = View.VISIBLE
                    pointsProgress.visibility = View.GONE
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error loading points", Toast.LENGTH_SHORT).show()
                    pointsText.text = "0"
                    pointsText.visibility = View.VISIBLE
                    pointsProgress.visibility = View.GONE
                }
        } else {
            pointsText.text = "0"
            pointsText.visibility = View.VISIBLE
            pointsProgress.visibility = View.GONE
        }
    }
}

