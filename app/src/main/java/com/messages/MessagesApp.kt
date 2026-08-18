package com.messages

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.messages.ui.theme.AfterCallState
import com.messages.ui.theme.DelayedSendingState
import com.messages.ui.theme.DeleteOldMessagesState
import com.messages.ui.theme.DeliveryConfirmationState
import com.messages.ui.theme.FontSizeState
import com.messages.ui.theme.LanguageState
import com.messages.ui.theme.MmsCompressionState
import com.messages.ui.theme.NotificationSettingsState
import com.messages.ui.theme.SwipeActionsState
import com.messages.ui.theme.ThemeState
import com.messages.utils.AnalyticsManager
import com.messages.utils.CrashlyticsManager
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider

class MessagesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AnalyticsManager.init()
        CrashlyticsManager.init()
        ThemeState.applyPersistedMode(this)
        FontSizeState.applyPersistedMode(this)
        LanguageState.applyPersistedMode(this)
        DelayedSendingState.applyPersistedMode(this)
        SwipeActionsState.applyPersistedMode(this)
        DeliveryConfirmationState.applyPersistedMode(this)
        DeleteOldMessagesState.applyPersistedMode(this)
        MmsCompressionState.applyPersistedMode(this)
        NotificationSettingsState.applyPersistedMode(this)
        AfterCallState.applyPersistedMode(this)
        EmojiManager.install(GoogleEmojiProvider())

        // Off the main thread — MobileAds.initialize() does blocking I/O internally.
        Thread { MobileAds.initialize(this) }.start()

        // Alarms don't survive a reboot (no boot receiver in this app), so re-arm here if it's missing.
        if (DeleteOldMessagesState.days.intValue > 0) {
            val scheduler = com.messages.utils.AlarmScheduler(this)
            if (!scheduler.isAutoDeleteScheduled()) {
                scheduler.scheduleAutoDeleteCheck()
            }
        }
    }
}
