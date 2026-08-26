package com.message.sms.texting.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_messages")
data class DeletedMessage(
    @PrimaryKey val id: Long
)
