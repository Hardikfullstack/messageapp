package com.message.sms.texting.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val contactName: String? = null,
    val reminderTimeMillis: Long,
    val note: String = "",
    val colorIndex: Int = 0
) {
    val formattedTime: String
        get() {
            val dateTime = Instant.ofEpochMilli(reminderTimeMillis).atZone(ZoneId.systemDefault())
            return dateTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
        }

    val formattedDayLabel: String
        get() {
            val date = Instant.ofEpochMilli(reminderTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val today = java.time.LocalDate.now()
            return when (java.time.temporal.ChronoUnit.DAYS.between(today, date)) {
                0L -> "Today"
                1L -> "Tomorrow"
                else -> date.format(DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()))
            }
        }
}
