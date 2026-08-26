package com.message.sms.texting.app.ads

import android.content.Context
import com.message.sms.texting.app.utils.SetupState

/**
 * Decides whether a given back-navigation (pop) anywhere in the app should trigger an
 * interstitial â€” every 4th one does, not every single back (spammy/policy-risky). The counter
 * resets after each trigger, so the pattern repeats: 3 backs with no ad, 4th shows one.
 *
 * Deliberately NOT triggered on the back-press that exits the app entirely (e.g. from Home) â€”
 * that never reaches this manager in the first place, since it's detected via NavController
 * destination changes, and an app-exit back doesn't produce one (nothing left to pop to).
 *
 * Two ad units: [primaryAdUnitId] is tried first, [fallbackAdUnitId] only if the primary isn't
 * ready â€” improves fill rate without needing the caller to know about the fallback. Only the
 * primary is preloaded eagerly (kept warm the whole session); the fallback loads reactively,
 * only once the primary has actually failed to be ready at a trigger â€” no point holding two ad
 * objects warm at all times for a trigger that only ever shows one of them.
 *
 * This object only *decides*; it doesn't call [InterstitialAdManager.show] itself â€” see the
 * caller in AppNavigation.kt for why the actual show() is deferred via a Channel instead of
 * called straight from here or from the NavController listener.
 */
object GlobalBackAdManager {
    private const val TRIGGER_EVERY = 3

    private var backPressCount = 0
    private var primaryAdUnitId: String? = null
    private var fallbackAdUnitId: String? = null

    fun configure(context: Context, primaryAdUnitId: String?, fallbackAdUnitId: String?) {
        this.primaryAdUnitId = primaryAdUnitId
        this.fallbackAdUnitId = fallbackAdUnitId
        primaryAdUnitId?.let { InterstitialAdManager.preload(context, it) }
    }

    /**
     * Call on every detected back-navigation (pop) within the app's nav graph. Returns the ad
     * unit id to show if this is the triggering (every 4th) back-press and a ready ad is
     * available, or null otherwise â€” showing it is the caller's responsibility.
     */
    fun resolveAdUnitIdForBackPress(context: Context): String? {
        // Don't interrupt onboarding/permission-granting with an ad on back-navigation.
        if (!SetupState.isFullySetUp(context)) return null

        backPressCount++
        if (backPressCount < TRIGGER_EVERY) return null
        backPressCount = 0

        val primary = primaryAdUnitId
        if (primary != null && InterstitialAdManager.isReady(primary)) return primary

        val fallback = fallbackAdUnitId
        if (fallback != null && InterstitialAdManager.isReady(fallback)) return fallback

        // Primary wasn't ready this time â€” start warming the fallback so it has a chance to be
        // ready by the next trigger, 4 back-presses from now.
        fallback?.let { InterstitialAdManager.preload(context, it) }
        return null
    }
}
