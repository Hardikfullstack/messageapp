package com.message.sms.texting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.message.sms.texting.app.model.BlockedContact
import com.message.sms.texting.app.repository.BlockedContactWithMessage
import com.message.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BlockedMessagesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)

    private val _blockedContacts = MutableStateFlow<List<BlockedContactWithMessage>>(emptyList())
    val blockedContacts: StateFlow<List<BlockedContactWithMessage>> = _blockedContacts.asStateFlow()

    private val _dropMessagesEnabled = MutableStateFlow(repository.isDropMessagesEnabled())
    val dropMessagesEnabled: StateFlow<Boolean> = _dropMessagesEnabled.asStateFlow()

    private val _selectedContacts = MutableStateFlow<Set<BlockedContactWithMessage>>(emptySet())
    val selectedContacts: StateFlow<Set<BlockedContactWithMessage>> = _selectedContacts.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getBlockedContactsWithLatestMessageFlow().collectLatest { contacts ->
                _blockedContacts.value = contacts
            }
        }
    }

    fun setDropMessagesEnabled(enabled: Boolean) {
        repository.setDropMessagesEnabled(enabled)
        _dropMessagesEnabled.value = enabled
    }

    fun toggleSelection(contact: BlockedContactWithMessage) {
        val current = _selectedContacts.value.toMutableSet()
        if (current.contains(contact)) {
            current.remove(contact)
        } else {
            current.add(contact)
        }
        _selectedContacts.value = current
    }

    fun clearSelection() {
        _selectedContacts.value = emptySet()
    }

    fun unblockSelectedContacts(): List<BlockedContactWithMessage> {
        val contactsToUnblock = _selectedContacts.value.toList()
        viewModelScope.launch {
            contactsToUnblock.forEach { contact ->
                repository.unblockContact(contact.address)
            }
            clearSelection()
        }
        return contactsToUnblock
    }

    fun blockContacts(contacts: List<BlockedContactWithMessage>) {
        viewModelScope.launch {
            contacts.forEach { contact ->
                repository.blockContact(contact.address, contact.contactName)
            }
        }
    }

    fun deleteSelectedContactsMessages() {
        val contactsToDelete = _selectedContacts.value.toList()
        viewModelScope.launch {
            contactsToDelete.forEach { contact ->
                repository.deleteMessagesByAddress(contact.address)
                // We keep them blocked, just delete their messages
            }
            clearSelection()
        }
    }
}
