package com.messages.sms.texting.app.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.ui.theme.Inter

/**
 * Star count is reported to the caller on [onRateClick] — this composable has no opinion on what
 * 1-3 vs 4-5 stars should trigger (email feedback vs in-app review), that routing lives in
 * SettingsScreen since it needs an Activity reference for the Play in-app review flow.
 */
@Composable
fun RateUsDialog(
    onRateClick: (stars: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(0) }
    val iconSize = 100.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .padding(top = iconSize / 2)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colorResource(R.color.light_gray))
                    .padding(horizontal = 20.dp)
                    .padding(top = iconSize / 2 + 12.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.rate_us_title),
                    fontFamily = Inter,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_title)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.rate_us_subtitle),
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_des),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (star in 1..5) {
                        IconButton(
                            onClick = { selectedStars = if (selectedStars == star) 0 else star },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (star <= selectedStars) {
                                        R.drawable.settings_ic_rate_us_star_filled
                                    } else {
                                        R.drawable.settings_ic_rate_us_star
                                    }
                                ),
                                contentDescription = null,
                                tint = if (star <= selectedStars) Color(0xFFEDD712) else Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onRateClick(selectedStars) },
                    enabled = selectedStars > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.primary),
                        disabledContainerColor = colorResource(R.color.primary).copy(alpha = 0.35f),
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = if (selectedStars in 1..3) {
                            stringResource(R.string.rate_us_button_low)
                        } else {
                            stringResource(R.string.rate_us_button)
                        },
                        fontFamily = Inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.rate_us_maybe_later),
                        fontFamily = Inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.light_color_gray),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

            }

            Icon(
                painter = painterResource(id = R.drawable.settings_ic_rate_us_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .offset(y = 14.dp),
                tint = Color.Unspecified
            )
        }
    }
}
