package com.messages.sms.texting.app.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/** "automatic" | "100kb" | "200kb" | "300kb" | "600kb" | "1000kb" | "2000kb" | "none" */
object MmsCompressionState {
    private const val PREFS = "messages_prefs"
    private const val KEY_MODE = "app_mms_compression_mode"

    var mode = mutableStateOf("300kb")

    /** Call once, as early as possible (Application.onCreate), before any Activity is created. */
    fun applyPersistedMode(context: Context) {
        mode.value = readMode(context)
    }

    fun setMode(context: Context, newMode: String) {
        mode.value = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, newMode)
            .apply()
    }

    /** Target size in bytes for a given mode; null means no compression / automatic (caller decides). */
    fun targetBytesFor(mode: String): Int? = when (mode) {
        "100kb" -> 100 * 1024
        "200kb" -> 200 * 1024
        "300kb" -> 300 * 1024
        "600kb" -> 600 * 1024
        "1000kb" -> 1000 * 1024
        "2000kb" -> 2000 * 1024
        else -> null // "automatic" and "none"
    }

    private fun readMode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, "300kb") ?: "300kb"
    }
}
