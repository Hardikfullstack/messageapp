package com.messages.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.messages.model.GroupMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMessageDao {
    @Insert
    suspend fun insert(message: GroupMessage): Long

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY date ASC")
    fun getMessagesForGroup(groupId: Long): Flow<List<GroupMessage>>

    @Query("DELETE FROM group_messages WHERE groupId = :groupId")
    suspend fun deleteMessagesForGroup(groupId: Long)

    @Query("UPDATE group_messages SET sendStatus = :status WHERE id = :id")
    suspend fun updateSendStatus(id: Long, status: Int)
}
