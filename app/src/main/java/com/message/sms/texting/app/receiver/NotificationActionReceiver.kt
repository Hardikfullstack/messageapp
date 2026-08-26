package com.message.sms.texting.app.receiver

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.message.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_READ = "com.message.sms.texting.app.action.NOTIF_MARK_READ"
        const val ACTION_DELETE = "com.message.sms.texting.app.action.NOTIF_DELETE"
        const val ACTION_REPLY = "com.message.sms.texting.app.action.NOTIF_REPLY"
        const val ACTION_COPY_OTP = "com.message.sms.texting.app.action.NOTIF_COPY_OTP"

        const val EXTRA_THREAD_ID = "extra_thread_id"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_OTP_CODE = "extra_otp_code"
        const val KEY_REPLY_TEXT = "key_reply_text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
        if (threadId == -1L) return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, threadId.toInt())

        val pendingResult = goAsync()
        val repository = SmsRepository(context.applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_MARK_READ -> {
                        repository.markThreadAsRead(threadId)
                        NotificationManagerCompat.from(context).cancel(notificationId)
                    }
                    ACTION_DELETE -> {
                        repository.deleteThreads(listOf(threadId))
                        NotificationManagerCompat.from(context).cancel(notificationId)
                    }
                    ACTION_REPLY -> {
                        val replyText = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(KEY_REPLY_TEXT)
                            ?.toString()
                            ?.trim()
                        val address = intent.getStringExtra(EXTRA_ADDRESS)
                        if (!replyText.isNullOrEmpty() && address != null) {
                            val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
                            repository.sendSms(address, replyText, threadId, contactName)
                            repository.markThreadAsRead(threadId)
                        }
                        NotificationManagerCompat.from(context).cancel(notificationId)
                    }
                    ACTION_COPY_OTP -> {
                        val code = intent.getStringExtra(EXTRA_OTP_CODE)
                        if (!code.isNullOrEmpty()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
                        }
                        repository.markThreadAsRead(threadId)
                        NotificationManagerCompat.from(context).cancel(notificationId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
