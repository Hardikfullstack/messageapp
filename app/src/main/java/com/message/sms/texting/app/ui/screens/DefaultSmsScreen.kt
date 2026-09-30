package com.message.sms.texting.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.message.sms.texting.app.ui.modifiers.animatedPulse
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.ClickableText
import androidx.lifecycle.viewmodel.compose.viewModel
import com.message.sms.texting.app.utils.AnalyticsManager
import com.message.sms.texting.app.viewmodel.AppConfigViewModel

/** The single welcome screen now -- combines what used to be OnboardingScreen's welcome copy
 * with the "set as default SMS app" step, matching the recommended flow: one screen, one button,
 * no ad (see the removed interstitial-after-set logic below -- "never an ad right after a
 * permission or default-SMS step" is one of the explicit ad rules this was built against). */
@Composable
fun DefaultSmsScreen(onDefaultSmsSet: () -> Unit, onPrivacyPolicyClick: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()

    // No interstitial preload here anymore -- new users get zero interstitial/app-open ads in
    // their first session. Native/banner (Home's own list ads) still get a head start, since
    // those follow the separate "no native ad above row 3-4" rule instead, not "no ad at all".
    LaunchedEffect(adConfig) {
        val result = adConfig?.result ?: return@LaunchedEffect
        if (result.google_ads_on_off != "on") return@LaunchedEffect
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

    // Guards against double-completion: the system "set default SMS" dialog's result callback
    // and the ON_RESUME polling loop below can both detect success and fire within moments of
    // each other, which without this would call onDefaultSmsSet() twice -- the visible symptom
    // being Home appearing, then a second nav transition sliding it in again.
    var hasCompletedDefaultSmsFlow by remember { mutableStateOf(false) }

    suspend fun proceedAfterDefaultSmsSet() {
        if (hasCompletedDefaultSmsFlow) return
        hasCompletedDefaultSmsFlow = true
        com.message.sms.texting.app.utils.AppPreferences(context).onboardingCompleted = true
        AnalyticsManager.logEventWithAction("default_sms_set", "DefaultSmsScreen", "completed")
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
    val strWelcomeTo = stringResource(R.string.welcome_to)
    val strTextMessaging = stringResource(R.string.text_messaging)
    val strPrivacyAgreePrefix = stringResource(R.string.privacy_agree_prefix)
    val strPrivacyPolicyLinkText = stringResource(R.string.privacy_policy_link_text)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        color = colorResource(R.color.bg_primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = strWelcomeTo,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Inter,
                    lineHeight = 30.sp,
                    color = colorResource(R.color.text_title),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strTextMessaging,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Inter,
                    lineHeight = 30.sp,
                    color = colorResource(R.color.primary),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Image(
                    painter = painterResource(id = R.drawable.default_permission_main),
                    contentDescription = strDefaultSmsIllustration,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(1.2f)
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

                Spacer(modifier = Modifier.height(20.dp))
            }

            val privacyAnnotatedText = buildAnnotatedString {
                append(strPrivacyAgreePrefix)
                append(" ")
                withStyle(
                    style = SpanStyle(
                        color = colorResource(R.color.primary),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(strPrivacyPolicyLinkText)
                }
            }
            ClickableText(
                text = privacyAnnotatedText,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontFamily = Inter,
                    color = colorResource(R.color.text_des),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { onPrivacyPolicyClick() }
            )

            Spacer(modifier = Modifier.height(14.dp))

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
                    .fillMaxWidth(0.9f)
                    .height(58.dp)
                    .animatedPulse(colorResource(R.color.primary)),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary))
            ) {
                Text(
                    text = strSetDefaultSmsButton,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Inter,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
