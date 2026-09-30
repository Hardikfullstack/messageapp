package com.message.sms.texting.app.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter

/** Shown from the After Call promo card's flow on MIUI devices -- after Overlay is granted and
 * the user then returns from MIUI's own Autostart manager screen with it still not granted, this
 * nudges them back into it instead of silently leaving the feature half-working. Same two-pill
 * layout as [DisableAfterCallDialog] for visual consistency with the rest of the app's dialogs. */
@Composable
fun AutostartNudgeDialog(
    onGrant: () -> Unit,
    onNotNow: () -> Unit
) {
    val strTitle = stringResource(R.string.after_call_autostart_popup_title)
    val strDesc = stringResource(R.string.after_call_autostart_popup_desc)
    val strGrant = stringResource(R.string.action_grant_autostart_permission)
    val strNotNow = stringResource(R.string.action_not_now)

    Dialog(onDismissRequest = onNotNow) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = strTitle,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = colorResource(R.color.text_title)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strDesc,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colorResource(R.color.text_des)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(colorResource(R.color.primary))
                            .clickable(onClick = onGrant)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strGrant,
                            color = Color.White,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(colorResource(R.color.light_color_gray).copy(alpha = 0.25f))
                            .clickable(onClick = onNotNow)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strNotNow,
                            color = colorResource(R.color.text_title),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
