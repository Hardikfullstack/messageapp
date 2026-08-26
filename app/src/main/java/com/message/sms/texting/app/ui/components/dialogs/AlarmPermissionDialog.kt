package com.message.sms.texting.app.ui.components.dialogs

import androidx.compose.foundation.background
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
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter

@Composable
fun AlarmPermissionDialog(
    onAllowClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val strAlarmPermissionTitle = stringResource(R.string.alarm_permission_title)
    val strAlarmPermissionDesc = stringResource(R.string.alarm_permission_desc)
    val strAllowPermission = stringResource(R.string.action_allow_permission)
    val strNotNow = stringResource(R.string.action_not_now)

    Dialog(onDismissRequest = onDismissClick) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(colorResource(R.color.light_gray))
                .padding(horizontal = 20.dp).padding(top = 15.dp , bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.scheduled_ic_alarm),
                contentDescription = null,
                modifier = Modifier.size(90.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strAlarmPermissionTitle,
                fontFamily = Inter,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.text_title)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strAlarmPermissionDesc,
                fontFamily = Inter,
                fontSize = 14.sp,
                color = colorResource(R.color.text_des),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAllowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = strAllowPermission,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            TextButton(
                onClick = onDismissClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strNotNow,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorResource(R.color.text_des)
                )
            }
        }
    }
}
