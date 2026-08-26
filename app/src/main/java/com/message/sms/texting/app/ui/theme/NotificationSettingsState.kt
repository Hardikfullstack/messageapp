package com.message.sms.texting.app.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * Global (app-wide) notification defaults, shown on the main "Notifications" settings screen.
 * Per-contact overrides (from ContactNotificationScreen, keys "preview_option_$threadId" /
 * "wake_screen_$threadId" in the separate "contact_notification_prefs" file) still win when set â€”
 * these are only the fallback used when no per-contact override exists.
 */
object NotificationSettingsState {
    private const val PREFS = "messages_prefs"
    private const val KEY_PREVIEW_OPTION = "global_notification_preview_option"
    private const val KEY_WAKE_SCREEN = "global_wake_screen_enabled"
    private const val KEY_BUTTON_1 = "notif_action_button_1"
    private const val KEY_BUTTON_2 = "notif_action_button_2"
    private const val KEY_BUTTON_3 = "notif_action_button_3"
    private const val KEY_QUICK_REPLY = "notif_quick_reply_enabled"
    private const val KEY_TAP_TO_DISMISS = "notif_tap_to_dismiss_enabled"

    /** 0 = Show name and message, 1 = Show name, 2 = Hide contents. */
    var previewOption = mutableIntStateOf(0)
    var wakeScreenEnabled = mutableStateOf(false)

    /** "none" | "mark_read" | "reply" | "call" | "delete" */
    var button1Action = mutableStateOf("mark_read")
    var button2Action = mutableStateOf("reply")
    var button3Action = mutableStateOf("none")

    /** Controls the notification channel's importance â€” HIGH shows the heads-up popup banner, DEFAULT stays silent in the tray. */
    var quickReplyEnabled = mutableStateOf(true)

    /**
     * Controls NotificationCompat.setAutoCancel â€” ON clears the notification automatically when
     * tapped/opened (default Android behavior); OFF leaves it in the tray until manually swiped away.
     * True "tap outside the popup to dismiss" isn't controllable via public API (System UI owns the
     * heads-up banner, not the app), so this is the closest real, honest analog.
     */
    var tapToDismissEnabled = mutableStateOf(true)

    fun applyPersistedMode(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        previewOption.intValue = prefs.getInt(KEY_PREVIEW_OPTION, 0)
        wakeScreenEnabled.value = prefs.getBoolean(KEY_WAKE_SCREEN, false)
        button1Action.value = prefs.getString(KEY_BUTTON_1, "mark_read") ?: "mark_read"
        button2Action.value = prefs.getString(KEY_BUTTON_2, "reply") ?: "reply"
        button3Action.value = prefs.getString(KEY_BUTTON_3, "none") ?: "none"
        quickReplyEnabled.value = prefs.getBoolean(KEY_QUICK_REPLY, true)
        tapToDismissEnabled.value = prefs.getBoolean(KEY_TAP_TO_DISMISS, true)
    }

    fun setQuickReplyEnabled(context: Context, enabled: Boolean) {
        quickReplyEnabled.value = enabled
        prefs(context).edit().putBoolean(KEY_QUICK_REPLY, enabled).apply()
    }

    fun readQuickReplyEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QUICK_REPLY, true)

    fun setTapToDismissEnabled(context: Context, enabled: Boolean) {
        tapToDismissEnabled.value = enabled
        prefs(context).edit().putBoolean(KEY_TAP_TO_DISMISS, enabled).apply()
    }

    fun readTapToDismissEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TAP_TO_DISMISS, true)

    fun setPreviewOption(context: Context, option: Int) {
        previewOption.intValue = option
        prefs(context).edit().putInt(KEY_PREVIEW_OPTION, option).apply()
    }

    fun setWakeScreenEnabled(context: Context, enabled: Boolean) {
        wakeScreenEnabled.value = enabled
        prefs(context).edit().putBoolean(KEY_WAKE_SCREEN, enabled).apply()
    }

    fun setButtonAction(context: Context, buttonIndex: Int, action: String) {
        when (buttonIndex) {
            1 -> button1Action.value = action
            2 -> button2Action.value = action
            3 -> button3Action.value = action
        }
        val key = when (buttonIndex) {
            1 -> KEY_BUTTON_1
            2 -> KEY_BUTTON_2
            else -> KEY_BUTTON_3
        }
        prefs(context).edit().putString(key, action).apply()
    }

    /** Reads straight from SharedPreferences â€” safe to call from NotificationHelper/receivers without composable state. */
    fun readPreviewOption(context: Context, threadId: Long): Int {
        val contactPrefs = context.getSharedPreferences("contact_notification_prefs", Context.MODE_PRIVATE)
        if (contactPrefs.contains("preview_option_$threadId")) {
            return contactPrefs.getInt("preview_option_$threadId", 0)
        }
        return prefs(context).getInt(KEY_PREVIEW_OPTION, 0)
    }

    fun readWakeScreenEnabled(context: Context, threadId: Long): Boolean {
        val contactPrefs = context.getSharedPreferences("contact_notification_prefs", Context.MODE_PRIVATE)
        if (contactPrefs.contains("wake_screen_$threadId")) {
            return contactPrefs.getBoolean("wake_screen_$threadId", false)
        }
        return prefs(context).getBoolean(KEY_WAKE_SCREEN, false)
    }

    fun readButtonActions(context: Context): Triple<String, String, String> {
        val p = prefs(context)
        return Triple(
            p.getString(KEY_BUTTON_1, "mark_read") ?: "mark_read",
            p.getString(KEY_BUTTON_2, "reply") ?: "reply",
            p.getString(KEY_BUTTON_3, "none") ?: "none"
        )
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
