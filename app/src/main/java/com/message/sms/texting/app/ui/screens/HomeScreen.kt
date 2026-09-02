package com.message.sms.texting.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter
import com.message.sms.texting.app.ui.theme.AvatarColors
import com.message.sms.texting.app.ui.theme.SwipeActionsState
import com.message.sms.texting.app.ui.theme.swipeActionColorFor
import com.message.sms.texting.app.ui.theme.swipeActionIconFor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.key
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.message.sms.texting.app.viewmodel.HomeViewModel
import com.message.sms.texting.app.model.SmsMessage
import com.message.sms.texting.app.ui.components.ContextualTopBar
import com.message.sms.texting.app.ui.components.CommonTopBar
import com.message.sms.texting.app.ui.components.MessageSkeletonUi
import com.message.sms.texting.app.ui.components.ScrollToTopButton
import androidx.lifecycle.viewmodel.compose.viewModel
import com.message.sms.texting.app.ui.components.dialogs.ConfirmationDialog
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextAlign
import com.message.sms.texting.app.navigation.Routes
import android.app.Activity
import androidx.activity.ComponentActivity
import com.message.sms.texting.app.ads.AppOpenBackgroundReturnTrigger
import com.message.sms.texting.app.ads.AppOpenCounter
import com.message.sms.texting.app.ads.BannerAdView
import com.message.sms.texting.app.ads.HomeBannerAdState
import com.message.sms.texting.app.ads.NativeAdTemplate
import com.message.sms.texting.app.ads.NativeAdView
import com.message.sms.texting.app.ui.components.dialogs.RateUsDialog
import com.message.sms.texting.app.ui.components.dialogs.UpdateAppDialog
import com.message.sms.texting.app.utils.AnalyticsManager
import com.message.sms.texting.app.utils.AppUpdateHelper
import com.message.sms.texting.app.utils.RateUsHelper
import com.message.sms.texting.app.utils.isRemoteVersionNewer
import com.message.sms.texting.app.viewmodel.AppConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToArchived: () -> Unit = {},
    navController: androidx.navigation.NavController,
    onShowSnackbar: (String, String, () -> Unit) -> Unit = { _, _, _ -> }
) {
    val selectedFilter by viewModel.currentCategory.collectAsState()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val selectedMessages by viewModel.selectedMessages.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroupIds by viewModel.selectedGroupIds.collectAsState()
    val isGroupSelectionMode = selectedGroupIds.isNotEmpty()

    var messageToDelete by remember { mutableStateOf<SmsMessage?>(null) }
    var messageToBlock by remember { mutableStateOf<SmsMessage?>(null) }
    var groupToDelete by remember { mutableStateOf<com.message.sms.texting.app.model.GroupWithLastMessage?>(null) }
    var showGroupBulkDeleteDialog by remember { mutableStateOf(false) }

    val isLoading =
        messages.loadState.refresh is androidx.paging.LoadState.Loading && messages.itemCount == 0

    val context = LocalContext.current

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped),
    // so the remote ad config isn't refetched per screen.
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val adsEnabled = adConfig?.result?.google_ads_on_off == "on"

    // Bottom-docked banner.
    val homeBannerAdUnitId = adConfig?.result?.let { result ->
        if (adsEnabled && result.banner_1_on_off == "on") result.banner_1?.takeIf { it.isNotBlank() } else null
    }

    // Interleaved in the message list: first item, then every 5th item after.
    val homeListNativeAdUnitId = adConfig?.result?.let { result ->
        if (adsEnabled && result.native_1_on_off == "on") result.native_1?.takeIf { it.isNotBlank() } else null
    }

    DisposableEffect(homeBannerAdUnitId) {
        if (homeBannerAdUnitId == null) HomeBannerAdState.clear()
        onDispose { HomeBannerAdState.clear() }
    }

    // Chat is where almost every Home session heads next (unlike e.g. Settings, which has no
    // single predictable "came from" screen) â€” give its banner a head start here rather than
    // waiting for ChatScreen to compose and start loading from scratch.
    LaunchedEffect(adConfig) {
        val result = adConfig?.result ?: return@LaunchedEffect
        if (adsEnabled && result.banner_2_on_off == "on") {
            result.banner_2?.takeIf { it.isNotBlank() }?.let {
                com.message.sms.texting.app.ads.BannerAdCache.preload(context, it)
            }
        }
        if (adsEnabled && result.native_8_on_off == "on") {
            result.native_8?.takeIf { it.isNotBlank() }?.let {
                com.message.sms.texting.app.ads.NativeAdCache.preload(context, it)
            }
        }
    }

    // Auto Rate Us â€” shown once, on the user's very first kill+reopen (AppOpenCounter's count=1,
    // which is already their first *return* to the app â€” the true first-ever launch never
    // touches this counter). Skipped if they've already rated via Settings, or if this prompt
    // has already fired once before.
    var showAutoRateUsDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (AppOpenCounter.currentCount(context) == 1 &&
            !RateUsHelper.hasAutoShown(context) &&
            !RateUsHelper.hasInteracted(context)
        ) {
            RateUsHelper.markAutoShown(context)
            showAutoRateUsDialog = true
        }
    }

    // In-app update â€” extra_data_2_message carries the latest version string from the panel; if
    // it's newer than this build, prompt to update. extra_data_5_on_off decides soft (dismissible)
    // vs hard (mandatory, no "Later") update; extra_data_2_on_off decides whether to use Play's
    // in-app update API (falling back to Play Store if it's unavailable) or just open Play Store.
    var showUpdateDialog by remember { mutableStateOf(false) }
    val appUpdateHelper = remember { AppUpdateHelper(context) }
    LaunchedEffect(adConfig) {
        val remoteVersion = adConfig?.result?.extra_data_2_message
        if (!remoteVersion.isNullOrBlank() && isRemoteVersionNewer(remoteVersion, com.message.sms.texting.app.BuildConfig.VERSION_NAME)) {
            showUpdateDialog = true
        }
    }

    // Maintenance (AppNavigation.kt) is a separate overlay Dialog() â€” without this guard, both
    // could show stacked at once if the panel ever has both flags on simultaneously.
    if (showUpdateDialog && adConfig?.result?.extra_data_1_on_off != "on") {
        val isSoftUpdate = adConfig?.result?.extra_data_5_on_off == "on"
        LaunchedEffect(Unit) {
            AnalyticsManager.logEventWithAction(
                eventName = "app_update_dialog",
                screenName = "HomeScreen",
                action = "Shown",
                extraParams = mapOf("type" to if (isSoftUpdate) "soft" else "hard")
            )
        }
        UpdateAppDialog(
            title = stringResource(R.string.update_title),
            description = stringResource(R.string.update_desc),
            onOkClick = {
                AnalyticsManager.logEventWithAction(
                    eventName = "app_update_dialog",
                    screenName = "HomeScreen",
                    action = "Update Accepted"
                )
                val openPlayStore = {
                    val playStoreLink = adConfig?.result?.app_link
                        ?.takeIf { it.isNotBlank() }
                        ?: "https://play.google.com/store/apps/details?id=${context.packageName}"
                    val uri = runCatching { android.net.Uri.parse(playStoreLink) }.getOrNull()
                    if (uri != null) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        try {
                            // Opening Play Store backgrounds/re-foregrounds this Activity â€” without
                            // this, that return would look like a normal app-switch-back and could
                            // trigger an App Open ad right as the user is trying to update.
                            AppOpenBackgroundReturnTrigger.isAdPaused = true
                            context.startActivity(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            // No Play Store app or browser available â€” nothing more we can do.
                        }
                    }
                }

                if (adConfig?.result?.extra_data_2_on_off == "on") {
                    appUpdateHelper.checkForUpdate(object : AppUpdateHelper.UpdateStatusListener {
                        override fun onUpdateAvailable(appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo) {
                            val activity = context as? Activity
                            if (activity != null) {
                                appUpdateHelper.startUpdate(
                                    activity,
                                    appUpdateInfo,
                                    com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE,
                                    999,
                                    onFailure = openPlayStore
                                )
                            } else {
                                openPlayStore()
                            }
                        }

                        override fun onUpdateNotAvailable() {
                            openPlayStore()
                        }

                        override fun onUpdateFailed(e: Exception) {
                            openPlayStore()
                        }

                        override fun onFlexibleUpdateDownloaded() {}
                    })
                } else {
                    openPlayStore()
                }
            },
            onCancelClick = if (isSoftUpdate) {
                {
                    AnalyticsManager.logEventWithAction(
                        eventName = "app_update_dialog",
                        screenName = "HomeScreen",
                        action = "Soft Update Cancelled/Dismissed"
                    )
                    showUpdateDialog = false
                }
            } else null
        )
    }

    val strUndo = stringResource(R.string.snackbar_undo)
    val strMessagesArchivedTemplate = stringResource(R.string.snackbar_messages_archived)
    val strGroupArchived = stringResource(R.string.snackbar_group_archived)
    val strMessageArchived = stringResource(R.string.snackbar_message_archived)
    val str1GroupArchivedBulk = stringResource(R.string.snackbar_1_group_archived_bulk)
    val strNGroupsArchivedBulkTemplate = stringResource(R.string.snackbar_n_groups_archived_bulk)
    val strBlockReportSpamTitle = stringResource(R.string.block_report_spam_title)
    val strBlockReportSpamText = stringResource(R.string.block_report_spam_text)
    val strActionBlock = stringResource(R.string.action_block)
    val strActionDelete = stringResource(R.string.action_delete)
    val strDeleteMessageTitle = stringResource(R.string.delete_message_title)
    val strDeleteConversationText = stringResource(R.string.delete_conversation_text)
    val str1MessageDeleted = stringResource(R.string.toast_1_message_deleted)
    val strNMessagesDeletedTemplate = stringResource(R.string.toast_n_messages_deleted)
    val str1ContactBlocked = stringResource(R.string.toast_1_contact_blocked)
    val strNContactsBlockedTemplate = stringResource(R.string.toast_n_contacts_blocked)
    val strMessageDeletedToast = stringResource(R.string.toast_message_deleted)
    val strDeleteGroupTitle = stringResource(R.string.delete_group_title)
    val strDeleteGroupText = stringResource(R.string.delete_group_text)
    val strGroupDeletedToast = stringResource(R.string.toast_group_deleted)
    val strDeleteNGroupsTemplate = stringResource(R.string.delete_n_groups_title)
    val strDeleteGroupsText = stringResource(R.string.delete_groups_text)
    val strGroupsDeletedToast = stringResource(R.string.toast_groups_deleted)
    val strMenuArchived = stringResource(R.string.home_menu_archived)
    val strMenuScheduled = stringResource(R.string.home_menu_scheduled)
    val strMenuBackupRestore = stringResource(R.string.home_menu_backup_restore)
    val strMenuStarredMessage = stringResource(R.string.home_menu_starred_message)
    val strMenuMarkAllRead = stringResource(R.string.home_menu_mark_all_read)
    val strMenuSelect = stringResource(R.string.home_menu_select)
    val strMenuSelectAll = stringResource(R.string.home_menu_select_all)
    val strMenuUnselectAll = stringResource(R.string.home_menu_unselect_all)
    val strMenuBlockList = stringResource(R.string.home_menu_block_list)
    val strMenuSettings = stringResource(R.string.home_menu_settings)
    val strSyncing = stringResource(R.string.home_syncing)
    val strNoMessagesFound = stringResource(R.string.home_no_messages_found)
    val strNoMessagesFoundFiltered = stringResource(R.string.home_no_messages_found_filtered)
    val strDraftPrefix = stringResource(R.string.draft_prefix)
    val strPhotoPlaceholder = stringResource(R.string.mms_photo_placeholder)
    val strPinned = stringResource(R.string.content_desc_pinned)
    val strFallbackGroupName = stringResource(R.string.fallback_group_name)
    val strNoMessagesYet = stringResource(R.string.no_messages_yet)
    val strContentDescScrollToTop = stringResource(R.string.content_desc_scroll_to_top)
    val homeCoroutineScope = rememberCoroutineScope()

    val listStates = listOf(
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState(),
        rememberLazyListState()
    )
    val unreadCounts by viewModel.unreadCounts.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val currentListState = listStates[selectedFilter]

    val currentFilter by viewModel.currentFilter.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var lastFilter by remember { mutableStateOf(currentFilter) }
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    // Returns true if the row should play its swipe-dismiss animation (the item is
    // leaving the current filtered view), false if it should snap back in place.
    fun handleMessageSwipeAction(action: String, msg: SmsMessage): Boolean {
        return when (action) {
            "archive" -> {
                viewModel.archiveMessage(msg.id, msg.threadId)
                onShowSnackbar(strMessageArchived, strUndo) { viewModel.unarchiveMessage(msg.id, msg.threadId) }
                true
            }
            "delete" -> {
                messageToDelete = msg
                false
            }
            "block" -> {
                messageToBlock = msg
                false
            }
            "call" -> {
                if (msg.address.matches(PhoneRegex)) {
                    initiateCall(context, msg.address)
                }
                false
            }
            "mark_read" -> {
                viewModel.markMessageRead(msg.id)
                false
            }
            "mark_unread" -> {
                viewModel.markMessageUnread(msg.id)
                false
            }
            else -> false
        }
    }

    fun handleGroupSwipeAction(action: String, group: com.message.sms.texting.app.model.GroupWithLastMessage): Boolean {
        return when (action) {
            "archive" -> {
                viewModel.archiveGroup(group.group.id)
                onShowSnackbar(strGroupArchived, strUndo) { viewModel.unarchiveGroup(group.group.id) }
                true
            }
            "delete" -> {
                groupToDelete = group
                false
            }
            // Block/call/mark read-unread aren't meaningful for a group broadcast.
            else -> false
        }
    }

    LaunchedEffect(currentFilter, messages.loadState.refresh) {
        if (lastFilter != currentFilter && messages.loadState.refresh is androidx.paging.LoadState.NotLoading) {
            currentListState.scrollToItem(0)
            lastFilter = currentFilter
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colorResource(R.color.bg_primary),
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            topBar = {
                if (isGroupSelectionMode) {
                    val isAllGroupsPinned = groups
                        .filter { selectedGroupIds.contains(it.group.id) }
                        .let { selected -> selected.isNotEmpty() && selected.all { it.group.isPinned } }

                    com.message.sms.texting.app.ui.components.GroupSelectionTopBar(
                        selectedCount = selectedGroupIds.size,
                        isAllPinned = isAllGroupsPinned,
                        onCloseClick = { viewModel.clearGroupSelection() },
                        onPinClick = { viewModel.togglePinSelectedGroups(!isAllGroupsPinned) },
                        onArchiveClick = {
                            val idsToArchive = selectedGroupIds.toList()
                            viewModel.archiveSelectedGroups()
                            onShowSnackbar(
                                if (idsToArchive.size == 1) str1GroupArchivedBulk else String.format(strNGroupsArchivedBulkTemplate, idsToArchive.size),
                                strUndo
                            ) {
                                idsToArchive.forEach { viewModel.unarchiveGroup(it) }
                            }
                        },
                        onDeleteClick = { showGroupBulkDeleteDialog = true }
                    )
                } else if (selectedMessages.isNotEmpty() || isSelectionMode) {
                    val isAllPinned =
                        selectedMessages.isNotEmpty() && selectedMessages.all { it.isPinned }
                    val hasUnread = selectedMessages.any { !it.read }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var showBlockDialog by remember { mutableStateOf(false) }

                    ContextualTopBar(
                        selectedCount = selectedMessages.size,
                        isAllPinned = isAllPinned,
                        hasUnread = hasUnread,
                        onCloseClick = { viewModel.clearSelection() },
                        onPinClick = { viewModel.togglePinSelected(!isAllPinned) },
                        onDeleteClick = { showDeleteDialog = true },
                        onArchiveClick = {
                            val count = selectedMessages.size
                            val archivedThreadIds = selectedMessages.map { it.threadId }.toList()
                            viewModel.archiveSelectedMessages()
                            onShowSnackbar(String.format(strMessagesArchivedTemplate, count), strUndo) {
                                viewModel.unarchiveThreads(archivedThreadIds)
                            }
                        },
                        onMarkUnreadClick = { viewModel.markSelectedAsUnread() },
                        onMarkReadClick = { viewModel.markSelectedAsRead() },
                        onBlockListClick = { showBlockDialog = true },
                        onAddContactClick = {
                            val address = selectedMessages.firstOrNull()?.address
                            if (address != null) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                    type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, address)
                                }
                                try {
                                    context.startActivity(intent)
                                    viewModel.clearSelection()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )

                    if (showBlockDialog) {
                        ConfirmationDialog(
                            title = strBlockReportSpamTitle,
                            text = strBlockReportSpamText,
                            confirmText = strActionBlock,
                            onConfirm = {
                                showBlockDialog = false
                                val count = selectedMessages.size
                                val selectedMsgs = selectedMessages.toList()
                                viewModel.blockSelectedMessages()
                                onShowSnackbar(if (count == 1) str1ContactBlocked else String.format(strNContactsBlockedTemplate, count), strUndo) {
                                    viewModel.unblockContacts(selectedMsgs)
                                }
                            },
                            onDismiss = { showBlockDialog = false }
                        )
                    }

                    if (showDeleteDialog) {
                        ConfirmationDialog(
                            title = strDeleteMessageTitle,
                            text = strDeleteConversationText,
                            confirmText = strActionDelete,
                            onConfirm = {
                                val count = selectedMessages.size
                                showDeleteDialog = false
                                viewModel.deleteSelectedMessages()
                                Toast.makeText(
                                    context,
                                    if (count == 1) str1MessageDeleted else String.format(strNMessagesDeletedTemplate, count),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onDismiss = { showDeleteDialog = false }
                        )
                    }
                } else {
                    var moreExpanded by remember { mutableStateOf(false) }

                    CommonTopBar(
                        title = "Messages",
                        onSearchClick = { navController.navigate(Routes.Search.route) },
                        onFilterClick = { showFilterDialog = true },
                        isFilterActive = currentFilter.type != com.message.sms.texting.app.ui.components.FilterType.DEFAULT,
                        onMoreClick = { moreExpanded = true },
                        moreDropdownContent = {
                            DropdownMenu(
                                expanded = moreExpanded,
                                onDismissRequest = { moreExpanded = false },
                                offset = DpOffset(x = (-14).dp, y = 5.dp),
                                modifier = Modifier.width(230.dp),
                                shape = RoundedCornerShape(15.dp),
                                containerColor = colorResource(R.color.menu_bg)
                            ) {
                                val moreMenuItems = listOf(
                                    Pair(strMenuArchived, R.drawable.home_ic_more_archived),
                                    Pair(strMenuScheduled, R.drawable.home_ic_more_scheduled),
                                    Pair(strMenuBackupRestore, R.drawable.home_ic_more_backup_restore),
                                    Pair(strMenuStarredMessage, R.drawable.home_ic_more_starred_message),
                                    Pair(strMenuMarkAllRead, R.drawable.home_ic_more_mark_all_as_read),
                                    Pair(strMenuSelect, R.drawable.home_ic_more_select),
                                    Pair(if (messages.itemCount > 0 && selectedMessages.size >= messages.itemCount) strMenuUnselectAll else strMenuSelectAll, R.drawable.home_ic_more_select_all),
                                    Pair(strMenuBlockList, R.drawable.home_ic_more_block_list),
                                    Pair(strMenuSettings, R.drawable.home_ic_more_settings)
                                )

                                moreMenuItems.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    item.first,
                                                    fontFamily = Inter,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 16.sp,
                                                    color = colorResource(id = R.color.chat_icon_secondary)
                                                )
                                                if (item.second == R.drawable.home_ic_more_settings) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(colorResource(id = R.color.primary))
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(id = item.second),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = colorResource(id = R.color.chat_icon_secondary)
                                            )
                                        },
                                        onClick = {
                                            moreExpanded = false
                                            if (item.second == R.drawable.home_ic_more_archived) {
                                                onNavigateToArchived()
                                            } else if (item.second == R.drawable.home_ic_more_starred_message) {
                                                navController.navigate(com.message.sms.texting.app.navigation.Routes.StarredMessages.route)
                                            } else if (item.second == R.drawable.home_ic_more_scheduled) {
                                                navController.navigate(com.message.sms.texting.app.navigation.Routes.ScheduledMessages.route)
                                            } else if (item.second == R.drawable.home_ic_more_mark_all_as_read) {
                                                viewModel.markAllAsRead()
                                            } else if (item.second == R.drawable.home_ic_more_settings) {
                                                navController.navigate(com.message.sms.texting.app.navigation.Routes.Settings.route)
                                            } else if (item.second == R.drawable.home_ic_more_block_list) {
                                                navController.navigate(com.message.sms.texting.app.navigation.Routes.BlockedMessages.route)
                                            } else if (item.second == R.drawable.home_ic_more_backup_restore) {
                                                navController.navigate(com.message.sms.texting.app.navigation.Routes.BackupRestore.route)
                                            } else if (item.second == R.drawable.home_ic_more_select) {
                                                if (messages.itemCount > 0) {
                                                    messages[0]?.let { firstMsg ->
                                                        viewModel.enableSelectionMode()
                                                        if (!selectedMessages.any { it.id == firstMsg.id }) {
                                                            viewModel.toggleSelection(firstMsg)
                                                        }
                                                    }
                                                } else {
                                                    viewModel.enableSelectionMode()
                                                }
                                            } else if (item.second == R.drawable.home_ic_more_select_all) {
                                                viewModel.toggleSelectAllMessages(messages.itemCount)
                                            }
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 20.dp,
                                            vertical = 8.dp
                                        )
                                    )
                                }
                            }
                        }
                    )
                    
                    if (showFilterDialog) {
                        com.message.sms.texting.app.ui.components.FilterDialog(
                            currentFilter = currentFilter,
                            onApply = { filter ->
                                showFilterDialog = false
                                viewModel.applyFilter(filter)
                            },
                            onDismiss = { showFilterDialog = false }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) {
                val filters = listOf(
                    Pair(stringResource(R.string.filter_inbox), R.drawable.home_ic_inbox),
                    Pair(stringResource(R.string.filter_known), R.drawable.home_ic_known),
                    Pair(stringResource(R.string.filter_unknown), R.drawable.home_ic_unknown),
                    Pair(stringResource(R.string.filter_transactions), R.drawable.home_ic_transations),
                    Pair(stringResource(R.string.filter_otps), R.drawable.home_ic_otps),
                    Pair(stringResource(R.string.filter_offers), R.drawable.home_ic_offers)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters.size) { index ->
                        val isSelected = selectedFilter == index
                        Surface(
                            onClick = {
                                viewModel.setCategory(index)
                            },
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSelected) colorResource(R.color.primary) else colorResource(
                                R.color.light_gray
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = filters[index].second),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else colorResource(R.color.text_des),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val count = unreadCounts[index] ?: 0
                                val textToShow = if (count > 0) "${filters[index].first}($count)" else filters[index].first

                                Text(
                                    text = textToShow,
                                    color = if (isSelected) Color.White else colorResource(R.color.text_des),
                                    fontSize = 13.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSyncing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .background(colorResource(R.color.light_gray), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colorResource(R.color.primary),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strSyncing,
                            fontSize = 12.sp,
                            color = colorResource(R.color.text_des),
                            fontFamily = Inter
                        )
                    }
                }

                val showGroupsSection = selectedFilter == 0 && groups.isNotEmpty()

                Box(modifier = Modifier.weight(1f)) {
                if (isLoading && !isSyncing) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(5) {
                            MessageSkeletonUi()
                        }
                    }
                } else if (!isLoading && messages.itemCount == 0 && !showGroupsSection) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (currentFilter.type == com.message.sms.texting.app.ui.components.FilterType.DEFAULT)
                                strNoMessagesFound
                            else
                                strNoMessagesFoundFiltered,
                            color = colorResource(R.color.text_des),
                            fontSize = 16.sp,
                            fontFamily = Inter,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    // null = native-ad row, Int = real message index. An ad row is inserted
                    // before message index 0 and before every 5th message after that.
                    val homeListRows = remember(messages.itemCount, homeListNativeAdUnitId) {
                        buildList {
                            for (i in 0 until messages.itemCount) {
                                if (homeListNativeAdUnitId != null && i % 5 == 0) add(null)
                                add(i)
                            }
                        }
                    }
                    key(selectedFilter) {
                        LazyColumn(
                            state = currentListState,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (showGroupsSection) {
                                items(groups, key = { "group_${it.group.id}" }) { groupWithLastMessage ->
                                    val isMessageSelectionActive = selectedMessages.isNotEmpty() || isSelectionMode
                                    GroupListItem(
                                        modifier = Modifier.animateItem(),
                                        groupWithLastMessage = groupWithLastMessage,
                                        isSelected = selectedGroupIds.contains(groupWithLastMessage.group.id),
                                        enabled = !isMessageSelectionActive,
                                        onClick = {
                                            if (isGroupSelectionMode) {
                                                viewModel.toggleGroupSelection(groupWithLastMessage.group.id)
                                            } else {
                                                navController.navigate(
                                                    Routes.Chat.createRoute(
                                                        threadId = 0L,
                                                        address = "",
                                                        contactName = null,
                                                        groupId = groupWithLastMessage.group.id
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleGroupSelection(groupWithLastMessage.group.id)
                                        },
                                        onRightSwipeAction = {
                                            handleGroupSwipeAction(SwipeActionsState.rightAction.value, groupWithLastMessage)
                                        },
                                        onLeftSwipeAction = {
                                            handleGroupSwipeAction(SwipeActionsState.leftAction.value, groupWithLastMessage)
                                        }
                                    )
                                }
                            }
                            items(
                                count = homeListRows.size,
                                key = { rowIndex ->
                                    val msgIndex = homeListRows.getOrNull(rowIndex)
                                    if (msgIndex == null || msgIndex >= messages.itemCount) {
                                        "ad_$rowIndex"
                                    } else {
                                        val id = messages.peek(msgIndex)?.id
                                        if (id != null) "${selectedFilter}_$id" else "placeholder_${selectedFilter}_$msgIndex"
                                    }
                                }
                            ) { rowIndex ->
                                val msgIndex = homeListRows.getOrNull(rowIndex)
                                if (msgIndex == null) {
                                    NativeAdView(
                                        adUnitId = homeListNativeAdUnitId!!,
                                        template = NativeAdTemplate.SMALL,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                    return@items
                                }
                                // homeListRows can briefly lag one recomposition behind messages.itemCount
                                // (e.g. right after an archive/delete swipe shrinks the paging list) â€” skip
                                // rendering rather than indexing past the end and crashing.
                                if (msgIndex >= messages.itemCount) return@items
                                val msg = messages[msgIndex]
                                if (msg != null) {
                                    val isSelected = selectedMessages.any { it.id == msg.id }
                                    MessageItemUi(
                                        modifier = Modifier.animateItem(
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        ),
                                        msg = msg,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isGroupSelectionMode) {
                                                // Groups and normal messages use separate selection modes.
                                            } else if (selectedMessages.isNotEmpty() || isSelectionMode) {
                                                viewModel.toggleSelection(msg)
                                            } else {
                                                navController.navigate(
                                                    Routes.Chat.createRoute(
                                                        msg.threadId,
                                                        msg.address,
                                                        msg.contactName
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (!isGroupSelectionMode) {
                                                viewModel.toggleSelection(msg)
                                            }
                                        },
                                        onRightSwipeAction = {
                                            handleMessageSwipeAction(SwipeActionsState.rightAction.value, msg)
                                        },
                                        onLeftSwipeAction = {
                                            handleMessageSwipeAction(SwipeActionsState.leftAction.value, msg)
                                        },
                                        draftText = drafts[msg.address]
                                    )
                                }
                            }
                        }
                    }
                } // Closes else (isLoading)
                } // Closes Box(weight)

                if (homeBannerAdUnitId != null) {
                    BannerAdView(
                        adUnitId = homeBannerAdUnitId,
                        onSizeKnown = { HomeBannerAdState.update(it) }
                    )
                }
            } // Closes Column
        } // Closes Scaffold

        if (messageToDelete != null) {
            ConfirmationDialog(
                title = strDeleteMessageTitle,
                text = strDeleteConversationText,
                confirmText = strActionDelete,
                onConfirm = {
                    viewModel.deleteMessage(messageToDelete!!.id, messageToDelete!!.threadId)
                    Toast.makeText(context, strMessageDeletedToast, Toast.LENGTH_SHORT).show()
                    messageToDelete = null
                },
                onDismiss = { messageToDelete = null }
            )
        }

        if (messageToBlock != null) {
            ConfirmationDialog(
                title = strBlockReportSpamTitle,
                text = strBlockReportSpamText,
                confirmText = strActionBlock,
                onConfirm = {
                    viewModel.blockMessage(messageToBlock!!.address, messageToBlock!!.contactName)
                    messageToBlock = null
                },
                onDismiss = { messageToBlock = null }
            )
        }

        if (groupToDelete != null) {
            ConfirmationDialog(
                title = strDeleteGroupTitle,
                text = strDeleteGroupText,
                confirmText = strActionDelete,
                onConfirm = {
                    viewModel.deleteGroup(groupToDelete!!.group.id)
                    Toast.makeText(context, strGroupDeletedToast, Toast.LENGTH_SHORT).show()
                    groupToDelete = null
                },
                onDismiss = { groupToDelete = null }
            )
        }

        if (showGroupBulkDeleteDialog) {
            val count = selectedGroupIds.size
            ConfirmationDialog(
                title = if (count == 1) strDeleteGroupTitle else String.format(strDeleteNGroupsTemplate, count),
                text = strDeleteGroupsText,
                confirmText = strActionDelete,
                onConfirm = {
                    viewModel.deleteSelectedGroups()
                    Toast.makeText(context, strGroupsDeletedToast, Toast.LENGTH_SHORT).show()
                    showGroupBulkDeleteDialog = false
                },
                onDismiss = { showGroupBulkDeleteDialog = false }
            )
        }

        if (showAutoRateUsDialog) {
            RateUsDialog(
                onRateClick = { stars ->
                    showAutoRateUsDialog = false
                    RateUsHelper.handleRating(context, stars)
                },
                onDismiss = { showAutoRateUsDialog = false }
            )
        }

        val bottomBannerHeight by HomeBannerAdState.heightDp
        // When the native ad row is inserted, it occupies index 0 â€” its skeleton/loaded-state
        // swap can shift the scroll position slightly, so "top" targets index 1 (the first real
        // message) instead of the ad row itself, which keeps landing/hiding reliable.
        val topIndex = if (homeListNativeAdUnitId != null) 1 else 0
        ScrollToTopButton(
            visible = currentListState.firstVisibleItemIndex > topIndex,
            onClick = { homeCoroutineScope.launch { currentListState.animateScrollToItem(topIndex) } },
            contentDescription = strContentDescScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp + bottomBannerHeight)
        )
    }
}

private val PhoneRegex = Regex("^[+]?[0-9\\s\\-()]+$")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItemUi(
    modifier: Modifier = Modifier,
    msg: SmsMessage,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    enableSwipe: Boolean = true,
    onRightSwipeAction: () -> Boolean = { false },
    onLeftSwipeAction: () -> Boolean = { false },
    draftText: String? = null
) {
    val isUnread = !msg.read
    val bgColor = if (isSelected) colorResource(R.color.selection_blue_bg) else colorResource(R.color.bg_primary)
    val avatarColor = remember(msg.address) {
        val hash = msg.address.hashCode() and 0x7FFFFFFF
        AvatarColors[hash % AvatarColors.size]
    }

    val isPhoneNumber = remember(msg.address) {
        msg.address.matches(PhoneRegex)
    }

    val otpCode = remember(msg.category, msg.body) {
        if (msg.category == 4) com.message.sms.texting.app.utils.extractOtpCode(msg.body) else null
    }
    val context = LocalContext.current
    val strOtpCodeCopied = stringResource(R.string.otp_code_copied_toast)
    val strCopy = stringResource(R.string.content_desc_copy)
    val strCopyOtp = stringResource(R.string.otp_copy_label)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> onRightSwipeAction()
                SwipeToDismissBoxValue.EndToStart -> onLeftSwipeAction()
                else -> false
            }
        },
        positionalThreshold = { it * 0.85f }
    )

    @Composable
    fun ContentRow() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

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
                    if (msg.contactName == null && isPhoneNumber) {
                        Icon(
                            painter = painterResource(id = R.drawable.home_ic_filled_person),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    } else {
                        Text(
                            text = msg.initial,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = Inter
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Row(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = msg.contactName ?: msg.address,
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_title),
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (!draftText.isNullOrBlank()) {
                        val draftColor = colorResource(R.color.primary)
                        val descColor = colorResource(R.color.text_des).copy(alpha = 0.8f)
                        val draftPrefix = stringResource(R.string.draft_prefix)
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(androidx.compose.ui.text.SpanStyle(color = draftColor, fontWeight = FontWeight.Medium)) {
                                    append(draftPrefix)
                                }
                                withStyle(androidx.compose.ui.text.SpanStyle(color = descColor)) {
                                    append(draftText)
                                }
                            },
                            fontSize = 14.sp,
                            fontFamily = Inter,
                            lineHeight = 20.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (msg.isMms && !msg.mmsImagePath.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.chat_ic_camera),
                                contentDescription = null,
                                tint = colorResource(R.color.text_des).copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = msg.body.ifBlank { stringResource(R.string.mms_photo_placeholder) },
                                fontSize = 14.sp,
                                color = colorResource(R.color.text_des).copy(alpha = 0.8f),
                                fontFamily = Inter,
                                lineHeight = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
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
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = msg.formattedTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.text_des),
                        fontFamily = Inter
                    )
                    if (isUnread || msg.isPinned) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (msg.isPinned) {
                                Icon(
                                    painter = painterResource(id = R.drawable.home_ic_clip),
                                    contentDescription = stringResource(R.string.content_desc_pinned),
                                    tint = colorResource(R.color.primary),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            if (isUnread) {
                                if (msg.unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                colorResource(R.color.primary),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = msg.unreadCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            fontFamily = Inter,
                                            modifier = Modifier.offset(y = (-1.5).dp)
                                        )
                                    }
                                } else {
                                    // Fallback to blue dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                colorResource(R.color.primary),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (otpCode != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("OTP", otpCode))
                        Toast.makeText(context, strOtpCodeCopied, Toast.LENGTH_SHORT).show()
                    }
                    .padding(start = 88.dp, end = 20.dp, bottom = 10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_ic_long_copy),
                    contentDescription = strCopy,
                    tint = colorResource(R.color.primary),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strCopyOtp,
                    fontSize = 13.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.primary)
                )
            }
        }
        }
    }

    if (enableSwipe) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            enableDismissFromStartToEnd = SwipeActionsState.rightAction.value != "none",
            enableDismissFromEndToStart = SwipeActionsState.leftAction.value != "none",
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val actionKey = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> SwipeActionsState.rightAction.value
                    SwipeToDismissBoxValue.EndToStart -> SwipeActionsState.leftAction.value
                    else -> "none"
                }
                val color by androidx.compose.animation.animateColorAsState(
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> swipeActionColorFor(actionKey)
                        else -> Color.Transparent
                    }
                )
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val icon = swipeActionIconFor(actionKey)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    if (icon != null) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }
            },
            content = { ContentRow() }
        )
    } else {
        Box(modifier = modifier) { ContentRow() }
    }
}

@Composable
fun GroupListItem(
    modifier: Modifier = Modifier,
    groupWithLastMessage: com.message.sms.texting.app.model.GroupWithLastMessage,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    enableSwipe: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRightSwipeAction: () -> Boolean = { false },
    onLeftSwipeAction: () -> Boolean = { false }
) {
    val group = groupWithLastMessage.group
    val bgColor = if (isSelected) colorResource(R.color.selection_blue_bg) else colorResource(R.color.bg_primary)
    val avatarColor = remember(group.id) {
        AvatarColors[(group.id % AvatarColors.size).toInt()]
    }
    val timeText = remember(groupWithLastMessage.lastMessageDate, group.createdAt) {
        formatGroupListTime(groupWithLastMessage.lastMessageDate ?: group.createdAt)
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (!enabled) return@rememberSwipeToDismissBoxState false
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> onRightSwipeAction()
                SwipeToDismissBoxValue.EndToStart -> onLeftSwipeAction()
                else -> false
            }
        },
        positionalThreshold = { it * 0.85f }
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ContentRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!group.avatarUri.isNullOrEmpty()) {
            AsyncImage(
                model = java.io.File(group.avatarUri),
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
                Icon(
                    painter = painterResource(id = R.drawable.contact_ic_group),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name.ifBlank { stringResource(R.string.fallback_group_name) },
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_title),
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = groupWithLastMessage.lastMessageBody ?: stringResource(R.string.no_messages_yet),
                    fontSize = 14.sp,
                    color = colorResource(R.color.text_des).copy(alpha = 0.8f),
                    fontFamily = Inter,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_des),
                    fontFamily = Inter
                )
                if (group.isPinned) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.home_ic_clip),
                        contentDescription = stringResource(R.string.content_desc_pinned),
                        tint = colorResource(R.color.primary),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    }

    if (enableSwipe) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            enableDismissFromStartToEnd = SwipeActionsState.rightAction.value != "none",
            enableDismissFromEndToStart = SwipeActionsState.leftAction.value != "none",
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val actionKey = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> SwipeActionsState.rightAction.value
                    SwipeToDismissBoxValue.EndToStart -> SwipeActionsState.leftAction.value
                    else -> "none"
                }
                val color by androidx.compose.animation.animateColorAsState(
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> swipeActionColorFor(actionKey)
                        else -> Color.Transparent
                    }
                )
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val icon = swipeActionIconFor(actionKey)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    if (icon != null) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }
            },
            content = { ContentRow() }
        )
    } else {
        Box(modifier = modifier) { ContentRow() }
    }
}

fun initiateCall(context: android.content.Context, address: String) {
    val telecomManager = context.getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
    val defaultDialer = telecomManager?.defaultDialerPackage

    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        val intent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
            data = android.net.Uri.parse("tel:$address")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
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
        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$address")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
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

fun formatGroupListTime(timeMillis: Long): String {
    val msgDateTime = java.time.Instant.ofEpochMilli(timeMillis).atZone(java.time.ZoneId.systemDefault())
    val msgDate = msgDateTime.toLocalDate()
    val today = java.time.LocalDate.now()
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(msgDate, today)

    return when {
        daysBetween == 0L -> msgDateTime.toLocalTime()
            .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.getDefault()))
        daysBetween == 1L -> "Yesterday"
        daysBetween in 2L..6L -> msgDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.getDefault()))
        msgDate.year == today.year -> msgDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.getDefault()))
        else -> msgDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy", java.util.Locale.getDefault()))
    }
}