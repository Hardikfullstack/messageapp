package com.messages.sms.texting.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ripple
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.messages.sms.texting.app.R

@Composable
fun CustomIconButton(
    iconRes: Int,
    modifier: Modifier = Modifier,
    size: Int = 24,
    tint: Color = colorResource(R.color.text_title),
    onClick: () -> Unit,
    contentDescription: String? = null
) {
    val rippleColor = colorResource(R.color.text_title)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = rippleColor),
                onClick = onClick
            )
            .padding(8.dp), // Add padding for touch target size
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            modifier = Modifier.size(size.dp),
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Composable
fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.light_gray))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = colorResource(R.color.text_title)),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.chat_ic_up),
                contentDescription = contentDescription,
                tint = colorResource(R.color.text_title),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotationDegrees)
            )
        }
    }
}
