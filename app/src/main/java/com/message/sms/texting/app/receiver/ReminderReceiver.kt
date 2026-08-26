package com.message.sms.texting.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.message.sms.texting.app.NotificationHelper
import com.message.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        val repository = SmsRepository(appContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = repository.getReminderById(reminderId)
                if (reminder != null) {
                    NotificationHelper.showCallBackNotification(appContext, reminder.id, reminder.address, reminder.contactName, reminder.note)
                    repository.deleteReminder(reminderId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
