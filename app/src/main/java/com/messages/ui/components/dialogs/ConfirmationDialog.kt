package com.messages.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.messages.R
import com.messages.ui.theme.Inter

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.action_delete),
    dismissText: String = stringResource(R.string.action_cancel_caps),
    confirmTextColor: Color = colorResource(R.color.primary),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    title, 
                    fontFamily = Inter, 
                    fontWeight = FontWeight.Medium, 
                    fontSize = 22.sp,
                    color = colorResource(R.color.text_title)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text, 
                    fontFamily = Inter, 
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_des)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            dismissText, 
                            color = colorResource(R.color.light_color_gray),
                            fontFamily = Inter, 
                            fontWeight = FontWeight.Medium, 
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text(
                            confirmText, 
                            color = confirmTextColor, 
                            fontFamily = Inter, 
                            fontWeight = FontWeight.Medium, 
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
