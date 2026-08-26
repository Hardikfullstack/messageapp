package com.message.sms.texting.app.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.message.sms.texting.app.R
import com.message.sms.texting.app.navigation.Routes
import com.message.sms.texting.app.ui.theme.Inter

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun DashboardScreen(parentNavController: NavController) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Role-held (default SMS app) doesn't guarantee the underlying runtime permissions stay
    // granted â€” the user can revoke READ_SMS/SEND_SMS/etc individually from system Settings
    // without losing the role, which silently breaks performSync() (SmsRepository.kt) since it
    // just no-ops without them. Ask normally first; only if that doesn't resolve it do we show
    // the settings-redirect dialog, and it can't be dismissed since the app can't work without it.
    val smsPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_MMS
    )
    var showSmsPermissionDialog by remember { mutableStateOf(false) }
    var isWaitingForSmsSettings by remember { mutableStateOf(false) }
    val smsPermissionCoroutineScope = rememberCoroutineScope()

    // Room's PagingSource auto-invalidates on table changes, so once this inserts the synced
    // messages, HomeScreen's message list (if already open) picks them up on its own â€” no
    // manual reload/ViewModel-recreation needed.
    fun syncSmsNow() {
        smsPermissionCoroutineScope.launch {
            com.message.sms.texting.app.repository.SmsRepository(context.applicationContext).performSync()
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { granted -> !granted }) {
            showSmsPermissionDialog = true
        } else {
            syncSmsNow()
        }
    }

    LaunchedEffect(Unit) {
        val isDefaultSms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager =
                context.getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager
            roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
        } else {
            android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
        if (isDefaultSms && smsPermissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }) {
            smsPermissionLauncher.launch(smsPermissions)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isWaitingForSmsSettings) {
                isWaitingForSmsSettings = false
                val stillMissing = smsPermissions.any {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                showSmsPermissionDialog = stillMissing
                if (!stillMissing) {
                    syncSmsNow()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showSmsPermissionDialog) {
        PermissionSettingsDialog(
            onDismiss = {},
            onOpenSettings = {
                isWaitingForSmsSettings = true
                val intent =
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            },
            permissionDescLabel = stringResource(R.string.permission_label_sms),
            permissionStepLabel = stringResource(R.string.permission_step_label_sms)
        )
    }

    val localNavController = rememberNavController()
    val navBackStackEntry by localNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val strNewMessage = stringResource(R.string.content_desc_new_message)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colorResource(R.color.bg_primary),
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            floatingActionButton = {
                if (currentRoute == Routes.Home.route || currentRoute == null) {
                    val bannerHeight by com.message.sms.texting.app.ads.HomeBannerAdState.heightDp
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp + bannerHeight)
                            .size(62.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colorResource(R.color.primary))
                            .clickable { parentNavController.navigate(Routes.NewChat.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.home_ic_message_btn),
                            contentDescription = strNewMessage,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = localNavController,
                startDestination = Routes.Home.route,
                modifier = Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                    start = 0.dp,
                    end = 0.dp,
                    bottom = 0.dp
                ),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 1000 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -1000 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -1000 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 1000 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
                composable(Routes.Home.route) {
                    HomeScreen(
                        onNavigateToArchived = { parentNavController.navigate(Routes.Archived.route) },
                        navController = parentNavController,
                        onShowSnackbar = { message, actionLabel, onAction ->
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = message,
                                    actionLabel = actionLabel,
                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                )
                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                    onAction()
                                }
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 30.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF1B1B24),
                contentColor = Color.White,
                actionColor = colorResource(R.color.primary)
            )
        }
    }
}

