package com.messages.sms.texting.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.messages.sms.texting.app.utils.AnalyticsManager

/**
 * Preloads an App Open ad and shows it on demand — used from Splash, gated by [AppOpenCounter]'s
 * cold-start cadence (not shown on every app foreground, only on specific cold-start counts).
 */
object AppOpenAdManager {
    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false

    fun preload(context: Context, adUnitId: String) {
        if (isLoading || appOpenAd != null) return
        isLoading = true
        AnalyticsManager.logAdEvent("app_open", adUnitId, "request")
        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
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

    fun isReady(): Boolean = appOpenAd != null

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
