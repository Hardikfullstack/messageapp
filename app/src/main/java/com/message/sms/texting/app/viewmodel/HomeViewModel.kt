package com.message.sms.texting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.message.sms.texting.app.model.SmsMessage
import com.message.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _messages = MutableStateFlow<PagingData<SmsMessage>>(PagingData.empty())
    val messages: StateFlow<PagingData<SmsMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentCategory = MutableStateFlow(0)
    val currentCategory: StateFlow<Int> = _currentCategory.asStateFlow()
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _selectedMessages = MutableStateFlow<Set<SmsMessage>>(emptySet())
    val selectedMessages: StateFlow<Set<SmsMessage>> = _selectedMessages.asStateFlow()
    
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    private val _unreadCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<Int, Int>> = _unreadCounts.asStateFlow()

    private val _selectedGroupIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedGroupIds: StateFlow<Set<Long>> = _selectedGroupIds.asStateFlow()

    val drafts: StateFlow<Map<String, String>> = repository.getAllDraftsFlow()
        .map { list -> list.associate { it.address to it.body } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val groups: StateFlow<List<com.message.sms.texting.app.model.GroupWithLastMessage>> = repository.getAllGroupsWithLastMessageFlow()
        .map { list -> list.filter { !it.group.isArchived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var messagesJob: Job? = null

    init {
        checkAndSyncData()
        
        // Listen for new SMS arrivals to trigger incremental sync
        repository.setupSmsObserver {
            viewModelScope.launch {
                // Background sync latest 50 messages to catch updates
                repository.performSync()
            }
        }

        viewModelScope.launch {
            repository.getUnreadCountsFlow().collect { counts ->
                _unreadCounts.value = counts
            }
        }
    }

    private fun checkAndSyncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.performSync()
            _isSyncing.value = false
            
            // Load inbox by default
            setCategory(0)
        }
    }

    private val _currentFilter = MutableStateFlow(com.message.sms.texting.app.ui.components.FilterConfig())
    val currentFilter: StateFlow<com.message.sms.texting.app.ui.components.FilterConfig> = _currentFilter.asStateFlow()

    // Call this when user clicks a filter chip
    fun setCategory(categoryId: Int) {
        _currentCategory.value = categoryId
        reloadMessages()
    }

    fun applyFilter(filterConfig: com.message.sms.texting.app.ui.components.FilterConfig) {
        _currentFilter.value = filterConfig
        reloadMessages()
    }

    private fun reloadMessages() {
        val categoryId = _currentCategory.value
        val filter = _currentFilter.value
        
        var startDate = 0L
        var endDate = Long.MAX_VALUE
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        when (filter.type) {
            com.message.sms.texting.app.ui.components.FilterType.DEFAULT -> {}
            com.message.sms.texting.app.ui.components.FilterType.TODAY -> {
                startDate = cal.timeInMillis
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                endDate = cal.timeInMillis - 1
            }
            com.message.sms.texting.app.ui.components.FilterType.MONTH -> {
                filter.month?.let { m ->
                    cal.set(java.util.Calendar.MONTH, m)
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    startDate = cal.timeInMillis
                    cal.add(java.util.Calendar.MONTH, 1)
                    endDate = cal.timeInMillis - 1
                }
            }
            com.message.sms.texting.app.ui.components.FilterType.YEAR -> {
                filter.year?.let { y ->
                    cal.set(java.util.Calendar.YEAR, y)
                    cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                    startDate = cal.timeInMillis
                    cal.add(java.util.Calendar.YEAR, 1)
                    endDate = cal.timeInMillis - 1
                }
            }
            com.message.sms.texting.app.ui.components.FilterType.DATE_RANGE -> {
                startDate = filter.startDate ?: 0L
                endDate = filter.endDate ?: Long.MAX_VALUE
            }
        }

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            _isLoading.value = true
            Pager(
                config = PagingConfig(
                    pageSize = 30,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { repository.getMessagesPagingSource(categoryId, startDate, endDate) }
            ).flow.cachedIn(viewModelScope).collect { fetchedMessages ->
                _messages.value = fetchedMessages
                _isLoading.value = false
            }
        }
    }

    // Selection Logic
    fun toggleSelection(message: SmsMessage) {
        val currentSet = _selectedMessages.value.toMutableSet()
        val existing = currentSet.find { it.id == message.id }
        if (existing != null) {
            currentSet.remove(existing)
            if (currentSet.isEmpty()) {
                clearSelection()
                return
            }
        } else {
            currentSet.add(message)
        }
        _selectedMessages.value = currentSet
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedMessages.value = emptySet()
        _isSelectionMode.value = false
    }

    fun enableSelectionMode() {
        _isSelectionMode.value = true
    }

    fun toggleSelectAllMessages(totalCount: Int) {
        if (_selectedMessages.value.size >= totalCount && totalCount > 0) {
            clearSelection()
        } else {
            viewModelScope.launch {
                val categoryId = _currentCategory.value
                val filter = _currentFilter.value
                var startDate = 0L
                var endDate = Long.MAX_VALUE
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                when (filter.type) {
                    com.message.sms.texting.app.ui.components.FilterType.DEFAULT -> {}
                    com.message.sms.texting.app.ui.components.FilterType.TODAY -> {
                        startDate = cal.timeInMillis
                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                        endDate = cal.timeInMillis - 1
                    }
                    com.message.sms.texting.app.ui.components.FilterType.MONTH -> {
                        filter.month?.let { m ->
                            cal.set(java.util.Calendar.MONTH, m)
                            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                            startDate = cal.timeInMillis
                            cal.add(java.util.Calendar.MONTH, 1)
                            endDate = cal.timeInMillis - 1
                        }
                    }
                    com.message.sms.texting.app.ui.components.FilterType.YEAR -> {
                        filter.year?.let { y ->
                            cal.set(java.util.Calendar.YEAR, y)
                            cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                            startDate = cal.timeInMillis
                            cal.add(java.util.Calendar.YEAR, 1)
                            endDate = cal.timeInMillis - 1
                        }
                    }
                    com.message.sms.texting.app.ui.components.FilterType.DATE_RANGE -> {
                        startDate = filter.startDate ?: 0L
                        endDate = filter.endDate ?: Long.MAX_VALUE
                    }
                }
                
                val allMsgs = repository.getMessagesList(categoryId, startDate, endDate)
                _selectedMessages.value = allMsgs.toSet()
                _isSelectionMode.value = true
            }
        }
    }

    fun markSelectedAsUnread() {
        val ids = _selectedMessages.value.map { it.id }.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.markAsUnread(ids)
                clearSelection()
            }
        }
    }

    fun markSelectedAsRead() {
        val threadIds = _selectedMessages.value.map { it.threadId }.toList()
        if (threadIds.isNotEmpty()) {
            viewModelScope.launch {
                repository.markThreadsAsRead(threadIds)
                clearSelection()
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val categoryId = _currentCategory.value
            val filter = _currentFilter.value
            var startDate = 0L
            var endDate = Long.MAX_VALUE
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            when (filter.type) {
                com.message.sms.texting.app.ui.components.FilterType.DEFAULT -> {}
                com.message.sms.texting.app.ui.components.FilterType.TODAY -> {
                    startDate = cal.timeInMillis
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    endDate = cal.timeInMillis - 1
                }
                com.message.sms.texting.app.ui.components.FilterType.MONTH -> {
                    filter.month?.let { m ->
                        cal.set(java.util.Calendar.MONTH, m)
                        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                        startDate = cal.timeInMillis
                        cal.add(java.util.Calendar.MONTH, 1)
                        endDate = cal.timeInMillis - 1
                    }
                }
                com.message.sms.texting.app.ui.components.FilterType.YEAR -> {
                    filter.year?.let { y ->
                        cal.set(java.util.Calendar.YEAR, y)
                        cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                        startDate = cal.timeInMillis
                        cal.add(java.util.Calendar.YEAR, 1)
                        endDate = cal.timeInMillis - 1
                    }
                }
                com.message.sms.texting.app.ui.components.FilterType.DATE_RANGE -> {
                    startDate = filter.startDate ?: 0L
                    endDate = filter.endDate ?: Long.MAX_VALUE
                }
            }
            repository.markAllAsRead(categoryId, startDate, endDate)
            clearSelection()
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

    fun archiveSelectedMessages() {
        val threadIds = _selectedMessages.value.map { it.threadId }.toList()
        if (threadIds.isNotEmpty()) {
            viewModelScope.launch {
                repository.setThreadsArchived(threadIds, true)
                clearSelection()
            }
        }
    }

    fun unarchiveThreads(threadIds: List<Long>) {
        viewModelScope.launch {
            repository.setThreadsArchived(threadIds, false)
        }
    }

    fun archiveThreads(threadIds: List<Long>) {
        viewModelScope.launch {
            repository.setThreadsArchived(threadIds, true)
        }
    }

    fun archiveMessage(id: Long, threadId: Long) {
        viewModelScope.launch {
            repository.setThreadsArchived(listOf(threadId), true)
            val remaining = _selectedMessages.value.filterNot { it.threadId == threadId }.toSet()
            _selectedMessages.value = remaining
            if (remaining.isEmpty()) {
                _isSelectionMode.value = false
            }
        }
    }

    fun unarchiveMessage(id: Long, threadId: Long) {
        viewModelScope.launch {
            repository.setThreadsArchived(listOf(threadId), false)
        }
    }

    fun archiveGroup(groupId: Long) {
        viewModelScope.launch {
            repository.setGroupArchived(groupId, true)
        }
    }

    fun unarchiveGroup(groupId: Long) {
        viewModelScope.launch {
            repository.setGroupArchived(groupId, false)
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            repository.deleteGroup(groupId)
        }
    }

    // Group selection (separate from normal-message selection)
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

    fun archiveSelectedGroups() {
        val ids = _selectedGroupIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setGroupsArchived(ids, true)
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

    fun deleteMessage(id: Long, threadId: Long) {
        viewModelScope.launch {
            repository.deleteThreads(listOf(threadId))
            val remaining = _selectedMessages.value.filterNot { it.threadId == threadId }.toSet()
            _selectedMessages.value = remaining
            if (remaining.isEmpty()) {
                _isSelectionMode.value = false
            }
        }
    }

    fun blockSelectedMessages() {
        val selected = _selectedMessages.value.toList()
        viewModelScope.launch {
            val distinctContacts = selected.map { 
                com.message.sms.texting.app.model.BlockedContact(it.address, it.contactName) 
            }.distinctBy { it.address }
            
            repository.blockContacts(distinctContacts)
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

    fun markMessageRead(id: Long) {
        viewModelScope.launch {
            repository.markAsRead(listOf(id))
        }
    }

    fun markMessageUnread(id: Long) {
        viewModelScope.launch {
            repository.markAsUnread(listOf(id))
        }
    }

    fun blockMessage(address: String, contactName: String?) {
        viewModelScope.launch {
            repository.blockContact(address, contactName)
        }
    }
}
