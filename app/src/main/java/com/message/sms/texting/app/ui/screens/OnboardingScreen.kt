package com.message.sms.texting.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter

/** The welcome copy that used to live here (OnboardingScreen composable) is now merged into
 * DefaultSmsScreen.kt -- one welcome screen with a single "set as default SMS app" button,
 * matching the recommended flow. This file now only holds PermissionSettingsDialog, still used
 * separately by DashboardScreen.kt for its in-context permission prompts. */
@Composable
fun PermissionSettingsDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    // Defaults preserve the original Phone/Call-Log wording used during onboarding; the Dashboard's
    // notification-permission dialog passes "Notification" instead so the steps stay accurate.
    permissionDescLabel: String = stringResource(R.string.permission_label_phone),
    permissionStepLabel: String = stringResource(R.string.permission_step_label_phone),
    // Off for SMS (the app can't work without it, so it's intentionally not closeable any way but
    // granting the permission) and on for Notification (a soft nudge the user can dismiss).
    showCloseIcon: Boolean = false
) {
    val strPermissionNeededTitle = stringResource(R.string.permission_needed_title)
    val strPermissionNeededDesc = String.format(stringResource(R.string.permission_needed_desc), permissionDescLabel)
    val strPermissionStep1 = stringResource(R.string.permission_step_1)
    val strPermissionStep2 = stringResource(R.string.permission_step_2)
    val strPermissionStep3 = String.format(stringResource(R.string.permission_step_3), permissionStepLabel)
    val strPermissionStep4 = stringResource(R.string.permission_step_4)
    val strActionOpenSettings = stringResource(R.string.action_open_settings)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strPermissionNeededTitle,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.text_title)
                )
                if (showCloseIcon) {
                    Icon(
                        painter = painterResource(id = R.drawable.chat_ic_close),
                        contentDescription = stringResource(R.string.content_desc_dismiss),
                        tint = colorResource(R.color.text_des),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onDismiss)
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = strPermissionNeededDesc,
                    color = colorResource(R.color.text_des),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                val steps = listOf(
                    strPermissionStep1,
                    strPermissionStep2,
                    strPermissionStep3,
                    strPermissionStep4
                )
                steps.forEach { step ->
                    Text(
                        text = step,
                        color = colorResource(R.color.text_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary)
                )
            ) {
                Text(
                    strActionOpenSettings,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Inter,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = colorResource(R.color.bg_primary)
    )
}
