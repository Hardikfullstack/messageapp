package com.messages.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupMessage(
    val address: String,
    val body: String,
    val date: Long,
    val read: Boolean,
    val type: Int
)

@Serializable
data class BackupGroupMessage(
    val body: String,
    val date: Long
)

@Serializable
data class BackupGroup(
    val name: String,
    val avatarUri: String?,
    val members: List<GroupMember>,
    val createdAt: Long,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val messages: List<BackupGroupMessage> = emptyList()
)

@Serializable
data class BackupPayload(
    val appName: String,
    val createdAt: Long,
    val messageCount: Int,
    val messages: List<BackupMessage>,
    val groups: List<BackupGroup> = emptyList()
)
