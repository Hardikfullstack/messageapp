package com.message.sms.texting.app.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter

/**
 * [onCancelClick] null = mandatory update â€” no "Later" option, can't be dismissed by outside-tap
 * or back-press either. Non-null = soft/optional update, dismissible via the "Later" button.
 */
@Composable
fun UpdateAppDialog(
    title: String,
    description: String,
    onOkClick: () -> Unit,
    onCancelClick: (() -> Unit)? = null
) {
    val strUpdateButton = stringResource(R.string.update_button)
    val strLater = stringResource(R.string.update_later)

    Dialog(
        onDismissRequest = { onCancelClick?.invoke() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = onCancelClick != null
        )
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(colorResource(R.color.light_gray))
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontFamily = Inter,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(R.color.text_title),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                fontFamily = Inter,
                fontSize = 15.sp,
                color = colorResource(R.color.text_des),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onOkClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = strUpdateButton,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onCancelClick != null) {
                Spacer(modifier = Modifier.height(2.dp))
                TextButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strLater,
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = colorResource(R.color.text_des),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
