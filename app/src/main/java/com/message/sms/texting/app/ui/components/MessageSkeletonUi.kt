package com.message.sms.texting.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.message.sms.texting.app.R

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    return this.background(colorResource(R.color.light_color_gray).copy(alpha = alpha))
}

@Composable
fun MessageSkeletonUi() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar skeleton
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.width(16.dp))

        Row(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time and Indicator
            Column(horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier.width(40.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).shimmerEffect())
            }
        }
    }
}

/** Generic paragraph-shaped skeleton â€” used while a WebView (Privacy Policy/Terms) loads. */
@Composable
fun TextSkeletonItem() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(0.92f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
    }
}

@Composable
fun ChatBubbleSkeletonUi(isSent: Boolean) {
    val align = if (isSent) Alignment.End else Alignment.Start
    val widthFraction = if (isSent) 0.5f else 0.7f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(50.dp)
                .clip(RoundedCornerShape(20))
                .shimmerEffect()
        )
    }
}
