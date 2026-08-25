package com.messages.sms.texting.app.mms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

/** Sends a single-image (+ optional text) MMS via the platform's MmsService. */
object MmsSender {

    private const val SEND_TIMEOUT_MS = 45_000L

    suspend fun send(
        context: Context,
        recipient: String,
        text: String?,
        imageBytes: ByteArray,
        imageMimeType: String
    ): Boolean = withTimeoutOrNull(SEND_TIMEOUT_MS) {
        sendInternal(context, recipient, text, imageBytes, imageMimeType)
    } ?: run {
        Log.e("MmsSender", "MMS send timed out after ${SEND_TIMEOUT_MS}ms with no result broadcast")
        false
    }

    private suspend fun sendInternal(
        context: Context,
        recipient: String,
        text: String?,
        imageBytes: ByteArray,
        imageMimeType: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val transactionId = UUID.randomUUID().toString()
            val pdu = PduComposer.buildSendReqPdu(
                recipient = recipient,
                text = text,
                imageBytes = imageBytes,
                imageMimeType = imageMimeType,
                transactionId = transactionId
            )

            val mmsDir = File(context.cacheDir, "mms_out").apply { if (!exists()) mkdirs() }
            val pduFile = File(mmsDir, "$transactionId.dat")
            pduFile.writeBytes(pdu)

            val pduUri = android.net.Uri.parse("content://${context.packageName}.mmsfileprovider/mms_out/${pduFile.name}")

            val action = "com.messages.sms.texting.app.MMS_SENT_$transactionId"
            var resolved = false

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    if (resolved) return
                    resolved = true
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // already unregistered
                    }
                    pduFile.delete()
                    val success = resultCode == Activity.RESULT_OK
                    if (continuation.isActive) continuation.resume(success)
                }
            }

            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(action),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            continuation.invokeOnCancellation {
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    // already unregistered
                }
            }

            val sentPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(action).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendMultimediaMessage(context, pduUri, null, null, sentPendingIntent)
        } catch (e: Exception) {
            Log.e("MmsSender", "MMS send threw an exception", e)
            if (continuation.isActive) continuation.resume(false)
        }
    }
}
