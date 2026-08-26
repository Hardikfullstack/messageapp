package com.message.sms.texting.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_contacts")
data class BlockedContact(
    @PrimaryKey val address: String,
    val contactName: String? = null
)
