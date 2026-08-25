package com.messages.sms.texting.app.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
    val address: String,
    val name: String?
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarUri: String?,
    val members: List<GroupMember>,
    val createdAt: Long,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false
)

@Entity(tableName = "group_messages")
data class GroupMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val body: String,
    val date: Long,
    val sendStatus: Int = SmsMessage.STATUS_NONE,
    val delayedUntilMillis: Long? = null
)

data class GroupWithLastMessage(
    @Embedded val group: Group,
    val lastMessageBody: String?,
    val lastMessageDate: Long?
)

data class GroupSearchResult(
    @Embedded val group: Group,
    val matchCount: Int,
    val lastMessageDate: Long?
)
