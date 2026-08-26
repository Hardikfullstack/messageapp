package com.message.sms.texting.app.ui.screens

import com.message.sms.texting.app.navigation.popBackStackWithAd

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.draw.scale
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.components.CustomSwitch
import com.message.sms.texting.app.ui.components.SecondaryTopBar
import com.message.sms.texting.app.ui.components.dialogs.NotificationPreviewsDialog
import com.message.sms.texting.app.ui.theme.Inter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactNotificationScreen(
    navController: NavController,
    threadId: Long,
    contactName: String?
) {
    val context = LocalContext.current
    val sharedPrefs =
        context.getSharedPreferences("contact_notification_prefs", Context.MODE_PRIVATE)

    // State for preferences
    // 0 = Show name and message, 1 = Show name, 2 = Hide contents
    var notificationPreviewOption by remember {
        mutableIntStateOf(sharedPrefs.getInt("preview_option_$threadId", 0))
    }
    var wakeScreenEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("wake_screen_$threadId", false))
    }

    var showPreviewsDialog by remember { mutableStateOf(false) }

    val strUnknownContact = stringResource(R.string.unknown_contact)
    val strNotifications = stringResource(R.string.notifications_label)
    val strTapToCustomize = stringResource(R.string.tap_to_customize)
    val strPreviewShowNameAndMessage = stringResource(R.string.preview_show_name_and_message)
    val strPreviewShowName = stringResource(R.string.preview_show_name)
    val strPreviewHideContents = stringResource(R.string.preview_hide_contents)
    val strNotificationPreviewsLabel = stringResource(R.string.notification_previews_label)
    val strNotificationPreviewsDialogTitle = stringResource(R.string.notification_previews_dialog_title)
    val strWakeScreenLabel = stringResource(R.string.wake_screen_label)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary),
        topBar = {
            SecondaryTopBar(
                title = contactName ?: strUnknownContact,
                onBackClick = { navController.popBackStackWithAd() },
                actions = { }
            )
        }
    ) { innerPadding ->
        val previewSubtitle = when (notificationPreviewOption) {
            0 -> strPreviewShowNameAndMessage
            1 -> strPreviewShowName
            else -> strPreviewHideContents
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Notifications (System Settings) â€” all messages share one notification channel,
            // so this deep-links to the same shared channel as Settings â†’ Notification.
            ContactSettingsRow(
                title = strNotifications,
                subtitle = strTapToCustomize,
                onClick = {
                    com.message.sms.texting.app.NotificationHelper.ensureDefaultChannelExists(context)
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            putExtra(Settings.EXTRA_CHANNEL_ID, com.message.sms.texting.app.NotificationHelper.DEFAULT_CHANNEL_ID)
                        }
                    } else {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    }
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = colorResource(R.color.light_gray),
                thickness = 1.dp
            )

            // Notification Previews
            ContactSettingsRow(
                title = strNotificationPreviewsLabel,
                subtitle = previewSubtitle,
                onClick = { showPreviewsDialog = true }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = colorResource(R.color.light_gray),
                thickness = 1.dp
            )

            // Wake Screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strWakeScreenLabel,
                    fontSize = 20.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_title)
                )
                CustomSwitch(
                    checked = wakeScreenEnabled,
                    onCheckedChange = { isChecked ->
                        wakeScreenEnabled = isChecked
                        sharedPrefs.edit().putBoolean("wake_screen_$threadId", isChecked).apply()
                    }
                )
            }
        }

        if (showPreviewsDialog) {
            val previewOptions = listOf(strPreviewShowNameAndMessage, strPreviewShowName, strPreviewHideContents)
            NotificationPreviewsDialog(
                title = strNotificationPreviewsDialogTitle,
                options = previewOptions,
                selectedOption = previewSubtitle,
                onDismiss = { showPreviewsDialog = false },
                onConfirm = { choice ->
                    val option = when (choice) {
                        strPreviewShowName -> 1
                        strPreviewHideContents -> 2
                        else -> 0
                    }
                    notificationPreviewOption = option
                    sharedPrefs.edit().putInt("preview_option_$threadId", option).apply()
                    showPreviewsDialog = false
                }
            )
        }
    }
}

@Composable
fun ContactSettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.text_title)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            fontFamily = Inter,
            color = colorResource(R.color.text_des)
        )
    }
}
