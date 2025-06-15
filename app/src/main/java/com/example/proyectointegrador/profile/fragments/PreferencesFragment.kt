package com.example.proyectointegrador.profile.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.updateLocale
import com.google.android.material.button.MaterialButton

class PreferencesFragment : Fragment() {

    private val PREFS_NAME = "user_preferences"
    private val KEY_LANGUAGE = "preferred_language"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preferences, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val radioGroup = view.findViewById<RadioGroup>(R.id.languageRadioGroup)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var savedLangCode = prefs.getString(KEY_LANGUAGE, null)

        // Si es la primera vez (no hay valor guardado), usar "en" por defecto y guardarlo
        if (savedLangCode == null) {
            savedLangCode = "en"
            prefs.edit().putString(KEY_LANGUAGE, savedLangCode).apply()
        }

        // Preseleccionar idioma guardado
        when (savedLangCode) {
            "es" -> radioGroup.check(R.id.radio_spanish)
            "en" -> radioGroup.check(R.id.radio_english)
            "pt" -> radioGroup.check(R.id.radio_portuguese)
            else -> radioGroup.clearCheck()
        }

        saveButton.setOnClickListener {
            val selectedLangCode = when (radioGroup.checkedRadioButtonId) {
                R.id.radio_spanish -> "es"
                R.id.radio_english -> "en"
                R.id.radio_portuguese -> "pt"
                else -> ""
            }

            if (selectedLangCode.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.select_language_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedLangCode != savedLangCode) {
                prefs.edit().putString(KEY_LANGUAGE, selectedLangCode).commit()
                updateLocale(requireContext(), selectedLangCode)
                Toast.makeText(requireContext(), getString(R.string.language_selected, selectedLangCode), Toast.LENGTH_SHORT).show()
                activity?.recreate()
            } else {
                Toast.makeText(requireContext(), getString(R.string.no_changes), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
