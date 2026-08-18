package com.messages.ui.screens

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
import com.messages.R
import com.messages.navigation.Routes
import com.messages.ui.theme.Inter

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
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

@Composable
fun DashboardScreen(parentNavController: NavController) {
    val context = LocalContext.current

    // Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Optionally handle the result here if needed
        }
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
                    val bannerHeight by com.messages.ads.HomeBannerAdState.heightDp
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

