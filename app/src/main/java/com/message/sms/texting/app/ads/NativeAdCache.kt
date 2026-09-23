package com.message.sms.texting.app.ads

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.message.sms.texting.app.utils.AnalyticsManager

/**
 * Holds a native ad *object* (independent of any View/Composable) loaded ahead of time â€” used
 * where we know a specific native placement is about to be needed before its screen even
 * composes (e.g. the Language screen's ad, preloaded from Splash for first-time users). Falls
 * back to loading on-render as usual (see [NativeAdView]) if nothing is cached â€” this is purely
 * an optional head start, not a requirement.
 */
object NativeAdCache {
    // Same staleness guidance InterstitialAdManager/AppOpenAdManager already follow for their
    // formats -- Google doesn't want a loaded-but-unshown ad handed to a view arbitrarily long
    // after it was requested. Without this, a preloaded-but-never-navigated-to slot (e.g. the
    // Language screen's ad if the user lingers on Permissions) could sit cached indefinitely.
    private const val EXPIRY_MS = 60 * 60 * 1000L

    private val ads = mutableMapOf<String, NativeAd>()
    private val loadTimesMs = mutableMapOf<String, Long>()
    private val loadingIds = mutableSetOf<String>()

    fun preload(context: Context, adUnitId: String) {
        if (adUnitId in loadingIds || isCached(adUnitId)) return
        loadingIds += adUnitId
        // Deferred a frame (Handler.post) instead of building/loading inline -- several preload()
        // calls firing back-to-back on the main thread during a cold start (Splash + MainActivity
        // both trigger some within the same startup window) can each cost enough time building the
        // AdLoader and starting the request (class loading, Play Services binder setup, especially
        // while MobileAds.initialize() is still running on its own background thread) that doing
        // them all synchronously in one go was slow enough to trip Android's ANR watchdog.
        // Posting lets each one run on its own message-queue iteration instead of stacking up.
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            AnalyticsManager.logAdEvent("native", adUnitId, "request")
            val adLoader = AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad ->
                    ads[adUnitId] = ad
                    loadTimesMs[adUnitId] = System.currentTimeMillis()
                    loadingIds -= adUnitId
                    AnalyticsManager.logAdEvent("native", adUnitId, "loaded")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingIds -= adUnitId
                        AnalyticsManager.logAdEvent("native", adUnitId, "failed_to_load")
                    }
                })
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    /** Self-clears (and destroys) an expired entry so a stale ad is never handed out, and so the
     * next [preload] call actually fires instead of being blocked by it forever. */
    private fun isCached(adUnitId: String): Boolean {
        val loadedAt = loadTimesMs[adUnitId]
        if (ads.containsKey(adUnitId) && loadedAt != null && System.currentTimeMillis() - loadedAt > EXPIRY_MS) {
            ads.remove(adUnitId)?.destroy()
            loadTimesMs.remove(adUnitId)
        }
        return ads.containsKey(adUnitId)
    }

    /** Hands over the cached ad for [adUnitId] if one finished loading and hasn't expired â€”
     * consumes it (won't be returned again), since a [NativeAd] can only ever be bound to one
     * view. */
    fun take(adUnitId: String): NativeAd? {
        if (!isCached(adUnitId)) return null
        loadTimesMs.remove(adUnitId)
        return ads.remove(adUnitId)
    }
}
