package com.messages.sms.texting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.messages.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatDetailsViewModel(
    application: Application,
    private val threadId: Long,
    private val address: String,
    private val groupId: Long? = null
) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)
    val isGroupMode: Boolean = groupId != null

    private val _isArchived = MutableStateFlow(false)

    val group: StateFlow<com.messages.sms.texting.app.model.Group?> = if (isGroupMode) {
        repository.getGroupByIdFlow(groupId!!)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        MutableStateFlow(null).asStateFlow()
    }

    val isArchived: StateFlow<Boolean> = if (isGroupMode) {
        group.map { it?.isArchived ?: false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    } else {
        _isArchived.asStateFlow()
    }

    val isBlocked: StateFlow<Boolean> = if (isGroupMode) {
        MutableStateFlow(false).asStateFlow()
    } else {
        repository.isContactBlockedFlow(address)
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)
    }

    private val _photoUri = MutableStateFlow<String?>(null)
    val photoUri: StateFlow<String?> = _photoUri.asStateFlow()

    private val _memberPhotoUris = MutableStateFlow<Map<String, String?>>(emptyMap())
    val memberPhotoUris: StateFlow<Map<String, String?>> = _memberPhotoUris.asStateFlow()

    init {
        if (!isGroupMode) {
            checkArchivedStatus()
            loadPhotoUri()
        } else {
            viewModelScope.launch {
                group.collect { g ->
                    if (g != null) {
                        val current = _memberPhotoUris.value
                        val missing = g.members.filter { it.address !in current.keys }
                        if (missing.isNotEmpty()) {
                            val newEntries = missing.associate { it.address to repository.getContactPhotoUri(it.address) }
                            _memberPhotoUris.value = current + newEntries
                        }
                    }
                }
            }
        }
    }

    fun renameGroup(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || groupId == null) return
        viewModelScope.launch {
            val current = repository.getGroupById(groupId) ?: return@launch
            repository.updateGroup(current.copy(name = trimmed))
        }
    }

    fun deleteGroup() {
        if (groupId == null) return
        viewModelScope.launch {
            repository.deleteGroup(groupId)
        }
    }

    fun updateGroupAvatar(avatarPath: String) {
        if (groupId == null) return
        viewModelScope.launch {
            val current = repository.getGroupById(groupId) ?: return@launch
            repository.updateGroup(current.copy(avatarUri = avatarPath))
        }
    }

    fun addMembers(newMembers: List<com.messages.sms.texting.app.model.GroupMember>) {
        if (groupId == null || newMembers.isEmpty()) return
        viewModelScope.launch {
            val current = repository.getGroupById(groupId) ?: return@launch
            val existingAddresses = current.members.map { it.address }.toSet()
            val toAdd = newMembers.filter { it.address !in existingAddresses }
            if (toAdd.isEmpty()) return@launch
            repository.updateGroup(current.copy(members = current.members + toAdd))
        }
    }

    fun removeMember(address: String) {
        if (groupId == null) return
        viewModelScope.launch {
            val current = repository.getGroupById(groupId) ?: return@launch
            if (current.members.size <= 1) return@launch
            repository.updateGroup(current.copy(members = current.members.filterNot { it.address == address }))
        }
    }

    private fun loadPhotoUri() {
        viewModelScope.launch {
            try {
                if (threadId != 0L) {
                    val msgs = repository.getMessagesByThreadId(threadId).first()
                    _photoUri.value = msgs.firstOrNull()?.photoUri ?: repository.getContactPhotoUri(address)
                } else {
                    _photoUri.value = repository.getContactPhotoUri(address)
                }
            } catch (e: Exception) {
                _photoUri.value = repository.getContactPhotoUri(address)
            }
        }
    }

    private fun checkArchivedStatus() {
        viewModelScope.launch {
            _isArchived.value = repository.isThreadArchived(threadId)
        }
    }

    fun toggleBlock(contactName: String?) {
        viewModelScope.launch {
            if (isBlocked.value) {
                repository.unblockContact(address)
            } else {
                repository.blockContact(address, contactName)
            }
        }
    }

    fun toggleArchive() {
        if (isGroupMode) {
            viewModelScope.launch {
                val current = repository.getGroupById(groupId!!) ?: return@launch
                repository.setGroupArchived(groupId, !current.isArchived)
            }
            return
        }
        viewModelScope.launch {
            val newState = !_isArchived.value
            repository.setThreadsArchived(listOf(threadId), newState)
            _isArchived.value = newState
        }
    }

    fun deleteThread() {
        viewModelScope.launch {
            repository.deleteThreads(listOf(threadId))
        }
    }
}
