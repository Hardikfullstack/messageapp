package com.messages.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Entity(tableName = "sms_messages")
data class SmsMessage(
    @PrimaryKey val id: Long,
    val threadId: Long,
    val address: String, // Sender number or name
    val body: String,
    val date: Long,
    val read: Boolean,
    val type: Int, // 1 = Inbox, 2 = Sent
    val isPinned: Boolean = false,
    val unreadCount: Int = 0,
    val category: Int = 0, // 0=Inbox, 1=Known, 2=Unknown, 3=Transactions, 4=OTPs, 5=Offers
    val contactName: String? = null, // Extracted from contacts
    val photoUri: String? = null,
    val isArchived: Boolean = false,
    val isStarred: Boolean = false,
    val isBlocked: Boolean = false,
    val isMms: Boolean = false,
    val mmsImagePath: String? = null,
    val sendStatus: Int = SmsMessage.STATUS_NONE,
    val delayedUntilMillis: Long? = null,
    val deliveredAtMillis: Long? = null
) {
    companion object {
        const val STATUS_NONE = 0
        const val STATUS_SENDING = 1
        const val STATUS_FAILED = 2
        const val STATUS_DELAYED = 3
    }

    val formattedTime: String
        get() {
            val msgDateTime = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
            val msgDate = msgDateTime.toLocalDate()
            val today = java.time.LocalDate.now()
            
            val daysBetween = ChronoUnit.DAYS.between(msgDate, today)
            
            return when {
                daysBetween == 0L -> {
                    // Today: 10:30 AM
                    msgDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
                }
                daysBetween == 1L -> {
                    "Yesterday"
                }
                daysBetween in 2L..6L -> {
                    // This week: Mon, Tue
                    msgDate.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
                }
                msgDate.year == today.year -> {
                    // This year: 12 Jul
                    msgDate.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))
                }
                else -> {
                    // Older: 12/07/23
                    msgDate.format(DateTimeFormatter.ofPattern("dd/MM/yy", Locale.getDefault()))
                }
            }
        }

    // Helper for initial
    val initial: String
        get() {
            val displayName = contactName ?: address
            return if (displayName.isNotBlank()) displayName.first().uppercase() else "?"
        }
}
