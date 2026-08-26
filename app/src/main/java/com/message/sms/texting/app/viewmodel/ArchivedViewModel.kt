package com.message.sms.texting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.message.sms.texting.app.model.SmsMessage
import com.message.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)

    val messages: Flow<PagingData<SmsMessage>> = Pager(
        config = PagingConfig(pageSize = 50, enablePlaceholders = false)
    ) {
        repository.getArchivedMessagesPagingSource()
    }.flow.cachedIn(viewModelScope)

    val drafts: StateFlow<Map<String, String>> = repository.getAllDraftsFlow()
        .map { list -> list.associate { it.address to it.body } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val archivedGroups: StateFlow<List<com.message.sms.texting.app.model.GroupWithLastMessage>> = repository.getAllGroupsWithLastMessageFlow()
        .map { list -> list.filter { it.group.isArchived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unarchiveGroup(groupId: Long) {
        viewModelScope.launch {
            repository.setGroupArchived(groupId, false)
        }
    }

    fun archiveGroup(groupId: Long) {
        viewModelScope.launch {
            repository.setGroupArchived(groupId, true)
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            repository.deleteGroup(groupId)
        }
    }

    // Group selection (Archived screen)
    private val _selectedGroupIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedGroupIds: StateFlow<Set<Long>> = _selectedGroupIds.asStateFlow()

    fun toggleGroupSelection(groupId: Long) {
        val current = _selectedGroupIds.value.toMutableSet()
        if (!current.remove(groupId)) {
            current.add(groupId)
        }
        _selectedGroupIds.value = current
    }

    fun clearGroupSelection() {
        _selectedGroupIds.value = emptySet()
    }

    fun unarchiveSelectedGroups() {
        val ids = _selectedGroupIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setGroupsArchived(ids, false)
            clearGroupSelection()
        }
    }

    fun deleteSelectedGroups() {
        val ids = _selectedGroupIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteGroups(ids)
            clearGroupSelection()
        }
    }

    fun togglePinSelectedGroups(isPinned: Boolean) {
        val ids = _selectedGroupIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setGroupsPinned(ids, isPinned)
            clearGroupSelection()
        }
    }

    private val _selectedMessages = MutableStateFlow<Set<SmsMessage>>(emptySet())
    val selectedMessages: StateFlow<Set<SmsMessage>> = _selectedMessages.asStateFlow()

    fun toggleSelection(message: SmsMessage) {
        val currentSet = _selectedMessages.value.toMutableSet()
        val exists = currentSet.find { it.id == message.id }
        if (exists != null) {
            currentSet.remove(exists)
        } else {
            currentSet.add(message)
        }
        _selectedMessages.value = currentSet
    }

    fun clearSelection() {
        _selectedMessages.value = emptySet()
    }

    fun markSelectedAsUnread() {
        // Not implementing thread-level unread since Android doesn't have a reliable way to mark all messages in a thread as unread, but we can do it locally if needed.
        // Actually, let's keep it simple and just do it via ids for now or not at all.
        val ids = _selectedMessages.value.map { it.id }.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.markAsUnread(ids)
                clearSelection()
            }
        }
    }

    fun markSelectedAsRead() {
        val selectedIds = _selectedMessages.value.map { it.id }.toList()
        viewModelScope.launch {
            repository.markAsRead(selectedIds)
            clearSelection()
        }
    }

    fun blockSelectedMessages() {
        val selected = _selectedMessages.value.toList()
        viewModelScope.launch {
            selected.forEach { msg ->
                repository.blockContact(msg.address, msg.contactName)
            }
            clearSelection()
        }
    }

    fun unblockContacts(messages: List<SmsMessage>) {
        viewModelScope.launch {
            messages.forEach { msg ->
                repository.unblockContact(msg.address)
            }
        }
    }

    fun togglePinSelected(isPinned: Boolean) {
        val threadIds = _selectedMessages.value.map { it.threadId }.toList()
        if (threadIds.isNotEmpty()) {
            viewModelScope.launch {
                repository.setThreadsPinned(threadIds, isPinned)
                clearSelection()
            }
        }
    }

    fun deleteSelectedMessages() {
        val threadIds = _selectedMessages.value.map { it.threadId }.toList()
        if (threadIds.isNotEmpty()) {
            viewModelScope.launch {
                repository.deleteThreads(threadIds)
                clearSelection()
            }
        }
    }

    fun unarchiveSelectedMessages() {
        val threadIds = _selectedMessages.value.map { it.threadId }.toList()
        if (threadIds.isNotEmpty()) {
            viewModelScope.launch {
                repository.setThreadsArchived(threadIds, false)
                clearSelection()
            }
        }
    }

    fun unarchiveMessage(id: Long, threadId: Long) {
        viewModelScope.launch {
            repository.setThreadsArchived(listOf(threadId), false)
        }
    }

    fun archiveThreads(threadIds: List<Long>) {
        viewModelScope.launch {
            repository.setThreadsArchived(threadIds, true)
        }
    }

    fun deleteMessage(id: Long, threadId: Long) {
        viewModelScope.launch {
            repository.deleteThreads(listOf(threadId))
        }
    }
}
