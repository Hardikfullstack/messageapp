package com.messages.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.messages.model.ScheduledMessage
import com.messages.repository.SmsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduledMessagesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)
    private val alarmScheduler = com.messages.utils.AlarmScheduler(application)

    val scheduledMessages: StateFlow<List<ScheduledMessage>> = repository.getAllScheduledMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancelScheduledMessage(id: Long) {
        viewModelScope.launch {
            alarmScheduler.cancelMessage(id)
            repository.deleteScheduledMessage(id)
        }
    }

    fun sendNow(msg: ScheduledMessage) {
        viewModelScope.launch {
            alarmScheduler.cancelMessage(msg.id)
            if (msg.groupId != null) {
                val group = repository.getGroupById(msg.groupId)
                if (group != null) {
                    repository.sendToGroup(group, msg.body)
                }
            } else {
                val threadId = android.provider.Telephony.Threads.getOrCreateThreadId(getApplication(), msg.address)
                repository.sendSms(msg.address, msg.body, threadId, msg.contactName)
            }
            repository.deleteScheduledMessage(msg.id)
        }
    }
}
