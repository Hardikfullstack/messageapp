package com.message.sms.texting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.message.sms.texting.app.model.Group
import com.message.sms.texting.app.model.GroupSearchResult
import com.message.sms.texting.app.model.MessageSearchResult
import com.message.sms.texting.app.model.SmsMessage
import com.message.sms.texting.app.repository.SmsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Contact results stream
    val contactResults: StateFlow<List<SmsMessage>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchContacts(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Message content results stream
    val messageResults: StateFlow<List<MessageSearchResult>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchMessageContent(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group name results stream
    val groupResults: StateFlow<List<Group>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchGroupsByName(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group message content results stream
    val groupMessageResults: StateFlow<List<GroupSearchResult>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchGroupMessageContent(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
