package com.example.proyectointegrador.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

fun updateLocale(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val resources = context.resources
    val config = resources.configuration

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocale(locale)
        context.createConfigurationContext(config)
    } else {
        @Suppress("DEPRECATION")
        config.locale = locale
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}



