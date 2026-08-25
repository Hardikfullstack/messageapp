package com.messages.sms.texting.app.receiver

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.messages.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduledMessageReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MESSAGE_ID = "extra_message_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId == -1L) return

        val pendingResult = goAsync()
        val repository = SmsRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduledMessage = repository.getScheduledMessageById(messageId)
                if (scheduledMessage != null) {
                    if (scheduledMessage.groupId != null) {
                        val group = repository.getGroupById(scheduledMessage.groupId)
                        if (group != null) {
                            repository.sendToGroup(group, scheduledMessage.body)
                        }
                    } else {
                        val threadId = Telephony.Threads.getOrCreateThreadId(context, scheduledMessage.address)
                        repository.sendSms(
                            address = scheduledMessage.address,
                            body = scheduledMessage.body,
                            threadId = threadId,
                            contactName = scheduledMessage.contactName
                        )
                    }

                    // Remove from scheduled DB
                    repository.deleteScheduledMessage(messageId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
