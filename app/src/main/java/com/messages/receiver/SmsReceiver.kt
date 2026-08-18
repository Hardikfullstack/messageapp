package com.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.content.ContentValues
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                if (msg != null) {
                    try {
                        val values = ContentValues().apply {
                            put(Telephony.Sms.ADDRESS, msg.displayOriginatingAddress)
                            put(Telephony.Sms.BODY, msg.displayMessageBody)
                            put(Telephony.Sms.DATE, msg.timestampMillis)
                            put(Telephony.Sms.READ, 0)
                            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                        }
                        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
