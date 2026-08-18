package com.messages.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.messages.model.GroupMember
import com.messages.repository.SmsRepository

class AddGroupNameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)

    suspend fun createGroup(name: String, avatarUri: String?, members: List<GroupMember>): Long {
        return repository.createGroup(name, avatarUri, members)
    }
}
