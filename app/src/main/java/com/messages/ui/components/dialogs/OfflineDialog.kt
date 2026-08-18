package com.messages.ui.components.dialogs

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.messages.R
import com.messages.ads.AppOpenBackgroundReturnTrigger
import com.messages.ui.theme.Inter

/**
 * Shown when connectivity drops — not blocking texting itself (this is an SMS app, that still
 * works without internet), but only dismissible via the explicit close icon, not outside-tap or
 * back-press, so it doesn't disappear before the user notices it.
 */
@Composable
fun OfflineDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strTitle = stringResource(R.string.offline_dialog_title)
    val strDesc = stringResource(R.string.offline_dialog_desc)
    val strButton = stringResource(R.string.offline_dialog_button)
    val strClose = stringResource(R.string.content_desc_close)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(colorResource(R.color.light_gray))
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(colorResource(R.color.primary)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_no_internet),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strTitle,
                    fontFamily = Inter,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.text_title)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strDesc,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    color = colorResource(R.color.text_des),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        // The Wi-Fi Settings round trip backgrounds and re-foregrounds this
                        // Activity — without this, that return would look like a normal
                        // app-switch-back and could trigger an App Open ad right as the user is
                        // just trying to fix their connection.
                        AppOpenBackgroundReturnTrigger.isAdPaused = true
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = strButton,
                        fontFamily = Inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Icon(
                painter = painterResource(id = R.drawable.chat_ic_close),
                contentDescription = strClose,
                tint = colorResource(R.color.text_des),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(14.dp)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}
