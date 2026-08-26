package com.message.sms.texting.app.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object AfterCallState {
    private const val PREFS = "messages_prefs"
    private const val KEY_ENABLED = "app_after_call_enabled"

    var enabled = mutableStateOf(true)

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

    /** Reads straight from SharedPreferences â€” safe to call from the call-end receiver without composable state. */
    fun readEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }
}
