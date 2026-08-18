package com.messages.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.messages.R

@Composable
fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) colorResource(R.color.primary) else Color(0xFF8E8E93),
        animationSpec = tween(durationMillis = 200)
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = tween(durationMillis = 200)
    )

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
