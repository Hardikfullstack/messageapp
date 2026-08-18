package com.messages.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messages.R
import com.messages.ui.theme.Inter
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size

@Composable
fun CommonTopBar(
    title: String,
    onSearchClick: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
    isFilterActive: Boolean = false,
    onMoreClick: (() -> Unit)? = null,
    titleFontSize: TextUnit = 24.sp,
    moreDropdownContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = 20.dp,
                end = 8.dp,
                top = 16.dp,
                bottom = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = titleFontSize,
            fontWeight = FontWeight.Medium,
            fontFamily = Inter,
            color = colorResource(R.color.primary),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            onSearchClick?.let {
                CustomIconButton(
                    iconRes = R.drawable.home_ic_search,
                    onClick = it,
                )
            }
            onFilterClick?.let {
                CustomIconButton(
                    iconRes = R.drawable.home_ic_filter,
                    onClick = it,
                    tint = if (isFilterActive) colorResource(R.color.primary) else colorResource(R.color.text_title)
                )
            }
            onMoreClick?.let {
                Box {
                    CustomIconButton(
                        iconRes = R.drawable.home_ic_more,
                        onClick = it,
                    )
                    moreDropdownContent?.invoke()
                }
            }
        }
    }
}

@Composable
fun ContextualTopBar(
    selectedCount: Int,
    isAllPinned: Boolean,
    hasUnread: Boolean,
    isArchivedScreen: Boolean = false,
    onCloseClick: () -> Unit,
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onMarkUnreadClick: () -> Unit,
    onMarkReadClick: () -> Unit,
    onBlockListClick: () -> Unit,
    onAddContactClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Pin/Unpin Icon
    val pinIcon = if (isAllPinned) R.drawable.longpress_ic_unpin else R.drawable.longpress_ic_pin
    val strClose = stringResource(R.string.content_desc_close)
    val strPinUnpin = stringResource(R.string.content_desc_pin_unpin)
    val strDelete = stringResource(R.string.content_desc_delete)
    val strArchive = stringResource(R.string.action_archive)
    val strUnarchive = stringResource(R.string.action_unarchive)
    val strMore = stringResource(R.string.content_desc_more)
    val strAddToContacts = stringResource(R.string.menu_add_to_contacts)
    val strMarkAsRead = stringResource(R.string.menu_mark_as_read)
    val strMarkAsUnread = stringResource(R.string.menu_mark_as_unread)
    val strUnselectAll = stringResource(R.string.home_menu_unselect_all)
    val strBlock = stringResource(R.string.menu_block)
    val readUnreadText = if (hasUnread) strMarkAsRead else strMarkAsUnread
    val readUnreadIcon =
        if (hasUnread) R.drawable.longpress_ic_more_mark_read else R.drawable.longpress_ic_more_mark_unread
    val readUnreadTint =
        if (hasUnread) colorResource(id = R.color.chat_icon_secondary) else Color.Unspecified

    val archiveIcon =
        if (isArchivedScreen) R.drawable.archived_ic_unarchive else R.drawable.home_ic_more_archived

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.bg_primary))
            .statusBarsPadding()
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 16.dp,
                bottom = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically,

        ) {
        // Close Icon
        CustomIconButton(
            iconRes = R.drawable.longpress_ic_close,
            contentDescription = strClose,
            onClick = { onCloseClick() }
        )

        Spacer(modifier = Modifier.width(2.dp))

        // Selected Count
        Text(
            text = selectedCount.toString(),
            fontSize = 24.sp,
            fontFamily = Inter,
            color = colorResource(id = R.color.text_title)
        )

        Spacer(modifier = Modifier.weight(1f))

        CustomIconButton(
            iconRes = pinIcon,
            contentDescription = strPinUnpin,
            onClick = { onPinClick() }
        )
        Spacer(modifier = Modifier.width(2.dp))

        // Delete Icon
        CustomIconButton(
            iconRes = R.drawable.longpress_ic_delete,
            contentDescription = strDelete,
            onClick = { onDeleteClick() }
        )

        Spacer(modifier = Modifier.width(2.dp))

        // Archive Icon
        CustomIconButton(
            iconRes = archiveIcon,
            contentDescription = if (isArchivedScreen) strUnarchive else strArchive,
            onClick = { onArchiveClick() }
        )

        Spacer(modifier = Modifier.width(2.dp))

        // More Options
        Box {
            CustomIconButton(
                iconRes = R.drawable.home_ic_more,
                contentDescription = strMore,
                onClick = { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = (-14).dp, y = 5.dp),
                modifier = Modifier.width(211.dp),
                shape = RoundedCornerShape(15.dp),
                containerColor = colorResource(R.color.menu_bg)
            ) {
                if (selectedCount == 1) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                strAddToContacts,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = colorResource(id = R.color.chat_icon_secondary)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.chat_ic_contacts),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = colorResource(id = R.color.chat_icon_secondary)
                            )
                        },
                        onClick = {
                            expanded = false
                            onAddContactClick()
                        },
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                DropdownMenuItem(
                    text = {
                        Text(
                            readUnreadText,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.chat_icon_secondary)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = readUnreadIcon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = readUnreadTint
                        )
                    },
                    onClick = {
                        expanded = false
                        if (hasUnread) onMarkReadClick() else onMarkUnreadClick()
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
                
                if (selectedCount > 1) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                strUnselectAll,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = colorResource(id = R.color.chat_icon_secondary)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.home_ic_more_select_all),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = colorResource(id = R.color.chat_icon_secondary)
                            )
                        },
                        onClick = {
                            expanded = false
                            onCloseClick()
                        },
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                DropdownMenuItem(
                    text = {
                        Text(
                            strBlock,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.chat_icon_secondary)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.home_ic_more_block_list),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colorResource(id = R.color.chat_icon_secondary)
                        )
                    },
                    onClick = {
                        expanded = false
                        onBlockListClick()
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun GroupSelectionTopBar(
    selectedCount: Int,
    isAllPinned: Boolean,
    isArchivedScreen: Boolean = false,
    onCloseClick: () -> Unit,
    onPinClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val pinIcon = if (isAllPinned) R.drawable.longpress_ic_unpin else R.drawable.longpress_ic_pin
    val archiveIcon = if (isArchivedScreen) R.drawable.archived_ic_unarchive else R.drawable.home_ic_more_archived
    val strClose = stringResource(R.string.content_desc_close)
    val strPinUnpin = stringResource(R.string.content_desc_pin_unpin)
    val strArchive = stringResource(R.string.action_archive)
    val strUnarchive = stringResource(R.string.action_unarchive)
    val strDelete = stringResource(R.string.content_desc_delete)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.bg_primary))
            .statusBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomIconButton(
            iconRes = R.drawable.longpress_ic_close,
            contentDescription = strClose,
            onClick = onCloseClick
        )

        Spacer(modifier = Modifier.width(2.dp))

        Text(
            text = selectedCount.toString(),
            fontSize = 24.sp,
            fontFamily = Inter,
            color = colorResource(id = R.color.text_title),
            modifier = Modifier.weight(1f)
        )

        CustomIconButton(
            iconRes = pinIcon,
            contentDescription = strPinUnpin,
            onClick = onPinClick
        )

        Spacer(modifier = Modifier.width(2.dp))

        CustomIconButton(
            iconRes = archiveIcon,
            contentDescription = if (isArchivedScreen) strUnarchive else strArchive,
            onClick = onArchiveClick
        )

        Spacer(modifier = Modifier.width(2.dp))

        CustomIconButton(
            iconRes = R.drawable.longpress_ic_delete,
            contentDescription = strDelete,
            onClick = onDeleteClick
        )
    }
}

@Composable
fun SecondaryTopBar(
    title: String,
    onBackClick: () -> Unit,
    showBackButton: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val strBack = stringResource(R.string.content_desc_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            CustomIconButton(
                iconRes = R.drawable.archived_ic_back,
                contentDescription = strBack,
                tint = colorResource(R.color.text_title),
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.text_title),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        actions()
    }
}

@Composable
fun ChatSelectionTopBar(
    selectedCount: Int,
    isStarred: Boolean = false,
    onClose: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onForward: () -> Unit,
    onDetails: () -> Unit
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val strClose = stringResource(R.string.content_desc_close)
    val strStar = stringResource(R.string.content_desc_star)
    val strDelete = stringResource(R.string.content_desc_delete)
    val strCopy = stringResource(R.string.content_desc_copy)
    val strMore = stringResource(R.string.content_desc_more)
    val strShare = stringResource(R.string.menu_share)
    val strForward = stringResource(R.string.menu_forward)
    val strViewDetails = stringResource(R.string.menu_view_details)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.bg_primary))
            .statusBarsPadding()
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 16.dp,
                bottom = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomIconButton(
            iconRes = R.drawable.longpress_ic_close,
            contentDescription = strClose,
            onClick = { onClose() }
        )

        Spacer(modifier = Modifier.width(2.dp))

        Text(
            text = selectedCount.toString(),
            fontSize = 24.sp,
            fontFamily = Inter,
            color = colorResource(id = R.color.text_title),
            modifier = Modifier
                .weight(1f)
        )

        if (selectedCount == 1) {
            CustomIconButton(
                iconRes = if (isStarred) R.drawable.chat_ic_long_star else R.drawable.home_ic_more_starred_message,
                contentDescription = strStar,
                tint = if (isStarred) colorResource(R.color.primary) else colorResource(R.color.text_title),
                onClick = { onStar() }
            )
            Spacer(modifier = Modifier.width(2.dp))
        }

        CustomIconButton(
            iconRes = R.drawable.longpress_ic_delete,
            contentDescription = strDelete,
            onClick = { onDelete() }
        )

        Spacer(modifier = Modifier.width(2.dp))

        CustomIconButton(
            iconRes = R.drawable.chat_ic_long_copy,
            contentDescription = strCopy,
            onClick = { onCopy() }
        )

        Spacer(modifier = Modifier.width(2.dp))

        Box {
            CustomIconButton(
                iconRes = R.drawable.home_ic_more,
                contentDescription = strMore,
                onClick = { moreMenuExpanded = true }
            )

            DropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false },
                offset = DpOffset(x = (-14).dp, y = 5.dp),
                modifier = Modifier.width(180.dp),
                shape = RoundedCornerShape(15.dp),
                containerColor = colorResource(R.color.menu_bg)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            strShare,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.chat_icon_secondary)
                        )
                    },
                    onClick = { moreMenuExpanded = false; onShare() },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.chat_ic_long_share),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colorResource(id = R.color.chat_icon_secondary)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            strForward,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.chat_icon_secondary)
                        )
                    },
                    onClick = { moreMenuExpanded = false; onForward() },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.chat_ic_long_forward),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colorResource(id = R.color.chat_icon_secondary)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                )
                if (selectedCount == 1) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                strViewDetails,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = colorResource(id = R.color.chat_icon_secondary)
                            )
                        },
                        onClick = { moreMenuExpanded = false; onDetails() },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.chat_ic_long_view_details),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = colorResource(id = R.color.chat_icon_secondary)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatSearchTopBar(
    query: String,
    currentMatch: Int,
    totalMatches: Int,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit
) {
    val strBack = stringResource(R.string.content_desc_back)
    val strSearchUp = stringResource(R.string.content_desc_search_up)
    val strSearchDown = stringResource(R.string.content_desc_search_down)
    val strClearSearch = stringResource(R.string.content_desc_clear_search)
    val strSearchResultsZero = stringResource(R.string.search_results_zero)
    val strSearchResultsCountTemplate = stringResource(R.string.search_results_count_template)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
//            .height(56.dp)
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 16.dp,
                bottom = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomIconButton(
            iconRes = R.drawable.archived_ic_back,
            contentDescription = strBack,
            onClick = onBack
        )

        Spacer(modifier = Modifier.width(8.dp))

        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = query,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Inter,
                color = colorResource(id = R.color.text_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (totalMatches == 0) strSearchResultsZero else String.format(strSearchResultsCountTemplate, currentMatch, totalMatches),
                fontSize = 12.sp,
                fontFamily = Inter,
                color = colorResource(id = R.color.text_des)
            )
        }

        CustomIconButton(
            iconRes = R.drawable.chat_ic_up,
            contentDescription = strSearchUp,
            onClick = onUp
        )
        CustomIconButton(
            iconRes = R.drawable.chat_ic_down,
            contentDescription = strSearchDown,
            onClick = onDown
        )
        CustomIconButton(
            iconRes = R.drawable.chat_ic_close,
            contentDescription = strClearSearch,
            onClick = onClose
        )
    }
}
