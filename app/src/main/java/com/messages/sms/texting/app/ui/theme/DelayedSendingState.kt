package com.messages.sms.texting.app.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/** "none" | "3s" | "5s" | "10s" */
object DelayedSendingState {
    private const val PREFS = "messages_prefs"
    private const val KEY_DELAYED_SENDING_MODE = "app_delayed_sending_mode"

    var mode = mutableStateOf("none")

    /** Call once, as early as possible (Application.onCreate), before any Activity is created. */
    fun applyPersistedMode(context: Context) {
        mode.value = readMode(context)
    }

    fun setMode(context: Context, newMode: String) {
        mode.value = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DELAYED_SENDING_MODE, newMode)
            .apply()
    }

    fun delayMillisFor(mode: String): Long = when (mode) {
        "3s" -> 3000L
        "5s" -> 5000L
        "10s" -> 10000L
        else -> 0L
    }

    private fun readMode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DELAYED_SENDING_MODE, "none") ?: "none"
    }
}
