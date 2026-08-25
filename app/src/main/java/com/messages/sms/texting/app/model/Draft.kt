package com.messages.sms.texting.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey val address: String,
    val threadId: Long,
    val body: String,
    val timestamp: Long
)
