package com.message.sms.texting.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.message.sms.texting.app.utils.AnalyticsManager
import com.message.sms.texting.app.utils.AppPreferences
import com.message.sms.texting.app.ui.modifiers.animatedPulse
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.message.sms.texting.app.ui.theme.MessagesTheme
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter

@Composable
fun OnboardingScreen(
    onContinueClicked: () -> Unit,
    onPrivacyPolicyClick: () -> Unit = {}
) {
    val view = LocalView.current
    val context = LocalContext.current
    val prefs = AppPreferences(context)
    var currentPermissionStep by remember { mutableStateOf(0) }

    // Tracks which permission the settings dialog below should describe, since it's shared
    // between the notification step (1) and the phone/call-log step (2).
    var deniedPermissionIsNotification by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isWaitingForSettings by remember { mutableStateOf(false) }

    // First denial of a given permission just stops (back to step 0, main button) and waits for
    // the user to tap it again -- no dialog yet, no auto-advance to the next permission. Only a
    // second denial in a row for that same permission opens the "go to Settings" dialog.
    var notificationDeniedOnce by remember { mutableStateOf(false) }
    var phoneDeniedOnce by remember { mutableStateOf(false) }

    // Used only to auto-skip the intro below if both permissions were already granted before
    // this screen was even reached -- re-checked at every point permission state could change.
    var isNotifGranted by remember { mutableStateOf(false) }
    var isPhoneGranted by remember { mutableStateOf(false) }
    fun refreshGrantedStatus() {
        isNotifGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        isPhoneGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(Unit) {
        refreshGrantedStatus()
        // Both permissions were already granted before this screen was even reached (e.g. the
        // user granted them via system Settings directly, without ever finishing this flow's own
        // button-driven steps) -- skip straight past the intro instead of showing it again.
        if (isNotifGranted && isPhoneGranted) {
            currentPermissionStep = 3
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshGrantedStatus()
        if (granted) {
            currentPermissionStep = 2
        } else if (!notificationDeniedOnce) {
            notificationDeniedOnce = true
            currentPermissionStep = 0
        } else {
            deniedPermissionIsNotification = true
            showSettingsDialog = true
        }
    }

    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        refreshGrantedStatus()
        val allGranted = result.values.all { it }
        if (allGranted) {
            currentPermissionStep = 3
        } else if (!phoneDeniedOnce) {
            phoneDeniedOnce = true
            currentPermissionStep = 0
        } else {
            deniedPermissionIsNotification = false
            showSettingsDialog = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isWaitingForSettings) {
                isWaitingForSettings = false
                refreshGrantedStatus()
                if (deniedPermissionIsNotification) {
                    val hasNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    if (hasNotif) {
                        showSettingsDialog = false
                        currentPermissionStep = 2
                    } else {
                        showSettingsDialog = true
                    }
                } else {
                    val hasPhoneGroup = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    if (hasPhoneGroup) {
                        showSettingsDialog = false
                        currentPermissionStep = 3
                    } else {
                        showSettingsDialog = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showSettingsDialog) {
        PermissionSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onOpenSettings = {
                isWaitingForSettings = true
                val intent =
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
                currentPermissionStep = 0
            },
            permissionDescLabel = if (deniedPermissionIsNotification) stringResource(R.string.permission_label_notification) else stringResource(R.string.permission_label_phone),
            permissionStepLabel = if (deniedPermissionIsNotification) stringResource(R.string.permission_step_label_notification) else stringResource(R.string.permission_step_label_phone)
        )
    }

    LaunchedEffect(currentPermissionStep) {
        when (currentPermissionStep) {
            1 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    currentPermissionStep = 2
                }
            }

            2 -> {
                phoneLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.CALL_PHONE
                    )
                )
            }

            3 -> {
                prefs.onboardingCompleted = true
                AnalyticsManager.logEventWithAction("onboarding_completed", "OnboardingScreen", "finished")
                onContinueClicked()
                currentPermissionStep = 0
            }
        }
    }
    LaunchedEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    val strWelcomeTo = stringResource(R.string.welcome_to)
    val strTextMessaging = stringResource(R.string.text_messaging)
    val strFeatureSmartNotificationsTitle = stringResource(R.string.feature_smart_notifications_title)
    val strFeatureSmartNotificationsDesc = stringResource(R.string.feature_smart_notifications_desc)
    val strFeatureAfterCallTitle = stringResource(R.string.feature_after_call_title)
    val strFeatureAfterCallDesc = stringResource(R.string.feature_after_call_desc)
    val strActionAgreeContinue = stringResource(R.string.action_agree_continue)
    val strPrivacyAgreePrefix = stringResource(R.string.privacy_agree_prefix)
    val strPrivacyPolicyLinkText = stringResource(R.string.privacy_policy_link_text)

    Surface(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
        color = colorResource(R.color.bg_primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp , vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Image(
                    painter = painterResource(id = R.drawable.permission_main),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 120.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { currentPermissionStep = 1 }
                        )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strWelcomeTo,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Inter,
                    lineHeight = 30.sp,
                    color = colorResource(R.color.text_title),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Text(
                    text = strTextMessaging,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Inter,
                    lineHeight = 30.sp,
                    color = colorResource(R.color.primary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                FeatureItem(
                    iconRes = R.drawable.permission_ic_notification,
                    title = strFeatureSmartNotificationsTitle,
                    description = strFeatureSmartNotificationsDesc,
                    onClick = { currentPermissionStep = 1 }
                )

                Spacer(modifier = Modifier.height(20.dp))

                FeatureItem(
                    iconRes = R.drawable.permission_ic_call,
                    title = strFeatureAfterCallTitle,
                    description = strFeatureAfterCallDesc,
                    onClick = { currentPermissionStep = 1 }
                )
            }

            Button(
                onClick = { currentPermissionStep = 1 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .animatedPulse(colorResource(R.color.primary)),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary)
                )
            ) {
                Text(
                    text = strActionAgreeContinue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Inter,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val privacyText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = colorResource(R.color.text_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = Inter
                    )
                ) {
                    // Trailing space in the XML string resource gets trimmed by the resource
                    // compiler at build time (unquoted strings lose leading/trailing whitespace),
                    // so the space before "Privacy Policy" is added explicitly here instead.
                    append(strPrivacyAgreePrefix.trimEnd() + " ")
                }
                pushStringAnnotation(tag = "privacy_policy", annotation = "privacy_policy")
                withStyle(
                    style = SpanStyle(
                        color = colorResource(R.color.primary),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        fontFamily = Inter,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(strPrivacyPolicyLinkText)
                }
                pop()
            }

            ClickableText(
                text = privacyText,
                style = TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp),
                onClick = { offset ->
                    privacyText.getStringAnnotations("privacy_policy", offset, offset)
                        .firstOrNull()?.let { onPrivacyPolicyClick() }
                }
            )
        }
    }
}

@Composable
fun FeatureItem(
    iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = colorResource(R.color.feature_icon_tint),
            modifier = Modifier
                .size(26.dp)
                .padding(top = 3.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Inter,
                color = colorResource(R.color.text_title)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 16.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.text_des),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun PermissionSettingsDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    // Defaults preserve the original Phone/Call-Log wording used during onboarding; the Dashboard's
    // notification-permission dialog passes "Notification" instead so the steps stay accurate.
    permissionDescLabel: String = stringResource(R.string.permission_label_phone),
    permissionStepLabel: String = stringResource(R.string.permission_step_label_phone)
) {
    val strPermissionNeededTitle = stringResource(R.string.permission_needed_title)
    val strPermissionNeededDesc = String.format(stringResource(R.string.permission_needed_desc), permissionDescLabel)
    val strPermissionStep1 = stringResource(R.string.permission_step_1)
    val strPermissionStep2 = stringResource(R.string.permission_step_2)
    val strPermissionStep3 = String.format(stringResource(R.string.permission_step_3), permissionStepLabel)
    val strPermissionStep4 = stringResource(R.string.permission_step_4)
    val strActionOpenSettings = stringResource(R.string.action_open_settings)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strPermissionNeededTitle,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.text_title)
            )
        },
        text = {
            Column {
                Text(
                    text = strPermissionNeededDesc,
                    color = colorResource(R.color.text_des),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                val steps = listOf(
                    strPermissionStep1,
                    strPermissionStep2,
                    strPermissionStep3,
                    strPermissionStep4
                )
                steps.forEach { step ->
                    Text(
                        text = step,
                        color = colorResource(R.color.text_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary)
                )
            ) {
                Text(
                    strActionOpenSettings,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Inter,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = colorResource(R.color.bg_primary)
    )
}
