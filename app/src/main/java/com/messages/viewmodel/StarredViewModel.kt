package com.messages.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.messages.model.SmsMessage
import com.messages.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StarredViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)
    
    private val _messages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messages: StateFlow<List<SmsMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadStarredMessages()
    }

    fun loadStarredMessages() {
        viewModelScope.launch {
            if (_messages.value.isEmpty()) {
                _isLoading.value = true
            }
            val starredMsgs = repository.getStarredMessages()
            _messages.value = starredMsgs
            _isLoading.value = false
        }
    }
}
