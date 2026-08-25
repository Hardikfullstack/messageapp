package com.messages.sms.texting.app.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.messages.sms.texting.app.model.Reminder

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Query("SELECT * FROM reminders WHERE address = :address ORDER BY reminderTimeMillis ASC")
    suspend fun getAllByAddress(address: String): List<Reminder>

    @Query("SELECT * FROM reminders WHERE address = :address ORDER BY reminderTimeMillis ASC")
    fun getAllByAddressFlow(address: String): kotlinx.coroutines.flow.Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
