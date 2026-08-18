package com.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.messages.R
import com.messages.ui.theme.Inter
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme

enum class FilterType { DEFAULT, TODAY, MONTH, YEAR, DATE_RANGE }

data class FilterConfig(
    val type: FilterType = FilterType.DEFAULT,
    val month: Int? = null, // 0-11
    val year: Int? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentFilter: FilterConfig,
    onApply: (FilterConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(currentFilter) }

    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val strFilterTitle = stringResource(R.string.filter_title)
    val strClose = stringResource(R.string.content_desc_close)
    val strFilterDefault = stringResource(R.string.filter_default)
    val strFilterToday = stringResource(R.string.filter_today)
    val strFilterMonth = stringResource(R.string.filter_month)
    val strFilterYear = stringResource(R.string.filter_year)
    val strFilterDateRange = stringResource(R.string.filter_date_range)
    val strActionReset = stringResource(R.string.action_reset)
    val strActionApply = stringResource(R.string.action_apply)
    val strActionOk = stringResource(R.string.action_ok)
    val strActionCancel = stringResource(R.string.action_cancel)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(23.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strFilterTitle,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = colorResource(R.color.text_title)
                    )
                    CustomIconButton(
                        iconRes = R.drawable.chat_ic_close,
                        contentDescription = strClose,
                        onClick = { onDismiss()  }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Default
                FilterOptionRow(
                    title = strFilterDefault,
                    selected = selectedFilter.type == FilterType.DEFAULT,
                    onClick = { selectedFilter = FilterConfig(FilterType.DEFAULT) }
                )

                // Today
                FilterOptionRow(
                    title = strFilterToday,
                    selected = selectedFilter.type == FilterType.TODAY,
                    onClick = { selectedFilter = FilterConfig(FilterType.TODAY) }
                )

                // Month
                val monthName = selectedFilter.month?.let {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.MONTH, it)
                    SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
                }
                FilterOptionRow(
                    title = strFilterMonth,
                    subtitle = if (selectedFilter.type == FilterType.MONTH) monthName else null,
                    selected = selectedFilter.type == FilterType.MONTH,
                    onClick = { showMonthPicker = true }
                )

                // Year
                FilterOptionRow(
                    title = strFilterYear,
                    subtitle = if (selectedFilter.type == FilterType.YEAR && selectedFilter.year != null) selectedFilter.year.toString() else null,
                    selected = selectedFilter.type == FilterType.YEAR,
                    onClick = { showYearPicker = true }
                )

                // Date Range
                val rangeText = if (selectedFilter.type == FilterType.DATE_RANGE && selectedFilter.startDate != null && selectedFilter.endDate != null) {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    "${sdf.format(Date(selectedFilter.startDate!!))} - ${sdf.format(Date(selectedFilter.endDate!!))}"
                } else null
                
                FilterOptionRow(
                    title = strFilterDateRange,
                    subtitle = rangeText,
                    selected = selectedFilter.type == FilterType.DATE_RANGE,
                    onClick = { showDateRangePicker = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { 
                        val defaultFilter = FilterConfig(FilterType.DEFAULT)
                        selectedFilter = defaultFilter
                        onApply(defaultFilter)
                    }) {
                        Text(
                            text = strActionReset,
                            fontFamily = Inter,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.light_color_gray)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onApply(selectedFilter) }) {
                        Text(
                            text = strActionApply,
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

    if (showMonthPicker) {
        MonthPickerDialog(
            currentMonth = selectedFilter.month ?: Calendar.getInstance().get(Calendar.MONTH),
            onMonthSelected = { month ->
                selectedFilter = FilterConfig(type = FilterType.MONTH, month = month)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    if (showYearPicker) {
        YearPickerDialog(
            currentYear = selectedFilter.year ?: Calendar.getInstance().get(Calendar.YEAR),
            onYearSelected = { year ->
                selectedFilter = FilterConfig(type = FilterType.YEAR, year = year)
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    if (showDateRangePicker) {
        val isDark = isSystemInDarkTheme()
        val dateRangeColors = if (isDark) {
            darkColorScheme(
                primary = colorResource(R.color.picker_primary),
                surface = colorResource(R.color.bg_primary),
                onSurface = colorResource(R.color.text_title),
                onSurfaceVariant = colorResource(R.color.text_des)
            )
        } else {
            lightColorScheme(
                primary = colorResource(R.color.picker_primary),
                surface = Color.White,
                onSurface = Color.Black,
                onSurfaceVariant = Color.DarkGray
            )
        }
        MaterialTheme(
            colorScheme = dateRangeColors
        ) {
            val dateRangePickerState = rememberDateRangePickerState(
                initialSelectedStartDateMillis = selectedFilter.startDate,
                initialSelectedEndDateMillis = selectedFilter.endDate
            )
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        if (dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null) {
                            selectedFilter = FilterConfig(
                                type = FilterType.DATE_RANGE,
                                startDate = dateRangePickerState.selectedStartDateMillis,
                                endDate = dateRangePickerState.selectedEndDateMillis
                            )
                        }
                        showDateRangePicker = false
                    }) {
                        Text(
                            text = strActionOk,
                            color = colorResource(R.color.picker_primary),
                            fontFamily = Inter,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text(
                            text = strActionCancel,
                            color = colorResource(R.color.picker_primary),
                            fontFamily = Inter,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    title = { },
                    modifier = Modifier.weight(1f),
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = colorResource(R.color.picker_primary),
                        selectedDayContentColor = Color.White,
                        selectedYearContainerColor = colorResource(R.color.picker_primary),
                        selectedYearContentColor = Color.White,
                        todayContentColor = colorResource(R.color.picker_primary),
                        todayDateBorderColor = colorResource(R.color.picker_primary),
                        dayInSelectionRangeContainerColor = colorResource(R.color.picker_primary).copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@Composable
fun FilterOptionRow(title: String, subtitle: String? = null, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Inter,
                fontSize = 16.sp,
                color = if (selected) colorResource(R.color.text_title) else colorResource(R.color.text_des)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    color = colorResource(R.color.light_color_gray),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        RadioButton(
            selected = selected,
            onClick = null, // handled by row click
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(R.color.primary),
                unselectedColor = Color.LightGray
            )
        )
    }
}

@Composable
fun MonthPickerDialog(currentMonth: Int, onMonthSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    val months = listOf(
        stringResource(R.string.month_january),
        stringResource(R.string.month_february),
        stringResource(R.string.month_march),
        stringResource(R.string.month_april),
        stringResource(R.string.month_may),
        stringResource(R.string.month_june),
        stringResource(R.string.month_july),
        stringResource(R.string.month_august),
        stringResource(R.string.month_september),
        stringResource(R.string.month_october),
        stringResource(R.string.month_november),
        stringResource(R.string.month_december)
    )
    val strSelectMonthTitle = stringResource(R.string.select_month_title)
    val strClose = stringResource(R.string.content_desc_close)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(23.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strSelectMonthTitle,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = colorResource(R.color.text_title)
                    )
                    CustomIconButton(
                        iconRes = R.drawable.chat_ic_close,
                        contentDescription = strClose,
                        onClick = { onDismiss()  }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                    items(12) { index ->
                        FilterOptionRow(
                            title = months[index],
                            selected = index == currentMonth,
                            onClick = { onMonthSelected(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YearPickerDialog(currentYear: Int, onYearSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    val currentYearInt = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYearInt downTo 1990).toList()
    val strSelectYearTitle = stringResource(R.string.select_year_title)
    val strClose = stringResource(R.string.content_desc_close)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(23.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strSelectYearTitle,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = colorResource(R.color.text_title)
                    )
                    CustomIconButton(
                        iconRes = R.drawable.chat_ic_close,
                        contentDescription = strClose,
                        modifier = Modifier.offset(x = 4.dp),
                        onClick = { onDismiss()  }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                    items(years) { year ->
                        FilterOptionRow(
                            title = year.toString(),
                            selected = year == currentYear,
                            onClick = { onYearSelected(year) }
                        )
                    }
                }
            }
        }
    }
}
