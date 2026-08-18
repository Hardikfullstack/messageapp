package com.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.messages.repository.SmsRepository
import com.messages.ui.theme.DeleteOldMessagesState
import com.messages.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoDeleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val days = DeleteOldMessagesState.readDays(appContext)
                if (days > 0) {
                    SmsRepository(appContext).purgeMessagesOlderThan(days)
                    AlarmScheduler(appContext).scheduleAutoDeleteCheck()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
