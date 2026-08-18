package com.messages.ui.screens

import com.messages.navigation.popBackStackWithAd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.messages.R
import com.messages.ui.components.CustomIconButton
import com.messages.ui.components.SecondaryTopBar
import com.messages.ui.theme.Inter
import com.messages.ui.theme.SwipeActionsState
import com.messages.ui.theme.swipeActionColorFor
import com.messages.ui.theme.swipeActionIconFor

private data class SwipeActionOption(
    val key: String,
    val label: String,
    val iconRes: Int?,
    val color: Color
)

@Composable
fun SwipeActionsScreen(navController: NavController) {
    val context = LocalContext.current

    val strSwipeActionsTitle = stringResource(R.string.swipe_actions_label)
    val strRightSwipeLabel = stringResource(R.string.right_swipe_label)
    val strLeftSwipeLabel = stringResource(R.string.left_swipe_label)
    val strActionChange = stringResource(R.string.action_change)
    val strSwipeActionNone = stringResource(R.string.swipe_action_none)
    val strActionArchive = stringResource(R.string.action_archive)
    val strSwipeActionDelete = stringResource(R.string.swipe_action_delete)
    val strMenuBlock = stringResource(R.string.menu_block)
    val strContentDescCall = stringResource(R.string.content_desc_call)
    val strMenuMarkAsRead = stringResource(R.string.menu_mark_as_read)
    val strMenuMarkAsUnread = stringResource(R.string.menu_mark_as_unread)
    val strActionCancelCaps = stringResource(R.string.action_cancel_caps)
    val strActionOk = stringResource(R.string.action_ok)
    val strClose = stringResource(R.string.content_desc_close)

    val actionOptions = listOf(
        SwipeActionOption("none", strSwipeActionNone, swipeActionIconFor("none"), swipeActionColorFor("none")),
        SwipeActionOption("archive", strActionArchive, swipeActionIconFor("archive"), swipeActionColorFor("archive")),
        SwipeActionOption("delete", strSwipeActionDelete, swipeActionIconFor("delete"), swipeActionColorFor("delete")),
        SwipeActionOption("block", strMenuBlock, swipeActionIconFor("block"), swipeActionColorFor("block")),
        SwipeActionOption("call", strContentDescCall, swipeActionIconFor("call"), swipeActionColorFor("call")),
        SwipeActionOption("mark_read", strMenuMarkAsRead, swipeActionIconFor("mark_read"), swipeActionColorFor("mark_read")),
        SwipeActionOption("mark_unread", strMenuMarkAsUnread, swipeActionIconFor("mark_unread"), swipeActionColorFor("mark_unread"))
    )

    val rightActionKey by SwipeActionsState.rightAction
    val leftActionKey by SwipeActionsState.leftAction
    val rightOption = actionOptions.firstOrNull { it.key == rightActionKey } ?: actionOptions[1]
    val leftOption = actionOptions.firstOrNull { it.key == leftActionKey } ?: actionOptions[2]

    var showRightDialog by remember { mutableStateOf(false) }
    var showLeftDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary),
        topBar = {
            SecondaryTopBar(
                title = strSwipeActionsTitle,
                onBackClick = { navController.popBackStackWithAd() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SwipeActionSection(
                sectionLabel = strRightSwipeLabel,
                changeLabel = strActionChange,
                selectedOption = rightOption,
                isRightSwipe = true,
                onChangeClick = { showRightDialog = true }
            )

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = colorResource(R.color.light_gray), thickness = 0.9.dp)
            Spacer(modifier = Modifier.height(28.dp))

            SwipeActionSection(
                sectionLabel = strLeftSwipeLabel,
                changeLabel = strActionChange,
                selectedOption = leftOption,
                isRightSwipe = false,
                onChangeClick = { showLeftDialog = true }
            )
        }
    }

    if (showRightDialog) {
        SwipeActionPickerDialog(
            title = strRightSwipeLabel,
            closeDescription = strClose,
            options = actionOptions,
            selectedKey = rightActionKey,
            cancelText = strActionCancelCaps,
            okText = strActionOk,
            onDismiss = { showRightDialog = false },
            onSelect = { key ->
                SwipeActionsState.setRightAction(context, key)
                showRightDialog = false
            }
        )
    }

    if (showLeftDialog) {
        SwipeActionPickerDialog(
            title = strLeftSwipeLabel,
            closeDescription = strClose,
            options = actionOptions,
            selectedKey = leftActionKey,
            cancelText = strActionCancelCaps,
            okText = strActionOk,
            onDismiss = { showLeftDialog = false },
            onSelect = { key ->
                SwipeActionsState.setLeftAction(context, key)
                showLeftDialog = false
            }
        )
    }
}

@Composable
private fun SwipeActionSection(
    sectionLabel: String,
    changeLabel: String,
    selectedOption: SwipeActionOption,
    isRightSwipe: Boolean,
    onChangeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sectionLabel,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Inter,
            color = colorResource(R.color.text_title)
        )
        Text(
            text = changeLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Inter,
            color = colorResource(R.color.primary),
            modifier = Modifier.clickable(onClick = onChangeClick)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = selectedOption.label,
        fontSize = 14.sp,
        fontFamily = Inter,
        color = colorResource(R.color.text_des)
    )
    Spacer(modifier = Modifier.height(14.dp))
    SwipePreviewRow(option = selectedOption, isRightSwipe = isRightSwipe)
}

@Composable
private fun SwipePreviewRow(option: SwipeActionOption, isRightSwipe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorResource(R.color.light_gray))
    ) {
        if (isRightSwipe && option.key != "none") {
            SwipeActionBox(option)
        }
        Box(modifier = Modifier.weight(1f)) {
            SwipePreviewSkeleton()
        }
        if (!isRightSwipe && option.key != "none") {
            SwipeActionBox(option)
        }
    }
}

@Composable
private fun SwipePreviewSkeleton() {
    val skeletonColor = colorResource(R.color.light_color_gray).copy(alpha = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(skeletonColor)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(skeletonColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(skeletonColor)
            )
        }
    }
}

@Composable
private fun SwipeActionBox(option: SwipeActionOption) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(76.dp)
            .background(option.color),
        contentAlignment = Alignment.Center
    ) {
        if (option.iconRes != null) {
            Icon(
                painter = painterResource(id = option.iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SwipeActionPickerDialog(
    title: String,
    closeDescription: String,
    options: List<SwipeActionOption>,
    selectedKey: String,
    cancelText: String,
    okText: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var selected by remember(selectedKey) { mutableStateOf(selectedKey) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(23.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = colorResource(R.color.text_title)
                    )
                    CustomIconButton(
                        iconRes = R.drawable.chat_ic_close,
                        contentDescription = closeDescription,
                        modifier = Modifier.offset(x = 4.dp),
                        onClick = onDismiss
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option.key }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (option.iconRes != null) {
                            Icon(
                                painter = painterResource(id = option.iconRes),
                                contentDescription = null,
                                tint = colorResource(R.color.chat_icon_secondary),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = option.label,
                            fontSize = 16.sp,
                            fontFamily = Inter,
                            color = colorResource(R.color.option_row_text),
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = option.key == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colorResource(R.color.primary),
                                unselectedColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = cancelText,
                            color = colorResource(R.color.light_color_gray),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onSelect(selected) }) {
                        Text(
                            text = okText,
                            color = colorResource(R.color.primary),
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
