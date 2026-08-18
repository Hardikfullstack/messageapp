package com.messages.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.messages.model.BlockedContact
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedContact: BlockedContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blockedContacts: List<BlockedContact>)

    @Query("DELETE FROM blocked_contacts WHERE address = :address")
    suspend fun deleteByAddress(address: String)

    @Query("SELECT * FROM blocked_contacts")
    fun getAllBlockedContactsFlow(): Flow<List<BlockedContact>>

    @Query("SELECT address FROM blocked_contacts")
    suspend fun getAllBlockedAddresses(): List<String>
    
    @Query("SELECT EXISTS(SELECT 1 FROM blocked_contacts WHERE address = :address LIMIT 1)")
    suspend fun isBlocked(address: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_contacts WHERE address = :address LIMIT 1)")
    fun isBlockedFlow(address: String): Flow<Boolean>

    @Query("""
        SELECT b.address, b.contactName, m.body, MAX(m.date) as date, m.threadId 
        FROM blocked_contacts b 
        LEFT JOIN sms_messages m ON b.address = m.address 
        GROUP BY b.address
        ORDER BY date DESC
    """)
    fun getBlockedContactsWithLatestMessageFlow(): Flow<List<BlockedContactWithMessage>>
}

data class BlockedContactWithMessage(
    val address: String,
    val contactName: String?,
    val body: String?,
    val date: Long?,
    val threadId: Long?
) {
    val formattedTime: String
        get() {
            if (date == null || date == 0L) return ""
            val msgDateTime = java.time.Instant.ofEpochMilli(date).atZone(java.time.ZoneId.systemDefault())
            val msgDate = msgDateTime.toLocalDate()
            val today = java.time.LocalDate.now()
            
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(msgDate, today)
            
            return when {
                daysBetween == 0L -> {
                    msgDateTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.getDefault()))
                }
                daysBetween == 1L -> {
                    "Yesterday"
                }
                daysBetween in 2L..6L -> {
                    msgDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.getDefault()))
                }
                msgDate.year == today.year -> {
                    msgDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.getDefault()))
                }
                else -> {
                    msgDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy", java.util.Locale.getDefault()))
                }
            }
        }
}
