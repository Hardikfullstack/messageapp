package com.messages.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/** "none" | "archive" | "delete" | "block" | "call" | "mark_read" | "mark_unread" */
object SwipeActionsState {
    private const val PREFS = "messages_prefs"
    private const val KEY_RIGHT = "swipe_action_right"
    private const val KEY_LEFT = "swipe_action_left"

    var rightAction = mutableStateOf("archive")
    var leftAction = mutableStateOf("delete")

    /** Call once, as early as possible (Application.onCreate), before any Activity is created. */
    fun applyPersistedMode(context: Context) {
        rightAction.value = readAction(context, KEY_RIGHT, "archive")
        leftAction.value = readAction(context, KEY_LEFT, "delete")
    }

    fun setRightAction(context: Context, action: String) {
        rightAction.value = action
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RIGHT, action)
            .apply()
    }

    fun setLeftAction(context: Context, action: String) {
        leftAction.value = action
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LEFT, action)
            .apply()
    }

    private fun readAction(context: Context, key: String, default: String): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, default) ?: default
    }
}
