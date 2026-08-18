package com.messages.ui.screens

import com.messages.navigation.popBackStackWithAd

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.messages.R
import com.messages.ui.components.CustomIconButton
import com.messages.ui.theme.Inter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImageViewerScreen(
    navController: NavController,
    imagePath: String,
    name: String?,
    date: Long
) {
    var rotation by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = rotation, animationSpec = tween(250), label = "rotation")

    val formattedDateTime = remember(date) {
        if (date > 0) SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(date)) else ""
    }

    val strBack = stringResource(R.string.content_desc_back)
    val strPhotoFallback = stringResource(R.string.mms_photo_placeholder)
    val strRotateImage = stringResource(R.string.content_desc_rotate_image)
    val strImage = stringResource(R.string.content_desc_image)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomIconButton(
                    iconRes = R.drawable.archived_ic_back,
                    contentDescription = strBack,
                    tint = Color.White,
                    onClick = { navController.popBackStackWithAd() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = name ?: strPhotoFallback,
                        fontSize = 16.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (formattedDateTime.isNotEmpty()) {
                        Text(
                            text = formattedDateTime,
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { rotation -= 90f },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chat_ic_rotate),
                        contentDescription = strRotateImage,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = strImage,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(rotationZ = animatedRotation)
            )
        }
    }
}
