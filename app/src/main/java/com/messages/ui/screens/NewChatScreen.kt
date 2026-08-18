package com.messages.ui.screens

import com.messages.navigation.popBackStackWithAd

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.messages.R
import com.messages.navigation.Routes
import com.messages.repository.SmsRepository
import com.messages.ui.components.SecondaryTopBar
import com.messages.ui.theme.Inter
import com.messages.ui.theme.AvatarColors
import com.messages.viewmodel.NewChatViewModel

private fun normalizeNumber(number: String): String = number.replace(Regex("[^0-9+]"), "")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewChatScreen(
    navController: NavController,
    viewModel: NewChatViewModel = viewModel(),
    isScheduling: Boolean = false,
    forwardText: String? = null,
    groupId: Long? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val repository = remember { SmsRepository(application) }
    val isAddMemberMode = groupId != null
    val groupedContacts by viewModel.groupedContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isNumberKeyboard by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    var isGroupSelectionMode by remember { mutableStateOf(isAddMemberMode) }
    val selectedGroupContacts = remember { mutableStateListOf<com.messages.viewmodel.ContactItem>() }
    var existingMemberAddresses by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(groupId) {
        if (groupId != null) {
            val group = repository.getGroupById(groupId)
            existingMemberAddresses = group?.members?.map { normalizeNumber(it.address) }?.toSet() ?: emptySet()
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                viewModel.loadContacts(context)
            }
        }
    )

    LaunchedEffect(Unit) {
        if (hasPermission) {
            viewModel.loadContacts(context)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val strActionAdd = stringResource(R.string.action_add)
    val strActionNext = stringResource(R.string.action_next)
    val strCreateGroup = stringResource(R.string.content_desc_create_group)
    val strAddMembersTitle = stringResource(R.string.new_chat_add_members_title)
    val strSelectMembersTitle = stringResource(R.string.new_chat_select_members_title)
    val strForwardToTitle = stringResource(R.string.new_chat_forward_to_title)
    val strNewChatTitle = stringResource(R.string.new_chat_title)
    val strTypeNameOrNumber = stringResource(R.string.type_name_or_number_placeholder)
    val strClear = stringResource(R.string.content_desc_clear)
    val strKeypadToggle = stringResource(R.string.content_desc_keypad_toggle)
    val strRemoveContactTemplate = stringResource(R.string.content_desc_remove_contact_template)
    val strPermissionRequiredContacts = stringResource(R.string.permission_required_contacts_text)
    val strNoResultsFound = stringResource(R.string.no_results_found)
    val strStarred = stringResource(R.string.content_desc_starred)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary), // Match screenshot background
        floatingActionButton = {
            if (isGroupSelectionMode) {
                val hasSelection = selectedGroupContacts.isNotEmpty()
                Box(
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (hasSelection) colorResource(R.color.primary)
                            else colorResource(R.color.primary).copy(alpha = 0.4f)
                        )
                        .clickable(enabled = hasSelection) {
                            if (isAddMemberMode) {
                                coroutineScope.launch {
                                    val current = repository.getGroupById(groupId!!)
                                    if (current != null) {
                                        val newMembers = selectedGroupContacts.map {
                                            com.messages.model.GroupMember(address = it.number, name = it.name)
                                        }
                                        val existingAddresses = current.members.map { it.address }.toSet()
                                        val toAdd = newMembers.filter { it.address !in existingAddresses }
                                        if (toAdd.isNotEmpty()) {
                                            repository.updateGroup(current.copy(members = current.members + toAdd))
                                        }
                                    }
                                    navController.popBackStackWithAd()
                                }
                            } else {
                                navController.navigate(
                                    Routes.AddGroupName.createRoute(
                                        selectedGroupContacts.map {
                                            com.messages.model.GroupMember(address = it.number, name = it.name)
                                        }
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isAddMemberMode) strActionAdd else strActionNext,
                            color = Color.White,
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (!isAddMemberMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.archived_ic_back),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(180f)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorResource(R.color.primary))
                        .clickable { isGroupSelectionMode = true }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.contact_ic_group), // Fallback icon for Create Group
                            contentDescription = strCreateGroup,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strCreateGroup,
                            color = Color.White,
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SecondaryTopBar(
                title = if (isAddMemberMode) strAddMembersTitle else if (isGroupSelectionMode) strSelectMembersTitle else if (forwardText != null) strForwardToTitle else strNewChatTitle,
                onBackClick = {
                    if (isGroupSelectionMode && !isAddMemberMode) {
                        isGroupSelectionMode = false
                        selectedGroupContacts.clear()
                    } else {
                        navController.popBackStackWithAd()
                    }
                }
            )

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorResource(R.color.light_gray))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = strTypeNameOrNumber,
                                color = colorResource(R.color.text_des),
                                fontSize = 14.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium,
                                overflow = TextOverflow.Ellipsis,
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
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    color = colorResource(R.color.text_title)
                                ),
                                cursorBrush = SolidColor(colorResource(R.color.primary)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (isNumberKeyboard) KeyboardType.Number else KeyboardType.Text
                                )
                            )
                        }
                    }
                    
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.chat_ic_close),
                            contentDescription = strClear,
                            tint = colorResource(R.color.text_des),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    searchQuery = ""
                                }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        painter = painterResource(id = if (isNumberKeyboard) R.drawable.contact_ic_keyboard else R.drawable.contact_ic_keypad_number),
                        contentDescription = strKeypadToggle,
                        tint = colorResource(R.color.text_des),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isNumberKeyboard = !isNumberKeyboard
                                focusRequester.requestFocus()
                            }
                    )
                }
            }

            AnimatedVisibility(
                visible = isGroupSelectionMode && selectedGroupContacts.isNotEmpty(),
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(selectedGroupContacts, key = { it.number }) { contact ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                if (contact.photoUri != null) {
                                    AsyncImage(
                                        model = contact.photoUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    val chipAvatarColor = remember(contact.name) {
                                        val hash = contact.name.hashCode() and 0x7FFFFFFF
                                        AvatarColors[hash % AvatarColors.size]
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(chipAvatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (contact.name.isNotBlank()) contact.name.first().toString().uppercase() else "#",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = Inter
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.chat_ic_close),
                                    contentDescription = String.format(strRemoveContactTemplate, contact.name),
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(colorResource(R.color.light_color_gray))
                                        .padding(3.dp)
                                        .clickable {
                                            selectedGroupContacts.removeAll { it.number == contact.number }
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = contact.name.substringBefore(" "),
                                fontSize = 12.sp,
                                fontFamily = Inter,
                                color = colorResource(R.color.text_des),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact List
            if (!hasPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strPermissionRequiredContacts, fontFamily = Inter)
                }
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.primary))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Pre-calculate indices for fast scroll
                    val headerIndices = remember(groupedContacts, searchQuery, existingMemberAddresses) {
                        val map = mutableMapOf<Char, Int>()
                        var currentIndex = 0
                        groupedContacts.forEach { (letter, contacts) ->
                            val filtered = contacts.filter {
                                (it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.number.contains(searchQuery)) &&
                                        (!isAddMemberMode || normalizeNumber(it.number) !in existingMemberAddresses)
                            }
                            if (filtered.isNotEmpty()) {
                                map[letter] = currentIndex
                                currentIndex += 1 + filtered.size
                            }
                        }
                        map
                    }

                    if (headerIndices.isEmpty() && searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strNoResultsFound,
                                color = colorResource(R.color.text_des),
                                fontFamily = Inter,
                                fontSize = 16.sp
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        groupedContacts.forEach { (letter, contacts) ->
                            val filteredContacts = contacts.filter {
                                (it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.number.contains(searchQuery)) &&
                                        (!isAddMemberMode || normalizeNumber(it.number) !in existingMemberAddresses)
                            }

                            if (filteredContacts.isNotEmpty()) {
                                stickyHeader {
                                    if (letter == '*') {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(colorResource(R.color.bg_primary))
                                                .padding(horizontal = 20.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.chat_ic_long_star),
                                                contentDescription = strStarred,
                                                tint = colorResource(R.color.text_des),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = letter.toString(),
                                            fontSize = 16.sp,
                                            fontFamily = Inter,
                                            fontWeight = FontWeight.Medium,
                                            color = colorResource(R.color.text_des),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(colorResource(R.color.bg_primary))
                                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                items(filteredContacts) { contact ->
                                    val avatarColor = remember(contact.name) {
                                        val hash = contact.name.hashCode() and 0x7FFFFFFF
                                        AvatarColors[hash % AvatarColors.size]
                                    }
                                    val isContactSelected = isGroupSelectionMode &&
                                            selectedGroupContacts.any { it.number == contact.number }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isContactSelected) colorResource(R.color.selection_blue_bg) else Color.Transparent)
                                            .clickable {
                                                if (isGroupSelectionMode) {
                                                    if (selectedGroupContacts.any { it.number == contact.number }) {
                                                        selectedGroupContacts.removeAll { it.number == contact.number }
                                                    } else {
                                                        selectedGroupContacts.add(contact)
                                                    }
                                                } else {
                                                    navController.navigate(
                                                        Routes.Chat.createRoute(
                                                            threadId = 0L, // 0 for new thread
                                                            address = contact.number,
                                                            contactName = contact.name,
                                                            isScheduling = isScheduling,
                                                            forwardText = forwardText
                                                        )
                                                    ) {
                                                        if (forwardText != null) {
                                                            popUpTo(Routes.NewChat.route) { inclusive = true }
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (contact.photoUri != null) {
                                            AsyncImage(
                                                model = contact.photoUri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
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
                                                Text(
                                                    text = if (contact.name.isNotBlank()) contact.name.first()
                                                        .toString().uppercase() else "#",
                                                    color = Color.White,
                                                    fontSize = 25.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = Inter
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                            Text(
                                                text = contact.name,
                                                fontSize = 16.sp,
                                                color = colorResource(R.color.text_title),
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = contact.number,
                                                fontSize = 14.sp,
                                                color = colorResource(R.color.text_des),
                                                fontFamily = Inter,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } // closes LazyColumn

                    // A-Z Scroller
                    if (headerIndices.isNotEmpty()) {
                        val alphabet = listOf('*') + ('A'..'Z').toList() + listOf('#')
                        var itemHeight by remember { mutableStateOf(0f) }
                        var selectedLetter by remember { mutableStateOf<Char?>(null) }

                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 8.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, _ ->
                                        if (itemHeight > 0) {
                                            val index = (change.position.y / itemHeight).toInt()
                                            val clampedIndex = index.coerceIn(0, alphabet.lastIndex)
                                            val letter = alphabet[clampedIndex]
                                            if (selectedLetter != letter) {
                                                selectedLetter = letter
                                                headerIndices[letter]?.let { targetIndex ->
                                                    coroutineScope.launch {
                                                        listState.scrollToItem(
                                                            targetIndex
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .onGloballyPositioned { coordinates ->
                                    itemHeight = coordinates.size.height.toFloat() / alphabet.size
                                },
                            verticalArrangement = Arrangement.Top
                        ) {
                            alphabet.forEach { letter ->
                                val isSelected = selectedLetter == letter
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colorResource(R.color.primary) else Color.Transparent)
                                        .clickable {
                                            selectedLetter = letter
                                            headerIndices[letter]?.let { targetIndex ->
                                                coroutineScope.launch {
                                                    listState.scrollToItem(
                                                        targetIndex
                                                    )
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (letter == '*') {
                                        Icon(
                                            painter = painterResource(id = R.drawable.chat_ic_long_star),
                                            contentDescription = strStarred,
                                            tint = if (isSelected) colorResource(R.color.white) else colorResource(
                                                R.color.text_des
                                            ),
                                            modifier = Modifier.size(if (isSelected) 13.dp else 10.dp)
                                        )
                                    } else {
                                        Text(
                                            text = letter.toString(),
                                            fontSize = if (isSelected) 13.sp else 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontFamily = Inter,
                                            color = if (isSelected) colorResource(R.color.white) else colorResource(
                                                R.color.text_des
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } // End Box
            }
        }
    }
}
