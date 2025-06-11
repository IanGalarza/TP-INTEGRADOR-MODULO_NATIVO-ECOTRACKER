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
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Patterns
import androidx.core.widget.doOnTextChanged


class RegisterFragment : Fragment() {

    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var editTextName: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextConfirmPassword: TextInputEditText
    private lateinit var buttonRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewLoginNow: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    private fun clearErrorOnTyping(editText: EditText, layout: TextInputLayout) {
        editText.doOnTextChanged { _, _, _, _ ->
            layout.error = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")

        editTextName = view.findViewById(R.id.name)
        editTextEmail = view.findViewById(R.id.email)
        editTextPassword = view.findViewById(R.id.password)
        editTextConfirmPassword = view.findViewById(R.id.confirmPassword)
        buttonRegister = view.findViewById(R.id.btn_register)
        progressBar = view.findViewById(R.id.progressBar)
        textViewLoginNow = view.findViewById(R.id.loginNow)
        nameLayout = view.findViewById(R.id.nameLayout)
        emailLayout = view.findViewById(R.id.emailLayout)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout)

        clearErrorOnTyping(editTextName, nameLayout)
        clearErrorOnTyping(editTextEmail, emailLayout)
        clearErrorOnTyping(editTextPassword, passwordLayout)
        clearErrorOnTyping(editTextConfirmPassword, confirmPasswordLayout)

        textViewLoginNow.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        buttonRegister.setOnClickListener {

            nameLayout.error = null
            emailLayout.error = null
            passwordLayout.error = null
            confirmPasswordLayout.error = null

            val name = editTextName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString()
            val confirmPassword = editTextConfirmPassword.text.toString()

            var isValid = true

            if (name.isEmpty()) {
                nameLayout.error = "Please enter your full name"
                isValid = false
            }

            if (email.isEmpty()) {
                emailLayout.error = "Please enter your email"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Please enter a valid email address"
                isValid = false
            }

            if (password.isEmpty()) {
                passwordLayout.error = "Please enter your password"
                isValid = false
            } else if (!passwordRegex.matches(password)) {
                passwordLayout.error = "Password must be at least 8 characters and include a lowercase, uppercase letter, and a number"
                isValid = false
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordLayout.error = "Please confirm your password"
                isValid = false
            } else if (password != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"
                isValid = false
            }

            // Si hay errores, no continuar

            if (!isValid) return@setOnClickListener


            progressBar.visibility = View.VISIBLE

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->

                    progressBar.visibility = View.GONE

                    if (task.isSuccessful) {

                        val userId = auth.currentUser?.uid ?: ""

                        val userMap = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "points" to 0
                        )
                        firestore.collection("users")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Account created", Toast.LENGTH_SHORT).show()

                                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error saving user info: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
