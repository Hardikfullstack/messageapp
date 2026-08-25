package com.messages.sms.texting.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.messages.sms.texting.app.R

/** Icon shown for a given swipe-action key ("none" | "archive" | "delete" | "block" | "call" | "mark_read" | "mark_unread"). */
fun swipeActionIconFor(action: String): Int? = when (action) {
    "archive" -> R.drawable.home_ic_more_archived
    "delete" -> R.drawable.longpress_ic_delete
    "block" -> R.drawable.home_ic_more_block_list
    "call" -> R.drawable.settings_ic_aftercall
    "mark_read" -> R.drawable.longpress_ic_more_mark_read
    "mark_unread" -> R.drawable.longpress_ic_more_mark_unread
    else -> null
}

/** Brand color shown behind the icon for a given swipe-action key. */
@Composable
fun swipeActionColorFor(action: String): Color = when (action) {
    "archive" -> colorResource(R.color.primary)
    "delete" -> Color.Red
    "block" -> Color(0xFFD84315)
    "call" -> Color(0xFF43A047)
    "mark_read", "mark_unread" -> colorResource(R.color.primary)
    else -> colorResource(R.color.light_color_gray)
}
