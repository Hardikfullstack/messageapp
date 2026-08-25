package com.messages.sms.texting.app.ui.screens

import com.messages.sms.texting.app.navigation.popBackStackWithAd

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.model.GroupMember
import com.messages.sms.texting.app.navigation.Routes
import com.messages.sms.texting.app.ui.components.CustomIconButton
import com.messages.sms.texting.app.ui.components.SecondaryTopBar
import com.messages.sms.texting.app.ui.components.dialogs.ConfirmationDialog
import com.messages.sms.texting.app.ui.theme.Inter
import com.messages.sms.texting.app.ui.theme.AvatarColors
import com.messages.sms.texting.app.viewmodel.ChatDetailsViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
private val PhoneRegex = Regex("^[+]?[0-9\\s\\-()]+$")

@Composable
fun ChatDetailsScreen(
    navController: NavController,
    threadId: Long,
    address: String,
    contactName: String?,
    groupId: Long? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val isGroupMode = groupId != null
    val viewModel: ChatDetailsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatDetailsViewModel::class.java)) {
                    return ChatDetailsViewModel(application, threadId, address, groupId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )

    val isArchived by viewModel.isArchived.collectAsState()
    val isBlocked by viewModel.isBlocked.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val group by viewModel.group.collectAsState()
    val memberPhotoUris by viewModel.memberPhotoUris.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isLoadingAvatar by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<GroupMember?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val isPhoneNumber = remember(address) {
        address.matches(PhoneRegex)
    }

    val strChatDetailsTitle = stringResource(R.string.chat_details_title)
    val strGroupDetailsTitle = stringResource(R.string.group_details_title)
    val strCall = stringResource(R.string.content_desc_call)
    val strUnableLoadSelectedImage = stringResource(R.string.toast_unable_load_selected_image)
    val strGroupAvatar = stringResource(R.string.content_desc_group_avatar)
    val strFallbackGroupName = stringResource(R.string.fallback_group_name)
    val strChangeGroupPhoto = stringResource(R.string.content_desc_change_group_photo)
    val strEditGroupName = stringResource(R.string.content_desc_edit_group_name)
    val strProfile = stringResource(R.string.content_desc_profile)
    val strMembersCountTemplate = stringResource(R.string.members_count_title)
    val strUnarchive = stringResource(R.string.action_unarchive)
    val strArchive = stringResource(R.string.action_archive)
    val strNotifications = stringResource(R.string.notifications_label)
    val strDeleteConversationLabel = stringResource(R.string.delete_conversation_label)
    val strUnblockContactLabel = stringResource(R.string.unblock_contact_label)
    val strBlockContactLabel = stringResource(R.string.block_contact_label)
    val strDeleteConversationTitle = stringResource(R.string.delete_conversation_title)
    val strDeleteGroupText = stringResource(R.string.delete_group_text)
    val strDeleteConversationText = stringResource(R.string.delete_conversation_text)
    val strActionDelete = stringResource(R.string.action_delete)
    val strUnblockContactTitle = stringResource(R.string.unblock_contact_title)
    val strBlockReportSpamTitle = stringResource(R.string.block_report_spam_title)
    val strRemoveContactBlocklistText = stringResource(R.string.remove_contact_blocklist_text)
    val strBlockReportSpamText = stringResource(R.string.block_report_spam_text)
    val strActionUnblockCaps = stringResource(R.string.action_unblock_caps)
    val strActionBlock = stringResource(R.string.action_block)
    val strRemoveMemberTemplate = stringResource(R.string.content_desc_remove_member_template)
    val strActionRemove = stringResource(R.string.action_remove)
    val strAddMemberLabel = stringResource(R.string.add_member_label)
    val strRemoveMemberTitle = stringResource(R.string.remove_member_title)
    val strRemoveMemberTextTemplate = stringResource(R.string.remove_member_text_template)
    val strRenameGroupTitle = stringResource(R.string.rename_group_title)
    val strActionSave = stringResource(R.string.action_save)
    val strActionCancelCaps = stringResource(R.string.action_cancel_caps)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary),
        topBar = {
            SecondaryTopBar(
                title = if (isGroupMode) strGroupDetailsTitle else strChatDetailsTitle,
                onBackClick = { navController.popBackStackWithAd() },
                actions = {
                    if (!isGroupMode && isPhoneNumber) {
                        CustomIconButton(
                            iconRes = R.drawable.chat_ic_phone,
                            contentDescription = strCall,
                            tint = colorResource(R.color.primary),
                            onClick = {
                                val telecomManager = context.getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                                val defaultDialer = telecomManager?.defaultDialerPackage

                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                                        data = android.net.Uri.parse("tel:$address")
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isGroupMode) {
                    val avatarImagePickerLauncher = rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
                        onResult = { uri ->
                            if (uri != null) {
                                isLoadingAvatar = true
                                coroutineScope.launch {
                                    val path = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        copyPickedImageToAppStorage(context, uri)
                                    }
                                    isLoadingAvatar = false
                                    if (path != null) {
                                        viewModel.updateGroupAvatar(path)
                                    } else {
                                        android.widget.Toast.makeText(context, strUnableLoadSelectedImage, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                    val openAvatarPicker = {
                        avatarImagePickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(84.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable { openAvatarPicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isLoadingAvatar -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(colorResource(R.color.light_gray)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.dp,
                                            color = colorResource(R.color.primary)
                                        )
                                    }
                                }
                                !group?.avatarUri.isNullOrEmpty() -> {
                                    AsyncImage(
                                        model = File(group!!.avatarUri!!),
                                        contentDescription = strGroupAvatar,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                                else -> {
                                    val avatarColor = remember(groupId) {
                                        AvatarColors[((groupId ?: 0L) % AvatarColors.size).toInt()]
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.contact_ic_group),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(colorResource(R.color.primary))
                                .clickable { openAvatarPicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.chat_ic_camera),
                                contentDescription = strChangeGroupPhoto,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    ) {
                        Text(
                            text = group?.name?.ifBlank { strFallbackGroupName } ?: strFallbackGroupName,
                            fontSize = 24.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            color = colorResource(R.color.text_title),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.chat_ic_edit),
                            contentDescription = strEditGroupName,
                            tint = colorResource(R.color.primary),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                } else {
                    val avatarColor = remember(address) {
                        val hash = address.hashCode() and 0x7FFFFFFF
                        AvatarColors[hash % AvatarColors.size]
                    }

                    if (!photoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = strProfile,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (contactName == null && isPhoneNumber) {
                                Icon(
                                    painter = painterResource(id = R.drawable.home_ic_filled_person),
                                    contentDescription = strProfile,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Text(
                                    text = (contactName ?: address).firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 40.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact Name & Number
                    Text(
                        text = contactName ?: address,
                        fontSize = 24.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        color = colorResource(R.color.text_title),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (contactName != null) address else " ",
                        fontSize = 18.sp,
                        fontFamily = Inter,
                        color = colorResource(id = R.color.text_des),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(
                modifier = Modifier
                    .height(1.dp)
                    .padding(horizontal = 20.dp),
                color = colorResource(R.color.light_gray),
                thickness = 0.9.dp
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (isGroupMode) {
                val memberCount = group?.members?.size ?: 0
                Text(
                    text = String.format(strMembersCountTemplate, memberCount),
                    fontSize = 13.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_des),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                GroupMembersList(
                    members = group?.members.orEmpty(),
                    memberPhotoUris = memberPhotoUris,
                    addMemberLabel = strAddMemberLabel,
                    removeMemberTemplate = strRemoveMemberTemplate,
                    onMemberClick = { member ->
                        navController.navigate(
                            Routes.Chat.createRoute(
                                threadId = 0L,
                                address = member.address,
                                contactName = member.name
                            )
                        )
                    },
                    onAddMemberClick = {
                        navController.navigate(Routes.NewChat.createRoute(groupId = groupId))
                    },
                    onRemoveMemberClick = { member ->
                        memberToRemove = member
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = colorResource(R.color.light_gray),
                    thickness = 0.9.dp
                )

                ChatDetailsOptionRow(
                    iconRes = if (isArchived) R.drawable.archived_ic_unarchive else R.drawable.home_ic_more_archived,
                    text = if (isArchived) strUnarchive else strArchive,
                    onClick = { viewModel.toggleArchive() }
                )

                ChatDetailsOptionRow(
                    iconRes = R.drawable.chat_ic_notification,
                    text = strNotifications,
                    onClick = {
                        navController.navigate(
                            Routes.ContactNotification.createRoute(
                                threadId = groupId ?: 0L,
                                contactName = group?.name?.ifBlank { strFallbackGroupName } ?: strFallbackGroupName
                            )
                        )
                    }
                )

                ChatDetailsOptionRow(
                    iconRes = R.drawable.longpress_ic_delete,
                    text = strDeleteConversationLabel,
                    onClick = { showDeleteDialog = true }
                )
            } else {
                // Options List
                ChatDetailsOptionRow(
                    iconRes = if (isArchived) R.drawable.archived_ic_unarchive else R.drawable.home_ic_more_archived,
                    text = if (isArchived) strUnarchive else strArchive,
                    enabled = !isBlocked,
                    onClick = {
                        viewModel.toggleArchive()
                    }
                )

                ChatDetailsOptionRow(
                    iconRes = R.drawable.chat_ic_notification,
                    text = strNotifications,
                    enabled = !isBlocked,
                    onClick = {
                        navController.navigate(
                            Routes.ContactNotification.createRoute(
                                threadId = threadId,
                                contactName = contactName ?: address
                            )
                        )
                    }
                )

                ChatDetailsOptionRow(
                    iconRes = R.drawable.longpress_ic_delete,
                    text = strDeleteConversationLabel,
                    onClick = { showDeleteDialog = true }
                )

                ChatDetailsOptionRow(
                    iconRes = R.drawable.home_ic_more_block_list,
                    text = if (isBlocked) strUnblockContactLabel else strBlockContactLabel,
                    iconTint = Color(0xFFFF0000),
                    onClick = { showBlockDialog = true }
                )
            }
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                title = strDeleteConversationTitle,
                text = if (isGroupMode) strDeleteGroupText else strDeleteConversationText,
                confirmText = strActionDelete,
                onConfirm = {
                    showDeleteDialog = false
                    if (isGroupMode) {
                        viewModel.deleteGroup()
                    } else {
                        viewModel.deleteThread()
                    }
                    navController.popBackStackWithAd(Routes.Dashboard.route, inclusive = false)
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }

        if (showBlockDialog) {
            ConfirmationDialog(
                title = if (isBlocked) strUnblockContactTitle else strBlockReportSpamTitle,
                text = if (isBlocked) strRemoveContactBlocklistText else strBlockReportSpamText,
                confirmText = if (isBlocked) strActionUnblockCaps else strActionBlock,
                onConfirm = {
                    showBlockDialog = false
                    viewModel.toggleBlock(contactName)
                },
                onDismiss = {
                    showBlockDialog = false
                }
            )
        }

        if (memberToRemove != null) {
            val member = memberToRemove!!
            ConfirmationDialog(
                title = strRemoveMemberTitle,
                text = String.format(strRemoveMemberTextTemplate, member.name ?: member.address),
                confirmText = strActionRemove,
                onConfirm = {
                    viewModel.removeMember(member.address)
                    memberToRemove = null
                },
                onDismiss = { memberToRemove = null }
            )
        }

        if (showRenameDialog && group != null) {
            RenameGroupDialog(
                initialName = group?.name.orEmpty(),
                title = strRenameGroupTitle,
                cancelText = strActionCancelCaps,
                saveText = strActionSave,
                onConfirm = { newName ->
                    viewModel.renameGroup(newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }
    }
}

@Composable
private fun GroupMembersList(
    members: List<GroupMember>,
    memberPhotoUris: Map<String, String?>,
    addMemberLabel: String,
    removeMemberTemplate: String,
    onMemberClick: (GroupMember) -> Unit,
    onAddMemberClick: () -> Unit,
    onRemoveMemberClick: (GroupMember) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddMemberClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.light_gray)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_ic_add),
                    contentDescription = addMemberLabel,
                    tint = colorResource(R.color.primary),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = addMemberLabel,
                fontSize = 16.sp,
                color = colorResource(R.color.primary),
                fontFamily = Inter,
                fontWeight = FontWeight.Medium
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 128.dp)
        ) {
            items(members, key = { it.address }) { member ->
                val avatarColor = remember(member.address) {
                    val hash = member.address.hashCode() and 0x7FFFFFFF
                    AvatarColors[hash % AvatarColors.size]
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMemberClick(member) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val memberPhotoUri = memberPhotoUris[member.address]
                    if (!memberPhotoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = memberPhotoUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (member.name == null) {
                                Icon(
                                    painter = painterResource(id = R.drawable.home_ic_filled_person),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            } else {
                                Text(
                                    text = if (member.name.isNotBlank()) member.name.first().uppercase() else "#",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = Inter
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name ?: member.address,
                            fontSize = 16.sp,
                            color = colorResource(R.color.text_title),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium
                        )
                        if (member.name != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = member.address,
                                fontSize = 14.sp,
                                color = colorResource(R.color.text_des),
                                fontFamily = Inter
                            )
                        }
                    }
                    if (members.size > 1) {
                        Icon(
                            painter = painterResource(id = R.drawable.chat_ic_close),
                            contentDescription = String.format(removeMemberTemplate, member.name ?: member.address),
                            tint = colorResource(R.color.text_des),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onRemoveMemberClick(member) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameGroupDialog(
    initialName: String,
    title: String,
    cancelText: String,
    saveText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    title,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    color = colorResource(R.color.text_title)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorResource(R.color.light_gray))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val customTextSelectionColors = TextSelectionColors(
                        handleColor = colorResource(R.color.primary),
                        backgroundColor = colorResource(R.color.primary).copy(alpha = 0.4f)
                    )
                    CompositionLocalProvider(
                        LocalTextSelectionColors provides customTextSelectionColors
                    ) {
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = Inter,
                                color = colorResource(R.color.text_title)
                            ),
                            cursorBrush = SolidColor(colorResource(R.color.primary)),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            cancelText,
                            color = colorResource(R.color.light_color_gray),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(name) },
                        enabled = name.isNotBlank()
                    ) {
                        Text(
                            saveText,
                            color = if (name.isNotBlank()) colorResource(R.color.primary) else colorResource(R.color.text_des),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailsOptionRow(
    iconRes: Int,
    text: String,
    iconTint: Color? = null,
    textColor: Color? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            tint = iconTint ?: colorResource(R.color.primary),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = Inter,
            color = textColor ?: colorResource(R.color.option_row_text)
        )
    }
}
