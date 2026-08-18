package com.messages.ui.screens

import com.messages.navigation.popBackStackWithAd

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.messages.R
import com.messages.ads.NativeAdTemplate
import com.messages.ads.NativeAdView
import com.messages.ads.interleaveAdEvery3
import com.messages.model.SmsMessage
import com.messages.navigation.Routes
import com.messages.ui.components.MessageSkeletonUi
import com.messages.ui.components.ScrollToTopButton
import com.messages.ui.components.SecondaryTopBar
import com.messages.ui.theme.Inter
import com.messages.ui.theme.AvatarColors
import com.messages.viewmodel.AppConfigViewModel
import com.messages.viewmodel.StarredViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

private val PhoneRegex = Regex("^[+]?[0-9\\s\\-()]+$")

@Composable
fun StarredMessagesScreen(
    navController: NavController,
    viewModel: StarredViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val context = LocalContext.current
    // Shares the same AppConfigViewModel instance created in MainActivity (Activity-scoped).
    val appConfigViewModel: AppConfigViewModel = viewModel(context as ComponentActivity)
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val listNativeAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.native_5_on_off == "on") {
            result.native_5?.takeIf { it.isNotBlank() }
        } else null
    }
    val starredRows = remember(messages, listNativeAdUnitId) {
        messages.interleaveAdEvery3(listNativeAdUnitId != null)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadStarredMessages()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val strStarredMessagesTitle = stringResource(R.string.starred_messages_title)
    val strNoStarredMessages = stringResource(R.string.content_desc_no_starred_messages)
    val strNoStarredMessagesText = stringResource(R.string.no_starred_messages_text)
    val strContentDescScrollToTop = stringResource(R.string.content_desc_scroll_to_top)
    val starredListState = rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = strStarredMessagesTitle,
                onBackClick = { navController.popBackStackWithAd() }
            )
        },
        containerColor = colorResource(R.color.bg_primary)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    repeat(5) {
                        MessageSkeletonUi()
                    }
                }
            } else if (messages.isEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val imageWidth = maxWidth * 0.8f
                    val imageHeight = imageWidth / 1.5f
                    val centerShift = 24.dp

                    Image(
                        painter = painterResource(id = R.drawable.starred_messages_main),
                        contentDescription = strNoStarredMessages,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = -centerShift)
                            .width(imageWidth)
                            .height(imageHeight)
                    )
                    Text(
                        text = strNoStarredMessagesText,
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
                    state = starredListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        count = starredRows.size,
                        key = { idx -> starredRows[idx]?.id ?: "ad_$idx" }
                    ) { idx ->
                        val msg = starredRows[idx]
                        if (msg == null) {
                            NativeAdView(
                                adUnitId = listNativeAdUnitId!!,
                                template = NativeAdTemplate.SMALL,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            StarredMessageItem(
                                modifier = Modifier.animateItem(),
                                msg = msg,
                                onClick = {
                                    navController.navigate(
                                        Routes.Chat.createRoute(
                                            threadId = msg.threadId,
                                            address = msg.address,
                                            contactName = msg.contactName,
                                            highlightMsgId = msg.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            ScrollToTopButton(
                visible = starredListState.firstVisibleItemIndex > 0,
                onClick = { coroutineScope.launch { starredListState.animateScrollToItem(0) } },
                contentDescription = strContentDescScrollToTop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
fun StarredMessageItem(
    modifier: Modifier = Modifier,
    msg: SmsMessage,
    onClick: () -> Unit
) {
    val strStarred = stringResource(R.string.content_desc_starred)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayName = msg.contactName ?: msg.address
        val avatarColor = remember(msg.address) {
            val hash = msg.address.hashCode() and 0x7FFFFFFF
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
                if (msg.contactName == null && isPhoneNumber) {
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
                    fontWeight = FontWeight.Medium,
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

            // Right Column (Time and Star)
            Column(horizontalAlignment = Alignment.End) {
                val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timeString = dateFormat.format(Date(msg.date))

                Text(
                    text = timeString,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_des),
                    fontFamily = Inter
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(colorResource(R.color.primary), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chat_ic_long_star),
                        contentDescription = strStarred,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
