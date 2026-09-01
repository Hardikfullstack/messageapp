package com.message.sms.texting.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.Settings
import android.telephony.TelephonyManager
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.AfterCallState
import com.message.sms.texting.app.utils.AfterCallMiniOverlay

class AfterCallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState: String = TelephonyManager.EXTRA_STATE_IDLE
        private var wasRinging = false
        private var callConnectTimeMs: Long = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val previousState = lastState
        lastState = state

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
                return
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Only stamp the connect time on the RINGING/IDLE -> OFFHOOK edge, not on repeat
                // OFFHOOK broadcasts the platform can fire mid-call (e.g. call waiting).
                if (previousState != TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    callConnectTimeMs = System.currentTimeMillis()
                }
                return
            }
            TelephonyManager.EXTRA_STATE_IDLE -> Unit
            else -> return
        }

        // Only act on a transition INTO idle from an active call -- not app startup or repeats.
        val wasActive = previousState == TelephonyManager.EXTRA_STATE_RINGING || previousState == TelephonyManager.EXTRA_STATE_OFFHOOK
        if (!wasActive) {
            wasRinging = false
            return
        }

        // RINGING -> OFFHOOK -> IDLE = answered incoming. OFFHOOK -> IDLE (no preceding RINGING
        // on this device) = outgoing -- an incoming call never reaches OFFHOOK locally without
        // first passing through RINGING. RINGING -> IDLE (never OFFHOOK) = missed/rejected.
        val type = if (previousState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
            if (wasRinging) CallLog.Calls.INCOMING_TYPE else CallLog.Calls.OUTGOING_TYPE
        } else {
            CallLog.Calls.MISSED_TYPE
        }
        val durationSeconds = if (callConnectTimeMs > 0L) {
            ((System.currentTimeMillis() - callConnectTimeMs) / 1000L).toInt().coerceAtLeast(0)
        } else 0

        wasRinging = false
        callConnectTimeMs = 0L

        if (!AfterCallState.readEnabled(context)) return
        if (!Settings.canDrawOverlays(context)) {
            // Can't show After Call at all without this -- nudge the user to re-grant it instead
            // of silently doing nothing (matches the decompiled competing app's own fallback for
            // this exact case).
            com.message.sms.texting.app.NotificationHelper.showOverlayPermissionMissingNotification(context.applicationContext)
            return
        }

        val appContext = context.applicationContext

        val cachedResult = com.message.sms.texting.app.viewmodel.AppConfigViewModel.readCachedResult(appContext)
        if (cachedResult?.google_ads_on_off == "on" && cachedResult.native_7_on_off == "on") {
            cachedResult.native_7?.takeIf { it.isNotBlank() }?.let {
                com.message.sms.texting.app.ads.NativeAdCache.preload(appContext, it)
            }
        }

        // Everything below runs synchronously, right here on this call -- no coroutine, no
        // goAsync(). A competing app's equivalent receiver (confirmed by decompiling it) does the
        // exact same thing: build and post its immediate UI inline, with zero thread-hops or
        // async scheduling, then use a plain Handler.postDelayed (not a coroutine) for the
        // delayed activity launch. Everything here is pure CPU work (string formatting, posting a
        // notification) -- nothing needs a background thread, and the immediacy seems to matter on
        // OEMs like MIUI where extra scheduling latency risks the process getting frozen before
        // anything gets a chance to show.
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        val durationText = String.format("%02d:%02d", minutes, seconds)

        val callInfoLine1 = when (type) {
            CallLog.Calls.OUTGOING_TYPE -> String.format(appContext.getString(R.string.after_call_duration_outgoing), durationText)
            CallLog.Calls.INCOMING_TYPE -> String.format(appContext.getString(R.string.after_call_duration_incoming), durationText)
            else -> appContext.getString(R.string.after_call_missed)
        }
        val timeText = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val callInfoLine2 = String.format(appContext.getString(R.string.after_call_just_now_template), timeText)

        com.message.sms.texting.app.ads.AppOpenBackgroundReturnTrigger.isAdPaused = true
        val activityIntent = Intent(appContext, com.message.sms.texting.app.ui.screens.AfterCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Blank on purpose -- no real number is ever known anymore. AfterCallScreen treats a
            // blank address as "Private Number" and disables the actions that need a real one
            // (call back, add contact, reminders, quick reply).
            putExtra("address", "")
            putExtra("callInfoLine1", callInfoLine1)
            putExtra("callInfoLine2", callInfoLine2)
        }

        // Immediate, always-safe feedback -- posting a notification is never subject to
        // background-launch restrictions, unlike startActivity() below. The invisible overlay
        // isn't decorative -- see AfterCallMiniOverlay's doc comment for why it's required.
        AfterCallMiniOverlay.show(appContext)
        com.message.sms.texting.app.NotificationHelper.showAfterCallFullScreenNotification(appContext, activityIntent)

        // Calling startActivity() immediately on call-end is exactly what OEMs like MIUI silently
        // block. Waiting ~2s first -- letting the just-ended call's telecom/audio teardown settle
        // -- is what makes the same call reliably succeed instead. Matches the timing (and the
        // plain Handler.postDelayed, not a coroutine) the decompiled competing app uses for the
        // identical retry.
        Handler(Looper.getMainLooper()).postDelayed({
            AfterCallMiniOverlay.hide()
            try {
                appContext.startActivity(activityIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 2000L)
    }
}
