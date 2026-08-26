package com.message.sms.texting.app.mms

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

/** Downloads the full MMS content the platform's MmsService already knows how to fetch
 * (APN/proxy handling included) once we hand it the Content-Location URL from the push. */
object MmsDownloader {

    private const val DOWNLOAD_TIMEOUT_MS = 60_000L

    suspend fun download(context: Context, contentLocation: String): ByteArray? =
        withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) { downloadInternal(context, contentLocation) } ?: run {
            Log.e("MmsDownloader", "MMS download timed out after ${DOWNLOAD_TIMEOUT_MS}ms with no result broadcast")
            null
        }

    private suspend fun downloadInternal(context: Context, contentLocation: String): ByteArray? = suspendCancellableCoroutine { continuation ->
        try {
            val transactionId = UUID.randomUUID().toString()
            val mmsDir = File(context.cacheDir, "mms_in").apply { if (!exists()) mkdirs() }
            val pduFile = File(mmsDir, "$transactionId.dat")
            pduFile.createNewFile()

            val fileUri = android.net.Uri.parse("content://${context.packageName}.mmsfileprovider/mms_in/${pduFile.name}")

            val action = "com.message.sms.texting.app.MMS_DOWNLOADED_$transactionId"
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
                    val success = resultCode == Activity.RESULT_OK
                    val bytes = if (success && pduFile.exists()) {
                        try {
                            pduFile.readBytes()
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                    pduFile.delete()
                    if (continuation.isActive) continuation.resume(bytes)
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

            val downloadedPendingIntent = PendingIntent.getBroadcast(
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

            smsManager.downloadMultimediaMessage(context, contentLocation, fileUri, null, downloadedPendingIntent)
        } catch (e: Exception) {
            Log.e("MmsDownloader", "MMS download threw an exception", e)
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
