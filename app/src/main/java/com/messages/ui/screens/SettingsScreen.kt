package com.messages.ui.screens

import com.messages.navigation.popBackStackWithAd

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.messages.R
import com.messages.ads.BannerAdView
import com.messages.navigation.Routes
import com.messages.repository.SmsRepository
import com.messages.viewmodel.AppConfigViewModel
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.messages.ui.components.CustomSwitch
import com.messages.ui.components.SecondaryTopBar
import com.messages.ui.components.dialogs.ConfirmationDialog
import com.messages.ui.components.dialogs.DeleteOldMessagesDialog
import com.messages.ui.components.dialogs.DisableAfterCallDialog
import com.messages.ui.components.dialogs.RadioOptionDialog
import com.messages.ui.components.dialogs.RateUsDialog
import com.messages.ui.theme.AfterCallState
import com.messages.ui.theme.DelayedSendingState
import com.messages.ui.theme.DeleteOldMessagesState
import com.messages.ui.theme.DeliveryConfirmationState
import com.messages.ui.theme.FontSizeState
import com.messages.ui.theme.Inter
import com.messages.ui.theme.LanguageState
import com.messages.ui.theme.MmsCompressionState
import com.messages.ui.theme.ThemeState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val settingsBannerAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.banner_3_on_off == "on") {
            result.banner_3?.takeIf { it.isNotBlank() }
        } else null
    }

    var deliveryConfirmations by remember { mutableStateOf(DeliveryConfirmationState.enabled.value) }
    var afterCallScreen by remember { mutableStateOf(AfterCallState.enabled.value) }
    var showDisableAfterCallDialog by remember { mutableStateOf(false) }

    var selectedThemeMode by remember { mutableStateOf(ThemeState.mode.value) }
    var selectedFontSizeMode by remember { mutableStateOf(FontSizeState.mode.value) }
    var selectedDelayedSendingMode by remember { mutableStateOf(DelayedSendingState.mode.value) }
    var selectedDeleteOldDays by remember { mutableStateOf(DeleteOldMessagesState.days.intValue) }
    var selectedMmsCompressionMode by remember { mutableStateOf(MmsCompressionState.mode.value) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showDelayedSendingDialog by remember { mutableStateOf(false) }
    var showMmsCompressionDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var showDeleteOldMessagesDialog by remember { mutableStateOf(false) }
    var showDeleteOldMessagesConfirm by remember { mutableStateOf(false) }
    var showRateUsDialog by remember { mutableStateOf(false) }
    var pendingDeleteOldDays by remember { mutableStateOf(0) }
    var deleteOldMessagesConfirmCount by remember { mutableStateOf(0) }
    var isDeletingOldMessages by remember { mutableStateOf(false) }

    val strSettingsTitle = stringResource(R.string.settings_title)
    val strSectionAppearance = stringResource(R.string.section_appearance)
    val strAppThemeLabel = stringResource(R.string.app_theme_label)
    val strFontSizeLabel = stringResource(R.string.font_size_label)
    val strAppLanguageLabel = stringResource(R.string.app_language_label)
    val strSectionGeneral = stringResource(R.string.section_general)
    val strDelayedSendingLabel = stringResource(R.string.delayed_sending_label)
    val strSwipeActionsLabel = stringResource(R.string.swipe_actions_label)
    val strSwipeActionsDesc = stringResource(R.string.swipe_actions_desc)
    val strDeliveryConfirmationsLabel = stringResource(R.string.delivery_confirmations_label)
    val strDeliveryConfirmationsDesc = stringResource(R.string.delivery_confirmations_desc)
    val strDeleteOldMessagesLabel = stringResource(R.string.delete_old_messages_label)
    val strSettingNever = stringResource(R.string.setting_never)
    val strDeleteOldMessagesDaily = stringResource(R.string.delete_old_messages_daily)
    val strDeleteOldMessagesWeekly = stringResource(R.string.delete_old_messages_weekly)
    val strDeleteOldMessagesMonthly = stringResource(R.string.delete_old_messages_monthly)
    val strDeleteOldMessagesYearly = stringResource(R.string.delete_old_messages_yearly)
    val strDeleteOldMessagesEveryNDays = stringResource(R.string.delete_old_messages_every_n_days)
    val strDeleteOldMessagesConfirmTitle = stringResource(R.string.delete_old_messages_confirm_title)
    val strDeleteOldMessagesConfirmDescTemplate = stringResource(R.string.delete_old_messages_confirm_desc_template)
    val strActionYes = stringResource(R.string.action_yes)
    val strDeleteOldMessagesDeleting = stringResource(R.string.delete_old_messages_deleting)
    val strAutoCompressMmsLabel = stringResource(R.string.auto_compress_mms_label)
    val strMmsCompressionAutomatic = stringResource(R.string.mms_compression_automatic)
    val strMmsCompression100kb = stringResource(R.string.mms_compression_100kb)
    val strMmsCompression200kb = stringResource(R.string.mms_compression_200kb)
    val strMmsCompression300kb = stringResource(R.string.mms_compression_300kb)
    val strMmsCompression600kb = stringResource(R.string.mms_compression_600kb)
    val strMmsCompression1000kb = stringResource(R.string.mms_compression_1000kb)
    val strMmsCompression2000kb = stringResource(R.string.mms_compression_2000kb)
    val strMmsCompressionNone = stringResource(R.string.mms_compression_none)
    val strSyncMessagesLabel = stringResource(R.string.sync_messages_label)
    val strResyncLabel = stringResource(R.string.resync_label)
    val strSyncingLabel = stringResource(R.string.syncing_label)
    val strMessagesSyncedToast = stringResource(R.string.messages_synced_toast)
    val strSectionNotificationCalling = stringResource(R.string.section_notification_calling)
    val strNotificationLabel = stringResource(R.string.notification_label)
    val strAfterCallLabel = stringResource(R.string.after_call_label)
    val strAfterCallDesc = stringResource(R.string.after_call_desc)
    val strSectionAboutUs = stringResource(R.string.section_about_us)
    val strRateAppLabel = stringResource(R.string.rate_app_label)
    val strShareAppLabel = stringResource(R.string.share_app_label)
    val strShareAppTextTemplate = stringResource(R.string.share_app_text_template)
    val strHelpFeedbackLabel = stringResource(R.string.help_feedback_label)
    val strHelpFeedbackEmailSubject = stringResource(R.string.help_feedback_email_subject)
    val strNoEmailApp = stringResource(R.string.toast_no_email_app)
    val strPrivacyPolicyLabel = stringResource(R.string.privacy_policy_label)
    val strConsentRevokeLabel = stringResource(R.string.consent_revoke_label)
    val strConsentNotAvailable = stringResource(R.string.consent_not_available_toast)
    val strAboutMessagesLabel = stringResource(R.string.about_messages_label)
    val strVersionTemplate = stringResource(R.string.version_template)
    val strChooseThemeTitle = stringResource(R.string.choose_theme_title)
    val strThemeSystemDefault = stringResource(R.string.theme_system_default)
    val strThemeLight = stringResource(R.string.theme_light)
    val strThemeDark = stringResource(R.string.theme_dark)
    val strFontSizeDialogTitle = stringResource(R.string.font_size_dialog_title)
    val strFontSizeSmall = stringResource(R.string.font_size_small)
    val strFontSizeNormal = stringResource(R.string.font_size_normal)
    val strFontSizeLarge = stringResource(R.string.font_size_large)
    val strDelayedSendingDialogTitle = stringResource(R.string.delayed_sending_dialog_title)
    val strDelayedSendingNoDelay = stringResource(R.string.delayed_sending_no_delay)
    val strDelayedSending3s = stringResource(R.string.delayed_sending_3s)
    val strDelayedSending5s = stringResource(R.string.delayed_sending_5s)
    val strDelayedSending10s = stringResource(R.string.delayed_sending_10s)

    val themeLabel = when (selectedThemeMode) {
        "light" -> strThemeLight
        "dark" -> strThemeDark
        else -> strThemeSystemDefault
    }
    val fontSizeLabel = when (selectedFontSizeMode) {
        "small" -> strFontSizeSmall
        "large" -> strFontSizeLarge
        else -> strFontSizeNormal
    }
    val delayedSendingLabel = when (selectedDelayedSendingMode) {
        "3s" -> strDelayedSending3s
        "5s" -> strDelayedSending5s
        "10s" -> strDelayedSending10s
        else -> strDelayedSendingNoDelay
    }
    val deleteOldMessagesLabel = when (selectedDeleteOldDays) {
        0 -> strSettingNever
        1 -> strDeleteOldMessagesDaily
        7 -> strDeleteOldMessagesWeekly
        30 -> strDeleteOldMessagesMonthly
        365 -> strDeleteOldMessagesYearly
        else -> String.format(strDeleteOldMessagesEveryNDays, selectedDeleteOldDays)
    }
    val mmsCompressionLabel = when (selectedMmsCompressionMode) {
        "automatic" -> strMmsCompressionAutomatic
        "100kb" -> strMmsCompression100kb
        "200kb" -> strMmsCompression200kb
        "600kb" -> strMmsCompression600kb
        "1000kb" -> strMmsCompression1000kb
        "2000kb" -> strMmsCompression2000kb
        "none" -> strMmsCompressionNone
        else -> strMmsCompression300kb
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.bg_primary))
    ) {
        SecondaryTopBar(
            title = strSettingsTitle,
            onBackClick = { navController.popBackStackWithAd() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SettingsSectionHeader(title = strSectionAppearance, topPadding = 12.dp)
            SettingsRow(
                iconRes = R.drawable.settings_ic_theme,
                title = strAppThemeLabel,
                subtitle = themeLabel,
                onClick = { showThemeDialog = true }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_font_size,
                title = strFontSizeLabel,
                subtitle = fontSizeLabel,
                onClick = { showFontSizeDialog = true }
            )
            val selectedLanguageCode by LanguageState.code
            SettingsRow(
                iconRes = R.drawable.settings_ic_language,
                title = strAppLanguageLabel,
                subtitle = LanguageState.englishNameFor(selectedLanguageCode),
                onClick = { navController.navigate(Routes.ChooseLanguage.createRoute()) },
                showDivider = false
            )

            SettingsSectionHeader(title = strSectionGeneral)
            SettingsRow(
                iconRes = R.drawable.settings_ic_delayed__sending,
                title = strDelayedSendingLabel,
                subtitle = delayedSendingLabel,
                onClick = { showDelayedSendingDialog = true }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_swipe_actions,
                title = strSwipeActionsLabel,
                subtitle = strSwipeActionsDesc,
                onClick = { navController.navigate(Routes.SwipeActions.route) }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_delivery_confirmations,
                title = strDeliveryConfirmationsLabel,
                subtitle = strDeliveryConfirmationsDesc,
                onClick = {
                    deliveryConfirmations = !deliveryConfirmations
                    DeliveryConfirmationState.setEnabled(context, deliveryConfirmations)
                },
                trailing = {
                    CustomSwitch(
                        checked = deliveryConfirmations,
                        onCheckedChange = {
                            deliveryConfirmations = it
                            DeliveryConfirmationState.setEnabled(context, it)
                        }
                    )
                }
            )
            SettingsRow(
                iconRes = R.drawable.longpress_ic_delete,
                title = strDeleteOldMessagesLabel,
                subtitle = if (isDeletingOldMessages) strDeleteOldMessagesDeleting else deleteOldMessagesLabel,
                onClick = { if (!isDeletingOldMessages) showDeleteOldMessagesDialog = true },
                trailing = if (isDeletingOldMessages) {
                    {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorResource(R.color.primary)
                        )
                    }
                } else null
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_auto_compress_mms,
                title = strAutoCompressMmsLabel,
                subtitle = mmsCompressionLabel,
                onClick = { showMmsCompressionDialog = true }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_sync_messages,
                title = strSyncMessagesLabel,
                subtitle = if (isSyncing) strSyncingLabel else strResyncLabel,
                onClick = {
                    if (!isSyncing) {
                        isSyncing = true
                        coroutineScope.launch {
                            try {
                                SmsRepository(context.applicationContext as android.app.Application).performSync()
                                Toast.makeText(context, strMessagesSyncedToast, Toast.LENGTH_SHORT).show()
                            } finally {
                                isSyncing = false
                            }
                        }
                    }
                },
                showDivider = false
            )
            if (isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = colorResource(R.color.primary),
                    trackColor = colorResource(R.color.light_gray)
                )
            }

            SettingsSectionHeader(title = strSectionNotificationCalling)
            SettingsRow(
                iconRes = R.drawable.chat_ic_notification,
                title = strNotificationLabel,
                subtitle = null,
                onClick = { navController.navigate(Routes.NotificationSettings.route) }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_aftercall,
                title = strAfterCallLabel,
                subtitle = strAfterCallDesc,
                onClick = {
                    // Turning ON needs no confirmation; turning OFF asks first (dialog below).
                    if (afterCallScreen) {
                        showDisableAfterCallDialog = true
                    } else {
                        afterCallScreen = true
                        AfterCallState.setEnabled(context, true)
                    }
                },
                trailing = {
                    CustomSwitch(
                        checked = afterCallScreen,
                        onCheckedChange = {
                            if (!it) {
                                showDisableAfterCallDialog = true
                            } else {
                                afterCallScreen = true
                                AfterCallState.setEnabled(context, true)
                            }
                        }
                    )
                },
                showDivider = false
            )

            SettingsSectionHeader(title = strSectionAboutUs)
            SettingsRow(
                iconRes = R.drawable.settings_ic_rate,
                title = strRateAppLabel,
                subtitle = null,
                onClick = { showRateUsDialog = true }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_share_app,
                title = strShareAppLabel,
                subtitle = null,
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            String.format(strShareAppTextTemplate, "https://play.google.com/store/apps/details?id=${context.packageName}")
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, strShareAppLabel))
                }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_help_feedback,
                title = strHelpFeedbackLabel,
                subtitle = null,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("parth@aavakar.com"))
                        putExtra(Intent.EXTRA_SUBJECT, strHelpFeedbackEmailSubject)
                    }
                    context.startActivity(Intent.createChooser(intent, strHelpFeedbackLabel))
                }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_privacy,
                title = strPrivacyPolicyLabel,
                subtitle = null,
                onClick = { navController.navigate(Routes.LegalWebView.createRoute("privacy")) }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_consent_revoke,
                title = strConsentRevokeLabel,
                subtitle = null,
                onClick = {
                    val activity = context as? ComponentActivity
                    if (activity != null) {
                        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
                        val params = ConsentRequestParameters.Builder().build()
                        consentInformation.requestConsentInfoUpdate(
                            activity,
                            params,
                            {
                                if (consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                                    UserMessagingPlatform.showPrivacyOptionsForm(activity) {}
                                } else {
                                    Toast.makeText(context, strConsentNotAvailable, Toast.LENGTH_SHORT).show()
                                }
                            },
                            {
                                Toast.makeText(context, strConsentNotAvailable, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
            SettingsRow(
                iconRes = R.drawable.settings_ic_about_messages,
                title = strAboutMessagesLabel,
                subtitle = String.format(strVersionTemplate, getAppVersionName(context)),
                onClick = { navController.navigate(Routes.About.route) },
                showDivider = false
            )
        }

        if (settingsBannerAdUnitId != null) {
            BannerAdView(adUnitId = settingsBannerAdUnitId, adaptive = true)
        }
    }

    if (showThemeDialog) {
        val themeOptions = listOf(strThemeSystemDefault, strThemeLight, strThemeDark)
        RadioOptionDialog(
            title = strChooseThemeTitle,
            options = themeOptions,
            selectedOption = themeLabel,
            onDismiss = { showThemeDialog = false },
            onConfirm = { choice ->
                val mode = when (choice) {
                    strThemeLight -> "light"
                    strThemeDark -> "dark"
                    else -> "system"
                }
                selectedThemeMode = mode
                ThemeState.setMode(context, mode)
                showThemeDialog = false
                (context as? android.app.Activity)?.recreate()
            }
        )
    }

    if (showFontSizeDialog) {
        val fontSizeOptions = listOf(strFontSizeSmall, strFontSizeNormal, strFontSizeLarge)
        RadioOptionDialog(
            title = strFontSizeDialogTitle,
            options = fontSizeOptions,
            selectedOption = fontSizeLabel,
            onDismiss = { showFontSizeDialog = false },
            onConfirm = { choice ->
                val mode = when (choice) {
                    strFontSizeSmall -> "small"
                    strFontSizeLarge -> "large"
                    else -> "normal"
                }
                selectedFontSizeMode = mode
                FontSizeState.setMode(context, mode)
                showFontSizeDialog = false
                (context as? android.app.Activity)?.recreate()
            }
        )
    }

    if (showDelayedSendingDialog) {
        val delayedSendingOptions = listOf(
            strDelayedSendingNoDelay,
            strDelayedSending3s,
            strDelayedSending5s,
            strDelayedSending10s
        )
        RadioOptionDialog(
            title = strDelayedSendingDialogTitle,
            options = delayedSendingOptions,
            selectedOption = delayedSendingLabel,
            onDismiss = { showDelayedSendingDialog = false },
            onConfirm = { choice ->
                val mode = when (choice) {
                    strDelayedSending3s -> "3s"
                    strDelayedSending5s -> "5s"
                    strDelayedSending10s -> "10s"
                    else -> "none"
                }
                selectedDelayedSendingMode = mode
                DelayedSendingState.setMode(context, mode)
                showDelayedSendingDialog = false
            }
        )
    }

    if (showDeleteOldMessagesDialog) {
        DeleteOldMessagesDialog(
            currentDays = selectedDeleteOldDays,
            onDismiss = { showDeleteOldMessagesDialog = false },
            onNever = {
                selectedDeleteOldDays = 0
                DeleteOldMessagesState.setDays(context, 0)
                com.messages.utils.AlarmScheduler(context).cancelAutoDeleteCheck()
                showDeleteOldMessagesDialog = false
            },
            onSave = { days ->
                showDeleteOldMessagesDialog = false
                pendingDeleteOldDays = days
                coroutineScope.launch {
                    deleteOldMessagesConfirmCount = SmsRepository(context.applicationContext as android.app.Application)
                        .countMessagesOlderThan(days)
                    showDeleteOldMessagesConfirm = true
                }
            }
        )
    }

    if (showDeleteOldMessagesConfirm) {
        ConfirmationDialog(
            title = strDeleteOldMessagesConfirmTitle,
            text = String.format(strDeleteOldMessagesConfirmDescTemplate, deleteOldMessagesConfirmCount),
            confirmText = strActionYes,
            onConfirm = {
                selectedDeleteOldDays = pendingDeleteOldDays
                DeleteOldMessagesState.setDays(context, pendingDeleteOldDays)
                com.messages.utils.AlarmScheduler(context).scheduleAutoDeleteCheck()
                showDeleteOldMessagesConfirm = false
                isDeletingOldMessages = true
                coroutineScope.launch {
                    try {
                        SmsRepository(context.applicationContext as android.app.Application)
                            .purgeMessagesOlderThan(pendingDeleteOldDays)
                    } finally {
                        isDeletingOldMessages = false
                    }
                }
            },
            onDismiss = { showDeleteOldMessagesConfirm = false }
        )
    }

    if (showMmsCompressionDialog) {
        val mmsCompressionOptions = listOf(
            strMmsCompressionAutomatic,
            strMmsCompression100kb,
            strMmsCompression200kb,
            strMmsCompression300kb,
            strMmsCompression600kb,
            strMmsCompression1000kb,
            strMmsCompression2000kb,
            strMmsCompressionNone
        )
        RadioOptionDialog(
            title = strAutoCompressMmsLabel,
            options = mmsCompressionOptions,
            selectedOption = mmsCompressionLabel,
            onDismiss = { showMmsCompressionDialog = false },
            onConfirm = { choice ->
                val mode = when (choice) {
                    strMmsCompressionAutomatic -> "automatic"
                    strMmsCompression100kb -> "100kb"
                    strMmsCompression200kb -> "200kb"
                    strMmsCompression600kb -> "600kb"
                    strMmsCompression1000kb -> "1000kb"
                    strMmsCompression2000kb -> "2000kb"
                    strMmsCompressionNone -> "none"
                    else -> "300kb"
                }
                selectedMmsCompressionMode = mode
                MmsCompressionState.setMode(context, mode)
                showMmsCompressionDialog = false
            }
        )
    }

    if (showRateUsDialog) {
        RateUsDialog(
            onRateClick = { stars ->
                showRateUsDialog = false
                com.messages.utils.RateUsHelper.handleRating(context, stars)
            },
            onDismiss = { showRateUsDialog = false }
        )
    }

    if (showDisableAfterCallDialog) {
        DisableAfterCallDialog(
            onProceed = {
                showDisableAfterCallDialog = false
                afterCallScreen = false
                AfterCallState.setEnabled(context, false)
            },
            onKeepIt = { showDisableAfterCallDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, topPadding: Dp = 24.dp) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontFamily = Inter,
        color = colorResource(R.color.light_color_gray),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = colorResource(R.color.primary),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    color = colorResource(R.color.text_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Normal,
                        color = colorResource(R.color.text_des),
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 64.dp, end = 20.dp),
                color = colorResource(R.color.light_gray),
                thickness = 0.9.dp
            )
        }
    }
}
