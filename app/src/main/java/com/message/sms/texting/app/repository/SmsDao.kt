package com.message.sms.texting.app.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource
import com.message.sms.texting.app.model.SmsMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<SmsMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SmsMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeletedMessages(deletedMessages: List<com.message.sms.texting.app.model.DeletedMessage>)

    @Query("SELECT id FROM deleted_messages")
    suspend fun getDeletedMessageIds(): List<Long>

    @Query("SELECT *, MAX(date) FROM sms_messages WHERE isArchived = 0 AND address NOT IN (SELECT address FROM blocked_contacts) AND date >= :startDate AND date <= :endDate GROUP BY threadId ORDER BY isPinned DESC, MAX(date) DESC")
    fun getAllMessagesFlow(startDate: Long, endDate: Long): PagingSource<Int, SmsMessage>

    @Query("SELECT *, MAX(date) FROM sms_messages WHERE isArchived = 0 AND address NOT IN (SELECT address FROM blocked_contacts) AND date >= :startDate AND date <= :endDate GROUP BY threadId ORDER BY isPinned DESC, MAX(date) DESC")
    suspend fun getAllMessagesList(startDate: Long, endDate: Long): List<SmsMessage>

    @Query("SELECT * FROM sms_messages WHERE threadId = :threadId ORDER BY date DESC")
    fun getMessagesByThreadIdFlow(threadId: Long): Flow<List<SmsMessage>>

    @Query("SELECT *, MAX(date) FROM sms_messages WHERE category = :category AND isArchived = 0 AND address NOT IN (SELECT address FROM blocked_contacts) AND date >= :startDate AND date <= :endDate GROUP BY threadId ORDER BY isPinned DESC, MAX(date) DESC")
    fun getMessagesByCategoryFlow(category: Int, startDate: Long, endDate: Long): PagingSource<Int, SmsMessage>

    @Query("SELECT *, MAX(date) FROM sms_messages WHERE category = :category AND isArchived = 0 AND address NOT IN (SELECT address FROM blocked_contacts) AND date >= :startDate AND date <= :endDate GROUP BY threadId ORDER BY isPinned DESC, MAX(date) DESC")
    suspend fun getMessagesByCategoryList(category: Int, startDate: Long, endDate: Long): List<SmsMessage>

    @Query("SELECT *, MAX(date) FROM sms_messages WHERE isArchived = 1 AND address NOT IN (SELECT address FROM blocked_contacts) GROUP BY threadId ORDER BY isPinned DESC, MAX(date) DESC")
    fun getArchivedMessagesFlow(): PagingSource<Int, SmsMessage>

    @Query("DELETE FROM sms_messages")
    suspend fun deleteAll()

    @Query("DELETE FROM sms_messages WHERE id IN (:ids)")
    suspend fun deleteMessages(ids: List<Long>)
    
    @Query("SELECT COUNT(id) FROM sms_messages")
    suspend fun getMessageCount(): Int

    @Query("UPDATE sms_messages SET read = 0 WHERE id IN (:ids)")
    suspend fun markAsUnread(ids: List<Long>)

    @Query("UPDATE sms_messages SET read = 1 WHERE id IN (:ids)")
    suspend fun markAsRead(ids: List<Long>)

    @Query("UPDATE sms_messages SET read = 1, unreadCount = 0 WHERE threadId = :threadId")
    suspend fun markThreadAsRead(threadId: Long)

    @Query("SELECT DISTINCT threadId FROM sms_messages WHERE read = 0 AND category = :category AND isArchived = 0 AND address NOT IN (SELECT address FROM blocked_contacts) AND date >= :startDate AND date <= :endDate")
    suspend fun getUnreadThreadIdsByCategory(category: Int, startDate: Long, endDate: Long): List<Long>

    @Query("SELECT DISTINCT threadId FROM sms_messages WHERE read = 0 AND isArchived = 0 AND address NOT IN (SELECT address FROM blocked_contacts) AND date >= :startDate AND date <= :endDate")
    suspend fun getAllUnreadThreadIds(startDate: Long, endDate: Long): List<Long>

    @Query("UPDATE sms_messages SET isPinned = 1 WHERE id IN (:ids)")
    suspend fun pinMessages(ids: List<Long>)

    @Query("UPDATE sms_messages SET isPinned = 0 WHERE id IN (:ids)")
    suspend fun unpinMessages(ids: List<Long>)

    @Query("UPDATE sms_messages SET isStarred = :isStarred WHERE id IN (:ids)")
    suspend fun updateStarStatus(ids: List<Long>, isStarred: Boolean)

    @Query("UPDATE sms_messages SET isArchived = :isArchived WHERE id IN (:ids)")
    suspend fun setArchived(ids: List<Long>, isArchived: Boolean)

    @Query("SELECT id FROM sms_messages WHERE threadId IN (:threadIds)")
    suspend fun getMessageIdsByThreadIds(threadIds: List<Long>): List<Long>

    @Query("SELECT id FROM sms_messages WHERE address = :address")
    suspend fun getMessageIdsByAddress(address: String): List<Long>

    @Query("SELECT id FROM sms_messages WHERE date < :cutoffMillis")
    suspend fun getMessageIdsOlderThan(cutoffMillis: Long): List<Long>

    @Query("DELETE FROM sms_messages WHERE threadId IN (:threadIds)")
    suspend fun deleteThreads(threadIds: List<Long>)

    @Query("UPDATE sms_messages SET isArchived = :isArchived WHERE threadId IN (:threadIds)")
    suspend fun setThreadsArchived(threadIds: List<Long>, isArchived: Boolean)

    @Query("UPDATE sms_messages SET isPinned = 1 WHERE threadId IN (:threadIds)")
    suspend fun pinThreads(threadIds: List<Long>)

    @Query("UPDATE sms_messages SET isPinned = 0 WHERE threadId IN (:threadIds)")
    suspend fun unpinThreads(threadIds: List<Long>)

    @Query("UPDATE sms_messages SET read = 1, unreadCount = 0 WHERE threadId IN (:threadIds)")
    suspend fun markThreadsAsRead(threadIds: List<Long>)

    @Query("SELECT id, isPinned, read, isArchived, isStarred, isMms, deliveredAtMillis FROM sms_messages")
    suspend fun getLocalStates(): List<MessageLocalState>

    @Query("SELECT * FROM sms_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): SmsMessage?

    @Query("UPDATE sms_messages SET sendStatus = :status WHERE id = :id")
    suspend fun updateSendStatus(id: Long, status: Int)

    @Query("UPDATE sms_messages SET deliveredAtMillis = :deliveredAtMillis WHERE id = :id")
    suspend fun setDeliveredAt(id: Long, deliveredAtMillis: Long)

    @Query("SELECT isArchived FROM sms_messages WHERE threadId = :threadId LIMIT 1")
    suspend fun isThreadArchived(threadId: Long): Boolean?

    @Query("SELECT id FROM sms_messages WHERE isStarred = 1")
    suspend fun getStarredMessageIds(): List<Long>

    @Query("SELECT COUNT(id) FROM sms_messages WHERE read = 0 AND isArchived = 0 AND address NOT IN (SELECT address FROM blocked_contacts)")
    fun getTotalUnreadCountFlow(): Flow<Int>

    @Query("SELECT category, COUNT(id) as count FROM sms_messages WHERE read = 0 AND isArchived = 0 AND address NOT IN (SELECT address FROM blocked_contacts) GROUP BY category")
    fun getUnreadCountsByCategoryFlow(): Flow<List<CategoryUnreadCount>>

    @Query("UPDATE sms_messages SET isBlocked = :isBlocked WHERE address = :address")
    suspend fun updateIsBlockedStatus(address: String, isBlocked: Boolean)

    @Query("UPDATE sms_messages SET isBlocked = :isBlocked WHERE address IN (:addresses)")
    suspend fun updateIsBlockedStatuses(addresses: List<String>, isBlocked: Boolean)

    @Query("DELETE FROM sms_messages WHERE address = :address")
    suspend fun deleteMessagesByAddress(address: String)

    @Query("""
        SELECT *, MAX(date) 
        FROM sms_messages 
        WHERE (contactName LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%') 
        AND address NOT IN (SELECT address FROM blocked_contacts) 
        GROUP BY threadId 
        ORDER BY MAX(date) DESC
    """)
    fun searchContacts(query: String): Flow<List<SmsMessage>>

    @Query("""
        SELECT m.*, (
            SELECT COUNT(id) FROM sms_messages s 
            WHERE s.threadId = m.threadId 
            AND s.body LIKE '%' || :query || '%'
        ) as matchCount 
        FROM sms_messages m 
        WHERE m.body LIKE '%' || :query || '%' 
        AND m.address NOT IN (SELECT address FROM blocked_contacts) 
        GROUP BY m.threadId 
        ORDER BY MAX(m.date) DESC
    """)
    fun searchMessageContent(query: String): Flow<List<com.message.sms.texting.app.model.MessageSearchResult>>
}

data class CategoryUnreadCount(
    val category: Int,
    val count: Int
)

data class MessageLocalState(
    val id: Long,
    val isPinned: Boolean,
    val read: Boolean,
    val isArchived: Boolean,
    val isStarred: Boolean,
    val isMms: Boolean = false,
    val deliveredAtMillis: Long? = null
)
