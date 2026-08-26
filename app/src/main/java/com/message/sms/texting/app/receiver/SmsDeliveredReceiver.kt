package com.message.sms.texting.app.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.message.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsDeliveredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (resultCode != Activity.RESULT_OK) return

        val messageId = intent.data?.lastPathSegment?.toLongOrNull() ?: return
        val pendingResult = goAsync()
        val repository = SmsRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.markSmsDelivered(messageId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
