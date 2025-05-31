package com.example.proyectointegrador.auth.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyectointegrador.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterFragment : Fragment() {

    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextConfirmPassword: TextInputEditText
    private lateinit var buttonRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewLoginNow: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editTextEmail = view.findViewById(R.id.email)
        editTextPassword = view.findViewById(R.id.password)
        editTextConfirmPassword = view.findViewById(R.id.confirmPassword)
        buttonRegister = view.findViewById(R.id.btn_register)
        progressBar = view.findViewById(R.id.progressBar)
        textViewLoginNow = view.findViewById(R.id.loginNow)

        textViewLoginNow.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        buttonRegister.setOnClickListener {
            progressBar.visibility = View.VISIBLE

            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()
            val confirmPassword = editTextConfirmPassword.text.toString()

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

            if (confirmPassword.isEmpty()) {

                Toast.makeText(requireContext(), "Confirm your password", Toast.LENGTH_SHORT).show()

                progressBar.visibility = View.GONE

                return@setOnClickListener
            }

            if (password != confirmPassword) {

                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()

                progressBar.visibility = View.GONE

                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->

                    progressBar.visibility = View.GONE

                    if (task.isSuccessful) {

                        Toast.makeText(requireContext(), "Account created", Toast.LENGTH_SHORT).show()

                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    } else {
                        Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
