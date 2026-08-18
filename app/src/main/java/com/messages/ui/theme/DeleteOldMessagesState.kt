package com.messages.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf

/** Retention window in days; 0 means the feature is off ("Never"). */
object DeleteOldMessagesState {
    private const val PREFS = "messages_prefs"
    private const val KEY_DAYS = "app_delete_old_messages_days"

    var days = mutableIntStateOf(0)

    /** Call once, as early as possible (Application.onCreate), before any Activity is created. */
    fun applyPersistedMode(context: Context) {
        days.intValue = readDays(context)
    }

    fun setDays(context: Context, newDays: Int) {
        days.intValue = newDays
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DAYS, newDays)
            .apply()
    }

    /** Reads straight from SharedPreferences — safe to call from a BroadcastReceiver's own process/context. */
    fun readDays(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_DAYS, 0)
    }
}
