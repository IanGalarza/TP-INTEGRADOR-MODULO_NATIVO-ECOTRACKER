package com.example.proyectointegrador.auth.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyectointegrador.MainActivity
import com.example.proyectointegrador.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewRegisterNow: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editTextEmail = view.findViewById(R.id.email)
        editTextPassword = view.findViewById(R.id.password)
        buttonLogin = view.findViewById(R.id.btn_login)
        progressBar = view.findViewById(R.id.progressBar)
        textViewRegisterNow = view.findViewById(R.id.registerNow)

        textViewRegisterNow.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        buttonLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE

            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Email", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Password", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    progressBar.visibility = View.GONE

                    if (task.isSuccessful) {

                        Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()

                        val intent = Intent(requireContext(), MainActivity::class.java)

                        startActivity(intent)

                        requireActivity().finish()

                    } else {
                        Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
