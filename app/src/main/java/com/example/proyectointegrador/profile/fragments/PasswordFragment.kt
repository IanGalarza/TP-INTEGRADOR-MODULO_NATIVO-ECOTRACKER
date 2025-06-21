package com.example.proyectointegrador.profile.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.example.proyectointegrador.R
import com.example.proyectointegrador.auth.AuthActivity
import com.example.proyectointegrador.databinding.FragmentPasswordBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class PasswordFragment : Fragment() {

    private var _binding: FragmentPasswordBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

    // Regex para validar contraseña
    private val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun clearErrorOnTyping(editText: android.widget.EditText, layout: TextInputLayout) {
        editText.doOnTextChanged { _, _, _, _ ->
            layout.error = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clearErrorOnTyping(binding.currentPassword, binding.currentPasswordLayout)
        clearErrorOnTyping(binding.newPassword, binding.newPasswordLayout)

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), getString(R.string.user_not_logged_in), Toast.LENGTH_SHORT).show()
            return
        }

        binding.saveButton.setOnClickListener {
            binding.currentPasswordLayout.error = null
            binding.newPasswordLayout.error = null

            val currentPassword = binding.currentPassword.text.toString()
            val newPassword = binding.newPassword.text.toString()

            var isValid = true

            // Validaciones

            if (currentPassword.isEmpty()) {
                binding.currentPasswordLayout.error = getString(R.string.error_current_password_required)
                isValid = false
            }

            if (newPassword.isEmpty()) {
                binding.newPasswordLayout.error = getString(R.string.error_password_required)
                isValid = false
            } else if (!passwordRegex.matches(newPassword)) {
                binding.newPasswordLayout.error = getString(R.string.error_password_invalid)
                isValid = false
            }

            // Si hay errores, no continuar

            if (!isValid) return@setOnClickListener

            setLoading(true)

            // Se updatea la contraseña y se reautentica para confirmar que sea el usuario

            val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), getString(R.string.password_updated_successfully), Toast.LENGTH_LONG).show()

                            // Desloguear y enviar a AuthActivity
                            auth.signOut()
                            val intent = Intent(requireContext(), AuthActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)

                        }
                        .addOnFailureListener { e ->
                            val msg = getString(R.string.error_updating_password, e.message ?: "-")
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                        .addOnCompleteListener {
                            setLoading(false)
                        }
                }
                .addOnFailureListener {
                    binding.currentPasswordLayout.error = getString(R.string.incorrect_current_password)
                    setLoading(false)
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.saveButton.isEnabled = !isLoading
        binding.saveButton.text = if (isLoading) getString(R.string.saving) else getString(R.string.save_changes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
