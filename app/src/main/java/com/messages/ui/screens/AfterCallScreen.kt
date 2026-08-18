package com.messages.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Telephony
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.messages.R
import com.messages.ads.NativeAdTemplate
import com.messages.ads.NativeAdView
import com.messages.repository.SmsRepository
import com.messages.ui.components.AfterCallCard
import com.messages.ui.components.AfterCallTabBar
import com.messages.ui.components.InlineDateTimePicker
import com.messages.ui.theme.Inter
import com.messages.viewmodel.AppConfigViewModel
import com.messages.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

/**
 * Chat navigation is abstracted behind [onOpenChat] instead of a direct NavController call,
 * since this screen is hosted in two very different places: inside the app's own NavHost (the
 * "Test After Call" trigger) and inside a bare WindowManager overlay window (the real after-call
 * flow) which has no ActivityResultRegistry — composing the real ChatScreen directly inside that
 * overlay crashes (its attachment button uses rememberLauncherForActivityResult), so the overlay
 * caller instead dismisses itself and launches MainActivity to the chat. [onFinish] is called
 * when the screen's job is done (a quick reply was sent) so the caller can pop back / dismiss.
 */
@Composable
fun AfterCallScreen(
    address: String,
    displayName: String?,
    isKnownContact: Boolean,
    callInfoLine1: String,
    callInfoLine2: String,
    onOpenChat: (threadId: Long, address: String, contactName: String?, forwardText: String?) -> Unit,
    onFinish: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    fun openChat() {
        val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
        onOpenChat(threadId, address, if (isKnownContact) displayName else null, null)
    }

    // Not viewModel(context as ComponentActivity) — this screen is also hosted inside a raw
    // WindowManager overlay window (see the doc comment above) where context isn't a
    // ComponentActivity at all. Plain viewModel() resolves via OverlayLifecycleOwner's
    // view-tree-attached ViewModelStoreOwner there, and via the host Activity everywhere else.
    val appConfigViewModel: AppConfigViewModel = viewModel()
    val adConfig by appConfigViewModel.appResponse.collectAsState()
    val nativeAdUnitId = adConfig?.result?.let { result ->
        if (result.google_ads_on_off == "on" && result.native_7_on_off == "on") {
            result.native_7?.takeIf { it.isNotBlank() }
        } else null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                AfterCallCard(
                    callInfoLine1 = callInfoLine1,
                    callInfoLine2 = callInfoLine2,
                    onMessageClick = { openChat() },
                    onCallClick = { initiateCall(context, address) }
                )
                AfterCallTabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            }
        },
        bottomBar = {
            if (nativeAdUnitId != null) {
                NativeAdView(
                    adUnitId = nativeAdUnitId,
                    template = NativeAdTemplate.MEDIUM,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "afterCallTabContent",
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(animationSpec = tween(250)) { width -> if (forward) width else -width } + fadeIn(animationSpec = tween(250)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(250)) { width -> if (forward) -width else width } + fadeOut(animationSpec = tween(250)))
            }
        ) { tab ->
            when (tab) {
                0 -> AfterCallMessageTab(onOpenChat = onOpenChat)
                1 -> AfterCallQuickReplyTab(
                    address = address,
                    displayName = displayName,
                    onOpenChat = onOpenChat
                )
                2 -> AfterCallReminderTab(address = address, displayName = displayName)
                3 -> AfterCallMoreTab(
                    address = address,
                    displayName = displayName,
                    onOpenChat = { openChat() }
                )
            }
        }
    }
}

/**
 * Hosts AfterCallScreen inside a WindowManager overlay window. Opening a chat dismisses the
 * overlay and launches MainActivity instead of composing ChatScreen in-place — see the
 * AfterCallScreen doc comment for why.
 */
@Composable
fun AfterCallOverlayRoot(
    address: String,
    displayName: String?,
    isKnownContact: Boolean,
    callInfoLine1: String,
    callInfoLine2: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    BackHandler(enabled = true) { onDismiss() }

    AfterCallScreen(
        address = address,
        displayName = displayName,
        isKnownContact = isKnownContact,
        callInfoLine1 = callInfoLine1,
        callInfoLine2 = callInfoLine2,
        onOpenChat = { threadId, chatAddress, contactName, forwardText ->
            onDismiss()
            com.messages.ui.theme.AfterCallReturnState.pending = com.messages.ui.theme.AfterCallReturnState.Info(
                address = address,
                displayName = displayName,
                isKnownContact = isKnownContact,
                callInfoLine1 = callInfoLine1,
                callInfoLine2 = callInfoLine2
            )
            val intent = Intent(context, com.messages.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to_chat", true)
                putExtra("threadId", threadId)
                putExtra("address", chatAddress)
                putExtra("contactName", contactName)
                if (forwardText != null) putExtra("forwardText", forwardText)
            }
            context.startActivity(intent)
        },
        onFinish = { onDismiss() }
    )
}

@Composable
private fun AfterCallMessageTab(
    onOpenChat: (threadId: Long, address: String, contactName: String?, forwardText: String?) -> Unit
) {
    val viewModel: HomeViewModel = viewModel()
    val messages = viewModel.messages.collectAsLazyPagingItems()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(count = messages.itemCount, key = messages.itemKey { it.id }) { index ->
            val msg = messages[index]
            if (msg != null) {
                MessageItemUi(
                    msg = msg,
                    isSelected = false,
                    enableSwipe = false,
                    onClick = {
                        onOpenChat(msg.threadId, msg.address, msg.contactName, null)
                    },
                    onLongClick = {}
                )
            }
        }
    }
}

/**
 * Send here never messages the call's contact directly — it opens that contact's ChatScreen with
 * the reply text carried over as a draft, so the user can review/edit before actually sending.
 */
@Composable
private fun AfterCallQuickReplyTab(
    address: String,
    displayName: String?,
    onOpenChat: (threadId: Long, address: String, contactName: String?, forwardText: String?) -> Unit
) {
    val context = LocalContext.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var personalText by remember { mutableStateOf("") }

    val presets = listOf(
        stringResource(R.string.after_call_quick_reply_1),
        stringResource(R.string.after_call_quick_reply_2),
        stringResource(R.string.after_call_quick_reply_3)
    )
    val strWritePersonal = stringResource(R.string.after_call_write_personal)

    fun openChatWith(text: String) {
        val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
        onOpenChat(threadId, address, displayName, text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 10.dp)
    ) {
        presets.forEachIndexed { index, preset ->
            AfterCallQuickReplyPresetRow(
                text = preset,
                isSelected = selectedIndex == index,
                onSelect = { selectedIndex = index },
                onSend = { openChatWith(preset) }
            )
        }
        AfterCallWritePersonalRow(
            isSelected = selectedIndex == 3,
            text = personalText,
            placeholder = strWritePersonal,
            onTextChange = { personalText = it },
            onSelect = { selectedIndex = 3 },
            onSend = { openChatWith(personalText) }
        )
    }
}

@Composable
private fun AfterCallSendButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colorResource(R.color.primary))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.chat_ic_send),
            contentDescription = stringResource(R.string.content_desc_send),
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AfterCallQuickReplyPresetRow(text: String, isSelected: Boolean, onSelect: () -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 15.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(R.color.primary),
                unselectedColor = Color.LightGray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontFamily = Inter,
            color = colorResource(R.color.text_title),
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            AfterCallSendButton(onClick = onSend)
        }
    }
}

@Composable
private fun AfterCallWritePersonalRow(
    isSelected: Boolean,
    text: String,
    placeholder: String,
    onTextChange: (String) -> Unit,
    onSelect: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp, start = 24.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.after_call_ic_write),
            contentDescription = null,
            tint = colorResource(R.color.text_des),
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onSelect)
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (isSelected) {
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        fontFamily = Inter,
                        color = colorResource(R.color.text_des)
                    )
                }
                val customTextSelectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                    handleColor = colorResource(R.color.primary),
                    backgroundColor = colorResource(R.color.primary).copy(alpha = 0.4f)
                )
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.text.selection.LocalTextSelectionColors provides customTextSelectionColors
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = Inter,
                            color = colorResource(R.color.text_title)
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colorResource(R.color.primary)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Text(
                text = placeholder,
                fontSize = 14.sp,
                fontFamily = Inter,
                color = colorResource(R.color.text_des),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelect)
            )
        }
        if (isSelected && text.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            AfterCallSendButton(onClick = onSend)
        }
    }
}

private val ReminderColorPalette = listOf(
    Color(0xFF4C6FFF),
    Color(0xFF5B6ABF),
    Color(0xFFE0507A),
    Color(0xFFA855C9),
    Color(0xFFE0654F),
    Color(0xFF8E5BC9),
    Color(0xFF4FA8E0)
)

@Composable
private fun AfterCallReminderTab(address: String, displayName: String?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { SmsRepository(context) }
    val alarmScheduler = remember { com.messages.utils.AlarmScheduler(context) }

    var showForm by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<com.messages.model.Reminder?>(null) }

    val remindersFlow = remember(address) { repository.getRemindersByAddressFlow(address) }
    val remindersList by remindersFlow.collectAsState(initial = emptyList())
    val reminders = remember(remindersList) {
        remindersList.filter { it.reminderTimeMillis > System.currentTimeMillis() }
    }

    val strNoReminder = stringResource(R.string.after_call_no_reminder)
    val strAddReminder = stringResource(R.string.after_call_add_reminder)

    if (showForm) {
        AfterCallReminderForm(
            initial = editingReminder,
            onCancel = {
                showForm = false
                editingReminder = null
            },
            onSave = { note, targetMillis, colorIndex ->
                val existing = editingReminder
                coroutineScope.launch {
                    if (existing != null) {
                        alarmScheduler.cancelReminder(existing.id)
                        repository.insertReminder(existing.copy(note = note, reminderTimeMillis = targetMillis, colorIndex = colorIndex))
                        alarmScheduler.scheduleReminder(existing.id, targetMillis)
                    } else {
                        val newReminder = com.messages.model.Reminder(
                            address = address,
                            contactName = displayName,
                            reminderTimeMillis = targetMillis,
                            note = note,
                            colorIndex = colorIndex
                        )
                        val id = repository.insertReminder(newReminder)
                        alarmScheduler.scheduleReminder(id, targetMillis)
                    }
                    showForm = false
                    editingReminder = null
                }
            }
        )
        return
    }

    if (reminders.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.after_call_ic_reminder_big),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strNoReminder,
                fontSize = 16.sp,
                fontFamily = Inter,
                color = colorResource(R.color.text_des)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(93.dp))
                    .background(colorResource(R.color.primary))
                    .clickable {
                        editingReminder = null
                        showForm = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strAddReminder,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(reminders, key = { it.id }) { reminder ->
                    AfterCallReminderRow(
                        reminder = reminder,
                        onEdit = {
                            editingReminder = reminder
                            showForm = true
                        },
                        onDelete = {
                            coroutineScope.launch {
                                alarmScheduler.cancelReminder(reminder.id)
                                repository.deleteReminder(reminder.id)
                            }
                        }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.primary))
                    .clickable {
                        editingReminder = null
                        showForm = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_ic_add),
                    contentDescription = strAddReminder,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AfterCallReminderRow(
    reminder: com.messages.model.Reminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strEdit = stringResource(R.string.content_desc_edit)
    val strDelete = stringResource(R.string.action_delete)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(ReminderColorPalette[reminder.colorIndex % ReminderColorPalette.size])
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.note,
                fontSize = 16.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.text_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.after_call_ic_reminder),
                    contentDescription = null,
                    tint = colorResource(R.color.text_des),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = reminder.formattedTime, fontSize = 13.sp, fontFamily = Inter, color = colorResource(R.color.text_des))
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    painter = painterResource(id = R.drawable.after_call_ic_calender),
                    contentDescription = null,
                    tint = colorResource(R.color.text_des),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = reminder.formattedDayLabel, fontSize = 13.sp, fontFamily = Inter, color = colorResource(R.color.text_des))
            }
        }
        com.messages.ui.components.CustomIconButton(
            iconRes = R.drawable.after_call_ic_write,
            size = 18,
            contentDescription = strEdit,
            tint = colorResource(R.color.text_des),
            onClick = onEdit
        )
        Spacer(modifier = Modifier.width(12.dp))
        com.messages.ui.components.CustomIconButton(
            iconRes = R.drawable.longpress_ic_delete,
            size = 18,
            contentDescription = strDelete,
            tint = colorResource(R.color.text_des),
            onClick = onDelete
        )
    }
}

@Composable
private fun AfterCallReminderForm(
    initial: com.messages.model.Reminder?,
    onCancel: () -> Unit,
    onSave: (note: String, targetMillis: Long, colorIndex: Int) -> Unit
) {
    val strRemindMeAbout = stringResource(R.string.after_call_remind_me_about)
    val strSave = stringResource(R.string.action_save)
    val strCancel = stringResource(R.string.action_cancel_caps)

    var note by remember { mutableStateOf(initial?.note ?: "") }
    var colorIndex by remember { mutableStateOf(initial?.colorIndex ?: 0) }

    val dayLabels = remember {
        val labels = mutableListOf("Today", "Tomorrow")
        val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM dd", java.util.Locale.getDefault())
        var date = java.time.LocalDate.now().plusDays(2)
        repeat(28) {
            labels.add(date.format(fmt))
            date = date.plusDays(1)
        }
        labels
    }

    val initialCal = remember {
        java.util.Calendar.getInstance().apply {
            if (initial != null) {
                timeInMillis = initial.reminderTimeMillis
            }
            // else: leave as now — the picker should default to the current time, not an offset.
        }
    }
    var selectedDayIndex by remember {
        mutableStateOf(
            if (initial != null) {
                val days = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(),
                    java.time.Instant.ofEpochMilli(initial.reminderTimeMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                ).toInt()
                days.coerceIn(0, dayLabels.size - 1)
            } else {
                0
            }
        )
    }
    var selectedHour by remember { mutableStateOf(initialCal.get(java.util.Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(initialCal.get(java.util.Calendar.MINUTE)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.after_call_ic_write),
                contentDescription = null,
                tint = colorResource(R.color.primary),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (note.isEmpty()) {
                    Text(text = strRemindMeAbout, fontSize = 16.sp, fontFamily = Inter, color = colorResource(R.color.text_des))
                }
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    textStyle = TextStyle(fontSize = 16.sp, fontFamily = Inter, color = colorResource(R.color.text_title)),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colorResource(R.color.primary)),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        InlineDateTimePicker(
            dayLabels = dayLabels,
            selectedDayIndex = selectedDayIndex,
            onDaySelected = { selectedDayIndex = it },
            selectedHour = selectedHour,
            onHourSelected = { selectedHour = it },
            selectedMinute = selectedMinute,
            onMinuteSelected = { selectedMinute = it }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReminderColorPalette.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (index == colorIndex) Modifier.border(2.dp, colorResource(R.color.text_title), CircleShape) else Modifier
                        )
                        .clickable { colorIndex = index }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(93.dp))
                    .background(colorResource(R.color.light_gray))
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Text(text = strCancel, fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colorResource(R.color.text_title))
            }
            val targetMillis = remember(selectedDayIndex, selectedHour, selectedMinute) {
                val cal = java.util.Calendar.getInstance()
                if (selectedDayIndex > 0) cal.add(java.util.Calendar.DAY_OF_YEAR, selectedDayIndex)
                cal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                cal.set(java.util.Calendar.MINUTE, selectedMinute)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            // Only relevant when Today is selected — Tomorrow/later dates can't land in the past.
            // Blocks the user from setting a reminder time earlier than right now. Compared
            // against "now" floored to the minute (not the raw millis) — targetMillis already
            // has seconds zeroed, so comparing against raw System.currentTimeMillis() would
            // falsely flag the current minute (or one just picked) as "past" once real seconds
            // tick forward past :00.
            val nowFloorToMinute = remember(selectedDayIndex, selectedHour, selectedMinute) {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            val isPastTime = targetMillis < nowFloorToMinute
            val canSave = note.isNotBlank() && !isPastTime
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(93.dp))
                    .background(if (canSave) colorResource(R.color.primary) else colorResource(R.color.light_color_gray))
                    .clickable(enabled = canSave) {
                        onSave(note.trim(), targetMillis, colorIndex)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = strSave, fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun AfterCallMoreTab(
    address: String,
    displayName: String?,
    onOpenChat: () -> Unit
) {
    val context = LocalContext.current
    val strAddContact = stringResource(R.string.after_call_action_add_contact)
    val strMessage = stringResource(R.string.after_call_action_message)
    val strSendMail = stringResource(R.string.after_call_action_send_mail)
    val strCalendar = stringResource(R.string.after_call_action_calendar)
    val strWeb = stringResource(R.string.after_call_action_web)
    val calendarTitleTemplate = stringResource(R.string.after_call_calendar_event_title_template)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 10.dp)
    ) {
        ChatDetailsOptionRow(
            iconRes = R.drawable.after_call_add_contact,
            text = strAddContact,
            iconTint = colorResource(R.color.text_title),
            fontSize = 16.sp,
            onClick = {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                    putExtra(ContactsContract.Intents.Insert.PHONE, address)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
        ChatDetailsOptionRow(
            iconRes = R.drawable.after_call_ic_chat,
            text = strMessage,
            iconTint = colorResource(R.color.text_title),
            fontSize = 16.sp,
            onClick = onOpenChat
        )
        ChatDetailsOptionRow(
            iconRes = R.drawable.after_call_ic_mail,
            text = strSendMail,
            iconTint = colorResource(R.color.text_title),
            fontSize = 16.sp,
            onClick = {
                val email = lookupContactEmail(context, address)
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    if (email != null) putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                }
                val chooser = Intent.createChooser(intent, "Complete action using")
                try {
                    AfterCallActivity.suppressNextLeaveFinish = true
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
        ChatDetailsOptionRow(
            iconRes = R.drawable.after_call_ic_calender,
            text = strCalendar,
            iconTint = colorResource(R.color.text_title),
            fontSize = 16.sp,
            onClick = {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                }
                val chooser = Intent.createChooser(intent, "Complete action using")
                try {
                    AfterCallActivity.suppressNextLeaveFinish = true
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
        ChatDetailsOptionRow(
            iconRes = R.drawable.after_call_ic_web,
            text = strWeb,
            iconTint = colorResource(R.color.text_title),
            fontSize = 16.sp,
            onClick = {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    AfterCallActivity.suppressNextLeaveFinish = true
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to browser if Google app or Web Search is not available
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(fallbackIntent)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        )
    }
}

/** Resolves the email address of the contact matching this phone number, if any (via PhoneLookup -> Email join). */
private fun lookupContactEmail(context: Context, address: String): String? {
    return try {
        val phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
        var contactId: String? = null
        context.contentResolver.query(phoneUri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) contactId = cursor.getString(0)
        }
        val id = contactId ?: return null
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(id),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }
}
