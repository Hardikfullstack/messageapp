package com.messages.sms.texting.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String, // Receiver number — empty string for group-scheduled messages (see groupId)
    val body: String,
    val scheduledTimeMillis: Long,
    val contactName: String? = null,
    val photoUri: String? = null,
    val groupId: Long? = null,
    val groupName: String? = null
) {
    val formattedTime: String
        get() {
            val msgDateTime = Instant.ofEpochMilli(scheduledTimeMillis).atZone(ZoneId.systemDefault())
            return msgDateTime.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a", Locale.getDefault()))
        }

    val initial: String
        get() {
            val displayName = contactName ?: address
            return if (displayName.isNotBlank()) displayName.first().uppercase() else "?"
        }
}
