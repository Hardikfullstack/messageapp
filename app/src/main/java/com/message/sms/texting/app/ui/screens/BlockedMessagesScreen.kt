package com.message.sms.texting.app.ui.screens

import com.message.sms.texting.app.navigation.popBackStackWithAd

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ads.NativeAdTemplate
import com.message.sms.texting.app.ads.NativeAdView
import com.message.sms.texting.app.ads.interleaveAdEvery3
import com.message.sms.texting.app.repository.BlockedContactWithMessage
import com.message.sms.texting.app.ui.components.SecondaryTopBar
import com.message.sms.texting.app.ui.components.CustomIconButton
import com.message.sms.texting.app.ui.components.CustomSwitch
import com.message.sms.texting.app.ui.components.ScrollToTopButton
import com.message.sms.texting.app.ui.components.dialogs.ConfirmationDialog
import com.message.sms.texting.app.ui.theme.Inter
import com.message.sms.texting.app.viewmodel.AppConfigViewModel
import com.message.sms.texting.app.viewmodel.BlockedMessagesViewModel
import com.message.sms.texting.app.navigation.Routes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.launch

class BlockedMessagesViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlockedMessagesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlockedMessagesViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BlockedMessagesScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: BlockedMessagesViewModel = viewModel(factory = BlockedMessagesViewModelFactory(application))

    val blockedContacts by viewModel.blockedContacts.collectAsState()
    val dropMessagesEnabled by viewModel.dropMessagesEnabled.collectAsState()
    val selectedContacts by viewModel.selectedContacts.collectAsState()

    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val listNativeAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.native_6_on_off == "on") {
            result.native_6?.takeIf { it.isNotBlank() }
        } else null
    }
    val blockedRows = remember(blockedContacts, listNativeAdUnitId) {
        blockedContacts.interleaveAdEvery3(listNativeAdUnitId != null)
    }

    var showUnblockDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val strClose = stringResource(R.string.content_desc_close)
    val strBlockedMessagesTitle = stringResource(R.string.blocked_messages_title)
    val strUnblock = stringResource(R.string.action_unblock)
    val strDelete = stringResource(R.string.content_desc_delete)
    val strDropMessagesTitle = stringResource(R.string.drop_messages_title)
    val strDropMessagesDesc = stringResource(R.string.drop_messages_desc)
    val strEmptyBlockedList = stringResource(R.string.content_desc_empty_blocked_list)
    val strEmptyBlockedListText = stringResource(R.string.empty_blocked_list_text)
    val strBlocked = stringResource(R.string.content_desc_blocked)
    val strBlockedContactFallback = stringResource(R.string.blocked_contact_fallback)
    val strUnblockContactTitle = stringResource(R.string.unblock_contact_title)
    val strRemoveContactBlocklistText = stringResource(R.string.remove_contact_blocklist_text)
    val strActionUnblockCaps = stringResource(R.string.action_unblock_caps)
    val strSnackbar1ContactUnblocked = stringResource(R.string.snackbar_1_contact_unblocked)
    val strSnackbarNContactsUnblocked = stringResource(R.string.snackbar_n_contacts_unblocked)
    val strSnackbarUndo = stringResource(R.string.snackbar_undo)
    val strDeleteMessagesTitle = stringResource(R.string.delete_messages_title)
    val strDeleteBlockedMessagesText = stringResource(R.string.delete_blocked_messages_text)
    val strActionDelete = stringResource(R.string.action_delete)
    val strContentDescScrollToTop = stringResource(R.string.content_desc_scroll_to_top)
    val blockedListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
            if (selectedContacts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.bg_primary))
                        .statusBarsPadding()
                        .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomIconButton(
                        iconRes = R.drawable.archived_ic_back,
                        contentDescription = strClose,
                        onClick = { viewModel.clearSelection() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strBlockedMessagesTitle,
                        fontSize = 20.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.text_title),
                        modifier = Modifier.weight(1f)
                    )
                    // Unblock icon
                    CustomIconButton(
                        iconRes = R.drawable.block_list_ic_unblock,
                        contentDescription = strUnblock,
                        onClick = { showUnblockDialog = true }
                    )
                    
                    val hasMessagesToDelete = selectedContacts.any { it.body != null }
                    if (hasMessagesToDelete) {
                        CustomIconButton(
                            iconRes = R.drawable.longpress_ic_delete,
                            contentDescription = strDelete,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
            } else {
                SecondaryTopBar(
                    title = strBlockedMessagesTitle,
                    onBackClick = { navController.popBackStackWithAd() }
                )
            }
        },
        containerColor = colorResource(R.color.bg_primary)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (blockedContacts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.light_gray))
                        .clickable { viewModel.setDropMessagesEnabled(!dropMessagesEnabled) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strDropMessagesTitle,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = colorResource(R.color.text_title)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strDropMessagesDesc,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = colorResource(R.color.text_des),
                            lineHeight = 20.sp
                        )
                    }
                    CustomSwitch(
                        checked = dropMessagesEnabled,
                        onCheckedChange = { viewModel.setDropMessagesEnabled(it) }
                    )
                }
            }

            if (blockedContacts.isEmpty()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val imageWidth = maxWidth * 0.8f
                    val imageHeight = imageWidth / 1.5f
                    val centerShift = 24.dp

                    Image(
                        painter = painterResource(id = R.drawable.block_list_main),
                        contentDescription = strEmptyBlockedList,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = -centerShift)
                            .width(imageWidth)
                            .height(imageHeight)
                    )
                    Text(
                        text = strEmptyBlockedListText,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = colorResource(R.color.text_des),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = imageHeight / 2 - 10.dp - centerShift)
                            .padding(horizontal = 20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(15.dp))
                LazyColumn(state = blockedListState, modifier = Modifier.weight(1f)) {
                    items(
                        count = blockedRows.size,
                        key = { idx -> blockedRows[idx]?.address ?: "ad_$idx" }
                    ) { idx ->
                      val contact = blockedRows[idx]
                      if (contact == null) {
                        NativeAdView(
                            adUnitId = listNativeAdUnitId!!,
                            template = NativeAdTemplate.SMALL,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                      } else {
                        val isSelected = selectedContacts.contains(contact)
                        val bgColor = if (isSelected) colorResource(R.color.primary).copy(alpha = 0.1f) else colorResource(R.color.bg_primary)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .background(bgColor)
                                .combinedClickable(
                                    onClick = {
                                        if (selectedContacts.isNotEmpty()) {
                                            viewModel.toggleSelection(contact)
                                        } else if (contact.threadId != null) {
                                            navController.navigate(
                                                Routes.Chat.createRoute(
                                                    threadId = contact.threadId,
                                                    address = contact.address,
                                                    contactName = contact.contactName
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(contact)
                                    }
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.block_list_ic_block),
                                    contentDescription = strBlocked,
                                    tint = colorResource(R.color.primary),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.contactName ?: contact.address,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = colorResource(R.color.text_title),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contact.body ?: strBlockedContactFallback,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = colorResource(R.color.text_des).copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (contact.formattedTime.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = contact.formattedTime,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = colorResource(R.color.text_des),
                                    modifier = Modifier.align(Alignment.Top)
                                )
                            }
                        }
                      }
                    }
                }
            } // Close if
        } // Close Column
    } // Close Scaffold's paddingValues lambda

    if (showUnblockDialog) {
            ConfirmationDialog(
                title = strUnblockContactTitle,
                text = strRemoveContactBlocklistText,
                confirmText = strActionUnblockCaps,
                onConfirm = {
                    showUnblockDialog = false
                    val unblocked = viewModel.unblockSelectedContacts()
                    coroutineScope.launch {
                        val count = unblocked.size
                        val result = snackbarHostState.showSnackbar(
                            message = if (count == 1) strSnackbar1ContactUnblocked else String.format(strSnackbarNContactsUnblocked, count),
                            actionLabel = strSnackbarUndo,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.blockContacts(unblocked)
                        }
                    }
                },
                onDismiss = { showUnblockDialog = false }
            )
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                title = strDeleteMessagesTitle,
                text = strDeleteBlockedMessagesText,
                confirmText = strActionDelete,
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.deleteSelectedContactsMessages()
                },
                onDismiss = { showDeleteDialog = false }
            )
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

        ScrollToTopButton(
            visible = blockedListState.firstVisibleItemIndex > 0,
            onClick = { coroutineScope.launch { blockedListState.animateScrollToItem(0) } },
            contentDescription = strContentDescScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        )
    } // Close Box
} 
