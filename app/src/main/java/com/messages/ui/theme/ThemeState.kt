package com.messages.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf

/** "system" | "light" | "dark" */
object ThemeState {
    private const val PREFS = "messages_prefs"
    private const val KEY_THEME_MODE = "app_theme_mode"

    var mode = mutableStateOf("system")

    /** Call once, as early as possible (Application.onCreate), before any Activity is created. */
    fun applyPersistedMode(context: Context) {
        val saved = readMode(context)
        mode.value = saved
        AppCompatDelegate.setDefaultNightMode(nightModeFor(saved))
    }

    fun setMode(context: Context, newMode: String) {
        mode.value = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, newMode)
            .apply()
        AppCompatDelegate.setDefaultNightMode(nightModeFor(newMode))
    }

    private fun nightModeFor(mode: String): Int = when (mode) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private fun readMode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, "system") ?: "system"
    }
}
