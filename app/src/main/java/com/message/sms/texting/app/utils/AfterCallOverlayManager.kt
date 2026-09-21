package com.message.sms.texting.app.utils

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.message.sms.texting.app.ui.screens.AfterCallOverlayRoot

private const val TAG = "AfterCallOverlay"

/** ComposeView is final and can't be subclassed, so back-key interception happens on this plain wrapper instead. */
private class KeyInterceptingFrameLayout(context: Context, private val onBack: () -> Unit) : FrameLayout(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onBack()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

/**
 * Shows the full After Call screen (contact card + Message/Quick Reply/Reminder/More tabs) as a
 * system overlay window â€” not a notification. A full-screen-intent notification only
 * auto-launches its target Activity while the device is locked; the whole point of this feature
 * is to appear immediately after a call ends even while the phone is unlocked and in active use,
 * which only a SYSTEM_ALERT_WINDOW overlay can do (it isn't subject to Android's
 * background-activity-start restrictions since it's a window, not an Activity).
 *
 * The window is focusable (unlike a small non-modal card) so the Quick Reply tab's text field and
 * the system back button work correctly.
 */
object AfterCallOverlayManager {
    private var rootView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var backInvokedCallback: OnBackInvokedCallback? = null

    // Reactive so show() doesn't have to wait on the contact-name lookup before first paint â€”
    // it's shown as soon as the caller has the CallLog data, and this gets filled in shortly
    // after via updateContactInfo() without needing to rebuild the overlay window.
    private val displayNameState = mutableStateOf<String?>(null)

    fun show(
        context: Context,
        address: String,
        callInfoLine1: String,
        callInfoLine2: String,
        initialDisplayName: String? = null
    ) {
        val appContext = context.applicationContext
        hide()

        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) {
            Log.e(TAG, "WindowManager unavailable")
            return
        }

        displayNameState.value = initialDisplayName

        val owner = OverlayLifecycleOwner(context.applicationContext as android.app.Application)
        val composeView = ComposeView(appContext).apply {
            setContent {
                val displayName = displayNameState.value
                AfterCallOverlayRoot(
                    address = address,
                    displayName = displayName,
                    isKnownContact = displayName != null,
                    callInfoLine1 = callInfoLine1,
                    callInfoLine2 = callInfoLine2,
                    onDismiss = { hide() }
                )
            }
        }
        val container = KeyInterceptingFrameLayout(appContext) { owner.onBackPressedDispatcher.onBackPressed() }.apply {
            addView(composeView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        owner.attachToView(container)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        try {
            windowManager.addView(container, layoutParams)
            rootView = container
            lifecycleOwner = owner
            // KeyInterceptingFrameLayout's dispatchKeyEvent(KEYCODE_BACK) above only ever fires for
            // a physical/3-button back press -- gesture navigation (the default on OnePlus/OxygenOS
            // and increasingly elsewhere) doesn't dispatch a KeyEvent at all, it goes through this
            // separate Predictive Back API instead. Without also registering here, swiping back
            // while this overlay is showing does nothing on gesture-nav devices. Each top-level
            // window (even one added via raw WindowManager, not just an Activity's) gets its own
            // dispatcher once attached, which is why this is registered after addView succeeds.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val callback = OnBackInvokedCallback { owner.onBackPressedDispatcher.onBackPressed() }
                container.findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    callback
                )
                backInvokedCallback = callback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    /** Fills in the resolved contact name once the (non-blocking) lookup finishes after show(). */
    fun updateContactInfo(displayName: String?) {
        displayNameState.value = displayName
    }

    fun hide() {
        val view = rootView ?: return
        rootView = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback?.let { view.findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(it) }
        }
        backInvokedCallback = null
        try {
            val windowManager = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            windowManager?.removeView(view)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay view", e)
        }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }
}
