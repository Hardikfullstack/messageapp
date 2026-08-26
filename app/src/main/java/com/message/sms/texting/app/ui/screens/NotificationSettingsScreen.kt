package com.message.sms.texting.app.ui.screens

import com.message.sms.texting.app.navigation.popBackStackWithAd

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.components.CustomSwitch
import com.message.sms.texting.app.ui.components.SecondaryTopBar
import com.message.sms.texting.app.ui.components.dialogs.NotificationPreviewsDialog
import com.message.sms.texting.app.ui.components.dialogs.RadioOptionDialog
import com.message.sms.texting.app.ui.theme.Inter
import com.message.sms.texting.app.ui.theme.NotificationSettingsState

@Composable
fun NotificationSettingsScreen(navController: NavController) {
    val context = LocalContext.current

    var wakeScreenEnabled by remember { mutableStateOf(NotificationSettingsState.wakeScreenEnabled.value) }
    var previewOption by remember { mutableStateOf(NotificationSettingsState.previewOption.intValue) }
    var button1ActionKey by remember { mutableStateOf(NotificationSettingsState.button1Action.value) }
    var button2ActionKey by remember { mutableStateOf(NotificationSettingsState.button2Action.value) }
    var button3ActionKey by remember { mutableStateOf(NotificationSettingsState.button3Action.value) }
    var quickReplyEnabled by remember { mutableStateOf(NotificationSettingsState.quickReplyEnabled.value) }
    var tapToDismissEnabled by remember { mutableStateOf(NotificationSettingsState.tapToDismissEnabled.value) }
    var showPreviewsDialog by remember { mutableStateOf(false) }

    val strTitle = stringResource(R.string.notifications_label)
    val strNotification = stringResource(R.string.notification_label)
    val strTapToCustomize = stringResource(R.string.tap_to_customize)
    val strNotificationPreviewsLabel = stringResource(R.string.notification_previews_label)
    val strPreviewShowNameAndMessage = stringResource(R.string.preview_show_name_and_message)
    val strPreviewShowName = stringResource(R.string.preview_show_name)
    val strPreviewHideContents = stringResource(R.string.preview_hide_contents)
    val strNotificationPreviewsDialogTitle = stringResource(R.string.notification_previews_dialog_title)
    val strWakeScreenLabel = stringResource(R.string.wake_screen_label)
    val strSectionGeneral = stringResource(R.string.section_general)
    val strButton1 = stringResource(R.string.notif_button_1_label)
    val strButton2 = stringResource(R.string.notif_button_2_label)
    val strButton3 = stringResource(R.string.notif_button_3_label)
    val strMarkAsRead = stringResource(R.string.menu_mark_as_read)
    val strReply = stringResource(R.string.notif_action_reply)
    val strNone = stringResource(R.string.swipe_action_none)
    val strCall = stringResource(R.string.content_desc_call)
    val strDelete = stringResource(R.string.swipe_action_delete)
    val strActionButtonTitleTemplate = stringResource(R.string.notif_action_button_title_template)
    val strSectionQuickReply = stringResource(R.string.section_quick_reply)
    val strQuickReplyDesc = stringResource(R.string.quick_reply_desc)
    val strTapToDismissLabel = stringResource(R.string.tap_to_dismiss_label)
    val strTapToDismissDesc = stringResource(R.string.tap_to_dismiss_desc)

    val previewLabel = when (previewOption) {
        1 -> strPreviewShowName
        2 -> strPreviewHideContents
        else -> strPreviewShowNameAndMessage
    }

    // Which button's picker is open: 0 = none, 1/2/3 = Button 1/2/3.
    var activeButtonDialog by remember { mutableStateOf(0) }

    fun labelFor(key: String): String = when (key) {
        "mark_read" -> strMarkAsRead
        "reply" -> strReply
        "call" -> strCall
        "delete" -> strDelete
        else -> strNone
    }
    fun keyFor(label: String): String = when (label) {
        strMarkAsRead -> "mark_read"
        strReply -> "reply"
        strCall -> "call"
        strDelete -> "delete"
        else -> "none"
    }
    val buttonActionOptions = listOf(strNone, strMarkAsRead, strReply, strCall, strDelete)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary),
        topBar = {
            SecondaryTopBar(
                title = strTitle,
                onBackClick = { navController.popBackStackWithAd() },
                actions = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            NotificationSettingsRow(
                title = strNotification,
                subtitle = strTapToCustomize,
                onClick = { openDefaultNotificationChannelSettings(context) }
            )
            NotificationSettingsDivider()
            NotificationSettingsRow(
                title = strNotificationPreviewsLabel,
                subtitle = previewLabel,
                onClick = { showPreviewsDialog = true }
            )
            NotificationSettingsDivider()
            NotificationSettingsToggleRow(
                title = strWakeScreenLabel,
                subtitle = null,
                checked = wakeScreenEnabled,
                onCheckedChange = {
                    wakeScreenEnabled = it
                    NotificationSettingsState.setWakeScreenEnabled(context, it)
                }
            )

            NotificationSettingsSectionHeader(title = strSectionGeneral)
            NotificationSettingsRow(
                title = strButton1,
                subtitle = labelFor(button1ActionKey),
                onClick = { activeButtonDialog = 1 }
            )
            NotificationSettingsRow(
                title = strButton2,
                subtitle = labelFor(button2ActionKey),
                onClick = { activeButtonDialog = 2 }
            )
            NotificationSettingsRow(
                title = strButton3,
                subtitle = labelFor(button3ActionKey),
                onClick = { activeButtonDialog = 3 }
            )

            NotificationSettingsSectionHeader(title = strSectionQuickReply)
            NotificationSettingsToggleRow(
                title = strSectionQuickReply,
                subtitle = strQuickReplyDesc,
                checked = quickReplyEnabled,
                onCheckedChange = {
                    quickReplyEnabled = it
                    NotificationSettingsState.setQuickReplyEnabled(context, it)
                }
            )
            NotificationSettingsToggleRow(
                title = strTapToDismissLabel,
                subtitle = strTapToDismissDesc,
                checked = tapToDismissEnabled,
                onCheckedChange = {
                    tapToDismissEnabled = it
                    NotificationSettingsState.setTapToDismissEnabled(context, it)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (activeButtonDialog != 0) {
            val currentKey = when (activeButtonDialog) {
                1 -> button1ActionKey
                2 -> button2ActionKey
                else -> button3ActionKey
            }
            RadioOptionDialog(
                title = String.format(strActionButtonTitleTemplate, activeButtonDialog),
                options = buttonActionOptions,
                selectedOption = labelFor(currentKey),
                onDismiss = { activeButtonDialog = 0 },
                onConfirm = { choice ->
                    val key = keyFor(choice)
                    when (activeButtonDialog) {
                        1 -> button1ActionKey = key
                        2 -> button2ActionKey = key
                        else -> button3ActionKey = key
                    }
                    NotificationSettingsState.setButtonAction(context, activeButtonDialog, key)
                    activeButtonDialog = 0
                }
            )
        }

        if (showPreviewsDialog) {
            val previewOptions = listOf(strPreviewShowNameAndMessage, strPreviewShowName, strPreviewHideContents)
            NotificationPreviewsDialog(
                title = strNotificationPreviewsDialogTitle,
                options = previewOptions,
                selectedOption = previewLabel,
                onDismiss = { showPreviewsDialog = false },
                onConfirm = { choice ->
                    val option = when (choice) {
                        strPreviewShowName -> 1
                        strPreviewHideContents -> 2
                        else -> 0
                    }
                    previewOption = option
                    NotificationSettingsState.setPreviewOption(context, option)
                    showPreviewsDialog = false
                }
            )
        }
    }
}

/** Opens the shared default channel's native system settings (Floating notifications / Sound / Vibration / Lock screen). */
private fun openDefaultNotificationChannelSettings(context: Context) {
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

@Composable
private fun NotificationSettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontFamily = Inter,
        color = colorResource(R.color.light_color_gray),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun NotificationSettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = colorResource(R.color.light_gray),
        thickness = 1.dp
    )
}

@Composable
private fun NotificationSettingsRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            color = colorResource(R.color.text_title)
        )
        if (!subtitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontFamily = Inter,
                color = colorResource(R.color.text_des)
            )
        }
    }
}

@Composable
private fun NotificationSettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Normal,
                color = colorResource(R.color.text_title)
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontFamily = Inter,
                    color = colorResource(R.color.text_des)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        CustomSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
