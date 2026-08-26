package com.message.sms.texting.app.ui.screens

import com.message.sms.texting.app.navigation.popBackStackWithAd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ads.NativeAdTemplate
import com.message.sms.texting.app.ads.NativeAdView
import com.message.sms.texting.app.ui.components.SecondaryTopBar
import com.message.sms.texting.app.ui.theme.AppLanguages
import com.message.sms.texting.app.ui.theme.Inter
import com.message.sms.texting.app.ui.theme.LanguageState
import com.message.sms.texting.app.utils.AnalyticsManager
import com.message.sms.texting.app.utils.AppPreferences
import com.message.sms.texting.app.viewmodel.AppConfigViewModel

@Composable
fun ChooseLanguageScreen(
    navController: NavController,
    isFirstRun: Boolean = false,
    onFirstRunDone: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCode by remember { mutableStateOf(LanguageState.code.value) }
    val strChooseLanguageTitle = stringResource(R.string.choose_language_title)
    val strDone = stringResource(R.string.content_desc_done)

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val bigNativeAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.native_2_on_off == "on") {
            result.native_2?.takeIf { it.isNotBlank() }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.bg_primary))
    ) {
        SecondaryTopBar(
            title = strChooseLanguageTitle,
            // First run: this is the very first screen after Splash, nothing to go back to.
            onBackClick = { if (!isFirstRun) navController.popBackStackWithAd() },
            showBackButton = !isFirstRun,
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(77.dp))
                        .background(colorResource(R.color.primary))
                        .clickable {
                            // setApplicationLocales() alone doesn't reliably refresh this running
                            // Activity's already-resolved strings â€” recreate() is required, same
                            // as the theme/font-size changes elsewhere in Settings. Navigate first
                            // so the saved instance state recreate() restores from already reflects
                            // the *next* screen (Onboarding / back to Settings), not this one.
                            LanguageState.setLanguage(context, selectedCode)
                            AnalyticsManager.logEventWithAction(
                                "language_changed",
                                "ChooseLanguageScreen",
                                selectedCode,
                                mapOf("first_run" to isFirstRun)
                            )
                            if (isFirstRun) {
                                AppPreferences(context).languageSelected = true
                                onFirstRunDone()
                            } else {
                                navController.popBackStackWithAd()
                            }
                            (context as? android.app.Activity)?.recreate()
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.language_ic_done),
                        contentDescription = strDone,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AppLanguages.forEach { language ->
                LanguageRow(
                    englishName = language.englishName,
                    nativeName = language.nativeName,
                    selected = language.code == selectedCode,
                    onClick = { selectedCode = language.code }
                )
            }
        }

        if (bigNativeAdUnitId != null) {
            NativeAdView(
                adUnitId = bigNativeAdUnitId,
                template = NativeAdTemplate.MEDIUM,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun LanguageRow(
    englishName: String,
    nativeName: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = englishName,
                    fontSize = 18.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_title),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "($nativeName)",
                    fontSize = 14.sp,
                    fontFamily = Inter,
                    color = colorResource(R.color.text_des),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.offset(x = 8.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorResource(R.color.primary),
                    unselectedColor = colorResource(R.color.light_color_gray)
                )
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = colorResource(R.color.light_gray),
            thickness = 0.9.dp
        )
    }
}
