package com.messages.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.messages.model.SmsMessage
import com.messages.repository.SmsRepository
import com.messages.ui.theme.DelayedSendingState
import com.messages.utils.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class ChatViewModel(
    application: Application,
    private val threadId: Long,
    val address: String,
    val contactName: String?,
    private val scheduledMessageId: Long? = null,
    val groupId: Long? = null
) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)
    val isGroupMode: Boolean = groupId != null

    private val _messages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messages: StateFlow<List<SmsMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(!isGroupMode)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messageToUpdate = MutableStateFlow<com.messages.model.ScheduledMessage?>(null)
    val messageToUpdate: StateFlow<com.messages.model.ScheduledMessage?> = _messageToUpdate.asStateFlow()

    val isBlocked: StateFlow<Boolean> = if (isGroupMode) {
        MutableStateFlow(false).asStateFlow()
    } else {
        repository.isContactBlockedFlow(address)
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)
    }

    val group: StateFlow<com.messages.model.Group?> = if (isGroupMode) {
        repository.getGroupByIdFlow(groupId!!)
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)
    } else {
        MutableStateFlow(null).asStateFlow()
    }

    private val draftKey: String get() = if (isGroupMode) "group_$groupId" else address

    init {
        if (isGroupMode) {
            loadGroupMessages()
        } else {
            loadMessages()
            if (scheduledMessageId != null) {
                viewModelScope.launch {
                    _messageToUpdate.value = repository.getScheduledMessageById(scheduledMessageId)
                }
            }
        }
    }

    private var actualThreadId = threadId

    private fun loadGroupMessages() {
        viewModelScope.launch {
            repository.getGroupMessagesFlow(groupId!!).collectLatest { groupMsgs ->
                _messages.value = groupMsgs.map { gm ->
                    SmsMessage(
                        id = gm.id,
                        threadId = groupId,
                        address = "",
                        body = gm.body,
                        date = gm.date,
                        read = true,
                        type = 2, // always "sent" — group replies land in each member's own thread
                        isPinned = false,
                        unreadCount = 0,
                        category = 0,
                        contactName = null,
                        photoUri = null,
                        isArchived = false,
                        sendStatus = gm.sendStatus,
                        delayedUntilMillis = gm.delayedUntilMillis
                    )
                }.sortedByDescending { it.date } // keep newest-first, matching the 1:1 thread query's ordering
                _isLoading.value = false
            }
        }
    }

    fun unblockContact() {
        viewModelScope.launch {
            repository.unblockContact(address)
        }
    }

    fun blockContact() {
        viewModelScope.launch {
            repository.blockContact(address, contactName)
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            
            if (actualThreadId == 0L) {
                try {
                    actualThreadId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        android.provider.Telephony.Threads.getOrCreateThreadId(getApplication<Application>(), address)
                    }
                } catch (e: Exception) {
                    actualThreadId = 0L
                }
            }

            // Mark thread as read in system and local DB
            repository.markThreadAsRead(actualThreadId)

            // First, trigger a sync to load all historical messages for this thread into the local DB
            repository.syncThreadMessages(actualThreadId)
            
            // Then, observe the database for this thread
            repository.getMessagesByThreadId(actualThreadId).collectLatest { msgs ->
                _messages.value = msgs
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        AnalyticsManager.logEventWithAction("message_sent", "ChatScreen", if (isGroupMode) "group" else "single")

        val delayMillis = DelayedSendingState.delayMillisFor(DelayedSendingState.mode.value)

        if (isGroupMode) {
            viewModelScope.launch {
                val currentGroup = repository.getGroupById(groupId!!) ?: return@launch
                repository.clearDraft(draftKey)
                // A delayed send can take up to 10s — run it on a process-scoped coroutine
                // so leaving this chat before it finishes doesn't cancel it mid-delay.
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    repository.sendToGroup(currentGroup, text, delayMillis)
                }
            }
            return
        }

        viewModelScope.launch {
            if (actualThreadId == 0L) {
                try {
                    actualThreadId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        android.provider.Telephony.Threads.getOrCreateThreadId(getApplication<Application>(), address)
                    }
                } catch (e: Exception) {
                    actualThreadId = 0L
                }
            }
            val threadIdForSend = actualThreadId
            repository.clearDraft(draftKey)
            // A delayed send can take up to 10s — run it on a process-scoped coroutine
            // so leaving this chat before it finishes doesn't cancel it mid-delay.
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                repository.sendSms(address, text, threadIdForSend, contactName, delayMillis)
            }
        }
    }

    fun sendMmsMessage(text: String, bitmap: android.graphics.Bitmap) {
        if (isGroupMode) return // MMS attachments aren't supported for broadcast groups yet
        AnalyticsManager.logEventWithAction("message_sent", "ChatScreen", "mms")

        viewModelScope.launch {
            if (actualThreadId == 0L) {
                try {
                    actualThreadId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        android.provider.Telephony.Threads.getOrCreateThreadId(getApplication<Application>(), address)
                    }
                } catch (e: Exception) {
                    actualThreadId = 0L
                }
            }
            val threadIdForSend = actualThreadId
            repository.clearDraft(draftKey)
            val delayMillis = DelayedSendingState.delayMillisFor(DelayedSendingState.mode.value)

            // MMS delivery can take a while (APN/network round trip, plus the optional
            // delay) — run it on a process-scoped coroutine so leaving this chat before
            // it finishes doesn't cancel it and leave the message stuck on "Sending...".
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                repository.sendMms(address, text, bitmap, threadIdForSend, contactName, delayMillis)
            }
        }
    }

    fun retrySms(id: Long) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.retrySms(id)
        }
    }

    fun retryMms(id: Long) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.retryMms(id)
        }
    }

    suspend fun loadDraft(): String? {
        return repository.getDraft(draftKey)?.body
    }

    fun saveDraft(text: String) {
        val key = draftKey
        viewModelScope.launch {
            repository.saveDraft(key, actualThreadId, text)
        }
    }

    fun scheduleMessage(text: String, timeInMillis: Long) {
        if (text.isBlank()) return
        AnalyticsManager.logEventWithAction("message_scheduled", "ChatScreen", if (isGroupMode) "group" else "single")

        viewModelScope.launch {
            val scheduledMessage = if (isGroupMode) {
                val group = repository.getGroupById(groupId!!)
                com.messages.model.ScheduledMessage(
                    id = scheduledMessageId ?: 0,
                    address = "",
                    body = text,
                    scheduledTimeMillis = timeInMillis,
                    groupId = groupId,
                    groupName = group?.name
                )
            } else {
                if (actualThreadId == 0L) {
                    try {
                        actualThreadId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            android.provider.Telephony.Threads.getOrCreateThreadId(getApplication<Application>(), address)
                        }
                    } catch (e: Exception) {
                        actualThreadId = 0L
                    }
                }

                val photoUri = repository.getContactPhotoUri(address)

                com.messages.model.ScheduledMessage(
                    id = scheduledMessageId ?: 0,
                    address = address,
                    body = text,
                    scheduledTimeMillis = timeInMillis,
                    contactName = contactName,
                    photoUri = photoUri
                )
            }

            if (scheduledMessageId != null) {
                repository.updateScheduledMessage(scheduledMessage)
                com.messages.utils.AlarmScheduler(getApplication()).cancelMessage(scheduledMessageId)
                com.messages.utils.AlarmScheduler(getApplication()).scheduleMessage(scheduledMessageId, timeInMillis)
            } else {
                val id = repository.insertScheduledMessage(scheduledMessage)
                com.messages.utils.AlarmScheduler(getApplication()).scheduleMessage(id, timeInMillis)
            }
        }
    }

    fun deleteMessages(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteMessages(ids)
        }
    }

    fun toggleStarredMessages(ids: List<Long>, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStarStatus(ids, isStarred)
        }
    }
}
