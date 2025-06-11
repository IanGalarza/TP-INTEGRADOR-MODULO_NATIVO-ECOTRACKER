package com.example.proyectointegrador.profile.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyectointegrador.R
import com.example.proyectointegrador.databinding.FragmentNameBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NameFragment : Fragment() {

    private var _binding: FragmentNameBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun clearErrorOnTyping(editText: EditText, layout: TextInputLayout) {
        editText.doOnTextChanged { _, _, _, _ ->
            layout.error = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clearErrorOnTyping(binding.nameInput, binding.usernameInputLayout)

        val uid = auth.currentUser?.uid ?: return

        // Ocultar contenido, mostrar loading

        binding.contentGroup.visibility = View.GONE
        binding.loadingProgressBar.visibility = View.VISIBLE

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val currentName = document.getString("name")
                binding.nameInput.setText(currentName ?: "")
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading name", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                // Mostrar contenido y ocultar loading

                binding.loadingProgressBar.visibility = View.GONE
                binding.contentGroup.visibility = View.VISIBLE
            }

        binding.saveButton.setOnClickListener {
            val newName = binding.nameInput.text.toString().trim()

            binding.usernameInputLayout.error = null

            if (newName.isEmpty()) {
                binding.usernameInputLayout.error = "Name cannot be empty"
                return@setOnClickListener
            }

            // Cambiar texto del botón y mostrar progress
            binding.saveButton.text = "Saving..."
            binding.saveProgressBar.visibility = View.VISIBLE
            binding.saveButton.isEnabled = false

            firestore.collection("users").document(uid)
                .update("name", newName)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error updating name", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    // Restaurar botón
                    binding.saveButton.text = getString(R.string.save_changes)
                    binding.saveProgressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
