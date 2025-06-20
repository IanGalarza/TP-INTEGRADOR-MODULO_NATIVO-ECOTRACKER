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
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.messaging.FirebaseMessaging


class PreferencesFragment : Fragment() {

    private val PREFS_NAME = "user_preferences"
    private val KEY_LANGUAGE = "preferred_language"
    private val KEY_LOCAL_NOTIFICATIONS = "local_notifications_enabled"
    private val KEY_FIREBASE_NOTIFICATIONS = "firebase_notifications_enabled"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preferences, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val radioGroup = view.findViewById<RadioGroup>(R.id.languageRadioGroup)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)


        val switchLocal = view.findViewById<SwitchCompat>(R.id.switch_local_notifications)
        val switchFirebase = view.findViewById<SwitchCompat>(R.id.switch_firebase_notifications)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var savedLangCode = prefs.getString(KEY_LANGUAGE, null)

        // Si es la primera vez (no hay valor guardado), usar "en" por defecto y guardarlo
        if (savedLangCode == null) {
            savedLangCode = "en"
            prefs.edit().putString(KEY_LANGUAGE, savedLangCode).apply()
        }

        // Verificar si es la primera vez y setear valores por defecto
        if (!prefs.contains(KEY_LOCAL_NOTIFICATIONS)) {
            prefs.edit().putBoolean(KEY_LOCAL_NOTIFICATIONS, true).apply()
        }
        if (!prefs.contains(KEY_FIREBASE_NOTIFICATIONS)) {
            prefs.edit().putBoolean(KEY_FIREBASE_NOTIFICATIONS, true).apply()
        }

        // Preseleccionar idioma guardado
        when (savedLangCode) {
            "es" -> radioGroup.check(R.id.radio_spanish)
            "en" -> radioGroup.check(R.id.radio_english)
            "pt" -> radioGroup.check(R.id.radio_portuguese)
            else -> radioGroup.clearCheck()
        }

        // Obtener valores guardados

        val localNotificationsEnabled = prefs.getBoolean(KEY_LOCAL_NOTIFICATIONS, true)
        val firebaseNotificationsEnabled = prefs.getBoolean(KEY_FIREBASE_NOTIFICATIONS, true)

        // Preselección switches

        switchLocal.isChecked = localNotificationsEnabled
        switchFirebase.isChecked = firebaseNotificationsEnabled

        // Garantizamos que esten sicronizados con sharedPreferences

        if (firebaseNotificationsEnabled) {
            FirebaseMessaging.getInstance().subscribeToTopic("firebase_notifications")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("firebase_notifications")
        }

        // Listeners automáticos

        switchLocal.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_LOCAL_NOTIFICATIONS, isChecked).apply()
        }

        switchFirebase.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_FIREBASE_NOTIFICATIONS, isChecked).apply()
            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("firebase_notifications")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("firebase_notifications")
            }
        }

        //Guardar idioma

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
