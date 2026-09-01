package com.message.sms.texting.app.ui.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.message.sms.texting.app.MainActivity
import com.message.sms.texting.app.ui.theme.AfterCallReturnState
import com.message.sms.texting.app.ui.theme.FontSizeState
import com.message.sms.texting.app.ui.theme.MessagesTheme

// AppCompatActivity (not plain ComponentActivity) â€” needed so the app's Light/Dark/System theme
// choice (applied globally via AppCompatDelegate.setDefaultNightMode in ThemeState) actually
// gets picked up by this Activity's Configuration; a bare ComponentActivity ignores it and falls
// back to the raw system dark-mode setting instead.
class AfterCallActivity : AppCompatActivity() {
    // Matches MainActivity's own override -- without this, screens shared between the two (e.g.
    // MessageItemUi, reused in the Message tab here) render at a different font scale than they
    // do under MainActivity, throwing off pixel-level tweaks (like the unread-count badge's
    // manual vertical offset) that were tuned against MainActivity's scale.
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("messages_prefs", Context.MODE_PRIVATE)
        val fontSizeMode = prefs.getString("app_font_size_mode", "normal") ?: "normal"
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = FontSizeState.scaleFor(fontSizeMode)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    companion object {
        /**
         * Set right before AfterCallMoreTab's Send Mail/Calendar/Web actions launch an external
         * app â€” those also trigger onUserLeaveHint, but per product decision should leave this
         * screen alive behind them (so back from the launched app returns here). Add Contact and
         * Message are NOT covered by this â€” they should close After Call like Home/Recents does.
         * Consumed (reset to false) on the very next leave-hint.
         */
        var suppressNextLeaveFinish = false

        /** True while this Activity is started (between onStart/onStop) â€” lets AfterCallReceiver
         * check, after a short delay, whether its startActivity() call actually resulted in this
         * screen showing, so it knows whether the full-screen-intent-notification fallback is
         * needed (see NotificationHelper.showAfterCallFullScreenNotification). */
        var isVisible = false
    }

    private var address by mutableStateOf("")
    private var callInfoLine1 by mutableStateOf("")
    private var callInfoLine2 by mutableStateOf("")
    private var contactName by mutableStateOf<String?>(null)

    // Every close path (X/back, leave-hint, the "Message" tap flow) routes through finish() â€”
    // overriding it here guarantees the flag is set before MainActivity resumes underneath, so an
    // App Open ad never sneaks in right as the user returns from this screen.
    override fun finish() {
        com.message.sms.texting.app.ads.AppOpenBackgroundReturnTrigger.isAdPaused = true
        super.finish()
    }

    override fun onStart() {
        super.onStart()
        isVisible = true
    }

    override fun onStop() {
        super.onStop()
        isVisible = false
    }

    // Home/Recents pressed while this screen is showing should close it, matching how other
    // apps' after-call screens behave â€” it shouldn't linger in the background/task switcher.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (suppressNextLeaveFinish) {
            suppressNextLeaveFinish = false
        } else if (!isFinishing) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent.getBooleanExtra("is_call_trampoline", false)) {
            val notifId = intent.getIntExtra("notif_id", -1)
            if (notifId != -1) {
                androidx.core.app.NotificationManagerCompat.from(this).cancel(notifId)
            }
            val callIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("call_intent", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Intent>("call_intent")
            }
            if (callIntent != null) {
                try {
                    startActivity(callIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            finish()
            return
        }

        // Ensure this activity can show over the lock screen and turns the screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Bottom nav bar stays hidden on this screen â€” a swipe from the edge can still reveal it
        // transiently (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE), but it auto-hides again afterward.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (!updateFromIntent(intent)) return

        setContent {
            MessagesTheme {
                AfterCallScreen(
                    address = address,
                    displayName = contactName,
                    isKnownContact = contactName != null,
                    callInfoLine1 = callInfoLine1,
                    callInfoLine2 = callInfoLine2,
                    onOpenChat = { threadId, chatAddress, name, forwardText ->
                        finish()
                        AfterCallReturnState.pending = AfterCallReturnState.Info(
                            address = address,
                            displayName = contactName,
                            isKnownContact = contactName != null,
                            callInfoLine1 = callInfoLine1,
                            callInfoLine2 = callInfoLine2
                        )
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("navigate_to_chat", true)
                            putExtra("threadId", threadId)
                            putExtra("address", chatAddress)
                            putExtra("contactName", name)
                            if (forwardText != null) putExtra("forwardText", forwardText)
                        }
                        startActivity(intent)
                    },
                    onOpenNewChat = { forwardText ->
                        finish()
                        AfterCallReturnState.pending = AfterCallReturnState.Info(
                            address = address,
                            displayName = contactName,
                            isKnownContact = contactName != null,
                            callInfoLine1 = callInfoLine1,
                            callInfoLine2 = callInfoLine2
                        )
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("navigate_to_new_chat", true)
                            if (forwardText != null) putExtra("forwardText", forwardText)
                        }
                        startActivity(intent)
                    },
                    onFinish = { finish() }
                )
            }
        }
    }

    // A second call ending while this screen is still showing for a previous one reuses this same
    // instance (FLAG_ACTIVITY_SINGLE_TOP) instead of creating a new one â€” without this override,
    // the new call's intent would be silently dropped and the screen would keep showing stale
    // info from the earlier call.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateFromIntent(intent)
    }

    /** Reads the call-info extras into state; returns false (and finishes the Activity) if the
     * intent doesn't have a usable address, matching this screen's only hard requirement. */
    private fun updateFromIntent(intent: Intent): Boolean {
        val newAddress = intent.getStringExtra("address")
        if (newAddress == null) {
            finish()
            return false
        }
        address = newAddress
        callInfoLine1 = intent.getStringExtra("callInfoLine1") ?: ""
        callInfoLine2 = intent.getStringExtra("callInfoLine2") ?: ""
        contactName = intent.getStringExtra("contactName")
        // This screen is now genuinely showing the data â€” the full-screen-intent fallback (if one
        // was posted because the earlier direct launch attempt seemed to have been blocked) is no
        // longer needed.
        com.message.sms.texting.app.NotificationHelper.cancelAfterCallFullScreenNotification(this)
        return true
    }

    // Returning to this Activity (e.g. after the mail/calendar chooser closes) resets the system
    // bars â€” re-hide the nav bar each time this window regains focus so it doesn't creep back.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowCompat.getInsetsController(window, window.decorView)
                .hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
