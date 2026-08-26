package com.message.sms.texting.app.ads

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Current height of the bottom banner ad on the Home screen, if any is showing (0.dp otherwise).
 * DashboardScreen's floating "new message" button reads this to sit above the banner instead of
 * being overlapped by it â€” the two composables aren't in the same subtree (Home's NavHost is
 * nested inside Dashboard's Scaffold), so this is a lightweight shared-state bridge between them,
 * matching the State-object pattern already used elsewhere in this app (ThemeState, LanguageState).
 */
object HomeBannerAdState {
    val heightDp = mutableStateOf(0.dp)

    fun update(height: Dp) {
        heightDp.value = height
    }

    fun clear() {
        heightDp.value = 0.dp
    }
}
