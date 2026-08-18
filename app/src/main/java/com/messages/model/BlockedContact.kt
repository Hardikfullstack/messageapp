package com.messages.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_contacts")
data class BlockedContact(
    @PrimaryKey val address: String,
    val contactName: String? = null
)
