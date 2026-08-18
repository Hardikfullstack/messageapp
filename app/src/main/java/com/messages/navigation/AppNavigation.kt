package com.messages.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.messages.ui.screens.SplashScreen
import com.messages.ui.screens.OnboardingScreen
import com.messages.ui.screens.PermissionScreen
import com.messages.ui.screens.DefaultSmsScreen
import com.messages.ui.screens.DashboardScreen
import com.messages.ui.screens.SettingsScreen
import com.messages.ui.screens.ArchivedScreen
import com.messages.ui.screens.StarredMessagesScreen
import com.messages.ui.screens.ChatScreen
import com.messages.ui.screens.ChatDetailsScreen
import com.messages.ui.screens.ContactNotificationScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import com.messages.ui.screens.SearchScreen
import com.messages.ui.screens.BlockedMessagesScreen
import com.messages.ui.screens.BackupRestoreScreen
import com.messages.ui.screens.NewChatScreen
import com.messages.ui.screens.ScheduledMessagesScreen
import com.messages.ui.screens.AddGroupNameScreen
import com.messages.ui.screens.ChooseLanguageScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.messages.ads.GlobalBackAdManager
import com.messages.ui.components.dialogs.OfflineDialog
import com.messages.viewmodel.AppConfigViewModel

@Composable
fun AppNavigation(deepLinkRoute: String? = null, onDeepLinkConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val isOnline by appConfigViewModel.isOnline.collectAsState()
    val adConfig by appConfigViewModel.appResponse.collectAsState()

    // Deliberately NOT gated on isOnline — this reads whatever the last successful fetch said
    // (cached or live), so once confirmed "on" it keeps blocking through a later offline period
    // instead of silently lifting just because connectivity dropped. Only a fresh fetch that
    // comes back "off" clears it. Takes priority over the Offline dialog below so they never
    // stack — being under maintenance while also offline still just shows Maintenance.
    val isMaintenanceOn = adConfig?.result?.extra_data_1_on_off == "on"
    if (isMaintenanceOn) {
        com.messages.ui.components.dialogs.MaintenanceDialog(message = adConfig?.result?.extra_data_1_message)
    }

    var showOfflineDialog by remember { mutableStateOf(false) }
    var wasOnline by remember { mutableStateOf(true) }
    LaunchedEffect(isOnline) {
        if (!isOnline && wasOnline) {
            showOfflineDialog = true
        } else if (isOnline) {
            showOfflineDialog = false
        }
        wasOnline = isOnline
    }
    if (showOfflineDialog && !isMaintenanceOn) {
        OfflineDialog(onDismiss = { showOfflineDialog = false })
    }

    // Catches the system back gesture/button here (every in-screen back button/icon calls
    // popBackStackWithAd() directly instead — see NavigationExt.kt). That function decides
    // *before* navigating whether this is the every-4th-back trigger: if so, it shows the
    // GlobalAdLoader overlay below, then the interstitial, and only pops the back stack once the
    // ad is dismissed — so the actual screen transition never runs concurrently with the ad
    // showing. That ordering is what makes this reliable: an earlier version tried to show the ad
    // *after* the pop/transition had already started, deferred by a guessed delay, which Android's
    // window system would silently queue instead of displaying whenever that guess was too short.
    // System back presses are intercepted individually within each composable route below,
    // because a root BackHandler outside the NavHost gets preempted by NavHost's own internal back handler.

    val showAdLoader by com.messages.navigation.GlobalAdLoader.isLoading.collectAsState()
    if (showAdLoader) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            com.messages.ads.AdLoadingScreen()
        }
    }



    // Captured once: whether THIS process cold-started directly into a deep link (e.g. tapping
    // "Message" in the After Call overlay launches MainActivity fresh). In that case we skip the
    // Splash branding screen entirely — reaching this deep link already implies the app is fully
    // set up (the overlay/notification that produced it only fires once onboarding/permissions
    // are done), so there's nothing for Splash to gate, and composing it first was causing a
    // visible flash before landing on the chat.
    val startsWithDeepLink = remember { deepLinkRoute != null }
    val initialDeepLinkRoute = remember { deepLinkRoute }

    // Logs Firebase's screen_view on every destination change, app-wide — one listener instead of
    // instrumenting each of the ~25 screens individually. Route strings carry arg placeholders/
    // query params (e.g. "chat_screen/{threadId}?address=..."); only the base segment before the
    // first "/" or "?" is used as the screen name so it stays a small, stable set of values.
    DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
            val screenName = destination.route?.substringBefore("/")?.substringBefore("?") ?: "unknown"
            com.messages.utils.AnalyticsManager.logScreenView(screenName)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (startsWithDeepLink && initialDeepLinkRoute != null) {
            navController.navigate(initialDeepLinkRoute)
            onDeepLinkConsumed()
        }
    }

    // Handles the case where the app is already running (e.g. Home already visible) and a new
    // deep-link intent arrives (like the After Call full-screen intent) — pushes it right away.
    // On cold start with no deep link, the current destination is still Splash, so this no-ops
    // and lets Splash's own onTimeout below perform the initial navigation instead.
    LaunchedEffect(deepLinkRoute) {
        if (deepLinkRoute != null && !startsWithDeepLink) {
            val currentRoute = navController.currentDestination?.route
            val setupRoutes = listOf(Routes.Splash.route, Routes.ChooseLanguage.route, Routes.Onboarding.route, Routes.Permissions.route, Routes.DefaultSms.route)
            if (currentRoute != null && currentRoute !in setupRoutes) {
                navController.navigate(deepLinkRoute)
                onDeepLinkConsumed()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentRoute = navController.currentDestination?.route
                val setupRoutes = listOf(Routes.Splash.route, Routes.ChooseLanguage.route, Routes.Onboarding.route, Routes.Permissions.route, Routes.DefaultSms.route)
                
                if (currentRoute != null && currentRoute !in setupRoutes) {
                    val isDefaultSms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val roleManager = context.getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager
                        roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
                    } else {
                        android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
                    }
                    if (!isDefaultSms) {
                        navController.navigate(Routes.DefaultSms.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (startsWithDeepLink) Routes.Dashboard.route else Routes.Splash.route,
        enterTransition = { 
            val setupRoutes = listOf(Routes.Splash.route, Routes.ChooseLanguage.route, Routes.Onboarding.route, Routes.Permissions.route, Routes.DefaultSms.route)
            if (initialState.destination.route in setupRoutes) {
                EnterTransition.None
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
            }
        },
        exitTransition = { 
            val setupRoutes = listOf(Routes.Splash.route, Routes.ChooseLanguage.route, Routes.Onboarding.route, Routes.Permissions.route, Routes.DefaultSms.route)
            if (initialState.destination.route in setupRoutes) {
                ExitTransition.None
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
            }
        },
        popEnterTransition = { 
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
        },
        popExitTransition = { 
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
        }
    ) {
        composable(Routes.Splash.route) {
            SplashScreen(
                skipAnimation = deepLinkRoute != null,
                onTimeout = { nextRoute ->
                    if (deepLinkRoute != null && nextRoute == Routes.Dashboard.route) {
                        navController.navigate(Routes.Dashboard.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                        navController.navigate(deepLinkRoute)
                        onDeepLinkConsumed()
                    } else {
                        navController.navigate(nextRoute) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.Onboarding.route) {
            OnboardingScreen(
                onContinueClicked = {
                    navController.navigate(Routes.Permissions.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.Permissions.route) {
            PermissionScreen(
                onAllPermissionsGranted = {
                    navController.navigate(Routes.DefaultSms.route) {
                        popUpTo(Routes.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DefaultSms.route) {
            DefaultSmsScreen(
                onDefaultSmsSet = {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.DefaultSms.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Dashboard.route) {
            DashboardScreen(parentNavController = navController)
        }

        composable(Routes.Settings.route) {
            BackHandler { navController.popBackStackWithAd() }
            SettingsScreen(navController = navController)
        }

        composable(Routes.SwipeActions.route) {
            BackHandler { navController.popBackStackWithAd() }
            com.messages.ui.screens.SwipeActionsScreen(navController = navController)
        }
        composable(Routes.NotificationSettings.route) {
            BackHandler { navController.popBackStackWithAd() }
            com.messages.ui.screens.NotificationSettingsScreen(navController = navController)
        }
        composable(Routes.About.route) {
            BackHandler { navController.popBackStackWithAd() }
            com.messages.ui.screens.AboutScreen(navController = navController)
        }

        composable(
            route = Routes.LegalWebView.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            BackHandler { navController.popBackStackWithAd() }
            val type = backStackEntry.arguments?.getString("type") ?: "privacy"
            com.messages.ui.screens.LegalWebViewScreen(navController = navController, type = type)
        }

        composable(
            route = Routes.ChooseLanguage.route,
            arguments = listOf(
                navArgument("firstRun") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val isFirstRun = backStackEntry.arguments?.getBoolean("firstRun") ?: false
            BackHandler(enabled = !isFirstRun) { navController.popBackStackWithAd() }
            ChooseLanguageScreen(
                navController = navController,
                isFirstRun = isFirstRun,
                onFirstRunDone = {
                    navController.navigate(Routes.Onboarding.route) {
                        popUpTo(Routes.ChooseLanguage.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Archived.route) {
            BackHandler { navController.popBackStackWithAd() }
            ArchivedScreen(navController = navController)
        }

        composable(Routes.Search.route) {
            BackHandler { navController.popBackStackWithAd() }
            SearchScreen(navController = navController)
        }

        composable(
            route = Routes.NewChat.route,
            arguments = listOf(
                navArgument("isScheduling") { type = NavType.BoolType; defaultValue = false },
                navArgument("forwardText") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("groupId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            BackHandler { navController.popBackStackWithAd() }
            val isScheduling = backStackEntry.arguments?.getBoolean("isScheduling") ?: false
            val forwardText = backStackEntry.arguments?.getString("forwardText")
            val groupId = backStackEntry.arguments?.getString("groupId")?.toLongOrNull()
            NewChatScreen(navController = navController, isScheduling = isScheduling, forwardText = forwardText, groupId = groupId)
        }

        composable(Routes.StarredMessages.route) {
            BackHandler { navController.popBackStackWithAd() }
            StarredMessagesScreen(navController = navController)
        }

        composable(
            route = Routes.AfterCall.route,
            arguments = listOf(
                navArgument("address") { type = NavType.StringType; defaultValue = "" },
                navArgument("displayName") { type = NavType.StringType; nullable = true; defaultValue = "null" },
                navArgument("isKnown") { type = NavType.BoolType; defaultValue = false },
                navArgument("line1") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("line2") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: ""
            val rawDisplayName = backStackEntry.arguments?.getString("displayName") ?: "null"
            val displayName = if (rawDisplayName == "null") null else rawDisplayName
            val isKnownContact = backStackEntry.arguments?.getBoolean("isKnown") ?: false
            val callInfoLine1 = backStackEntry.arguments?.getString("line1") ?: ""
            val callInfoLine2 = backStackEntry.arguments?.getString("line2") ?: ""
            com.messages.ui.screens.AfterCallScreen(
                address = address,
                displayName = displayName,
                isKnownContact = isKnownContact,
                callInfoLine1 = callInfoLine1,
                callInfoLine2 = callInfoLine2,
                onOpenChat = { threadId, chatAddress, contactName, forwardText ->
                    navController.navigate(
                        Routes.Chat.createRoute(
                            threadId = threadId,
                            address = chatAddress,
                            contactName = contactName,
                            forwardText = forwardText
                        )
                    )
                },
                onFinish = { navController.popBackStackWithAd() }
            )
        }

        composable(
            route = Routes.Chat.route,
            arguments = listOf(
                navArgument("threadId") { type = NavType.LongType },
                navArgument("address") { type = NavType.StringType; nullable = true; defaultValue = " " },
                navArgument("contactName") { type = NavType.StringType; nullable = true; defaultValue = "null" },
                navArgument("highlightMsgId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("isScheduling") { type = NavType.BoolType; defaultValue = false },
                navArgument("scheduledMessageId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("searchQuery") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("forwardText") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("groupId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getLong("threadId") ?: 0L
            val address = backStackEntry.arguments?.getString("address") ?: ""
            val rawContactName = backStackEntry.arguments?.getString("contactName") ?: "null"
            val contactName = if (rawContactName == "null") null else rawContactName

            val highlightMsgIdStr = backStackEntry.arguments?.getString("highlightMsgId")
            val highlightMsgId = highlightMsgIdStr?.toLongOrNull()

            val isScheduling = backStackEntry.arguments?.getBoolean("isScheduling") ?: false

            val scheduledMessageIdStr = backStackEntry.arguments?.getString("scheduledMessageId")
            val scheduledMessageId = scheduledMessageIdStr?.toLongOrNull()

            val searchQuery = backStackEntry.arguments?.getString("searchQuery")
            val forwardText = backStackEntry.arguments?.getString("forwardText")
            val groupId = backStackEntry.arguments?.getString("groupId")?.toLongOrNull()

            // One-shot: if this chat was opened from the After Call overlay (dismissing itself to
            // launch MainActivity, since ChatScreen can't run inside that overlay window), pressing
            // back here should bring the After Call screen back up instead of landing on Home.
            val afterCallReturnInfo = remember {
                com.messages.ui.theme.AfterCallReturnState.pending.also {
                    com.messages.ui.theme.AfterCallReturnState.pending = null
                }
            }
            if (afterCallReturnInfo != null) {
                BackHandler {
                    navController.popBackStackWithAd()
                    com.messages.utils.AfterCallOverlayManager.show(
                        context = context,
                        address = afterCallReturnInfo.address,
                        callInfoLine1 = afterCallReturnInfo.callInfoLine1,
                        callInfoLine2 = afterCallReturnInfo.callInfoLine2,
                        initialDisplayName = afterCallReturnInfo.displayName
                    )
                }
            }

            ChatScreen(
                navController = navController,
                threadId = threadId,
                address = address,
                contactName = contactName,
                highlightMsgId = highlightMsgId,
                isScheduling = isScheduling,
                scheduledMessageId = scheduledMessageId,
                searchQuery = searchQuery,
                forwardText = forwardText,
                groupId = groupId
            )
        }

        composable(
            route = Routes.ChatDetails.route,
            arguments = listOf(
                navArgument("threadId") { type = NavType.LongType },
                navArgument("address") { type = NavType.StringType; nullable = true; defaultValue = " " },
                navArgument("contactName") { type = NavType.StringType; nullable = true; defaultValue = "null" },
                navArgument("isArchived") { type = NavType.BoolType; defaultValue = false },
                navArgument("groupId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            BackHandler { navController.popBackStackWithAd() }
            val threadId = backStackEntry.arguments?.getLong("threadId") ?: 0L
            val address = backStackEntry.arguments?.getString("address") ?: ""
            val rawContactName = backStackEntry.arguments?.getString("contactName") ?: "null"
            val contactName = if (rawContactName == "null") null else rawContactName
            val groupId = backStackEntry.arguments?.getString("groupId")?.toLongOrNull()

            ChatDetailsScreen(
                navController = navController,
                threadId = threadId,
                address = address,
                contactName = contactName,
                groupId = groupId
            )
        }

        composable(
            route = Routes.ContactNotification.route,
            arguments = listOf(
                navArgument("threadId") { type = NavType.LongType },
                navArgument("contactName") { type = NavType.StringType; nullable = true; defaultValue = "null" }
            )
        ) { backStackEntry ->
            BackHandler { navController.popBackStackWithAd() }
            val threadId = backStackEntry.arguments?.getLong("threadId") ?: 0L
            val rawContactName = backStackEntry.arguments?.getString("contactName") ?: "null"
            val contactName = if (rawContactName == "null") null else rawContactName

            ContactNotificationScreen(
                navController = navController,
                threadId = threadId,
                contactName = contactName
            )
        }

        composable(Routes.ScheduledMessages.route) {
            BackHandler { navController.popBackStackWithAd() }
            ScheduledMessagesScreen(navController)
        }

        composable(Routes.BlockedMessages.route) {
            BackHandler { navController.popBackStackWithAd() }
            BlockedMessagesScreen(navController)
        }

        composable(Routes.BackupRestore.route) {
            BackHandler { navController.popBackStackWithAd() }
            BackupRestoreScreen(navController)
        }

        composable(
            route = Routes.AddGroupName.route,
            arguments = listOf(
                navArgument("members") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            BackHandler { navController.popBackStackWithAd() }
            val membersJson = backStackEntry.arguments?.getString("members")
            val members = if (membersJson.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    kotlinx.serialization.json.Json.decodeFromString(
                        kotlinx.serialization.builtins.ListSerializer(com.messages.model.GroupMember.serializer()),
                        membersJson
                    )
                } catch (e: Exception) {
                    emptyList()
                }
            }
            AddGroupNameScreen(navController = navController, members = members)
        }

        composable(
            route = Routes.ImageViewer.route,
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType; defaultValue = "" },
                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = "null" },
                navArgument("date") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            BackHandler { navController.popBackStackWithAd() }
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            val rawName = backStackEntry.arguments?.getString("name") ?: "null"
            val name = if (rawName == "null") null else rawName
            val date = backStackEntry.arguments?.getLong("date") ?: 0L
            com.messages.ui.screens.ImageViewerScreen(
                navController = navController,
                imagePath = imagePath,
                name = name,
                date = date
            )
        }
    }
}
