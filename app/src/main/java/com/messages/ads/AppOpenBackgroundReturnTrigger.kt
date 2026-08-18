package com.messages.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.messages.utils.SetupState

/**
 * Shows an App Open ad when the app returns to the foreground after being backgrounded (user
 * switched to another app / Home, then came back) — separate from [AppOpenCounter]'s cold-start
 * (kill+reopen) cadence, since Splash isn't re-entered on a simple background→foreground return.
 * Only every 2nd such return shows the ad, so quick app-switches (camera, share sheet) don't
 * interrupt every single time. Call [init] once (e.g. from MainActivity) when config is ready.
 */
object AppOpenBackgroundReturnTrigger : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private var isInitialized = false
    private var isColdStart = true
    private var returnCount = 0
    private var currentActivity: Activity? = null
    private var adUnitId: String? = null

    /**
     * One-shot skip for the very next return-to-foreground — set this to true right before
     * intentionally sending the user to system Settings for something unrelated to "switching
     * away from the app" (e.g. the Offline dialog's Wi-Fi Settings shortcut), so that return
     * doesn't get treated as an app-switch-back and show an ad right when they're just fixing
     * their connection. Consumed (reset to false) the next time onStart fires.
     */
    var isAdPaused = false

    fun init(application: Application, adUnitId: String) {
        this.adUnitId = adUnitId
        AppOpenAdManager.preload(application, adUnitId)
        if (isInitialized) return
        isInitialized = true
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isColdStart) {
            // The app's own first launch — not a "returned from background" moment.
            isColdStart = false
            return
        }
        if (isAdPaused) {
            isAdPaused = false
            return
        }
        returnCount++
        if (returnCount % 2 != 0) return

        val activity = currentActivity ?: return
        val unitId = adUnitId ?: return

        // Don't interrupt onboarding/permission-granting — bouncing to system Settings for
        // overlay/MIUI/exact-alarm permissions and back would otherwise count as "returns" too.
        if (!SetupState.isFullySetUp(activity)) return

        // If the process is brought to foreground specifically because the After Call screen
        // is launching (after a phone call ends), don't interrupt it with an App Open Ad.
        // Showing an ad here also triggers AfterCallActivity's onUserLeaveHint, which instantly
        // finishes the After Call screen, dropping the user back to the app's main screen unexpectedly.
        if (activity is com.messages.ui.screens.AfterCallActivity) return

        if (AppOpenAdManager.isReady()) {
            AppOpenAdManager.show(activity, unitId) {}
        }
    }

    // Application.ActivityLifecycleCallbacks — only currentActivity tracking is needed here.
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
