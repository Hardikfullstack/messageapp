package com.messages.sms.texting.app.ui.screens

import com.messages.sms.texting.app.navigation.popBackStackWithAd

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vanniktech.emoji.EmojiView
import com.vanniktech.emoji.EmojiTheming
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.model.SmsMessage
import com.messages.sms.texting.app.ui.components.ChatSelectionTopBar
import com.messages.sms.texting.app.ui.components.SecondaryTopBar
import com.messages.sms.texting.app.ui.components.dialogs.ConfirmationDialog
import com.messages.sms.texting.app.ui.components.dialogs.MessageDetailsDialog
import com.messages.sms.texting.app.ui.theme.Inter
import com.messages.sms.texting.app.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.messages.sms.texting.app.ui.components.CustomIconButton
import com.messages.sms.texting.app.ui.components.ScrollToTopButton
import com.messages.sms.texting.app.navigation.Routes
import android.provider.ContactsContract
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.messages.sms.texting.app.ui.components.ChatBubbleSkeletonUi
import com.messages.sms.texting.app.ui.components.dialogs.AlarmPermissionDialog
import com.messages.sms.texting.app.ui.components.dialogs.DateTimePickerDialog
import android.app.AlarmManager
import android.os.Build
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import com.messages.sms.texting.app.ads.BannerAdView
import com.messages.sms.texting.app.ads.InterstitialAdManager
import com.messages.sms.texting.app.ads.NativeAdTemplate
import com.messages.sms.texting.app.ads.NativeAdView
import com.messages.sms.texting.app.ads.waitUntilAdReady
import com.messages.sms.texting.app.viewmodel.AppConfigViewModel

@Composable
fun ChatScreen(
    navController: NavController,
    threadId: Long,
    address: String,
    contactName: String?,
    highlightMsgId: Long? = null,
    isScheduling: Boolean = false,
    scheduledMessageId: Long? = null,
    searchQuery: String? = null,
    forwardText: String? = null,
    groupId: Long? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val isGroupMode = groupId != null

    val strCopied = stringResource(R.string.toast_copied)
    val strShareMessageTitle = stringResource(R.string.share_message_title)
    val strUnblockToSend = stringResource(R.string.toast_unblock_to_send)
    val strGroupMmsUnsupported = stringResource(R.string.toast_group_mms_unsupported)
    val strUnblockToSchedule = stringResource(R.string.toast_unblock_to_schedule)
    val strContactUnblocked = stringResource(R.string.toast_contact_unblocked)
    val strUndo = stringResource(R.string.snackbar_undo)
    val strScheduledTimeFuture = stringResource(R.string.toast_scheduled_time_future)
    val strNoInternet = stringResource(R.string.toast_no_internet)
    val strTurnOnLocation = stringResource(R.string.toast_turn_on_location)
    val strUnableGetLocation = stringResource(R.string.toast_unable_get_location)
    val strNoAppForLink = stringResource(R.string.toast_no_app_for_link)
    val strNoAppForDialer = stringResource(R.string.toast_no_app_for_dialer)
    val strUnblockInstructionsTemplate = stringResource(R.string.unblock_instructions_text)
    val strBlocked = stringResource(R.string.content_desc_blocked)
    val strUnblock = stringResource(R.string.action_unblock)
    val strCantReply = stringResource(R.string.cant_reply_text)
    val strNoMessages = stringResource(R.string.content_desc_no_messages)
    val strSayHi = stringResource(R.string.say_hi_text)
    val strDeleteMessageTitle = stringResource(R.string.delete_message_title)
    val strActionDelete = stringResource(R.string.action_delete)
    val strContentDescCall = stringResource(R.string.content_desc_call)
    val strContentDescInfo = stringResource(R.string.content_desc_info)
    val strDeleteMessagesConfirmText = stringResource(R.string.delete_messages_confirm_text)
    val strMessagesDeletedGeneric = stringResource(R.string.toast_messages_deleted_generic)
    val strScrollToBottom = stringResource(R.string.content_desc_scroll_to_bottom)
    val strChatDateToday = stringResource(R.string.chat_date_today)
    val viewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(application, threadId, address, contactName, scheduledMessageId, groupId) as T
            }
        }
    )

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val chatBannerAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.banner_2_on_off == "on") {
            result.banner_2?.takeIf { it.isNotBlank() }
        } else null
    }
    val chatNativeAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.native_8_on_off == "on") {
            result.native_8?.takeIf { it.isNotBlank() }
        } else null
    }
    val scheduleInterstitialAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.interstitial_2_on_off == "on") {
            result.interstitial_2?.takeIf { it.isNotBlank() }
        } else null
    }
    var isWaitingForScheduleAd by remember { mutableStateOf(false) }

    DisposableEffect(threadId, address) {
        if (!isGroupMode) {
            com.messages.sms.texting.app.AppState.activeThreadId = threadId
            com.messages.sms.texting.app.AppState.activeAddress = address
            // Clear any existing notification for this thread now that it's actually being viewed —
            // regardless of how the user got here (tapping the notification is only one path).
            androidx.core.app.NotificationManagerCompat.from(context).cancel(threadId.toInt())
        }
        onDispose {
            if (!isGroupMode) {
                com.messages.sms.texting.app.AppState.activeThreadId = null
                com.messages.sms.texting.app.AppState.activeAddress = null
            }
        }
    }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val messageToUpdate by viewModel.messageToUpdate.collectAsState()
    val isBlocked by viewModel.isBlocked.collectAsState()
    val group by viewModel.group.collectAsState()

    var inputText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var initialValuesSet by remember { mutableStateOf(false) }
    var isEmojiPickerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(messageToUpdate) {
        if (messageToUpdate != null && !initialValuesSet) {
            inputText = androidx.compose.ui.text.input.TextFieldValue(
                text = messageToUpdate!!.body,
                selection = androidx.compose.ui.text.TextRange(messageToUpdate!!.body.length)
            )
            initialValuesSet = true
        }
    }

    var forwardTextApplied by remember { mutableStateOf(false) }
    LaunchedEffect(forwardText) {
        if (!forwardText.isNullOrEmpty() && !forwardTextApplied) {
            inputText = androidx.compose.ui.text.input.TextFieldValue(
                text = forwardText,
                selection = androidx.compose.ui.text.TextRange(forwardText.length)
            )
            forwardTextApplied = true
        }
    }

    // Editing a scheduled message is a distinct flow; don't let it read/write
    // the per-address draft.
    val isDraftEligible = scheduledMessageId == null
    var isDraftLoadComplete by remember { mutableStateOf(false) }

    // Restore an unsent draft for this conversation, unless we're forwarding
    // a message or editing a scheduled message (those take priority).
    LaunchedEffect(Unit) {
        if (isDraftEligible && forwardText.isNullOrEmpty()) {
            val draft = viewModel.loadDraft()
            if (!draft.isNullOrEmpty() && inputText.text.isEmpty()) {
                inputText = androidx.compose.ui.text.input.TextFieldValue(
                    text = draft,
                    selection = androidx.compose.ui.text.TextRange(draft.length)
                )
            }
        }
        isDraftLoadComplete = true
    }

    val latestInputText by rememberUpdatedState(inputText)

    // Debounced auto-save while typing, so a mid-session kill doesn't lose the draft.
    LaunchedEffect(inputText.text, isDraftLoadComplete) {
        if (isDraftEligible && isDraftLoadComplete) {
            kotlinx.coroutines.delay(500)
            viewModel.saveDraft(inputText.text)
        }
    }

    // Save immediately when the app is backgrounded (Home button etc.), since the
    // process can be killed later without this composable ever being disposed.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (isDraftEligible && event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.saveDraft(latestInputText.text)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Save when leaving the chat via back navigation.
    DisposableEffect(Unit) {
        onDispose {
            if (isDraftEligible) {
                viewModel.saveDraft(latestInputText.text)
            }
        }
    }

    var activeSearchQuery by remember { mutableStateOf(searchQuery) }
    
    val searchMatchIndices = remember(messages, activeSearchQuery) {
        if (activeSearchQuery.isNullOrBlank()) emptyList()
        else {
            val query = activeSearchQuery!!
            messages.mapIndexedNotNull { index, msg ->
                if (msg.body.contains(query, ignoreCase = true)) index else null
            }
        }
    }
    
    var currentMatchIndex by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(searchMatchIndices, currentMatchIndex) {
        if (searchMatchIndices.isNotEmpty() && currentMatchIndex in searchMatchIndices.indices) {
            listState.animateScrollToItem(searchMatchIndices[currentMatchIndex])
        }
    }

    val selectedMessageIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedMessageIds.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val isImeVisible =
        androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

    androidx.activity.compose.BackHandler {
        if (isSelectionMode) {
            selectedMessageIds.clear()
        } else if (activeSearchQuery != null) {
            activeSearchQuery = null
        } else if (isEmojiPickerExpanded) {
            isEmojiPickerExpanded = false
        } else {
            if (isScheduling) {
                navController.popBackStackWithAd(Routes.ScheduledMessages.route, inclusive = false)
            } else {
                navController.popBackStackWithAd()
            }
        }
    }

    val isReplyable = remember(address, isGroupMode) {
        isGroupMode || address.matches(Regex("^[+]?[0-9\\s\\-()]+$"))
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf<SmsMessage?>(null) }
    var hasScrolledToHighlight by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(messages, highlightMsgId) {
        if (highlightMsgId != null && messages.isNotEmpty() && !hasScrolledToHighlight) {
            val groupedMsgs = messages.groupBy {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.date
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            var targetIndex = -1
            var currentIndex = 0
            for ((_, msgsForDate) in groupedMsgs) {
                val idx = msgsForDate.indexOfFirst { it.id == highlightMsgId }
                if (idx != -1) {
                    targetIndex = currentIndex + idx
                    break
                }
                currentIndex += msgsForDate.size + 1 // +1 for date header
            }

            if (targetIndex != -1) {
                listState.scrollToItem(targetIndex)
                hasScrolledToHighlight = true
            }
        }
    }

    if (isWaitingForScheduleAd) {
        com.messages.sms.texting.app.ads.AdLoadingScreen(modifier = Modifier.fillMaxSize())
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = colorResource(R.color.bg_primary),
        snackbarHost = { 
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1B1B24),
                    contentColor = Color.White,
                    actionColor = colorResource(R.color.primary)
                )
            }
        },
        topBar = {
            if (activeSearchQuery != null) {
                com.messages.sms.texting.app.ui.components.ChatSearchTopBar(
                    query = activeSearchQuery!!,
                    currentMatch = if (searchMatchIndices.isEmpty()) 0 else searchMatchIndices.size - currentMatchIndex,
                    totalMatches = searchMatchIndices.size,
                    onUp = {
                        if (searchMatchIndices.isNotEmpty()) {
                            // Up means older message. Since messages are sorted descending, older is larger index.
                            if (currentMatchIndex < searchMatchIndices.size - 1) currentMatchIndex++
                        }
                    },
                    onDown = {
                        if (searchMatchIndices.isNotEmpty()) {
                            // Down means newer message -> smaller index.
                            if (currentMatchIndex > 0) currentMatchIndex--
                        }
                    },
                    onClose = { activeSearchQuery = null },
                    onBack = {
                        if (isEmojiPickerExpanded) {
                            isEmojiPickerExpanded = false
                        } else if (isImeVisible) {
                            focusManager.clearFocus()
                        } else {
                            if (isScheduling) {
                                navController.popBackStackWithAd(Routes.ScheduledMessages.route, inclusive = false)
                            } else {
                                navController.popBackStackWithAd()
                            }
                        }
                    }
                )
            } else if (isSelectionMode) {
                val selectedMsgs = messages.filter { selectedMessageIds.contains(it.id) }
                val isAllStarred = selectedMsgs.isNotEmpty() && selectedMsgs.all { it.isStarred }

                ChatSelectionTopBar(
                    selectedCount = selectedMessageIds.size,
                    isStarred = isAllStarred,
                    onClose = { selectedMessageIds.clear() },
                    onStar = {
                        val selectedMsgs = messages.filter { selectedMessageIds.contains(it.id) }
                        val isAllStarred = selectedMsgs.all { it.isStarred }
                        val newStarredStatus = !isAllStarred
                        viewModel.toggleStarredMessages(
                            selectedMessageIds.toList(),
                            newStarredStatus
                        )
                        selectedMessageIds.clear()
                    },
                    onDelete = {
                        showDeleteDialog = true
                    },
                    onCopy = {
                        val msgs = messages.filter { selectedMessageIds.contains(it.id) }
                            .sortedBy { it.date }
                        val textToCopy = msgs.joinToString("\n") { it.body }
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied Messages", textToCopy)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, strCopied, Toast.LENGTH_SHORT).show()
                        selectedMessageIds.clear()
                    },
                    onShare = {
                        val msgs = messages.filter { selectedMessageIds.contains(it.id) }
                            .sortedBy { it.date }
                        val textToShare = msgs.joinToString("\n") { it.body }
                        val shareIntent =
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, textToShare)
                            }
                        context.startActivity(
                            android.content.Intent.createChooser(
                                shareIntent,
                                strShareMessageTitle
                            )
                        )
                        selectedMessageIds.clear()
                    },
                    onForward = {
                        val msgs = messages.filter { selectedMessageIds.contains(it.id) }
                            .sortedBy { it.date }
                        val textToForward = msgs.joinToString("\n") { it.body }
                        selectedMessageIds.clear()
                        navController.navigate(Routes.NewChat.createRoute(forwardText = textToForward))
                    },
                    onDetails = {
                        val selectedMsg =
                            messages.find { it.id == selectedMessageIds.firstOrNull() }
                        if (selectedMsg != null) {
                            showDetailsDialog = selectedMsg
                        }
                        selectedMessageIds.clear()
                    }
                )
            } else {
                SecondaryTopBar(
                    title = if (isGroupMode) group?.name?.ifBlank { "Group" } ?: "Group" else contactName ?: address,
                    onBackClick = {
                        if (isEmojiPickerExpanded) {
                            isEmojiPickerExpanded = false
                        } else if (isImeVisible) {
                            focusManager.clearFocus()
                        } else {
                            if (isScheduling) {
                                navController.popBackStackWithAd(Routes.ScheduledMessages.route, inclusive = false)
                            } else {
                                navController.popBackStackWithAd()
                            }
                        }
                    },
                    actions = {
                        if (isReplyable && !isGroupMode) {
                            CustomIconButton(
                                iconRes = R.drawable.chat_ic_phone,
                                contentDescription = strContentDescCall,
                                tint = colorResource(R.color.chat_icon_secondary),
                                onClick = { 
                                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                                    val defaultDialer = telecomManager?.defaultDialerPackage
                                    
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                        val intent = Intent(Intent.ACTION_CALL).apply {
                                            data = Uri.parse("tel:$address")
                                            if (defaultDialer != null) setPackage(defaultDialer)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            intent.setPackage(null)
                                            try {
                                                context.startActivity(intent)
                                            } catch (e2: Exception) {
                                                e2.printStackTrace()
                                            }
                                        }
                                    } else {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$address")
                                            if (defaultDialer != null) setPackage(defaultDialer)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            intent.setPackage(null)
                                            try {
                                                context.startActivity(intent)
                                            } catch (e2: Exception) {
                                                e2.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        CustomIconButton(
                            iconRes = R.drawable.chat_ic_info,
                            contentDescription = strContentDescInfo,
                            tint = colorResource(R.color.chat_icon_secondary),
                            onClick = {
                                navController.navigate(
                                    Routes.ChatDetails.createRoute(
                                        threadId = threadId,
                                        address = address,
                                        contactName = contactName,
                                        isArchived = false, // The details screen will fetch the real status
                                        groupId = groupId
                                    )
                                )
                            }
                        )
                    }
                )
            }
        },
        bottomBar = {
            Column {
                if (chatNativeAdUnitId != null) {
                    NativeAdView(
                        adUnitId = chatNativeAdUnitId,
                        template = NativeAdTemplate.MEDIUM,
                        compact = true,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            if (isReplyable) {
                ChatBottomInput(
                    text = inputText,
                    onTextChanged = { inputText = it },
                    autoFocus = !forwardText.isNullOrEmpty(),
                    onSend = { bitmap ->
                        if (isBlocked) {
                            android.widget.Toast.makeText(context, strUnblockToSend, android.widget.Toast.LENGTH_SHORT).show()
                            return@ChatBottomInput
                        }
                        if (bitmap != null) {
                            if (isGroupMode) {
                                android.widget.Toast.makeText(
                                    context,
                                    strGroupMmsUnsupported,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.sendMmsMessage(inputText.text, bitmap)
                                inputText = androidx.compose.ui.text.input.TextFieldValue("")
                            }
                        } else if (inputText.text.isNotBlank()) {
                            viewModel.sendMessage(inputText.text)
                            inputText = androidx.compose.ui.text.input.TextFieldValue("")
                        }
                    },
                    onSchedule = { timeInMillis ->
                        if (isBlocked) {
                            android.widget.Toast.makeText(context, strUnblockToSchedule, android.widget.Toast.LENGTH_SHORT).show()
                            return@ChatBottomInput
                        }
                        if (inputText.text.isNotBlank()) {
                            viewModel.scheduleMessage(inputText.text, timeInMillis)
                            inputText = androidx.compose.ui.text.input.TextFieldValue("")

                            val navigateAfterSchedule = {
                                if (isScheduling || scheduledMessageId != null) {
                                    navController.popBackStackWithAd(Routes.ScheduledMessages.route, inclusive = false)
                                } else {
                                    navController.navigate(Routes.ScheduledMessages.route)
                                }
                            }
                            val activity = context as? Activity
                            coroutineScope.launch {
                                // Offline — a cached config can still say the ad is "on" with
                                // nothing able to load it; don't wait out the full timeout for an
                                // ad that can never arrive.
                                if (activity != null && scheduleInterstitialAdUnitId != null && appConfigViewModel.isOnline.value) {
                                    if (!InterstitialAdManager.isReady(scheduleInterstitialAdUnitId)) {
                                        isWaitingForScheduleAd = true
                                        waitUntilAdReady { InterstitialAdManager.isReady(scheduleInterstitialAdUnitId) }
                                        isWaitingForScheduleAd = false
                                    }
                                    if (InterstitialAdManager.isReady(scheduleInterstitialAdUnitId)) {
                                        InterstitialAdManager.show(activity, scheduleInterstitialAdUnitId) { navigateAfterSchedule() }
                                    } else {
                                        navigateAfterSchedule()
                                    }
                                } else {
                                    navigateAfterSchedule()
                                }
                            }
                        }
                    },
                    isScheduling = isScheduling,
                    initialScheduledTime = messageToUpdate?.scheduledTimeMillis,
                    isGroupChat = isGroupMode,
                    isEmojiPickerExpanded = isEmojiPickerExpanded,
                    onEmojiPickerExpandedChange = { isEmojiPickerExpanded = it },
                    onScheduleIntentShown = {
                        scheduleInterstitialAdUnitId?.let { InterstitialAdManager.preload(context, it) }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.bg_primary))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strCantReply,
                        color = colorResource(R.color.text_des),
                        fontSize = 14.sp,
                        fontFamily = Inter,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (chatBannerAdUnitId != null) {
                BannerAdView(adUnitId = chatBannerAdUnitId, adaptive = true)
            }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isBlocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(colorResource(R.color.light_gray), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(top = 16.dp , start = 16.dp, end = 16.dp, bottom = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.block_list_ic_block),
                            contentDescription = strBlocked,
                            tint = colorResource(R.color.text_title),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strBlocked,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = colorResource(R.color.text_title)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(strUnblockInstructionsTemplate, contactName ?: address),
                                fontFamily = Inter,
                                fontSize = 14.sp,
                                color = colorResource(R.color.text_des),
                                lineHeight = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strUnblock,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colorResource(R.color.primary),
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                viewModel.unblockContact()
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = strContactUnblocked,
                                        actionLabel = strUndo,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.blockContact()
                                    }
                                }
                            }
                            .padding(8.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.Bottom // Simulate chats from bottom
                    ) {
                        ChatBubbleSkeletonUi(isSent = false)
                        ChatBubbleSkeletonUi(isSent = true)
                        ChatBubbleSkeletonUi(isSent = false)
                        ChatBubbleSkeletonUi(isSent = false)
                        ChatBubbleSkeletonUi(isSent = true)
                    }
                } else if (messages.isEmpty()) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val imageWidth = maxWidth * 0.8f
                        val imageHeight = imageWidth / 1.5f
                        val centerShift = 24.dp

                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.archived_main),
                            contentDescription = strNoMessages,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = -centerShift)
                                .width(imageWidth)
                                .height(imageHeight)
                        )
                        Text(
                            text = strSayHi,
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
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                itemsIndexed(
                    items = messages,
                    key = { _, msg -> msg.id }
                ) { index, msg ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // messages is ordered newest-first (DESC); the next-older message is at index + 1.
                        val showDateSeparator = index == messages.lastIndex || !isSameDay(msg.date, messages[index + 1].date)
                        if (showDateSeparator) {
                            val dateFormatted = formatChatDateSeparator(msg.date, strChatDateToday)
                            Text(
                                text = dateFormatted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                textAlign = TextAlign.Center,
                                color = colorResource(R.color.light_color_gray),
                                fontSize = 12.sp,
                                fontFamily = Inter
                            )
                        }

                        ChatBubble(
                            modifier = Modifier.animateItem(),
                            msg = msg,
                            isSelected = selectedMessageIds.contains(msg.id),
                            isSelectionMode = isSelectionMode,
                            searchQuery = activeSearchQuery,
                            isSearchFocused = searchMatchIndices.isNotEmpty() && currentMatchIndex in searchMatchIndices.indices && searchMatchIndices[currentMatchIndex] == index,
                            onToggleSelection = {
                                // Group messages live in a separate table with their own id
                                // space, so selecting them for star/delete (which act on real
                                // SMS ids) isn't safe — selection is disabled in group mode.
                                if (!isGroupMode) {
                                    if (selectedMessageIds.contains(msg.id)) {
                                        selectedMessageIds.remove(msg.id)
                                    } else {
                                        selectedMessageIds.add(msg.id)
                                    }
                                }
                            },
                            onRetry = { if (msg.isMms) viewModel.retryMms(msg.id) else viewModel.retrySms(msg.id) },
                            onImageClick = { path ->
                                navController.navigate(
                                    Routes.ImageViewer.createRoute(
                                        imagePath = path,
                                        name = contactName ?: address,
                                        date = msg.date
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

                    ScrollToTopButton(
                        visible = listState.firstVisibleItemIndex > 0,
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                        contentDescription = strScrollToBottom,
                        rotationDegrees = 180f,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
    }
        }
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = strDeleteMessageTitle,
            text = strDeleteMessagesConfirmText,
            onConfirm = {
                viewModel.deleteMessages(selectedMessageIds.toList())
                selectedMessageIds.clear()
                showDeleteDialog = false
                Toast.makeText(context, strMessagesDeletedGeneric, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showDetailsDialog != null) {
        val msg = showDetailsDialog!!
        MessageDetailsDialog(
            msg = msg,
            onDismiss = { showDetailsDialog = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    msg: SmsMessage,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    searchQuery: String? = null,
    isSearchFocused: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onRetry: (Long) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val strAttachedImage = stringResource(R.string.content_desc_attached_image)
    val strStarred = stringResource(R.string.content_desc_starred)
    val strSending = stringResource(R.string.sending_text)
    val strFailedToSendRetry = stringResource(R.string.failed_to_send_retry)
    val strDelayedMessage = stringResource(R.string.content_desc_delayed_message)
    val strDeliveredAtTemplate = stringResource(R.string.delivered_at_template)

    val isSent = msg.type == 2
    val bubbleColor =
        if (isSelected) colorResource(R.color.primary)
        else if (isSent) colorResource(R.color.selection_blue_bg)
        else colorResource(R.color.light_gray)
    val textColor = if (isSelected) Color.White else colorResource(R.color.message_text)
    val align = if (isSent) Alignment.End else Alignment.Start

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection()
                },
                onLongClick = {
                    onToggleSelection()
                }
            )
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = align
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val maxWidth = configuration.screenWidthDp.dp * 0.7f

        val isImageMms = msg.isMms && !msg.mmsImagePath.isNullOrEmpty()
        val isDelayed = msg.sendStatus == SmsMessage.STATUS_DELAYED

        Row(verticalAlignment = Alignment.Bottom) {
            if (isDelayed) {
                DelayedSendRing(delayedUntilMillis = msg.delayedUntilMillis, contentDescription = strDelayedMessage)
                Spacer(modifier = Modifier.width(6.dp))
            }
        Box(modifier = Modifier.widthIn(max = maxWidth)) {
            Box(
                modifier = Modifier
                    .then(if (isImageMms) Modifier else Modifier.background(bubbleColor, RoundedCornerShape(20.dp)))
                    .padding(
                        horizontal = if (isImageMms && msg.body.isBlank()) 0.dp else 16.dp,
                        vertical = if (isImageMms && msg.body.isBlank()) 0.dp else 12.dp
                    )
            ) {
                val context = LocalContext.current
                val linkColor = if (isSelected) Color.White else colorResource(R.color.primary)

                val hasSearchMatch = !searchQuery.isNullOrBlank() && msg.body.contains(searchQuery, ignoreCase = true)
                val highlightStart = if (hasSearchMatch) msg.body.indexOf(searchQuery!!, ignoreCase = true) else -1
                val highlightEnd = if (hasSearchMatch) highlightStart + searchQuery!!.length else -1
                val highlightBgColor = if (isSearchFocused) Color(0xFFFFCB00) else colorResource(R.color.primary)
                val highlightTextColor = if (isSearchFocused) Color.Black else Color.White

                val annotatedBody = buildMessageAnnotatedString(
                    text = msg.body,
                    linkColor = linkColor,
                    highlightStart = highlightStart,
                    highlightEnd = highlightEnd,
                    highlightBgColor = highlightBgColor,
                    highlightTextColor = highlightTextColor
                )

                var textLayoutResult by remember(msg.id) {
                    mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null)
                }

                Column {
                    if (msg.isMms && !msg.mmsImagePath.isNullOrEmpty()) {
                        AsyncImage(
                            model = java.io.File(msg.mmsImagePath),
                            contentDescription = strAttachedImage,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(vertical = if (msg.body.isBlank()) 0.dp else 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .size(250.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelection()
                                        } else {
                                            onImageClick(msg.mmsImagePath)
                                        }
                                    },
                                    onLongClick = { onToggleSelection() }
                                )
                        )
                    }
                    if (msg.body.isNotBlank()) {
                        Text(
                            text = annotatedBody,
                            fontSize = 18.sp,
                            fontFamily = Inter,
                            color = textColor,
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier.pointerInput(msg.id, isSelectionMode) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        if (isSelectionMode) {
                                            onToggleSelection()
                                            return@detectTapGestures
                                        }
                                        val position = textLayoutResult?.getOffsetForPosition(offset) ?: return@detectTapGestures
                                        val urlAnnotation = annotatedBody.getStringAnnotations(
                                            tag = MESSAGE_URL_ANNOTATION_TAG,
                                            start = position,
                                            end = position
                                        ).firstOrNull()
                                        val phoneAnnotation = annotatedBody.getStringAnnotations(
                                            tag = MESSAGE_PHONE_ANNOTATION_TAG,
                                            start = position,
                                            end = position
                                        ).firstOrNull()
                                        when {
                                            urlAnnotation != null -> openMessageUrl(context, urlAnnotation.item)
                                            phoneAnnotation != null -> openPhoneNumber(context, phoneAnnotation.item)
                                        }
                                    },
                                    onLongPress = { onToggleSelection() }
                                )
                            }
                        )
                    }
                }
            }

            if (msg.isStarred) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-12).dp, y = (10).dp)
                        .size(24.dp)
                        .background(colorResource(R.color.primary), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chat_ic_long_star),
                        contentDescription = strStarred,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
        }
        }

        if (msg.sendStatus == SmsMessage.STATUS_SENDING || msg.sendStatus == SmsMessage.STATUS_DELAYED) {
            Text(
                text = strSending,
                fontSize = 12.sp,
                fontFamily = Inter,
                color = colorResource(R.color.text_des),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        } else if (msg.sendStatus == SmsMessage.STATUS_FAILED) {
            Text(
                text = strFailedToSendRetry,
                fontSize = 12.sp,
                fontFamily = Inter,
                color = Color(0xFFD32F2F),
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    .clickable { onRetry(msg.id) }
            )
        } else if (isSent && msg.deliveredAtMillis != null) {
            val deliveredTime = remember(msg.deliveredAtMillis) {
                java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(msg.deliveredAtMillis))
            }
            Text(
                text = String.format(strDeliveredAtTemplate, deliveredTime),
                fontSize = 12.sp,
                fontFamily = Inter,
                color = colorResource(R.color.text_des),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
        /*else {
            val messageTime = remember(msg.date) {
                java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(msg.date))
            }
            Text(
                text = messageTime,
                fontSize = 11.sp,
                fontFamily = Inter,
                color = colorResource(R.color.light_color_gray),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }*/
    }
}

@Composable
fun DelayedSendRing(delayedUntilMillis: Long?, contentDescription: String) {
    val progress = remember(delayedUntilMillis) { Animatable(0f) }
    LaunchedEffect(delayedUntilMillis) {
        val remaining = (delayedUntilMillis ?: System.currentTimeMillis()) - System.currentTimeMillis()
        if (remaining > 0) {
            progress.animateTo(1f, animationSpec = tween(remaining.toInt(), easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val ringColor = colorResource(R.color.primary)
    Box(
        modifier = Modifier
            .size(32.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(ringColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.chat_ic_pause),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun ChatBottomInput(
    text: androidx.compose.ui.text.input.TextFieldValue,
    onTextChanged: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSend: (Bitmap?) -> Unit,
    onSchedule: (Long) -> Unit,
    isScheduling: Boolean = false,
    initialScheduledTime: Long? = null,
    isGroupChat: Boolean = false,
    isEmojiPickerExpanded: Boolean,
    onEmojiPickerExpandedChange: (Boolean) -> Unit,
    autoFocus: Boolean = false,
    // Fired the moment the user actually shows intent to schedule a message (opens the date/time
    // picker) — not on every chat open. Most chats are never used to schedule anything, so eagerly
    // preloading this interstitial as soon as any chat opens wastes a fill on a placement most
    // sessions never reach; deferring the preload to here means it only spends a request when
    // there's real signal the user is heading toward that flow.
    onScheduleIntentShown: () -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = isEmojiPickerExpanded) {
        onEmojiPickerExpandedChange(false)
    }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(isScheduling) }
    LaunchedEffect(Unit) {
        // Covers re-opening an already-scheduled message for editing — showDateTimePicker starts
        // true in that case (set above), so the menu-item click below never fires.
        if (isScheduling) onScheduleIntentShown()
    }

    var scheduledTime by remember(initialScheduledTime) { mutableStateOf(initialScheduledTime) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val strContactInsertTemplate = stringResource(R.string.contact_insert_template)
    val strUnableLoadCapturedImage = stringResource(R.string.toast_unable_load_captured_image)
    val strUnableLoadSelectedImage = stringResource(R.string.toast_unable_load_selected_image)
    val strNoInternet = stringResource(R.string.toast_no_internet)
    val strTurnOnLocation = stringResource(R.string.toast_turn_on_location)
    val strUnableGetLocation = stringResource(R.string.toast_unable_get_location)
    val strMenuContacts = stringResource(R.string.menu_contacts)
    val strMenuCamera = stringResource(R.string.menu_camera)
    val strMenuGallery = stringResource(R.string.menu_gallery)
    val strMenuSchedule = stringResource(R.string.menu_schedule)
    val strMenuLocation = stringResource(R.string.menu_location)
    val strContentDescAttachedImage = stringResource(R.string.content_desc_attached_image)
    val strContentDescRemove = stringResource(R.string.content_desc_remove)
    val strScheduledForText = stringResource(R.string.scheduled_for_text)
    val strContentDescCancelSchedule = stringResource(R.string.content_desc_cancel_schedule)
    val strContentDescToggleMenu = stringResource(R.string.content_desc_toggle_menu)
    val strTypeMessagePlaceholder = stringResource(R.string.type_message_placeholder)
    val strContentDescEmoji = stringResource(R.string.content_desc_emoji)
    val strScheduledTimeFuture = stringResource(R.string.toast_scheduled_time_future)
    val strContentDescSend = stringResource(R.string.content_desc_send)
    val strPermissionRequiredTitle = stringResource(R.string.permission_required_title)
    val strCameraPermissionText = stringResource(R.string.camera_permission_text)
    val strActionOk = stringResource(R.string.action_ok)
    val strLocationPermissionText = stringResource(R.string.location_permission_text)

    val focusRequester = remember { FocusRequester() }
    val currentText by rememberUpdatedState(text)
    val currentOnTextChanged by rememberUpdatedState(onTextChanged)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val contactPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickContact(),
        onResult = { uri ->
            if (uri != null) {
                val contactInfo = getContactNameAndNumber(context, uri)
                if (contactInfo != null) {
                    val (name, number) = contactInfo
                    val textToAppend = String.format(strContactInsertTemplate, name, number)
                    val newText =
                        if (currentText.text.isEmpty()) textToAppend else currentText.text + "\n" + textToAppend
                    currentOnTextChanged(
                        currentText.copy(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(newText.length)
                        )
                    )
                }
            }
        }
    )

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoadingCameraImage by remember { mutableStateOf(false) }

    fun createCameraImageUri(): Uri {
        val dir = java.io.File(context.cacheDir, "camera_images").apply { if (!exists()) mkdirs() }
        val file = java.io.File(dir, "camera_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val uri = cameraImageUri
            if (success && uri != null) {
                isLoadingCameraImage = true
                coroutineScope.launch {
                    val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        loadDownsampledBitmap(context, uri)
                    }
                    isLoadingCameraImage = false
                    if (bitmap != null) {
                        attachedBitmap = bitmap
                        isMenuExpanded = false
                    } else {
                        Toast.makeText(context, strUnableLoadCapturedImage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    fun launchCamera() {
        val uri = createCameraImageUri()
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    var isLoadingGalleryImage by remember { mutableStateOf(false) }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                isLoadingGalleryImage = true
                coroutineScope.launch {
                    val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        loadDownsampledBitmap(context, uri)
                    }
                    isLoadingGalleryImage = false
                    if (bitmap != null) {
                        attachedBitmap = bitmap
                        isMenuExpanded = false
                    } else {
                        Toast.makeText(context, strUnableLoadSelectedImage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                showCameraPermissionDialog = true
            }
        }
    )

    val fusedLocationClient = remember {
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
    }

    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationSettingsReturnTrigger by remember { mutableStateOf(0) }

    val locationSettingsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
        onResult = { locationSettingsReturnTrigger++ }
    )

    fun isDeviceLocationEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    fun shareCurrentLocation() {
        if (!isNetworkAvailable(context)) {
            Toast.makeText(context, strNoInternet, Toast.LENGTH_SHORT).show()
            return
        }

        if (!isDeviceLocationEnabled()) {
            Toast.makeText(context, strTurnOnLocation, Toast.LENGTH_LONG)
                .show()
            try {
                locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        isFetchingLocation = true
        fetchCurrentLocationLink(context, fusedLocationClient) { mapsLink ->
            isFetchingLocation = false
            if (mapsLink != null) {
                val newText =
                    if (currentText.text.isEmpty()) mapsLink else currentText.text + "\n" + mapsLink
                currentOnTextChanged(
                    currentText.copy(
                        text = newText,
                        selection = androidx.compose.ui.text.TextRange(newText.length)
                    )
                )
                // Only close the attachment menu once the location has actually landed in the composer.
                isMenuExpanded = false
            } else {
                Toast.makeText(
                    context,
                    strUnableGetLocation,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Only auto-fetch if the user actually turned Location on before coming back;
    // if they backed out without enabling it, do nothing (no forced loop).
    LaunchedEffect(locationSettingsReturnTrigger) {
        if (locationSettingsReturnTrigger > 0 && isDeviceLocationEnabled()) {
            shareCurrentLocation()
        }
    }

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                shareCurrentLocation()
            } else {
                showLocationPermissionDialog = true
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.bg_primary))
            .padding(vertical = 12.dp)
    ) {
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.light_gray))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttachmentMenuItem(
                    iconRes = R.drawable.chat_ic_contacts,
                    text = strMenuContacts,
                    onClick = {
                        contactPickerLauncher.launch(null)
                    })
                AttachmentMenuItem(iconRes = R.drawable.chat_ic_camera, text = strMenuCamera, isLoading = isLoadingCameraImage, onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        launchCamera()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                })
                AttachmentMenuItem(
                    iconRes = R.drawable.chat_ic_gallery,
                    text = strMenuGallery,
                    isLoading = isLoadingGalleryImage,
                    onClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
                AttachmentMenuItem(iconRes = R.drawable.chat_ic_schedule, text = strMenuSchedule, onClick = {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        alarmManager.canScheduleExactAlarms()
                    } else {
                        true
                    }
                    if (canSchedule) {
                        showDateTimePicker = true
                        onScheduleIntentShown()
                    } else {
                        showAlarmPermissionDialog = true
                    }
                })
                AttachmentMenuItem(
                    iconRes = R.drawable.chat_ic_location,
                    text = strMenuLocation,
                    isLoading = isFetchingLocation,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            shareCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                )
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

        if (showDateTimePicker) {
            DateTimePickerDialog(
                onDateTimeSelected = { timeInMillis ->
                    showDateTimePicker = false
                    scheduledTime = timeInMillis
                    isMenuExpanded = false
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
                onDismissRequest = { showDateTimePicker = false }
            )
        }

        if (isMenuExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        colorResource(R.color.light_gray),
                        RoundedCornerShape(24.dp)
                    )
                    .animateContentSize()
                    .padding(vertical = 15.dp)
            ) {
                AnimatedVisibility(visible = attachedBitmap != null) {
                    Box(modifier = Modifier.padding(bottom = 8.dp, start = 20.dp, end = 20.dp)) {
                        attachedBitmap?.let { bmp ->
                            Box {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = strContentDescAttachedImage,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .size(20.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                        .clickable { attachedBitmap = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.chat_ic_close),
                                        contentDescription = strContentDescRemove,
                                        modifier = Modifier.size(10.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = scheduledTime != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = strScheduledForText,
                                    fontSize = 12.sp,
                                    fontFamily = Inter,
                                    color = colorResource(R.color.text_des)
                                )
                                Text(
                                    text = scheduledTime?.let {
                                        val cal = java.util.Calendar.getInstance()
                                        val todayYear = cal.get(java.util.Calendar.YEAR)
                                        val todayDay = cal.get(java.util.Calendar.DAY_OF_YEAR)

                                        cal.timeInMillis = it
                                        val scheduledYear = cal.get(java.util.Calendar.YEAR)
                                        val scheduledDay = cal.get(java.util.Calendar.DAY_OF_YEAR)

                                        val pattern = if (todayYear == scheduledYear && todayDay == scheduledDay) {
                                            "h:mm a"
                                        } else {
                                            "d MMM, h:mm a"
                                        }
                                        val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                                        formatter.format(java.util.Date(it)).replace(".", "").uppercase()
                                    } ?: "",
                                    fontSize = 14.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorResource(R.color.primary)
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.chat_ic_close),
                                contentDescription = strContentDescCancelSchedule,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { scheduledTime = null },
                                tint = colorResource(R.color.text_title)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            color = Color(0xFF464555),
                            thickness = 0.5.dp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colorResource(R.color.text_des))
                            .clickable { isMenuExpanded = !isMenuExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        val addIconRes =
                            if (isMenuExpanded) R.drawable.chat_ic_close else R.drawable.chat_ic_add
                        Icon(
                            painter = painterResource(id = addIconRes),
                            contentDescription = strContentDescToggleMenu,
                            modifier = Modifier.size(14.5.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.text.isEmpty()) {
                            Text(
                                text = strTypeMessagePlaceholder,
                                color = Color(0xFF82828E),
                                fontSize = 16.sp,
                                fontFamily = Inter
                            )
                        }
                        val customTextSelectionColors = TextSelectionColors(
                            handleColor = colorResource(R.color.primary),
                            backgroundColor = colorResource(R.color.primary).copy(alpha = 0.4f)
                        )
                        CompositionLocalProvider(
                            LocalTextSelectionColors provides customTextSelectionColors
                        ) {
                            BasicTextField(
                                value = text,
                                onValueChange = onTextChanged,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontFamily = Inter,
                                    color = colorResource(R.color.text_title)
                                ),
                                cursorBrush = SolidColor(colorResource(R.color.primary)),
                                maxLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { state ->
                                        if (state.isFocused) {
                                            onEmojiPickerExpandedChange(false)
                                        }
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.chat_ic_emoji),
                        contentDescription = strContentDescEmoji,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                val newValue = !isEmojiPickerExpanded
                                onEmojiPickerExpandedChange(newValue)
                                if (newValue) {
                                    keyboardController?.hide()
                                } else {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            },
                        tint = if (isEmojiPickerExpanded) colorResource(R.color.primary) else colorResource(
                            R.color.chat_icon_secondary
                        )
                    )

                }
            } // End of Column

            Spacer(modifier = Modifier.width(12.dp))

            val isSendEnabled = text.text.isNotBlank() || attachedBitmap != null
            val sendBtnColor =
                if (isSendEnabled) colorResource(R.color.primary) else colorResource(R.color.primary).copy(
                    alpha = 0.3f
                )

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(sendBtnColor, CircleShape)
                    .clickable(enabled = isSendEnabled) {
                        if (scheduledTime != null) {
                            if (scheduledTime!! > System.currentTimeMillis()) {
                                onSchedule(scheduledTime!!)
                                scheduledTime = null
                                attachedBitmap = null
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    strScheduledTimeFuture,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            onSend(attachedBitmap)
                            attachedBitmap = null
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_ic_send),
                    contentDescription = strContentDescSend,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(visible = isEmojiPickerExpanded) {
            AndroidView(
                factory = { context ->
                    val themedContext =
                        android.view.ContextThemeWrapper(context, R.style.Theme_EmojiAppCompat)
                    EmojiView(themedContext).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setUp(
                            rootView = this,
                            onEmojiClickListener = { emoji ->
                                val newText = currentText.text + emoji.unicode
                                currentOnTextChanged(
                                    currentText.copy(
                                        text = newText,
                                        selection = androidx.compose.ui.text.TextRange(newText.length)
                                    )
                                )
                            },
                            onEmojiBackspaceClickListener = {
                                if (currentText.text.isNotEmpty()) {
                                    val newText = currentText.text.dropLast(1)
                                    currentOnTextChanged(
                                        currentText.copy(
                                            text = newText,
                                            selection = androidx.compose.ui.text.TextRange(newText.length)
                                        )
                                    )
                                }
                            },
                            editText = null,
                            theming = EmojiTheming(
                                backgroundColor = ContextCompat.getColor(context, R.color.bg_primary),
                                primaryColor = ContextCompat.getColor(
                                    context,
                                    R.color.light_color_gray
                                ),
                                secondaryColor = ContextCompat.getColor(context, R.color.primary),
                                textColor = ContextCompat.getColor(context, R.color.text_title),
                                textSecondaryColor = ContextCompat.getColor(
                                    context,
                                    R.color.text_des
                                ),
                                dividerColor = ContextCompat.getColor(context, R.color.light_gray)
                            ),
                            searchEmoji = com.vanniktech.emoji.search.NoSearchEmoji
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(300.dp)
            )
        }

        if (showCameraPermissionDialog) {
            ConfirmationDialog(
                title = strPermissionRequiredTitle,
                text = strCameraPermissionText,
                confirmText = strActionOk,
                onConfirm = {
                    showCameraPermissionDialog = false
                    val intent =
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .apply {
                                data =
                                    android.net.Uri.fromParts("package", context.packageName, null)
                            }
                    context.startActivity(intent)
                },
                onDismiss = { showCameraPermissionDialog = false }
            )
        }

        if (showLocationPermissionDialog) {
            ConfirmationDialog(
                title = strPermissionRequiredTitle,
                text = strLocationPermissionText,
                confirmText = strActionOk,
                onConfirm = {
                    showLocationPermissionDialog = false
                    val intent =
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .apply {
                                data =
                                    android.net.Uri.fromParts("package", context.packageName, null)
                            }
                    context.startActivity(intent)
                },
                onDismiss = { showLocationPermissionDialog = false }
            )
        }
    }
}

fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun formatChatDateSeparator(messageTimeMillis: Long, todayLabel: String): String {
    return if (isSameDay(messageTimeMillis, System.currentTimeMillis())) {
        todayLabel
    } else {
        SimpleDateFormat("EEEE dd MMM, hh:mm a", Locale.getDefault()).format(Date(messageTimeMillis))
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private const val MESSAGE_URL_ANNOTATION_TAG = "URL"
private const val MESSAGE_PHONE_ANNOTATION_TAG = "PHONE"
private const val MIN_PHONE_DIGITS = 7

fun buildMessageAnnotatedString(
    text: String,
    linkColor: Color,
    highlightStart: Int = -1,
    highlightEnd: Int = -1,
    highlightBgColor: Color = Color.Transparent,
    highlightTextColor: Color = Color.Unspecified
): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)

        val occupiedRanges = mutableListOf<IntRange>()

        val urlMatcher = android.util.Patterns.WEB_URL.matcher(text)
        while (urlMatcher.find()) {
            val start = urlMatcher.start()
            val end = urlMatcher.end()
            occupiedRanges.add(start until end)
            addStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    color = linkColor,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                start = start,
                end = end
            )
            addStringAnnotation(
                tag = MESSAGE_URL_ANNOTATION_TAG,
                annotation = text.substring(start, end),
                start = start,
                end = end
            )
        }

        val phoneMatcher = android.util.Patterns.PHONE.matcher(text)
        while (phoneMatcher.find()) {
            val start = phoneMatcher.start()
            val end = phoneMatcher.end()
            val digitCount = text.substring(start, end).count { it.isDigit() }
            val overlapsUrl = occupiedRanges.any { it.first < end && start < it.last + 1 }
            if (digitCount < MIN_PHONE_DIGITS || overlapsUrl) continue

            addStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    color = linkColor,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                start = start,
                end = end
            )
            addStringAnnotation(
                tag = MESSAGE_PHONE_ANNOTATION_TAG,
                annotation = text.substring(start, end),
                start = start,
                end = end
            )
        }

        if (highlightStart in text.indices && highlightEnd in (highlightStart + 1)..text.length) {
            addStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    background = highlightBgColor,
                    color = highlightTextColor,
                    fontWeight = FontWeight.Bold
                ),
                start = highlightStart,
                end = highlightEnd
            )
        }
    }
}

fun openMessageUrl(context: Context, url: String) {
    val normalizedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
        url
    } else {
        "https://$url"
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, context.getString(R.string.toast_no_app_for_link), Toast.LENGTH_SHORT).show()
    }
}

fun openPhoneNumber(context: Context, phoneNumber: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, context.getString(R.string.toast_no_app_for_dialer), Toast.LENGTH_SHORT).show()
    }
}

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sampleSize = 1
    var w = width
    var h = height
    while (w / 2 >= maxDimension || h / 2 >= maxDimension) {
        w /= 2
        h /= 2
        sampleSize *= 2
    }
    return sampleSize
}

fun loadDownsampledBitmap(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val sampleSize = calculateSampleSize(info.size.width, info.size.height, maxDimension)
                if (sampleSize > 1) {
                    decoder.setTargetSampleSize(sampleSize)
                }
                decoder.isMutableRequired = false
            }
        } else {
            val boundsOptions = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input, null, boundsOptions)
            }
            val sampleSize = calculateSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, maxDimension)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
                android.graphics.BitmapFactory.decodeStream(input, null, options)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@SuppressLint("MissingPermission")
fun fetchCurrentLocationLink(
    context: Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onResult: (String?) -> Unit
) {
    fun fallbackToLastLocation(reason: Exception?) {
        reason?.printStackTrace()
        fusedLocationClient.lastLocation
            .addOnSuccessListener { lastLocation ->
                if (lastLocation != null) {
                    onResult("https://maps.google.com/?q=${lastLocation.latitude},${lastLocation.longitude}")
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onResult(null)
            }
    }

    val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
    fusedLocationClient.getCurrentLocation(
        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
        cancellationTokenSource.token
    ).addOnSuccessListener { location ->
        if (location != null) {
            onResult("https://maps.google.com/?q=${location.latitude},${location.longitude}")
        } else {
            fallbackToLastLocation(null)
        }
    }.addOnFailureListener { e ->
        fallbackToLastLocation(e)
    }
}

fun getContactNameAndNumber(context: Context, uri: Uri): Pair<String, String>? {
    var name = ""
    var number = ""
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex) ?: ""
                }
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                if (idIndex != -1 && hasPhoneIndex != -1) {
                    val contactId = it.getString(idIndex)
                    val hasPhone = it.getString(hasPhoneIndex)

                    if (hasPhone != null && hasPhone.toInt() > 0) {
                        val pCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contactId),
                            null
                        )
                        pCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val phoneIndex =
                                    pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (phoneIndex != -1) {
                                    number = pc.getString(phoneIndex) ?: ""
                                }
                            }
                        }
                    }
                }
            }
        }
        if (name.isNotEmpty() || number.isNotEmpty()) {
            return Pair(name, number)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

@Composable
fun AttachmentMenuItem(iconRes: Int, text: String, isLoading: Boolean = false, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(colorResource(R.color.bg_primary), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = colorResource(R.color.primary)
                )
            } else {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = text,
                    modifier = Modifier.size(26.dp),
                    tint = colorResource(R.color.chat_icon_secondary)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.chat_icon_secondary)
        )
    }
}

