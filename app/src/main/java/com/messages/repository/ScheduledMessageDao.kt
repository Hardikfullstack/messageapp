package com.messages.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.messages.model.ScheduledMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ScheduledMessage): Long
    
    @androidx.room.Update
    suspend fun update(message: ScheduledMessage)

    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledTimeMillis ASC")
    fun getAllScheduledMessages(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): ScheduledMessage?

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
