package com.message.sms.texting.app.navigation

import androidx.navigation.NavController
import android.content.Context
import android.app.Activity
import com.message.sms.texting.app.ads.GlobalBackAdManager
import kotlinx.coroutines.flow.MutableStateFlow

object GlobalAdLoader {
    val isLoading = MutableStateFlow(false)
}

fun NavController.popBackStackWithAd(
    route: String? = null,
    inclusive: Boolean = false
) {
    val adUnitId = GlobalBackAdManager.resolveAdUnitIdForBackPress(this.context)
    if (adUnitId != null) {
        val activity = this.context as? Activity
        if (activity != null) {
            // resolveAdUnitIdForBackPress already confirmed this ad is ready (isReady check
            // happens inside it) â€” show immediately instead of an artificial delay/loader.
            com.message.sms.texting.app.ads.InterstitialAdManager.show(activity, adUnitId) {
                if (route != null) {
                    this.popBackStack(route, inclusive)
                } else {
                    this.popBackStack()
                }
            }
            return
        }
    }
    if (route != null) {
        this.popBackStack(route, inclusive)
    } else {
        this.popBackStack()
    }
}
