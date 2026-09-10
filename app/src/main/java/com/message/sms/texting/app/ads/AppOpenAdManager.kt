package com.message.sms.texting.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.message.sms.texting.app.utils.AnalyticsManager

/**
 * Preloads an App Open ad and shows it on demand â€” used from Splash, gated by [AppOpenCounter]'s
 * cold-start cadence (not shown on every app foreground, only on specific cold-start counts).
 */
object AppOpenAdManager {
    // Google's own App Open ad guidance: a loaded ad is only valid for ~4 hours -- calling
    // show() past that is undefined (sometimes neither onAdDismissedFullScreenContent nor
    // onAdFailedToShowFullScreenContent fires at all), which left Splash's loader stuck forever
    // waiting on a completion callback that was never going to come. Confirmed on a real,
    // published build: the ad had been sitting preloaded-but-unshown long enough to expire.
    private const val EXPIRY_MS = 4 * 60 * 60 * 1000L

    private var appOpenAd: AppOpenAd? = null
    private var loadTimeMs: Long = 0L
    private var isLoading = false

    fun preload(context: Context, adUnitId: String) {
        if (isLoading || isReady()) return
        isLoading = true
        AnalyticsManager.logAdEvent("app_open", adUnitId, "request")
        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTimeMs = System.currentTimeMillis()
                    isLoading = false
                    AnalyticsManager.logAdEvent("app_open", adUnitId, "loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    AnalyticsManager.logAdEvent("app_open", adUnitId, "failed_to_load")
                }
            }
        )
    }

    /** Self-clears an expired ad so the next [preload] call (isLoading/isReady both now false)
     * actually fires instead of being blocked by a stale reference forever. */
    fun isReady(): Boolean {
        if (appOpenAd != null && System.currentTimeMillis() - loadTimeMs > EXPIRY_MS) {
            appOpenAd = null
        }
        return appOpenAd != null
    }

    /**
     * Shows the preloaded ad if ready; [onDismissed] always fires exactly once either way.
     * Pass [adUnitId] to have the next one auto-preload right after this one is dismissed
     * (used by the background-return trigger, which may need another later in the same session).
     */
    fun show(activity: Activity, adUnitId: String? = null, onDismissed: () -> Unit) {
        val ad = appOpenAd
        if (ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                adUnitId?.let {
                    AnalyticsManager.logAdEvent("app_open", it, "dismissed")
                    preload(activity.applicationContext, it)
                }
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                adUnitId?.let {
                    AnalyticsManager.logAdEvent("app_open", it, "failed_to_show")
                    preload(activity.applicationContext, it)
                }
                onDismissed()
            }

            override fun onAdClicked() {
                adUnitId?.let { AnalyticsManager.logAdEvent("app_open", it, "clicked") }
            }
        }
        adUnitId?.let { AnalyticsManager.logAdEvent("app_open", it, "shown") }
        ad.show(activity)
    }
}
