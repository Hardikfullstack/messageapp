package com.message.sms.texting.app.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.message.sms.texting.app.model.Draft
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: Draft)

    @Query("SELECT * FROM drafts WHERE address = :address LIMIT 1")
    suspend fun getDraft(address: String): Draft?

    @Query("SELECT * FROM drafts")
    fun getAllDrafts(): Flow<List<Draft>>

    @Query("DELETE FROM drafts WHERE address = :address")
    suspend fun deleteDraft(address: String)
}
