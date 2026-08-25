package com.messages.sms.texting.app.ui.components.dialogs

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
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.ui.theme.Inter

/**
 * Shown when the user tries to turn OFF the After Call switch in Settings — the primary
 * ("Keep it") button intentionally stays enabled, "Proceed" (with disabling) is the secondary
 * one, nudging toward keeping the feature on rather than a neutral confirm/cancel pair.
 */
@Composable
fun DisableAfterCallDialog(
    onProceed: () -> Unit,
    onKeepIt: () -> Unit
) {
    val strTitle = stringResource(R.string.after_call_disable_dialog_title)
    val strDesc = stringResource(R.string.after_call_disable_dialog_desc)
    val strProceed = stringResource(R.string.action_proceed)
    val strKeepIt = stringResource(R.string.action_keep_it)

    Dialog(onDismissRequest = onKeepIt) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(colorResource(R.color.light_color_gray).copy(alpha = 0.25f))
                            .clickable(onClick = onProceed)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strProceed,
                            color = colorResource(R.color.text_title),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(colorResource(R.color.primary))
                            .clickable(onClick = onKeepIt)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strKeepIt,
                            color = Color.White,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
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
