package com.message.sms.texting.app.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.message.sms.texting.app.model.Group
import com.message.sms.texting.app.model.GroupSearchResult
import com.message.sms.texting.app.model.GroupWithLastMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Insert
    suspend fun insert(group: Group): Long

    @Update
    suspend fun update(group: Group)

    @Delete
    suspend fun delete(group: Group)

    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<Group>>

    @Query(
        """
        SELECT g.*, lm.body AS lastMessageBody, lm.date AS lastMessageDate
        FROM groups g
        LEFT JOIN (
            SELECT gm1.groupId, gm1.body, gm1.date
            FROM group_messages gm1
            INNER JOIN (
                SELECT groupId, MAX(date) AS maxDate FROM group_messages GROUP BY groupId
            ) gm2 ON gm1.groupId = gm2.groupId AND gm1.date = gm2.maxDate
        ) lm ON lm.groupId = g.id
        ORDER BY g.isPinned DESC, COALESCE(lm.date, g.createdAt) DESC
        """
    )
    fun getAllGroupsWithLastMessage(): Flow<List<GroupWithLastMessage>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): Group?

    @Query("SELECT * FROM groups WHERE id = :id")
    fun getGroupByIdFlow(id: Long): Flow<Group?>

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE groups SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE groups SET isArchived = :isArchived WHERE id IN (:ids)")
    suspend fun setArchivedBulk(ids: List<Long>, isArchived: Boolean)

    @Query("UPDATE groups SET isPinned = :isPinned WHERE id IN (:ids)")
    suspend fun setPinnedBulk(ids: List<Long>, isPinned: Boolean)

    @Query("DELETE FROM groups WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM groups WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchGroupsByName(query: String): Flow<List<Group>>

    @Query(
        """
        SELECT g.*,
            (SELECT COUNT(id) FROM group_messages gm WHERE gm.groupId = g.id AND gm.body LIKE '%' || :query || '%') AS matchCount,
            (SELECT MAX(date) FROM group_messages gm2 WHERE gm2.groupId = g.id) AS lastMessageDate
        FROM groups g
        WHERE EXISTS (
            SELECT 1 FROM group_messages gm3 WHERE gm3.groupId = g.id AND gm3.body LIKE '%' || :query || '%'
        )
        ORDER BY lastMessageDate DESC
        """
    )
    fun searchGroupMessageContent(query: String): Flow<List<GroupSearchResult>>
}
