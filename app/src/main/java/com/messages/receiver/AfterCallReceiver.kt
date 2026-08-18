package com.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.messages.R
import com.messages.ui.theme.AfterCallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Numbers CallLog uses for withheld/unknown numbers — nothing we can message/call back, so skip these. */
private val UNACTIONABLE_NUMBERS = setOf("-1", "-2", "-3")

private data class CallLogMatch(val number: String?, val duration: Int, val type: Int)

class AfterCallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState: String = TelephonyManager.EXTRA_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val wasActive = lastState == TelephonyManager.EXTRA_STATE_RINGING || lastState == TelephonyManager.EXTRA_STATE_OFFHOOK
        lastState = state

        // Only act on a transition INTO idle from an active call — not app startup or repeats.
        if (state != TelephonyManager.EXTRA_STATE_IDLE || !wasActive) return

        if (!AfterCallState.readEnabled(context)) return
        if (!Settings.canDrawOverlays(context)) return
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return

        val appContext = context.applicationContext

        val cachedResult = com.messages.viewmodel.AppConfigViewModel.readCachedResult(appContext)
        if (cachedResult?.google_ads_on_off == "on" && cachedResult.native_7_on_off == "on") {
            cachedResult.native_7?.takeIf { it.isNotBlank() }?.let {
                com.messages.ads.NativeAdCache.preload(appContext, it)
            }
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val callEndTimeMs = System.currentTimeMillis()
                val match = waitForCallLogRow(appContext, callEndTimeMs)

                val address = match?.number
                if (address.isNullOrBlank() || address in UNACTIONABLE_NUMBERS) {
                    return@launch
                }
                val duration = match.duration
                val type = match.type

                val minutes = duration / 60
                val seconds = duration % 60
                val durationText = String.format("%02d:%02d", minutes, seconds)

                val callInfoLine1 = when (type) {
                    CallLog.Calls.OUTGOING_TYPE -> String.format(appContext.getString(R.string.after_call_duration_outgoing), durationText)
                    CallLog.Calls.INCOMING_TYPE -> String.format(appContext.getString(R.string.after_call_duration_incoming), durationText)
                    else -> appContext.getString(R.string.after_call_missed)
                }
                val timeText = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                val callInfoLine2 = String.format(appContext.getString(R.string.after_call_just_now_template), timeText)

                val contactName = lookupContactName(appContext, address)

                withContext(Dispatchers.Main) {

                    com.messages.ads.AppOpenBackgroundReturnTrigger.isAdPaused = true
                    val intent = Intent(appContext, com.messages.ui.screens.AfterCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("address", address)
                        putExtra("callInfoLine1", callInfoLine1)
                        putExtra("callInfoLine2", callInfoLine2)
                        if (contactName != null) {
                            putExtra("contactName", contactName)
                        }
                    }
                    appContext.startActivity(intent)

                    // Belt-and-suspenders fallback: on devices where the startActivity() above
                    // gets silently swallowed (OnePlus/OxygenOS in particular — see
                    // NotificationHelper.showAfterCallFullScreenNotification), a short delayed
                    // check catches it and falls back to a full-screen-intent notification
                    // instead. Detached from pendingResult (not awaited) so it doesn't hold the
                    // broadcast open — on devices where the direct launch already works, this is a
                    // no-op, since isVisible flips true almost immediately.
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1000)
                        if (!com.messages.ui.screens.AfterCallActivity.isVisible) {
                            com.messages.NotificationHelper.showAfterCallFullScreenNotification(
                                appContext, intent, address, contactName
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Matches CallLog.Calls.DATE (the call's START time, not end) against [callEndTimeMs] by
     * adding the row's own duration to estimate when it actually ended — a long answered call
     * legitimately started well before it ended, so comparing the raw start time against "now"
     * would reject the very row we just want. 120s of slack on top covers ringing time (not
     * included in duration) before the call connected. */
    private fun checkCallLogRow(context: Context, callEndTimeMs: Long): CallLogMatch? {
        // No SQL-level LIMIT here — some OEM CallLogProvider implementations (e.g. MIUI's) reject
        // a "LIMIT" token in the sortOrder string with IllegalArgumentException. We only ever read
        // the first row anyway, so plain DESC ordering is enough.
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val rowDate = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val rowDuration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                val estimatedEndTimeMs = rowDate + (rowDuration * 1000L)
                if (estimatedEndTimeMs >= callEndTimeMs - 120_000L) {
                    return CallLogMatch(
                        number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)),
                        duration = rowDuration,
                        type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    )
                }
            }
        }
        return null
    }

    /**
     * Waits for the just-ended call's CallLog row instead of polling on a fixed interval —
     * reacts the moment the OS actually writes it via a ContentObserver, so there's no artificial
     * per-tick delay once the row is genuinely available. A 500ms fallback tick rides along on
     * the same trigger in case some OEM's CallLogProvider doesn't call notifyChange() reliably,
     * and everything is capped at a 5s ceiling either way.
     */
    private suspend fun waitForCallLogRow(context: Context, callEndTimeMs: Long): CallLogMatch? {
        checkCallLogRow(context, callEndTimeMs)?.let { return it }

        val trigger = Channel<Unit>(Channel.CONFLATED)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trigger.trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)

        try {
            return withTimeoutOrNull(5000L) {
                val fallbackTicker = launch {
                    while (isActive) {
                        delay(500)
                        trigger.trySend(Unit)
                    }
                }
                try {
                    var result: CallLogMatch? = null
                    for (unit in trigger) {
                        val match = checkCallLogRow(context, callEndTimeMs)
                        if (match != null) {
                            result = match
                            break
                        }
                    }
                    result
                } finally {
                    fallbackTicker.cancel()
                }
            }
        } finally {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    /** Best-effort contact name lookup for the number, matching the pattern used elsewhere in the app. */
    private fun lookupContactName(context: Context, address: String): String? {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(address)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
