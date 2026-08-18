package com.messages.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.messages.ui.theme.Inter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.messages.utils.AppPreferences
import com.messages.utils.MiuiUtils
import com.messages.navigation.Routes
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.messages.R
import com.messages.ads.AdLoadingScreen
import com.messages.ads.AppOpenAdManager
import com.messages.ads.AppOpenCounter
import com.messages.ads.ColdStartAdType
import com.messages.ads.InterstitialAdManager
import com.messages.ads.NativeAdCache
import com.messages.ads.waitUntilAdReady
import com.messages.viewmodel.AppConfigViewModel

private val SplashLogoWidth = 190.dp
private val SplashLogoHeight = 162.dp
private val SplashDotsOffsetX = 49.dp
private val SplashDotsOffsetY = 58.dp
private val SplashDotSize = 14.dp
private val SplashDotSpacing = 10.dp
private val SplashGradientTop = Color(0xFF105FE9)
private val SplashGradientBottom = Color(0xFF0E60CA)

@Composable
fun SplashScreen(onTimeout: (String) -> Unit, skipAnimation: Boolean = false) {
    val view = LocalView.current
    var displayedText by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }
    var isTyping by remember { mutableStateOf(false) }
    var showAdLoader by remember { mutableStateOf(false) }

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(view.context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()

    val canRequestAds by com.messages.ads.UmpConsentManager.canRequestAds.collectAsState()

    val fullText = remember { appConfigViewModel.dynamicAppName.value }

    // Preload as soon as config is available — runs in parallel with the splash animation below,
    // so the ad (if this open's cadence calls for one) is ready by the time we're done typing.
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
        // returning user who skips Language entirely and lands straight on Home.
        if (!AppPreferences(view.context).languageSelected) {
            // scratch.
            if (result.native_2_on_off == "on") {
                result.native_2?.takeIf { it.isNotBlank() }?.let {
                    NativeAdCache.preload(view.context, it)
                }
            }
        } else {
            // Home is the screen almost every returning session lands on (unlike e.g. Schedule's

            if (result.native_1_on_off == "on") {
                result.native_1?.takeIf { it.isNotBlank() }?.let {
                    NativeAdCache.preload(view.context, it)
                }
            }
            if (result.banner_1_on_off == "on") {
                result.banner_1?.takeIf { it.isNotBlank() }?.let {
                    com.messages.ads.BannerAdCache.preload(view.context, it)
                }
            }
        }
    }

    LaunchedEffect(isTyping) {
        if (isTyping) {
            showCursor = true
        } else {
            while (true) {
                delay(500)
                showCursor = !showCursor
            }
        }
    }

    LaunchedEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val hasNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    view.context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        val hasPhone = ContextCompat.checkSelfPermission(
            view.context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            view.context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        val prefs = AppPreferences(view.context)
        val isFullyOnboarded = prefs.onboardingCompleted && hasNotif && hasPhone
        val isPermissionsDone = isFullyOnboarded && Settings.canDrawOverlays(view.context) &&
                (!MiuiUtils.isMiui() || (prefs.miuiPermissionsCompleted && prefs.miuiAutostartCompleted))

        val isDefaultSms =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager =
                    view.context.getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager
                roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
            } else {
                Telephony.Sms.getDefaultSmsPackage(view.context) == view.context.packageName
            }

        // Skipped for deep-link launches (e.g. tapping a message in the After Call overlay) —
        // the branding animation only makes sense for a normal cold app open.
        if (!skipAnimation) {
            delay(300)

            isTyping = true
            fullText.forEachIndexed { index, char ->
                displayedText += char
                delay(Random.nextLong(150, 250))
            }
            isTyping = false

            delay(800)
        }

        val nextRoute =
            if (!prefs.languageSelected) Routes.ChooseLanguage.createRoute(firstRun = true)
            else if (isPermissionsDone && isDefaultSms) Routes.Dashboard.route
            else if (isPermissionsDone) Routes.DefaultSms.route
            else if (isFullyOnboarded) Routes.Permissions.route
            else Routes.Onboarding.route

        // The cold-start App Open/Interstitial cadence only applies once setup is fully done —
        // showing an ad mid-onboarding would be jarring, and those screens have their own ads.
        if (!skipAnimation && nextRoute == Routes.Dashboard.route) {
            val openCount = AppOpenCounter.incrementAndGet(view.context)
            val adType = AppOpenCounter.adTypeFor(openCount)
            val activity = view.context as? Activity
            val adsEnabled =
                appConfigViewModel.appResponse.value?.result?.google_ads_on_off == "on" &&
                        appConfigViewModel.isOnline.value

            if (adsEnabled && activity != null && adType == ColdStartAdType.APP_OPEN) {
                if (!AppOpenAdManager.isReady()) {
                    showAdLoader = true
                    waitUntilAdReady { AppOpenAdManager.isReady() }
                }
                if (AppOpenAdManager.isReady()) {
                    AppOpenAdManager.show(activity) { onTimeout(nextRoute) }
                    return@LaunchedEffect
                }
            } else if (adsEnabled && activity != null && adType == ColdStartAdType.INTERSTITIAL) {
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
            .background(Brush.verticalGradient(listOf(SplashGradientTop, SplashGradientBottom))),
        contentAlignment = Alignment.Center
    ) {
        if (showAdLoader) {
            AdLoadingScreen(modifier = Modifier.fillMaxSize())
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SplashLogo()
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayedText,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter
                    )
                    Text(
                        text = "|",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = Inter,
                        modifier = Modifier.alpha(if (showCursor) 1f else 0f)
                    )
                }
            }
        }
    }
}

/** App's message-bubble logo with an animated "typing" three-dot indicator over the bubble. */
@Composable
private fun SplashLogo() {
    // Entrance: logo isn't there at all at first (scale 0 / invisible), then pops in with a
    // bouncy zoom-in — only once that settles does the typing-dot loop start.
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150)
        launch { alpha.animateTo(1f, animationSpec = tween(durationMillis = 400)) }
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
    }
    Box(
        modifier = Modifier
            .width(SplashLogoWidth)
            .height(SplashLogoHeight)
            .scale(scale.value)
            .alpha(alpha.value)
    ) {
        Image(
            painter = painterResource(R.drawable.logo_message),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        TypingDots(
            modifier = Modifier.offset(x = SplashDotsOffsetX, y = SplashDotsOffsetY),
            dotColor = SplashGradientTop,
            dotSize = SplashDotSize,
            spacing = SplashDotSpacing
        )
    }
}

/** Three dots that bounce in a left-to-right wave, like a chat app's "typing…" indicator. */
@Composable
private fun TypingDots(modifier: Modifier = Modifier, dotColor: Color, dotSize: Dp, spacing: Dp) {
    val transition = rememberInfiniteTransition(label = "typingDots")
    val bouncePx = with(LocalDensity.current) { 6.dp.toPx() }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(3) { index ->
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0f at 0
                        -bouncePx at 200
                        0f at 400
                    },
                    initialStartOffset = StartOffset(index * 150)
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .background(dotColor, CircleShape)
            )
        }
    }
}
