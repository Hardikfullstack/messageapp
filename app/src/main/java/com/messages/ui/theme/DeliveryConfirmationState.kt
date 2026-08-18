package com.messages.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object DeliveryConfirmationState {
    private const val PREFS = "messages_prefs"
    private const val KEY_ENABLED = "app_delivery_confirmations_enabled"

    var enabled = mutableStateOf(false)

    /** Call once, as early as possible (Application.onCreate), before any Activity is created. */
    fun applyPersistedMode(context: Context) {
        enabled.value = readEnabled(context)
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled.value = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, value)
            .apply()
    }

    private fun readEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }
}
