package com.messages.sms.texting.app.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.ui.components.CustomIconButton
import com.messages.sms.texting.app.ui.theme.Inter

@Composable
fun DeleteOldMessagesDialog(
    currentDays: Int,
    onDismiss: () -> Unit,
    onNever: () -> Unit,
    onSave: (Int) -> Unit
) {
    var daysText by remember { mutableStateOf(if (currentDays > 0) currentDays.toString() else "") }
    val selectedDays = daysText.toIntOrNull()

    val strTitle = stringResource(R.string.delete_old_messages_label)
    val strDesc = stringResource(R.string.delete_old_messages_dialog_desc)
    val strDaily = stringResource(R.string.delete_old_messages_daily)
    val strWeekly = stringResource(R.string.delete_old_messages_weekly)
    val strMonthly = stringResource(R.string.delete_old_messages_monthly)
    val strYearly = stringResource(R.string.delete_old_messages_yearly)
    val strDaysFieldLabel = stringResource(R.string.delete_old_messages_days_field_label)
    val strDaysPlaceholder = stringResource(R.string.delete_old_messages_days_placeholder)
    val strClose = stringResource(R.string.content_desc_close)
    val strCancel = stringResource(R.string.action_cancel_caps)
    val strNever = stringResource(R.string.setting_never)
    val strSave = stringResource(R.string.action_save)

    val presets = listOf(1 to strDaily, 7 to strWeekly, 30 to strMonthly, 365 to strYearly)

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
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strTitle,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = colorResource(R.color.text_title),
                        modifier = Modifier.weight(1f)
                    )
                    CustomIconButton(
                        iconRes = R.drawable.chat_ic_close,
                        contentDescription = strClose,
                        modifier = Modifier.offset(x = 4.dp),
                        onClick = onDismiss
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strDesc,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    color = colorResource(R.color.text_des)
                )

                Spacer(modifier = Modifier.height(4.dp))

                presets.forEach { (days, label) ->
                    CompactOptionRow(
                        title = label,
                        selected = selectedDays == days,
                        onClick = { daysText = days.toString() }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strDaysFieldLabel,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    color = colorResource(R.color.light_color_gray)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorResource(R.color.bg_primary))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (daysText.isEmpty()) {
                        Text(
                            text = strDaysPlaceholder,
                            fontFamily = Inter,
                            fontSize = 16.sp,
                            color = colorResource(R.color.text_des)
                        )
                    }
                    val customTextSelectionColors = TextSelectionColors(
                        handleColor = colorResource(R.color.primary),
                        backgroundColor = colorResource(R.color.primary).copy(alpha = 0.4f)
                    )
                    CompositionLocalProvider(
                        LocalTextSelectionColors provides customTextSelectionColors
                    ) {
                        BasicTextField(
                            value = daysText,
                            onValueChange = { new ->
                                if (new.length <= 4 && new.all { it.isDigit() }) daysText = new
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = Inter,
                                fontSize = 16.sp,
                                color = colorResource(R.color.text_title),
                                textAlign = TextAlign.Start
                            ),
                            cursorBrush = SolidColor(colorResource(R.color.primary)),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = strCancel,
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.light_color_gray),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onNever) {
                        Text(
                            text = strNever.uppercase(),
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.primary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = { selectedDays?.let { if (it > 0) onSave(it) } },
                        enabled = selectedDays != null && selectedDays > 0
                    ) {
                        Text(
                            text = strSave,
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.primary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontFamily = Inter,
            fontSize = 16.sp,
            color = if (selected) colorResource(R.color.text_title) else colorResource(R.color.text_des)
        )
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(R.color.primary),
                unselectedColor = Color.LightGray
            ),
            modifier = Modifier.size(20.dp)
        )
    }
}
