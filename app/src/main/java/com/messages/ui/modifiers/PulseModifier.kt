package com.messages.ui.modifiers

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.animatedPulse(
    pulseColor: Color,
    targetExpansionDp: Float = 12f,
    animationDuration: Int = 1500
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    
    val expansion by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = targetExpansionDp,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_expansion"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    this.drawBehind {
        val expansionPx = expansion.dp.toPx()
        drawRoundRect(
            color = pulseColor,
            alpha = alpha,
            topLeft = androidx.compose.ui.geometry.Offset(-expansionPx, -expansionPx),
            size = androidx.compose.ui.geometry.Size(size.width + 2 * expansionPx, size.height + 2 * expansionPx),
            cornerRadius = CornerRadius(size.height / 2 + expansionPx, size.height / 2 + expansionPx)
        )
    }
}
