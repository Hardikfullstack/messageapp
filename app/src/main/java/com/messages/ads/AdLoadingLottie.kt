package com.messages.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.messages.R
import com.messages.ui.theme.Inter

/** Just the animation — [AdLoadingScreen] is the full-screen version most callers want. */
@Composable
fun AdLoadingLottie(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ad_loding))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

/**
 * Full-screen (edge to edge, covers status/nav bar area — no app bar) loading state shown while
 * we briefly wait for an App Open/Interstitial ad to finish loading (see [waitUntilAdReady]) —
 * the animation with "Ad is loading…" beneath it, matching how other apps show this moment.
 */
@Composable
fun AdLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.bg_primary)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-40).dp)
        ) {
            AdLoadingLottie(modifier = Modifier.size(280.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.ad_is_loading),
                color = colorResource(R.color.text_des),
                fontSize = 14.sp,
                fontFamily = Inter
            )
        }
    }
}
