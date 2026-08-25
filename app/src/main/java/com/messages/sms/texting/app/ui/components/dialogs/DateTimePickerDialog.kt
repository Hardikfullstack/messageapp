package com.messages.sms.texting.app.ui.components.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.messages.sms.texting.app.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    onDateTimeSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val strSelectTime = stringResource(R.string.select_time_title)
    val strSwitchKeyboardInput = stringResource(R.string.content_desc_switch_keyboard_input)
    val strSwitchDialInput = stringResource(R.string.content_desc_switch_dial_input)
    val strCancel = stringResource(R.string.action_cancel)
    val strOk = stringResource(R.string.action_ok)
    val strScheduledTimeFuture = stringResource(R.string.toast_scheduled_time_future)

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    // We need to keep track of whether the dialog was explicitly cancelled
    var isCancelled by remember { mutableStateOf(false) }

    if (!showTimePicker && !isCancelled) {
        DisposableEffect(Unit) {
            val datePickerDialog = DatePickerDialog(
                context,
                R.style.CustomPickerDialogStyle,
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = dayOfMonth
                    showTimePicker = true
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.datePicker.firstDayOfWeek = Calendar.MONDAY
            datePickerDialog.setCanceledOnTouchOutside(false)

            datePickerDialog.setOnShowListener { dialog ->
                val d = dialog as DatePickerDialog
                val positiveBtn = d.getButton(DatePickerDialog.BUTTON_POSITIVE)
                val negativeBtn = d.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                
                // Set custom extra bold typeface
                val extraBoldTypeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter_extrabold)
                positiveBtn.typeface = extraBoldTypeface
                negativeBtn.typeface = extraBoldTypeface

                // Disable all caps so it says "Cancel" instead of "CANCEL"
                positiveBtn.isAllCaps = false
                negativeBtn.isAllCaps = false
            }

            datePickerDialog.setOnCancelListener {
                isCancelled = true
                onDismissRequest()
            }

            datePickerDialog.show()
            onDispose { datePickerDialog.dismiss() }
        }
    } else if (showTimePicker && !isCancelled) {
        val isDark = isSystemInDarkTheme()
        val pickerColors = if (isDark) {
            darkColorScheme(
                primary = colorResource(R.color.picker_primary),
                onPrimary = Color.White,
                surface = colorResource(R.color.bg_primary),
                onSurface = colorResource(R.color.text_title),
                surfaceVariant = colorResource(R.color.light_gray),
                onSurfaceVariant = colorResource(R.color.text_title)
            )
        } else {
            lightColorScheme(
                primary = colorResource(R.color.picker_primary),
                onPrimary = Color.White,
                surface = Color.White,
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFE0E0E0),
                onSurfaceVariant = Color.Black
            )
        }

        val customTypography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )

        MaterialTheme(
            colorScheme = pickerColors,
            typography = customTypography
        ) {
            val timePickerState = rememberTimePickerState(
                initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = calendar.get(Calendar.MINUTE),
                is24Hour = false
            )

            // NEW: State to track if we are showing the Dial (TimePicker) or Keyboard (TimeInput)
            var isDialMode by remember { mutableStateOf(true) }

            Dialog(
                onDismissRequest = {
                    isCancelled = true
                    onDismissRequest()
                },
                properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colorResource(R.color.bg_primary),
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    Column(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strSelectTime,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorResource(R.color.text_des)
                        )

                        // NEW: Toggle between TimePicker and TimeInput based on state
                        if (isDialMode) {
                            TimePicker(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors(
                                    selectorColor = colorResource(R.color.picker_primary),
                                    periodSelectorSelectedContainerColor = colorResource(R.color.picker_primary).copy(alpha = 0.2f),
                                    periodSelectorSelectedContentColor = colorResource(R.color.picker_primary),
                                    timeSelectorSelectedContainerColor = colorResource(R.color.picker_primary).copy(alpha = 0.2f),
                                    timeSelectorSelectedContentColor = colorResource(R.color.picker_primary)
                                )
                            )
                        } else {
                            TimeInput(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors(
                                    periodSelectorSelectedContainerColor = colorResource(R.color.picker_primary).copy(alpha = 0.2f),
                                    periodSelectorSelectedContentColor = colorResource(R.color.picker_primary),
                                    timeSelectorSelectedContainerColor = colorResource(R.color.picker_primary).copy(alpha = 0.2f),
                                    timeSelectorSelectedContentColor = colorResource(R.color.picker_primary)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // NEW: Toggle the mode on click and swap the icon
                            IconButton(onClick = { isDialMode = !isDialMode }) {
                                if (isDialMode) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.contact_ic_keyboard),
                                        contentDescription = strSwitchKeyboardInput,
                                        tint = colorResource(R.color.text_des)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_schedule_clock),
                                        contentDescription = strSwitchDialInput,
                                        tint = colorResource(R.color.text_des)
                                    )
                                }
                            }

                            Row {
                                TextButton(
                                    onClick = {
                                        isCancelled = true
                                        onDismissRequest()
                                    }
                                ) {
                                    Text(strCancel, fontWeight = FontWeight.ExtraBold, color = colorResource(R.color.picker_primary))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        val selectedCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, selectedYear)
                                            set(Calendar.MONTH, selectedMonth)
                                            set(Calendar.DAY_OF_MONTH, selectedDay)
                                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                            set(Calendar.MINUTE, timePickerState.minute)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        
                                        if (selectedCal.timeInMillis > System.currentTimeMillis()) {
                                            onDateTimeSelected(selectedCal.timeInMillis)
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                strScheduledTimeFuture,
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                ) {
                                    Text(strOk, fontWeight = FontWeight.ExtraBold, color = colorResource(R.color.picker_primary))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}