package com.messages.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messages.R
import com.messages.ui.theme.Inter

@Composable
fun AfterCallCard(
    callInfoLine1: String,
    callInfoLine2: String,
    onMessageClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val strSend = stringResource(R.string.content_desc_send)
    val strCall = stringResource(R.string.content_desc_call)
    val strPrivateNumber = stringResource(R.string.after_call_private_number)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.bg_primary))
                    .padding(start = 74.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strPrivateNumber,
                    fontSize = 16.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colorResource(R.color.primary))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMessageClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_msg),
                        contentDescription = strSend,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.light_gray))
                    .padding(start = 74.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = callInfoLine1,
                        fontSize = 12.sp,
                        fontFamily = Inter,
                        color =  colorResource(R.color.text_des),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = callInfoLine2,
                        fontSize = 12.sp,
                        fontFamily = Inter,
                        color = colorResource(R.color.text_des),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCallClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chat_ic_phone),
                        contentDescription = strCall,
                        tint = colorResource(R.color.call_green),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.light_color_gray)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.home_ic_filled_person),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
