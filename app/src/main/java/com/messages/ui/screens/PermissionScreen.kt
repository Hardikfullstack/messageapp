package com.messages.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.messages.ui.modifiers.animatedPulse
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messages.R
import com.messages.ui.theme.Inter
import com.messages.utils.MiuiUtils
import com.messages.utils.OnePlusUtils
import com.messages.utils.PowerUtils
import com.messages.utils.AppPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PermissionStep {
    OVERLAY, BATTERY_OPTIMIZATION, MIUI_PERMISSIONS, MIUI_AUTOSTART, ONEPLUS_AUTOSTART, DONE
}

@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    // Single source of truth for step order, used both for the initial value and for advancing
    // after each step returns — keeps the sequence in one place instead of duplicated logic.
    fun computeNextStep(): PermissionStep {
        return if (!Settings.canDrawOverlays(context)) PermissionStep.OVERLAY
        else if (PowerUtils.shouldPromptForBatteryOptimization() && !PowerUtils.isIgnoringBatteryOptimizations(context) && !prefs.batteryOptimizationCompleted) PermissionStep.BATTERY_OPTIMIZATION
        else if (MiuiUtils.isMiui() && !MiuiUtils.isMiuiBackgroundPopupGranted(context) && !prefs.miuiPermissionsCompleted) PermissionStep.MIUI_PERMISSIONS
        else if (MiuiUtils.isMiui() && !MiuiUtils.isMiuiAutostartGranted(context) && !prefs.miuiAutostartCompleted) PermissionStep.MIUI_AUTOSTART
        else if (OnePlusUtils.isOnePlus() && !prefs.onePlusAutostartCompleted) PermissionStep.ONEPLUS_AUTOSTART
        else PermissionStep.DONE
    }

    var currentStep by remember { mutableStateOf(computeNextStep()) }

    // If device is neither MIUI nor OnePlus, those steps are skipped entirely.

    fun checkNextStepAfterOverlay() {
        if (Settings.canDrawOverlays(context)) {
            currentStep = computeNextStep()
        }
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkNextStepAfterOverlay()
    }

    val miuiPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (MiuiUtils.isMiuiBackgroundPopupGranted(context)) {
            prefs.miuiPermissionsCompleted = true
        }
        checkNextStepAfterOverlay()
    }

    val miuiAutoStartLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (MiuiUtils.isMiuiAutostartGranted(context)) {
            prefs.miuiAutostartCompleted = true
        }
        checkNextStepAfterOverlay()
    }

    // No reliable "was it granted" callback for this one (the system dialog's result code isn't
    // meaningfully different either way) — treat it as a one-time ask regardless of outcome.
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        prefs.batteryOptimizationCompleted = true
        checkNextStepAfterOverlay()
    }

    // No reliable programmatic check on OxygenOS (unlike MIUI's AppOps codes) — same one-time-ask
    // treatment.
    val onePlusAutoStartLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        prefs.onePlusAutostartCompleted = true
        checkNextStepAfterOverlay()
    }

    val coroutineScope = rememberCoroutineScope()

    val startAutoReturnPolling = {
        coroutineScope.launch {
            while (!Settings.canDrawOverlays(context)) {
                delay(300)
            }
            // Permission granted, bring app back to front
            try {
                val returnIntent = Intent(
                    context,
                    Class.forName("${context.packageName}.MainActivity")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(returnIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Auto-launch Overlay settings only the very first time this screen is ever reached (a
    // persisted flag, not a per-composition one) — skips a tap for the common first-run case.
    // On later app opens where the permission is still ungranted (app killed mid-flow, etc),
    // the user sees this screen first and taps "Go to Settings" themselves instead.
    LaunchedEffect(Unit) {
        if (!Settings.canDrawOverlays(context) && !prefs.overlayPermissionAutoPrompted) {
            prefs.overlayPermissionAutoPrompted = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayLauncher.launch(intent)
            startAutoReturnPolling()
        } else {
            checkNextStepAfterOverlay()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkNextStepAfterOverlay()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentStep) {
        if (currentStep == PermissionStep.DONE) {
            onAllPermissionsGranted()
        }
    }

    if (currentStep != PermissionStep.DONE) {
        // Every step past OVERLAY reuses the same generic "extra permissions for reliable
        // background operation" copy/image — deliberately not OEM-named (see comment below).
        val isGenericExtraStep = currentStep != PermissionStep.OVERLAY
        val backgroundColor = colorResource(R.color.bg_primary)
        val textColor = colorResource(R.color.text_title)
        val descColor = colorResource(R.color.text_des)

        // Deliberately doesn't name the OEM (MIUI runs on Redmi/POCO/Black Shark too, not just
        // phones branded "Xiaomi"), matching what most apps do to avoid a brand mismatch/calling-out feel.
        val strPermissionMiuiTitle = stringResource(R.string.permission_miui_title)
        val strPermissionsRequiredTitle = stringResource(R.string.permissions_required_title)
        val strPermissionOverlayDesc = stringResource(R.string.permission_overlay_desc)
        val strPermissionMiuiDesc = stringResource(R.string.permission_miui_desc)
        val strActionGoToSettings = stringResource(R.string.action_go_to_settings)
        val strActionGrantPermission = stringResource(R.string.action_grant_permission)
        val strActionGrantAutostartPermission = stringResource(R.string.action_grant_autostart_permission)

        val onPermissionActionClick: () -> Unit = {
            when (currentStep) {
                PermissionStep.OVERLAY -> {
                    if (Settings.canDrawOverlays(context)) {
                        checkNextStepAfterOverlay()
                    } else {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        overlayLauncher.launch(intent)
                        startAutoReturnPolling()
                    }
                }

                PermissionStep.BATTERY_OPTIMIZATION -> {
                    try {
                        batteryOptimizationLauncher.launch(PowerUtils.ignoreBatteryOptimizationsIntent(context))
                    } catch (e: Exception) {
                        // Not available on this device (some OEMs block it entirely) — move on.
                        prefs.batteryOptimizationCompleted = true
                        currentStep = computeNextStep()
                    }
                }

                PermissionStep.MIUI_PERMISSIONS -> {
                    try {
                        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                        intent.setClassName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.permissions.PermissionsEditorActivity"
                        )
                        intent.putExtra("extra_pkgname", context.packageName)
                        miuiPermissionsLauncher.launch(intent)
                    } catch (e: Exception) {
                        // Screen isn't launchable on this device — mark it done so we don't retry forever.
                        prefs.miuiPermissionsCompleted = true
                        currentStep = computeNextStep()
                    }
                }

                PermissionStep.MIUI_AUTOSTART -> {
                    try {
                        val intent = Intent()
                        intent.setClassName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                        miuiAutoStartLauncher.launch(intent)
                    } catch (e: Exception) {
                        prefs.miuiAutostartCompleted = true
                        currentStep = computeNextStep()
                    }
                }

                PermissionStep.ONEPLUS_AUTOSTART -> {
                    try {
                        val intent = Intent().apply {
                            setClassName(
                                "com.oneplus.security",
                                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                            )
                        }
                        onePlusAutoStartLauncher.launch(intent)
                    } catch (e: Exception) {
                        prefs.onePlusAutostartCompleted = true
                        currentStep = computeNextStep()
                    }
                }

                PermissionStep.DONE -> {}
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Title/description are short, fixed text — they stay pinned right below the top
                // spacer. Only the image (below) gets the flexible/centered leftover space.
                Spacer(modifier = Modifier.height(48.dp))

                val title =
                    if (isGenericExtraStep) strPermissionMiuiTitle else strPermissionsRequiredTitle
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Inter,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    modifier = Modifier.clickable(onClick = onPermissionActionClick)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val description = when (currentStep) {
                    PermissionStep.OVERLAY -> strPermissionOverlayDesc
                    PermissionStep.MIUI_PERMISSIONS -> strPermissionMiuiDesc
                    PermissionStep.MIUI_AUTOSTART -> strPermissionMiuiDesc
                    PermissionStep.BATTERY_OPTIMIZATION -> strPermissionMiuiDesc
                    PermissionStep.ONEPLUS_AUTOSTART -> strPermissionMiuiDesc
                    else -> ""
                }

                Text(
                    text = description,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = Inter,
                    fontStyle = if (isGenericExtraStep) FontStyle.Italic else FontStyle.Normal,
                    color = descColor,
                    modifier = Modifier.clickable(onClick = onPermissionActionClick)
                )

                // weight(1f) here is valid — this Column's parent is NOT scrollable, unlike
                // before, so the image gets the leftover space and centers within it while
                // title/description above stay fixed at their position.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isGenericExtraStep) {
                        Image(
                            painter = painterResource(
                                id = if (currentStep == PermissionStep.MIUI_PERMISSIONS) {
                                    R.drawable.display_pop_up_main
                                } else {
                                    // MIUI_AUTOSTART, BATTERY_OPTIMIZATION, ONEPLUS_AUTOSTART all
                                    // reuse this — same "let it run in the background" concept, and
                                    // there's no dedicated illustration for the latter two yet.
                                    R.drawable.auto_start_main
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .offset(y = (-40).dp)
                                .clickable(onClick = onPermissionActionClick)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.allow_display_over_other_apps_main),
                            contentDescription = null,
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .offset(y = (-40).dp)
                                .clickable(onClick = onPermissionActionClick)
                        )
                    }
                }

                val buttonText = when (currentStep) {
                    PermissionStep.OVERLAY -> strActionGoToSettings
                    PermissionStep.MIUI_PERMISSIONS -> strActionGrantPermission
                    PermissionStep.MIUI_AUTOSTART -> strActionGrantAutostartPermission
                    PermissionStep.BATTERY_OPTIMIZATION -> strActionGrantPermission
                    PermissionStep.ONEPLUS_AUTOSTART -> strActionGrantAutostartPermission
                    else -> ""
                }

                Button(
                    onClick = onPermissionActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .padding(bottom = 8.dp)
                        .animatedPulse(colorResource(R.color.primary)),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary))
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 20.sp,
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
