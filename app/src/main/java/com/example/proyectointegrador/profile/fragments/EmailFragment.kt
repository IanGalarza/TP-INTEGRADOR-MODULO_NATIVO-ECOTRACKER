package com.example.proyectointegrador.profile.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.example.proyectointegrador.R
import com.example.proyectointegrador.auth.AuthActivity
import com.example.proyectointegrador.databinding.FragmentEmailBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class EmailFragment : Fragment() {

    private var _binding: FragmentEmailBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun clearErrorOnTyping(editText: android.widget.EditText, layout: TextInputLayout) {
        editText.doOnTextChanged { _, _, _, _ ->
            layout.error = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clearErrorOnTyping(binding.email, binding.emailInputLayout)

        val user = auth.currentUser ?: return

        binding.email.setText(user.email ?: "")

        // Validaciones

        binding.saveButton.setOnClickListener {
            val newEmail = binding.email.text.toString().trim()
            binding.emailInputLayout.error = null

            if (newEmail.isEmpty()) {
                binding.emailInputLayout.error = "Email cannot be empty"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                binding.emailInputLayout.error = "Invalid email format"
                return@setOnClickListener
            }

            //Se envia un mail de verificacion y se desloguea al usuario.

            user.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "A verification email has been sent to $newEmail. Please check it to confirm the change.",
                        Toast.LENGTH_LONG
                    ).show()

                    auth.signOut()

                    // Ir a AuthActivity
                    val intent = Intent(requireContext(), AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to send verification email: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

