package com.message.sms.texting.app.ui.screens

import com.message.sms.texting.app.navigation.popBackStackWithAd

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.message.sms.texting.app.R
import com.message.sms.texting.app.model.Group
import com.message.sms.texting.app.model.GroupSearchResult
import com.message.sms.texting.app.model.MessageSearchResult
import com.message.sms.texting.app.model.SmsMessage
import com.message.sms.texting.app.navigation.Routes
import com.message.sms.texting.app.ui.components.CustomIconButton
import com.message.sms.texting.app.ui.theme.Inter
import com.message.sms.texting.app.ui.theme.AvatarColors
import com.message.sms.texting.app.viewmodel.SearchViewModel
import java.io.File

@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    color: Color = colorResource(R.color.text_title),
    highlightColor: Color = colorResource(R.color.primary),
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    maxLines: Int = 1
) {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = Inter,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }

    val startIndex = text.indexOf(query, ignoreCase = true)
    val endIndex = startIndex + query.length

    val annotatedString = buildAnnotatedString {
        append(text.substring(0, startIndex))
        withStyle(style = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
            append(text.substring(startIndex, endIndex))
        }
        append(text.substring(endIndex))
    }

    Text(
        text = annotatedString,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = Inter,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val contactResults by viewModel.contactResults.collectAsState()
    val messageResults by viewModel.messageResults.collectAsState()
    val groupResults by viewModel.groupResults.collectAsState()
    val groupMessageResults by viewModel.groupMessageResults.collectAsState()

    val strBack = stringResource(R.string.content_desc_back)
    val strSearchMessages = stringResource(R.string.search_messages_placeholder)
    val strClearSearch = stringResource(R.string.content_desc_clear_search)
    val strYourConversationsWillAppear = stringResource(R.string.your_conversations_will_appear)
    val strResultInMessages = stringResource(R.string.result_in_messages)
    val strNoResultsFoundTemplate = stringResource(R.string.no_results_found_template)

    Scaffold(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.bg_primary))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorResource(R.color.light_gray))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomIconButton(
                        iconRes = R.drawable.archived_ic_back,
                        contentDescription = strBack,
                        tint = colorResource(R.color.text_title),
                        onClick = { navController.popBackStackWithAd() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = strSearchMessages,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.1.sp,
                                    fontSize = 14.sp,
                                    color = colorResource(R.color.text_des)
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
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = colorResource(R.color.text_title)
                                    ),
                                    cursorBrush = SolidColor(colorResource(R.color.primary)),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CustomIconButton(
                                iconRes = R.drawable.chat_ic_close,
                                size = 20,
                                contentDescription = strClearSearch,
                                onClick = { viewModel.updateSearchQuery("")  }
                            )
                        }
                    }
                }
            }

            if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strYourConversationsWillAppear,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = colorResource(R.color.text_des),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Contact Results
                    if (contactResults.isNotEmpty()) {
                        items(contactResults) { msg ->
                            ContactResultItem(
                                msg = msg,
                                query = searchQuery,
                                onClick = { navigateToChat(navController, msg, searchQuery) }
                            )
                        }
                    }

                    // Group Results
                    if (groupResults.isNotEmpty()) {
                        items(groupResults) { group ->
                            GroupResultItem(
                                group = group,
                                query = searchQuery,
                                onClick = { navigateToGroupChat(navController, group, searchQuery) }
                            )
                        }
                    }

                    if (messageResults.isNotEmpty() || groupMessageResults.isNotEmpty()) {
                        item {
                            Divider(
                                color = colorResource(R.color.light_gray),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                            )
                            Text(
                                text = strResultInMessages,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = colorResource(R.color.text_title),
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 8.dp)
                            )
                        }

                        items(messageResults) { result ->
                            MessageResultItem(
                                result = result,
                                query = searchQuery,
                                onClick = { navigateToChat(navController, result.message, searchQuery) }
                            )
                        }

                        items(groupMessageResults) { result ->
                            GroupMessageResultItem(
                                result = result,
                                onClick = { navigateToGroupChat(navController, result.group, null) }
                            )
                        }
                    }

                    if (contactResults.isEmpty() && messageResults.isEmpty() && groupResults.isEmpty() && groupMessageResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(strNoResultsFoundTemplate, searchQuery),
                                    fontFamily = Inter,
                                    fontSize = 14.sp,
                                    color = colorResource(R.color.text_des)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val SearchPhoneRegex = Regex("^[+]?[0-9\\s\\-()]+$")

@Composable
fun ContactResultItem(msg: SmsMessage, query: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarColor = remember(msg.address) {
            val hash = msg.address.hashCode() and 0x7FFFFFFF
            AvatarColors[hash % AvatarColors.size]
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
                if (msg.contactName == null && msg.address.matches(SearchPhoneRegex)) {
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

        Column(modifier = Modifier.weight(1f)) {
            val titleText = msg.contactName ?: msg.address
            HighlightedText(
                text = titleText,
                query = query,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.text_title),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = msg.body,
                fontSize = 14.sp,
                color = colorResource(R.color.text_des).copy(alpha = 0.8f),
                fontFamily = Inter,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = msg.formattedTime,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.text_des),
            fontFamily = Inter
        )
    }
}

@Composable
fun MessageResultItem(result: MessageSearchResult, query: String, onClick: () -> Unit) {
    val msg = result.message
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarColor = remember(msg.address) {
            val hash = msg.address.hashCode() and 0x7FFFFFFF
            AvatarColors[hash % AvatarColors.size]
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
                if (msg.contactName == null && msg.address.matches(SearchPhoneRegex)) {
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = msg.contactName ?: msg.address,
                fontSize = 16.sp,
                color = colorResource(R.color.text_title),
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))

            val strMatch1Message = stringResource(R.string.match_1_message)
            val strMatchNMessagesTemplate = stringResource(R.string.match_n_messages_template)
            val matchText =
                if (result.matchCount == 1) strMatch1Message else String.format(strMatchNMessagesTemplate, result.matchCount)
            HighlightedText(
                text = matchText,
                query = query, // We won't highlight "1 message", but using HighlightedText is safe. Actually we should highlight the body if we want, but user said "1 messages aesa likha aaye"
                fontSize = 14.sp,
                color = colorResource(R.color.text_des).copy(alpha = 0.8f),
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = msg.formattedTime,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.text_des),
            fontFamily = Inter
        )
    }
}

@Composable
fun GroupResultItem(group: Group, query: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!group.avatarUri.isNullOrEmpty()) {
            AsyncImage(
                model = File(group.avatarUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
        } else {
            val avatarColor = remember(group.id) {
                AvatarColors[(group.id % AvatarColors.size).toInt()]
            }
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
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val strFallbackGroupName = stringResource(R.string.fallback_group_name)
            val strNMembersTemplate = stringResource(R.string.n_members_template)
            HighlightedText(
                text = group.name.ifBlank { strFallbackGroupName },
                query = query,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.text_title),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(strNMembersTemplate, group.members.size),
                fontSize = 14.sp,
                color = colorResource(R.color.text_des).copy(alpha = 0.8f),
                fontFamily = Inter,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GroupMessageResultItem(result: GroupSearchResult, onClick: () -> Unit) {
    val group = result.group
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!group.avatarUri.isNullOrEmpty()) {
            AsyncImage(
                model = File(group.avatarUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
        } else {
            val avatarColor = remember(group.id) {
                AvatarColors[(group.id % AvatarColors.size).toInt()]
            }
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
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val strFallbackGroupName = stringResource(R.string.fallback_group_name)
            val strMatch1Message = stringResource(R.string.match_1_message)
            val strMatchNMessagesTemplate = stringResource(R.string.match_n_messages_template)
            Text(
                text = group.name.ifBlank { strFallbackGroupName },
                fontSize = 16.sp,
                color = colorResource(R.color.text_title),
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val matchText = if (result.matchCount == 1) strMatch1Message else String.format(strMatchNMessagesTemplate, result.matchCount)
            Text(
                text = matchText,
                fontSize = 14.sp,
                color = colorResource(R.color.text_des).copy(alpha = 0.8f),
                fontFamily = Inter,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (result.lastMessageDate != null) {
            Text(
                text = formatGroupListTime(result.lastMessageDate),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.text_des),
                fontFamily = Inter
            )
        }
    }
}

private fun navigateToChat(navController: NavController, msg: SmsMessage, searchQuery: String? = null) {
    if (msg.threadId != null) {
        navController.navigate(
            Routes.Chat.createRoute(
                threadId = msg.threadId,
                address = msg.address,
                contactName = msg.contactName,
                searchQuery = searchQuery
            )
        )
    }
}

private fun navigateToGroupChat(navController: NavController, group: Group, searchQuery: String?) {
    navController.navigate(
        Routes.Chat.createRoute(
            threadId = 0L,
            address = "",
            contactName = null,
            searchQuery = searchQuery,
            groupId = group.id
        )
    )
}
