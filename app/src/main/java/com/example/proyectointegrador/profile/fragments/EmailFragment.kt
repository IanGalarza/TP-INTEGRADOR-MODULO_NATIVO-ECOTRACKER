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
                binding.emailInputLayout.error = getString(R.string.error_email_required)
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                binding.emailInputLayout.error = getString(R.string.error_email_invalid)
                return@setOnClickListener
            }

            //Se envia un mail de verificacion y se desloguea al usuario.

            user.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener {
                    val msg = getString(R.string.email_verification_sent, newEmail)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

                    auth.signOut()

                    // Ir a AuthActivity
                    val intent = Intent(requireContext(), AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    val error = getString(R.string.email_verification_failed, e.message ?: "-")
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

