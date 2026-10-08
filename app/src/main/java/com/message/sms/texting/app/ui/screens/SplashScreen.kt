package com.message.sms.texting.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.message.sms.texting.app.ui.theme.Inter
import kotlinx.coroutines.delay

import com.message.sms.texting.app.utils.AppPreferences
import com.message.sms.texting.app.navigation.Routes
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ads.AdLoadingScreen
import com.message.sms.texting.app.ads.AppOpenAdManager
import com.message.sms.texting.app.ads.AppOpenCounter
import com.message.sms.texting.app.ads.ColdStartAdType
import com.message.sms.texting.app.ads.InterstitialAdManager
import com.message.sms.texting.app.ads.NativeAdCache
import com.message.sms.texting.app.ads.waitUntilAdReady
import com.message.sms.texting.app.viewmodel.AppConfigViewModel

private val SplashLogoBoxSize = 120.dp
private val SplashLogoBoxRadius = 20.dp

@Composable
fun SplashScreen(onTimeout: (String) -> Unit, skipAnimation: Boolean = false) {
    val view = LocalView.current
    var showAdLoader by remember { mutableStateOf(false) }

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(view.context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()

    val canRequestAds by com.message.sms.texting.app.ads.UmpConsentManager.canRequestAds.collectAsState()

    // Preload as soon as config is available â€” runs in parallel with the splash below, so the ad
    // (if this open's cadence calls for one) is ready by the time the splash pause is done.
    LaunchedEffect(adConfig, canRequestAds) {
        if (!canRequestAds) return@LaunchedEffect
        val result = adConfig?.result ?: return@LaunchedEffect
        if (result.google_ads_on_off != "on") return@LaunchedEffect
        if (result.app_open_1_on_off == "on") {
            result.app_open_1?.takeIf { it.isNotBlank() }?.let {
                AppOpenAdManager.preload(view.context, it)
            }
        }
        if (result.interstitial_3_on_off == "on") {
            result.interstitial_3?.takeIf { it.isNotBlank() }?.let {
                InterstitialAdManager.preload(view.context, it)
            }
        }
        // native_2 (ChooseLanguageScreen's ad) is NOT preloaded here anymore -- moved to
        // MainActivity's own LaunchedEffect(adConfig, canRequestAds), which stays alive for the
        // whole Activity session. This one only lives as long as Splash itself (~1.1s branding
        // delay) -- if adConfig arrived any later than that (e.g. the very first fetch, no cache
        // yet), Splash had already navigated away to Permissions before this ever got a chance to
        // fire, and nothing was left listening for adConfig to catch up. ChooseLanguageScreen
        // (reached after Permissions) then found nothing cached and loaded fresh, visibly slower
        // -- exactly the first-run race this was rewritten to fix.
        if (AppPreferences(view.context).languageSelected) {
            if (result.native_1_on_off == "on") {
                result.native_1?.takeIf { it.isNotBlank() }?.let {
                    NativeAdCache.preload(view.context, it)
                }
            }
            if (result.banner_1_on_off == "on") {
                result.banner_1?.takeIf { it.isNotBlank() }?.let {
                    com.message.sms.texting.app.ads.BannerAdCache.preload(view.context, it)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val activity = view.context as? Activity
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Gesture navigation devices skip hiding the nav bar (status bar hiding is unaffected
            // and stays unconditional) -- see MainActivity's matching comment: hiding the nav bar
            // has been observed to also disable the OS's own edge-swipe back gesture entirely on
            // some OEM skins (OxygenOS/ColorOS).
            val typesToHide = if (com.message.sms.texting.app.utils.isGestureNavigationEnabled(activity)) {
                WindowInsetsCompat.Type.statusBars()
            } else {
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            }
            insetsController.hide(typesToHide)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val prefs = AppPreferences(view.context)
        // Notification permission is now asked in context on the inbox itself (see
        // HomeScreen.kt), and phone/call-log permissions plus Overlay/MIUI-autostart only matter
        // for the After Call feature, which moved to an opt-in offered later from Settings --
        // none of them gate first-launch setup anymore. This screen's job is just the welcome
        // step and handing off to language/default-SMS.
        val isFullyOnboarded = prefs.onboardingCompleted

        // Auto-detect from the device's own language if it's one of the app's supported ones --
        // skips ChooseLanguageScreen (and its ad) entirely for most users; only a genuinely
        // unsupported device language still shows the manual picker. Manual override always
        // stays available from Settings regardless of how this was set.
        if (!prefs.languageSelected) {
            val deviceLanguageCode = java.util.Locale.getDefault().language
            val supportedMatch = com.message.sms.texting.app.ui.theme.AppLanguages
                .find { it.code == deviceLanguageCode }
            if (supportedMatch != null) {
                com.message.sms.texting.app.ui.theme.LanguageState.setLanguage(view.context, supportedMatch.code)
                prefs.languageSelected = true
            }
        }

        val isDefaultSms =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager =
                    view.context.getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager
                roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
            } else {
                Telephony.Sms.getDefaultSmsPackage(view.context) == view.context.packageName
            }

        // Skipped for deep-link launches (e.g. tapping a message in the After Call overlay) â€”
        // the brief branding pause only makes sense for a normal cold app open.
        if (!skipAnimation) {
            delay(1100)
        }

        // Permissions.route (Overlay/Battery/MIUI+OnePlus autostart -- all for After Call) is
        // deliberately not part of this chain anymore; it's only reached later, opt-in, from
        // Settings once a user actually turns After Call on. Onboarding's old welcome copy is
        // now merged into DefaultSmsScreen itself (one screen, one button) -- so a user who
        // hasn't finished onboarding yet goes straight there; it sets onboardingCompleted once
        // default-SMS is confirmed, then hands off to language/Dashboard.
        val nextRoute =
            if (!isFullyOnboarded) Routes.DefaultSms.route
            else if (!prefs.languageSelected) Routes.ChooseLanguage.createRoute(firstRun = true)
            else if (!isDefaultSms) Routes.DefaultSms.route
            else Routes.Dashboard.route

        // The cold-start App Open/Interstitial cadence only applies once setup is fully done â€”
        // showing an ad mid-onboarding would be jarring, and those screens have their own ads.
        if (!skipAnimation && nextRoute == Routes.Dashboard.route) {
            val openCount = AppOpenCounter.incrementAndGet(view.context)
            val adType = AppOpenCounter.adTypeFor(openCount)
            val activity = view.context as? Activity
            val adsEnabled =
                appConfigViewModel.appResponse.value?.result?.google_ads_on_off == "on" &&
                        appConfigViewModel.isOnline.value

            val appOpenSlotOn = appConfigViewModel.appResponse.value?.result?.app_open_1_on_off == "on"
            val interstitialSlotOn = appConfigViewModel.appResponse.value?.result?.interstitial_3_on_off == "on"

            if (adsEnabled && appOpenSlotOn && activity != null && adType == ColdStartAdType.APP_OPEN) {
                if (!AppOpenAdManager.isReady()) {
                    showAdLoader = true
                    waitUntilAdReady { AppOpenAdManager.isReady() }
                }
                if (AppOpenAdManager.isReady()) {
                    AppOpenAdManager.show(activity) { onTimeout(nextRoute) }
                    return@LaunchedEffect
                }
            } else if (adsEnabled && interstitialSlotOn && activity != null && adType == ColdStartAdType.INTERSTITIAL) {
                val interstitialAdUnitId =
                    appConfigViewModel.appResponse.value?.result?.interstitial_3
                if (!interstitialAdUnitId.isNullOrBlank()) {
                    if (!InterstitialAdManager.isReady(interstitialAdUnitId)) {
                        showAdLoader = true
                        waitUntilAdReady { InterstitialAdManager.isReady(interstitialAdUnitId) }
                    }
                    if (InterstitialAdManager.isReady(interstitialAdUnitId)) {
                        InterstitialAdManager.show(activity, interstitialAdUnitId) {
                            onTimeout(
                                nextRoute
                            )
                        }
                        return@LaunchedEffect
                    }
                }
            }
        }

        onTimeout(nextRoute)
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.bg_primary)),
        contentAlignment = Alignment.Center
    ) {
        if (showAdLoader) {
            AdLoadingScreen(modifier = Modifier.fillMaxSize())
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SplashLogo()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.splash_brand_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Inter,
                    color = colorResource(R.color.text_title)
                )
            }
            SplashLoaderBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 34.dp)
            )
        }
    }
}

/** App's message-bubble logo, plain -- no background box behind it, just a rounded clip on the
 * image itself. */
@Composable
private fun SplashLogo() {
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = null,
        modifier = Modifier
            .size(SplashLogoBoxSize)
            .clip(RoundedCornerShape(SplashLogoBoxRadius))
    )
}

@Composable
private fun SplashLoaderBar(modifier: Modifier = Modifier) {
    androidx.compose.material3.LinearProgressIndicator(
        modifier = modifier
            .height(6.dp),
        color = colorResource(R.color.primary),
        trackColor = colorResource(R.color.primary).copy(alpha = 0.25f),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt
    )
}
