package com.message.sms.texting.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import com.message.sms.texting.app.mms.MmsDownloader
import com.message.sms.texting.app.mms.PduParser
import com.message.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.getByteArrayExtra("data")
        if (data == null) {
            Log.e("MmsReceiver", "No PDU bytes in WAP push intent")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = PduParser.parseNotificationInd(data)
                val contentLocation = notification.contentLocation
                if (contentLocation == null) {
                    Log.e("MmsReceiver", "No Content-Location in M-Notification.ind, cannot download")
                    return@launch
                }

                val pduBytes = MmsDownloader.download(context, contentLocation)
                if (pduBytes == null) {
                    Log.e("MmsReceiver", "MMS download failed/returned no bytes")
                    return@launch
                }
                val retrieved = PduParser.parseRetrieveConf(pduBytes)
                val address = retrieved.from ?: "Unknown"

                val repository = SmsRepository(context.applicationContext as Application)
                val isBlocked = repository.isAddressBlockedSuspend(address)
                val dropMessages = repository.isDropMessagesEnabled()
                if (isBlocked && dropMessages) {
                    return@launch
                }

                val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
                val isCurrentlyActive = (threadId == AppState.activeThreadId || address == AppState.activeAddress)

                repository.insertIncomingMms(
                    address = address,
                    body = retrieved.text,
                    imageBytes = retrieved.imageBytes,
                    imageMimeType = retrieved.imageMimeType,
                    threadId = threadId,
                    isRead = isCurrentlyActive
                )

                if (!isBlocked) {
                    var contactName: String? = null
                    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
                    context.contentResolver.query(
                        uri,
                        arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) contactName = cursor.getString(0)
                    }

                    NotificationHelper.showSmsNotification(
                        context = context,
                        threadId = threadId,
                        address = address,
                        body = retrieved.text.ifBlank { "ðŸ“· Photo" },
                        contactName = contactName
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
