package com.messages.sms.texting.app.ui.screens

import com.messages.sms.texting.app.navigation.popBackStackWithAd

import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.ads.NativeAdTemplate
import com.messages.sms.texting.app.ads.NativeAdView
import com.messages.sms.texting.app.ads.interleaveAdEvery3
import com.messages.sms.texting.app.model.ScheduledMessage
import com.messages.sms.texting.app.navigation.Routes
import com.messages.sms.texting.app.ui.components.CustomIconButton
import com.messages.sms.texting.app.ui.components.ScrollToTopButton
import com.messages.sms.texting.app.ui.components.SecondaryTopBar
import com.messages.sms.texting.app.ui.components.dialogs.AlarmPermissionDialog
import kotlinx.coroutines.launch
import com.messages.sms.texting.app.ui.theme.Inter
import com.messages.sms.texting.app.ui.theme.AvatarColors
import com.messages.sms.texting.app.viewmodel.AppConfigViewModel
import com.messages.sms.texting.app.viewmodel.ScheduledMessagesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PhoneRegex = Regex("^[+]?[0-9\\s\\-()]+$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessagesScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: ScheduledMessagesViewModel = viewModel()
    val scheduledMessages by viewModel.scheduledMessages.collectAsState()
    var showAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf<ScheduledMessage?>(null) }
    val scheduledListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val listNativeAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.native_3_on_off == "on") {
            result.native_3?.takeIf { it.isNotBlank() }
        } else null
    }
    val scheduledRows = remember(scheduledMessages, listNativeAdUnitId) {
        scheduledMessages.interleaveAdEvery3(listNativeAdUnitId != null)
    }

    LaunchedEffect(scheduledMessages) {
        if (showOptionsDialog != null && scheduledMessages.none { it.id == showOptionsDialog?.id }) {
            showOptionsDialog = null
        }
    }

    val strScheduledMessagesTitle = stringResource(R.string.scheduled_messages_title)
    val strAddScheduledMessage = stringResource(R.string.content_desc_add_scheduled_message)
    val strNoScheduledMessages = stringResource(R.string.content_desc_no_scheduled_messages)
    val strNoScheduledMessagesText = stringResource(R.string.no_scheduled_messages_text)
    val strScheduledMessageDialogTitle = stringResource(R.string.scheduled_message_dialog_title)
    val strClose = stringResource(R.string.content_desc_close)
    val strOptionUpdateMessage = stringResource(R.string.option_update_message)
    val strOptionSendNow = stringResource(R.string.option_send_now)
    val strOptionCopyText = stringResource(R.string.option_copy_text)
    val strOptionDeleteMessage = stringResource(R.string.option_delete_message)
    val strToastSendingMessage = stringResource(R.string.toast_sending_message)
    val strToastTextCopied = stringResource(R.string.toast_text_copied)
    val strToastMessageDeleted = stringResource(R.string.toast_message_deleted)
    val strContentDescScrollToTop = stringResource(R.string.content_desc_scroll_to_top)

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary),
        topBar = {
            SecondaryTopBar(
                title = strScheduledMessagesTitle,
                onBackClick = { navController.popBackStackWithAd() }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(bottom = 20.dp, end = 8.dp)
                    .size(62.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorResource(R.color.primary))
                    .clickable {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            alarmManager.canScheduleExactAlarms()
                        } else {
                            true
                        }
                        if (canSchedule) {
                            navController.navigate(Routes.NewChat.createRoute(isScheduling = true))
                        } else {
                            showAlarmPermissionDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_ic_add),
                    contentDescription = strAddScheduledMessage,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { innerPadding ->
        if (scheduledMessages.isEmpty()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val imageWidth = maxWidth * 0.8f
                val imageHeight = imageWidth / 1.5f
                val centerShift = 24.dp

                Image(
                    painter = painterResource(id = R.drawable.scheduled_main),
                    contentDescription = strNoScheduledMessages,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = -centerShift)
                        .width(imageWidth)
                        .height(imageHeight)
                )
                Text(
                    text = strNoScheduledMessagesText,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_des),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = imageHeight / 2 + 10.dp - centerShift)
                        .padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                state = scheduledListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                items(
                    count = scheduledRows.size,
                    key = { idx -> scheduledRows[idx]?.id ?: "ad_$idx" }
                ) { idx ->
                    val msg = scheduledRows[idx]
                    if (msg == null) {
                        NativeAdView(
                            adUnitId = listNativeAdUnitId!!,
                            template = NativeAdTemplate.SMALL,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        ScheduledMessageItem(
                            modifier = Modifier.animateItem(),
                            msg = msg,
                            onClick = {
                                showOptionsDialog = msg
                            },
                            onLongClick = {
                                showOptionsDialog = msg
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAlarmPermissionDialog) {
        AlarmPermissionDialog(
            onAllowClick = {
                showAlarmPermissionDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            },
            onDismissClick = { showAlarmPermissionDialog = false }
        )
    }

    if (showOptionsDialog != null) {
        val selectedMsg = showOptionsDialog!!
        androidx.compose.ui.window.Dialog(onDismissRequest = { showOptionsDialog = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colorResource(R.color.light_gray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strScheduledMessageDialogTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = Inter,
                            color = colorResource(R.color.text_title)
                        )
                        CustomIconButton(
                            iconRes = R.drawable.longpress_ic_close,
                            contentDescription = strClose,
                            size = 20,
                            onClick = {showOptionsDialog = null  }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val options = listOf(
                        0 to strOptionUpdateMessage,
                        1 to strOptionSendNow,
                        2 to strOptionCopyText,
                        3 to strOptionDeleteMessage
                    )
                    options.forEach { (optionId, optionLabel) ->
                        Text(
                            text = optionLabel,
                            fontSize = 16.sp,
                            fontFamily = Inter,
                            color = colorResource(R.color.option_row_text),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsDialog = null
                                    when (optionId) {
                                        0 -> {
                                            navController.navigate(
                                                Routes.Chat.createRoute(
                                                    threadId = 0L,
                                                    address = selectedMsg.address,
                                                    contactName = selectedMsg.contactName,
                                                    isScheduling = false, // Not auto-opening picker because data is pre-filled
                                                    scheduledMessageId = selectedMsg.id,
                                                    groupId = selectedMsg.groupId
                                                )
                                            )
                                        }
                                        1 -> {
                                            viewModel.sendNow(selectedMsg)
                                            Toast.makeText(context, strToastSendingMessage, Toast.LENGTH_SHORT).show()
                                        }
                                        2 -> {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Copied Text", selectedMsg.body))
                                            Toast.makeText(context, strToastTextCopied, Toast.LENGTH_SHORT).show()
                                        }
                                        3 -> {
                                            viewModel.cancelScheduledMessage(selectedMsg.id)
                                            Toast.makeText(context, strToastMessageDeleted, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }

    ScrollToTopButton(
        visible = scheduledListState.firstVisibleItemIndex > 0,
        onClick = { coroutineScope.launch { scheduledListState.animateScrollToItem(0) } },
        contentDescription = strContentDescScrollToTop,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 20.dp)
    )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduledMessageItem(
    modifier: Modifier = Modifier,
    msg: ScheduledMessage,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isGroupMessage = msg.groupId != null
        val displayName = if (isGroupMessage) msg.groupName.orEmpty() else msg.contactName ?: msg.address
        val avatarColor = remember(msg.address, msg.groupId) {
            val hash = (if (isGroupMessage) msg.groupId.toString() else msg.address).hashCode() and 0x7FFFFFFF
            AvatarColors[hash % AvatarColors.size]
        }
        val isPhoneNumber = remember(msg.address) {
            msg.address.matches(PhoneRegex)
        }

        if (!msg.photoUri.isNullOrEmpty()) {
            AsyncImage(
                model = msg.photoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                if (isGroupMessage) {
                    Icon(
                        painter = painterResource(id = R.drawable.contact_ic_group),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                } else if (msg.contactName == null && isPhoneNumber) {
                    Icon(
                        painter = painterResource(id = R.drawable.home_ic_filled_person),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                } else {
                    Text(
                        text = displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right Content Wrapper
        Row(modifier = Modifier.weight(1f)) {
            // Left Column (Title and Description)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_title),
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg.body,
                    fontSize = 14.sp,
                    color = colorResource(R.color.text_des).copy(alpha = 0.8f),
                    fontFamily = Inter,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Column (Time)
            Column(horizontalAlignment = Alignment.End) {
                val cal = java.util.Calendar.getInstance()
                val todayYear = cal.get(java.util.Calendar.YEAR)
                val todayDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
                
                cal.timeInMillis = msg.scheduledTimeMillis
                val scheduledYear = cal.get(java.util.Calendar.YEAR)
                val scheduledDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
                
                val pattern = if (todayYear == scheduledYear && todayDay == scheduledDay) {
                    "h:mm a"
                } else {
                    "d MMM, h:mm a"
                }
                
                val dateFormat = SimpleDateFormat(pattern, Locale.US)
                val timeString = dateFormat.format(Date(msg.scheduledTimeMillis)).uppercase().replace(".", "")

                Text(
                    text = timeString,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_des),
                    fontFamily = Inter
                )
            }
        }
    }
}
