package com.message.sms.texting.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.message.sms.texting.app.ui.theme.AfterCallState
import com.message.sms.texting.app.ui.theme.DelayedSendingState
import com.message.sms.texting.app.ui.theme.DeleteOldMessagesState
import com.message.sms.texting.app.ui.theme.DeliveryConfirmationState
import com.message.sms.texting.app.ui.theme.FontSizeState
import com.message.sms.texting.app.ui.theme.LanguageState
import com.message.sms.texting.app.ui.theme.MmsCompressionState
import com.message.sms.texting.app.ui.theme.NotificationSettingsState
import com.message.sms.texting.app.ui.theme.SwipeActionsState
import com.message.sms.texting.app.ui.theme.ThemeState
import com.message.sms.texting.app.utils.AnalyticsManager
import com.message.sms.texting.app.utils.CrashlyticsManager
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

        // Off the main thread â€” MobileAds.initialize() does blocking I/O internally.
        Thread { MobileAds.initialize(this) }.start()

        // Alarms don't survive a reboot (no boot receiver in this app), so re-arm here if it's missing.
        if (DeleteOldMessagesState.days.intValue > 0) {
            val scheduler = com.message.sms.texting.app.utils.AlarmScheduler(this)
            if (!scheduler.isAutoDeleteScheduled()) {
                scheduler.scheduleAutoDeleteCheck()
            }
        }
    }
}
