package com.message.sms.texting.app.ui.components.dialogs

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.message.sms.texting.app.R
import com.message.sms.texting.app.ui.theme.Inter

/**
 * Server-controlled kill switch (extra_data_1_on_off == "on") â€” blocks the whole app. No close
 * button/outside-tap/back-press dismissal at all, unlike OfflineDialog: this reflects the last
 * confirmed state from the panel, which can be stale-but-still-"on" while offline, and should
 * keep blocking through that â€” only a fresh fetch that comes back "off" clears it (see
 * AppNavigation.kt, where this is driven directly off the cached/live AppConfigViewModel state
 * with no isOnline gating).
 */
@Composable
fun MaintenanceDialog(message: String?) {
    val context = LocalContext.current
    val strTitle = stringResource(R.string.maintenance_dialog_title)
    val strDefaultDesc = stringResource(R.string.maintenance_dialog_desc)
    val strExit = stringResource(R.string.maintenance_dialog_exit_button)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(colorResource(R.color.light_gray))
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 24.dp),
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
                    painter = painterResource(id = R.drawable.ic_maintenance),
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
                text = message?.takeIf { it.isNotBlank() } ?: strDefaultDesc,
                fontFamily = Inter,
                fontSize = 14.sp,
                color = colorResource(R.color.text_des),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { (context as? Activity)?.finishAffinity() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = strExit,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
