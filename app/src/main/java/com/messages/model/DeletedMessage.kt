package com.messages.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_messages")
data class DeletedMessage(
    @PrimaryKey val id: Long
)
