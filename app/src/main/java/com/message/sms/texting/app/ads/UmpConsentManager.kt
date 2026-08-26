package com.message.sms.texting.app.ads

import android.app.Activity
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Gathers the user's advertising consent (GDPR/UK-GDPR, via Google's UMP SDK) once at app
 * startup, before the first ad request of the session. UMP/AdMob detect on their own whether this
 * is even required for the user's region (EEA/UK/applicable US states) â€” everywhere else (e.g.
 * India, where this app is mostly tested) this resolves near-instantly with no form ever shown.
 *
 * Once resolved here, the rest of the session's ad requests â€” all the scattered preload/show call
 * sites elsewhere in the app (Home, Chat, Schedule, After Call, back-nav, ...) â€” don't need their
 * own gating: the Google Mobile Ads SDK reads the recorded consent signal itself for every
 * request. Only the *first* request of the session needs to wait for this to resolve, which is
 * why this is gathered as early as possible (MainActivity.onCreate) and the earliest ad-preload
 * points (MainActivity's own effect, SplashScreen's) wait on [canRequestAds] before firing.
 */
object UmpConsentManager {
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds

    private var hasGathered = false

    suspend fun gatherConsent(activity: Activity) {
        if (hasGathered) return
        hasGathered = true

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        val updateSucceeded = suspendCancellableCoroutine { cont ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                { cont.resume(true) },
                { cont.resume(false) }
            )
        }

        if (updateSucceeded) {
            suspendCancellableCoroutine { cont ->
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }

        // Even if the update failed (e.g. offline), canRequestAds() falls back to whatever was
        // already recorded from a previous successful session â€” never blocks ads forever just
        // because this one attempt couldn't reach Google's servers.
        _canRequestAds.value = consentInformation.canRequestAds()
    }
}
