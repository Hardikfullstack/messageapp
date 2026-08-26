package com.message.sms.texting.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter
import com.message.sms.texting.app.ui.components.CommonTopBar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.message.sms.texting.app.ui.modifiers.animatedPulse
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.message.sms.texting.app.ads.AdLoadingScreen
import com.message.sms.texting.app.ads.InterstitialAdManager
import com.message.sms.texting.app.ads.waitUntilAdReady
import com.message.sms.texting.app.utils.AnalyticsManager
import com.message.sms.texting.app.viewmodel.AppConfigViewModel

@Composable
fun DefaultSmsScreen(onDefaultSmsSet: () -> Unit) {
    val context = LocalContext.current
    val isDefaultSms = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName

    val coroutineScope = rememberCoroutineScope()

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()

    LaunchedEffect(adConfig) {
        val result = adConfig?.result ?: return@LaunchedEffect
        if (result.google_ads_on_off != "on") return@LaunchedEffect
        if (result.interstitial_1_on_off == "on") {
            result.interstitial_1?.takeIf { it.isNotBlank() }?.let {
                InterstitialAdManager.preload(context, it)
            }
        }
        // This is the last onboarding step â€” Home is next for a first-run user too, so give its
        // native/banner a head start here (Splash skipped this for first-run, to prioritize
        // Language's own preload instead â€” see SplashScreen.kt).
        if (result.native_1_on_off == "on") {
            result.native_1?.takeIf { it.isNotBlank() }?.let {
                com.message.sms.texting.app.ads.NativeAdCache.preload(context, it)
            }
        }
        if (result.banner_1_on_off == "on") {
            result.banner_1?.takeIf { it.isNotBlank() }?.let {
                com.message.sms.texting.app.ads.BannerAdCache.preload(context, it)
            }
        }
    }

    var isWaitingForAd by remember { mutableStateOf(false) }
    // Guards against double-completion: the system "set default SMS" dialog's result callback
    // and the ON_RESUME polling loop below can both detect success and fire within moments of
    // each other, which without this would show the ad twice and call onDefaultSmsSet() twice â€”
    // the visible symptom being Home appearing, then a second nav transition sliding it in again.
    var hasCompletedDefaultSmsFlow by remember { mutableStateOf(false) }

    // Shows an interstitial right after the app is confirmed as the default SMS app, then
    // proceeds. Waits briefly (with a loading animation) for the ad to finish loading if it
    // isn't ready yet, instead of silently skipping it â€” falls through to onDefaultSmsSet()
    // either way once ready or timed out, never blocking setup indefinitely.
    suspend fun proceedAfterDefaultSmsSet() {
        if (hasCompletedDefaultSmsFlow) return
        hasCompletedDefaultSmsFlow = true
        AnalyticsManager.logEventWithAction("default_sms_set", "DefaultSmsScreen", "completed")

        val interstitialAdUnitId = appConfigViewModel.appResponse.value?.result?.let { result ->
            if (result.google_ads_on_off == "on" && result.interstitial_1_on_off == "on") {
                result.interstitial_1?.takeIf { it.isNotBlank() }
            } else null
        }
        val activity = context as? Activity
        // Offline â€” a cached config can still say the ad is "on" with nothing able to load it;
        // don't wait out the full timeout for an ad that can never arrive.
        if (activity != null && interstitialAdUnitId != null && appConfigViewModel.isOnline.value) {
            if (!InterstitialAdManager.isReady(interstitialAdUnitId)) {
                isWaitingForAd = true
                waitUntilAdReady { InterstitialAdManager.isReady(interstitialAdUnitId) }
            }
            if (InterstitialAdManager.isReady(interstitialAdUnitId)) {
                InterstitialAdManager.show(activity, interstitialAdUnitId) { onDefaultSmsSet() }
                return
            }
        }
        onDefaultSmsSet()
    }

    fun checkIsDefaultSms(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager =
                context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
            return roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
        }
        return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }

    LaunchedEffect(Unit) {
        if (checkIsDefaultSms()) {
            proceedAfterDefaultSmsSet()
        }
    }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (checkIsDefaultSms()) {
            coroutineScope.launch { proceedAfterDefaultSmsSet() }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    repeat(5) {
                        if (checkIsDefaultSms()) {
                            proceedAfterDefaultSmsSet()
                            return@launch
                        }
                        kotlinx.coroutines.delay(200)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val strDefaultSmsIllustration = stringResource(R.string.content_desc_default_sms_illustration)
    val strDefaultSmsDescription = stringResource(R.string.default_sms_description)
    val strSetDefaultSmsButton = stringResource(R.string.set_default_sms_button)

    if (isWaitingForAd) {
        AdLoadingScreen(modifier = Modifier.fillMaxSize())
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(R.color.bg_primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            CommonTopBar(title = "Messages")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.default_permission_main),
                    contentDescription = strDefaultSmsIllustration,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1.5f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = strDefaultSmsDescription,
                    fontSize = 16.sp,
                    fontFamily = Inter,
                    color = colorResource(R.color.text_des),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val roleManager =
                                context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
                            if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_SMS)) {
                                val intent =
                                    roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS)
                                defaultSmsLauncher.launch(intent)
                            } else {
                                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                                intent.putExtra(
                                    Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                                    context.packageName
                                )
                                defaultSmsLauncher.launch(intent)
                            }
                        } else {
                            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                            intent.putExtra(
                                Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                                context.packageName
                            )
                            defaultSmsLauncher.launch(intent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                        .animatedPulse(colorResource(R.color.primary)),
                    shape = RoundedCornerShape(77.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary))
                ) {
                    Text(
                        text = strSetDefaultSmsButton,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Inter,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
