package com.messages.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/** "small" | "normal" | "large" */
object FontSizeState {
    private const val PREFS = "messages_prefs"
    private const val KEY_FONT_SIZE_MODE = "app_font_size_mode"

    var mode = mutableStateOf("normal")

    /** Call once, as early as possible (Application.onCreate), before any Activity is created. */
    fun applyPersistedMode(context: Context) {
        mode.value = readMode(context)
    }

    fun setMode(context: Context, newMode: String) {
        mode.value = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FONT_SIZE_MODE, newMode)
            .apply()
    }

    fun scaleFor(mode: String): Float = when (mode) {
        "small" -> 0.9f
        "large" -> 1.15f
        else -> 1.0f
    }

    private fun readMode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FONT_SIZE_MODE, "normal") ?: "normal"
    }
}
