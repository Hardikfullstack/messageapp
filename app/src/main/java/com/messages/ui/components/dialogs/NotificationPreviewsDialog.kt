package com.messages.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.messages.R
import com.messages.ui.components.CustomIconButton
import com.messages.ui.components.FilterOptionRow
import com.messages.ui.theme.Inter

/** A wider RadioOptionDialog variant with a smaller, single-line-ellipsized title — used for Notification Previews (global and per-contact). */
@Composable
fun NotificationPreviewsDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selected by remember(selectedOption) { mutableStateOf(selectedOption) }
    val strClose = stringResource(R.string.content_desc_close)
    val strCancel = stringResource(R.string.action_cancel_caps)
    val strOk = stringResource(R.string.action_ok)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(23.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = colorResource(R.color.text_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    CustomIconButton(
                        iconRes = R.drawable.chat_ic_close,
                        contentDescription = strClose,
                        modifier = Modifier.offset(x = 4.dp),
                        onClick = onDismiss
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                options.forEach { option ->
                    FilterOptionRow(
                        title = option,
                        selected = selected == option,
                        onClick = { selected = option }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = strCancel,
                            fontFamily = Inter,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.light_color_gray)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(selected) }) {
                        Text(
                            text = strOk,
                            fontFamily = Inter,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.primary)
                        )
                    }
                }
            }
        }
    }
}
