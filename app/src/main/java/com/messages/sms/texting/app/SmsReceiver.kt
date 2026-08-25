package com.messages.sms.texting.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.content.ContentValues
import android.provider.Telephony
import android.util.Log
import android.app.Application
import com.messages.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.provider.ContactsContract
import android.net.Uri

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                if (msg != null) {
                    try {
                        val address = msg.displayOriginatingAddress ?: ""
                        val body = msg.displayMessageBody ?: ""
                        val timestamp = msg.timestampMillis
                        
                        val pendingResult = goAsync()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val repository = SmsRepository(context.applicationContext as Application)
                                val isBlocked = repository.isAddressBlockedSuspend(address)
                                val dropMessages = repository.isDropMessagesEnabled()

                                // If drop messages is enabled and contact is blocked, we completely ignore it.
                                if (isBlocked && dropMessages) {
                                    return@launch
                                }

                                // Get threadId first to check if chat is active
                                val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
                                val isCurrentlyActive = (threadId == AppState.activeThreadId || address == AppState.activeAddress)

                                val values = ContentValues().apply {
                                    put(Telephony.Sms.ADDRESS, address)
                                    put(Telephony.Sms.BODY, body)
                                    put(Telephony.Sms.DATE, timestamp)
                                    put(Telephony.Sms.READ, if (isCurrentlyActive) 1 else 0)
                                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                                }
                                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)

                                // Only show notification if NOT blocked
                                if (!isBlocked) {
                                    var contactName: String? = null
                                    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
                                    val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
                                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                                        if (cursor.moveToFirst()) {
                                            contactName = cursor.getString(0)
                                        }
                                    }

                                    NotificationHelper.showSmsNotification(
                                        context = context,
                                        threadId = threadId,
                                        address = address,
                                        body = body,
                                        contactName = contactName
                                    )
                                }

                                repository.syncThreadMessages(threadId)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
