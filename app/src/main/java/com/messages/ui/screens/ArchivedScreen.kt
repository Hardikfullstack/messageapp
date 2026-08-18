package com.messages.ui.screens

import com.messages.navigation.popBackStackWithAd

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.messages.R
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch
import com.messages.ads.NativeAdTemplate
import com.messages.ads.NativeAdView
import com.messages.ads.buildPagingAdRows
import com.messages.ui.components.MessageSkeletonUi
import com.messages.ui.components.ScrollToTopButton
import com.messages.ui.components.SecondaryTopBar
import com.messages.ui.components.dialogs.ConfirmationDialog
import com.messages.ui.theme.Inter
import com.messages.viewmodel.AppConfigViewModel
import com.messages.viewmodel.ArchivedViewModel
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.messages.ui.components.ContextualTopBar
import com.messages.navigation.Routes

class ArchivedViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchivedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArchivedViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: ArchivedViewModel = viewModel(factory = ArchivedViewModelFactory(application))

    val messages = viewModel.messages.collectAsLazyPagingItems()
    val isLoading =
        messages.loadState.refresh is androidx.paging.LoadState.Loading && messages.itemCount == 0
    val selectedMessages by viewModel.selectedMessages.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val archivedGroups by viewModel.archivedGroups.collectAsState()
    val selectedGroupIds by viewModel.selectedGroupIds.collectAsState()
    val isGroupSelectionMode = selectedGroupIds.isNotEmpty()
    var showGroupBulkDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val listNativeAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.native_4_on_off == "on") {
            result.native_4?.takeIf { it.isNotBlank() }
        } else null
    }
    val archivedRows = remember(messages.itemCount, listNativeAdUnitId) {
        buildPagingAdRows(messages.itemCount, listNativeAdUnitId != null)
    }
    val archivedListState = rememberLazyListState()
    val strContentDescScrollToTop = stringResource(R.string.content_desc_scroll_to_top)

    val strSnackbar1GroupUnarchived = stringResource(R.string.snackbar_1_group_unarchived)
    val strSnackbarNGroupsUnarchived = stringResource(R.string.snackbar_n_groups_unarchived)
    val strSnackbarUndo = stringResource(R.string.snackbar_undo)
    val strSnackbarMessagesUnarchived = stringResource(R.string.snackbar_messages_unarchived)
    val strBlockReportSpamTitle = stringResource(R.string.block_report_spam_title)
    val strBlockReportSpamText = stringResource(R.string.block_report_spam_text)
    val strActionBlock = stringResource(R.string.action_block)
    val strToast1ContactBlocked = stringResource(R.string.toast_1_contact_blocked)
    val strToastNContactsBlocked = stringResource(R.string.toast_n_contacts_blocked)
    val strDeleteMessageTitle = stringResource(R.string.delete_message_title)
    val strDeleteConversationText = stringResource(R.string.delete_conversation_text)
    val strActionDelete = stringResource(R.string.action_delete)
    val strToast1MessageDeleted = stringResource(R.string.toast_1_message_deleted)
    val strToastNMessagesDeleted = stringResource(R.string.toast_n_messages_deleted)
    val strArchivedMessagesTitle = stringResource(R.string.archived_messages_title)
    val strArchivedTitle = stringResource(R.string.home_menu_archived)
    val strContentDescNewMessage = stringResource(R.string.content_desc_new_message)
    val strContentDescArchivedEmptyState = stringResource(R.string.content_desc_archived_empty_state)
    val strArchivedEmptyStateText = stringResource(R.string.archived_empty_state_text)
    val strDeleteGroupTitle = stringResource(R.string.delete_group_title)
    val strDeleteNGroupsTitle = stringResource(R.string.delete_n_groups_title)
    val strDeleteGroupsText = stringResource(R.string.delete_groups_text)
    val strToastGroupsDeleted = stringResource(R.string.toast_groups_deleted)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colorResource(R.color.bg_primary),
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            topBar = {
                if (isGroupSelectionMode) {
                    val isAllGroupsPinned = archivedGroups
                        .filter { selectedGroupIds.contains(it.group.id) }
                        .let { selected -> selected.isNotEmpty() && selected.all { it.group.isPinned } }

                    com.messages.ui.components.GroupSelectionTopBar(
                        selectedCount = selectedGroupIds.size,
                        isAllPinned = isAllGroupsPinned,
                        isArchivedScreen = true,
                        onCloseClick = { viewModel.clearGroupSelection() },
                        onPinClick = { viewModel.togglePinSelectedGroups(!isAllGroupsPinned) },
                        onArchiveClick = {
                            val idsToUnarchive = selectedGroupIds.toList()
                            viewModel.unarchiveSelectedGroups()
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = if (idsToUnarchive.size == 1) strSnackbar1GroupUnarchived else String.format(strSnackbarNGroupsUnarchived, idsToUnarchive.size),
                                    actionLabel = strSnackbarUndo,
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    idsToUnarchive.forEach { viewModel.archiveGroup(it) }
                                }
                            }
                        },
                        onDeleteClick = { showGroupBulkDeleteDialog = true }
                    )
                } else if (selectedMessages.isNotEmpty()) {
                    val isAllPinned =
                        selectedMessages.isNotEmpty() && selectedMessages.all { it.isPinned }
                    val hasUnread = selectedMessages.any { !it.read }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var showBlockDialog by remember { mutableStateOf(false) }

                    ContextualTopBar(
                        selectedCount = selectedMessages.size,
                        isAllPinned = isAllPinned,
                        hasUnread = hasUnread,
                        isArchivedScreen = true,
                        onCloseClick = { viewModel.clearSelection() },
                        onPinClick = { viewModel.togglePinSelected(!isAllPinned) },
                        onDeleteClick = { showDeleteDialog = true },
                        onArchiveClick = {
                            val count = selectedMessages.size
                            val unarchivedThreadIds = selectedMessages.map { it.threadId }.toList()
                            viewModel.unarchiveSelectedMessages()
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = String.format(strSnackbarMessagesUnarchived, count),
                                    actionLabel = strSnackbarUndo,
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.archiveThreads(unarchivedThreadIds)
                                }
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
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = if (count == 1) strToast1ContactBlocked else String.format(strToastNContactsBlocked, count),
                                        actionLabel = strSnackbarUndo,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.unblockContacts(selectedMsgs)
                                    }
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
                                    if (count == 1) strToast1MessageDeleted else String.format(strToastNMessagesDeleted, count),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onDismiss = { showDeleteDialog = false }
                        )
                    }
                } else {
                    val titleText = if (messages.itemCount > 0) strArchivedMessagesTitle else strArchivedTitle
                    SecondaryTopBar(
                        title = titleText,
                        onBackClick = { navController.popBackStackWithAd() }
                    )
                }
            },
            floatingActionButton = {
                if (selectedMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .size(62.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colorResource(R.color.primary))
                            .clickable { navController.navigate(Routes.NewChat.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.home_ic_message_btn),
                            contentDescription = strContentDescNewMessage,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    repeat(5) {
                        MessageSkeletonUi()
                    }
                }
            } else if (messages.itemCount == 0 && archivedGroups.isEmpty()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val imageWidth = maxWidth * 0.8f
                    val imageHeight = imageWidth / 1.5f
                    val centerShift = 24.dp

                    Image(
                        painter = painterResource(id = R.drawable.archived_main),
                        contentDescription = strContentDescArchivedEmptyState,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = -centerShift)
                            .width(imageWidth)
                            .height(imageHeight)
                    )
                    Text(
                        text = strArchivedEmptyStateText,
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
                    state = archivedListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding()
                        ),
                ) {
                    items(archivedGroups, key = { "group_${it.group.id}" }) { groupWithLastMessage ->
                        val isMessageSelectionActive = selectedMessages.isNotEmpty()
                        GroupListItem(
                            modifier = Modifier.animateItem(),
                            groupWithLastMessage = groupWithLastMessage,
                            isSelected = selectedGroupIds.contains(groupWithLastMessage.group.id),
                            enabled = !isMessageSelectionActive,
                            enableSwipe = false,
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
                            }
                        )
                    }
                    items(
                        count = archivedRows.size,
                        key = { rowIdx ->
                            val idx = archivedRows.getOrNull(rowIdx)
                            if (idx == null || idx >= messages.itemCount) "ad_$rowIdx" else "msg_${messages[idx]?.id ?: idx}"
                        }
                    ) { rowIdx ->
                        val index = archivedRows.getOrNull(rowIdx)
                        if (index == null) {
                            NativeAdView(
                                adUnitId = listNativeAdUnitId!!,
                                template = NativeAdTemplate.SMALL,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            return@items
                        }
                        if (index >= messages.itemCount) return@items
                        val msg = messages[index]
                        if (msg != null) {
                            MessageItemUi(
                                modifier = Modifier.animateItem(),
                                msg = msg,
                                isSelected = selectedMessages.contains(msg),
                                enableSwipe = false,
                                onClick = {
                                    if (isGroupSelectionMode) {
                                        // Groups and normal messages use separate selection modes.
                                    } else if (selectedMessages.isNotEmpty()) {
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
                                draftText = drafts[msg.address]
                            )
                        }
                    }
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

        if (showGroupBulkDeleteDialog) {
            val count = selectedGroupIds.size
            ConfirmationDialog(
                title = if (count == 1) strDeleteGroupTitle else String.format(strDeleteNGroupsTitle, count),
                text = strDeleteGroupsText,
                confirmText = strActionDelete,
                onConfirm = {
                    viewModel.deleteSelectedGroups()
                    Toast.makeText(context, strToastGroupsDeleted, Toast.LENGTH_SHORT).show()
                    showGroupBulkDeleteDialog = false
                },
                onDismiss = { showGroupBulkDeleteDialog = false }
            )
        }

        ScrollToTopButton(
            visible = archivedListState.firstVisibleItemIndex > 0,
            onClick = { coroutineScope.launch { archivedListState.animateScrollToItem(0) } },
            contentDescription = strContentDescScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        )
    }
}
