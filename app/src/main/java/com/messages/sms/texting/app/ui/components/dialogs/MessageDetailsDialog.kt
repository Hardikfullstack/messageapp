package com.messages.sms.texting.app.ui.components.dialogs

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
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.model.SmsMessage
import com.messages.sms.texting.app.ui.theme.Inter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageDetailsDialog(
    msg: SmsMessage,
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
            val strMessageDetailsTitle = stringResource(R.string.message_details_title)
            val strDirectionFrom = stringResource(R.string.direction_from)
            val strDirectionTo = stringResource(R.string.direction_to)
            val strTypeSmsLabel = stringResource(R.string.type_sms_label)
            val strLabelValueTemplate = stringResource(R.string.label_value_template)
            val strReceivedLabel = stringResource(R.string.received_label)
            val strSentLabel = stringResource(R.string.sent_label)
            val strActionOk = stringResource(R.string.action_ok)

            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = strMessageDetailsTitle,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    color = colorResource(R.color.text_title)
                )
                Spacer(modifier = Modifier.height(16.dp))

                val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
                val dateString = dateFormat.format(Date(msg.date))
                val directionText = if (msg.type == 1) strDirectionFrom else strDirectionTo

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(strTypeSmsLabel, fontSize = 16.sp, color = colorResource(R.color.option_row_text), fontFamily = Inter)
                    Text(String.format(strLabelValueTemplate, directionText, msg.address), fontSize = 16.sp, color = colorResource(R.color.option_row_text), fontFamily = Inter)
                    Text(String.format(strLabelValueTemplate, if (msg.type == 1) strReceivedLabel else strSentLabel, dateString), fontSize = 16.sp, color = colorResource(R.color.option_row_text), fontFamily = Inter)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            strActionOk,
                            color = colorResource(R.color.primary),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
