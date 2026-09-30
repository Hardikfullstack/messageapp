package com.message.sms.texting.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.message.sms.texting.app.ads.AppOpenBackgroundReturnTrigger
import com.message.sms.texting.app.ads.GlobalBackAdManager
import com.message.sms.texting.app.ads.UmpConsentManager
import com.message.sms.texting.app.navigation.AppNavigation
import com.message.sms.texting.app.ui.theme.FontSizeState
import com.message.sms.texting.app.ui.theme.MessagesTheme
import com.message.sms.texting.app.viewmodel.AppConfigViewModel

class MainActivity : AppCompatActivity() {
    private var deepLinkRoute by mutableStateOf<String?>(null)

    // Applied at the Configuration level (not just a Compose LocalDensity override) so
    // Dialogs/DropdownMenus/Popups â€” which spin up their own sub-window and re-derive
    // density from that window's own Resources â€” pick it up too, not just plain screen content.
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("messages_prefs", Context.MODE_PRIVATE)
        val fontSizeMode = prefs.getString("app_font_size_mode", "normal") ?: "normal"
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = FontSizeState.scaleFor(fontSizeMode)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    // Play Core's IMMEDIATE in-app-update flow (HomeScreen.kt) is launched the classic
    // startActivityForResult way, not via the newer Activity Result API -- a Composable can't
    // override onActivityResult itself, so the outcome is forwarded through a small shared object
    // for whichever screen wants to react to it (currently: just logging it, see
    // InAppUpdateResult's doc comment for why nothing more than that is needed right now).
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == com.message.sms.texting.app.utils.InAppUpdateResult.REQUEST_CODE) {
            com.message.sms.texting.app.utils.InAppUpdateResult.pendingResultCode = resultCode
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() â€” hands off from the system splash (Theme.App.Starting,
        // see themes.xml) to postSplashScreenTheme as soon as this Activity's first frame is
        // drawn, so our own SplashScreen.kt composable takes over instead of the system splash
        // lingering (its default dismiss condition is "first frame drawn", which is what we want).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            // Instantiated here so its init{} fires and fetches the remote ad/app config on
            // startup â€” temporary, only for inspecting the API response via Logcat for now.
            val appConfigViewModel: AppConfigViewModel = viewModel()
            val adConfig by appConfigViewModel.appResponse.collectAsState()
            val canRequestAds by UmpConsentManager.canRequestAds.collectAsState()

            val insetsController = androidx.compose.runtime.remember {
                WindowCompat.getInsetsController(window, window.decorView)
            }

            LaunchedEffect(Unit) {
                if (!com.message.sms.texting.app.utils.isGestureNavigationEnabled(this@MainActivity)) {
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                }
            }

            // Gathered once, as early as possible â€” the very first ad request of the session
            // (right below, and SplashScreen's own preloads) waits on canRequestAds instead of
            // firing before consent is known. See UmpConsentManager's doc comment.
            LaunchedEffect(Unit) {
                UmpConsentManager.gatherConsent(this@MainActivity)
            }

            LaunchedEffect(adConfig, canRequestAds) {
                if (!canRequestAds) return@LaunchedEffect
                val result = adConfig?.result ?: return@LaunchedEffect
                if (result.google_ads_on_off != "on") return@LaunchedEffect

                if (result.app_open_1_on_off == "on") {
                    result.app_open_1?.takeIf { it.isNotBlank() }?.let {
                        AppOpenBackgroundReturnTrigger.init(application, it)
                    }
                }

                // Every-4th-back-navigation interstitial (any screen) â€” gated by the panel's own
                // back_click master switch ("1" = on; "0"/null/anything else = feature fully off),
                // separate from the individual ad-unit on/off flags below.
                if (result.back_click == "1") {
                    val backPrimary = result.interstitial_4?.takeIf { result.interstitial_4_on_off == "on" && it.isNotBlank() }
                    val backFallback = result.interstitial_5?.takeIf { result.interstitial_5_on_off == "on" && it.isNotBlank() }
                    if (backPrimary != null || backFallback != null) {
                        GlobalBackAdManager.configure(application, backPrimary, backFallback)
                    }
                }

                // ChooseLanguageScreen's ad used to be preloaded here for the first-run case, but
                // that screen no longer shows an ad at all when isFirstRun is true (first-session
                // setup screens shouldn't carry ads) -- and !languageSelected only ever happens
                // during that same first-run flow in this app's nav structure (a later manual
                // visit from Settings always has languageSelected already true by then), so this
                // preload had nothing left to serve. Removed rather than left preloading an ad
                // that could never actually be shown.
            }

            MessagesTheme(darkTheme = isSystemInDarkTheme()) {
                AppNavigation(
                    deepLinkRoute = deepLinkRoute,
                    onDeepLinkConsumed = { deepLinkRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("navigate_to_chat", false)) {
            val threadId = intent.getLongExtra("threadId", 0L)
            val address = intent.getStringExtra("address") ?: ""
            val contactName = intent.getStringExtra("contactName")
            val forwardText = intent.getStringExtra("forwardText")
            deepLinkRoute = com.message.sms.texting.app.navigation.Routes.Chat.createRoute(threadId, address, contactName, forwardText = forwardText)
        } else if (intent.getBooleanExtra("navigate_to_new_chat", false)) {
            val forwardText = intent.getStringExtra("forwardText")
            deepLinkRoute = com.message.sms.texting.app.navigation.Routes.NewChat.createRoute(forwardText = forwardText)
        }
    }
}