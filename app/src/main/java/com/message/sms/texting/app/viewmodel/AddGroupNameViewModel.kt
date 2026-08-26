package com.message.sms.texting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.message.sms.texting.app.model.GroupMember
import com.message.sms.texting.app.repository.SmsRepository

class AddGroupNameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)

    suspend fun createGroup(name: String, avatarUri: String?, members: List<GroupMember>): Long {
        return repository.createGroup(name, avatarUri, members)
    }
}
