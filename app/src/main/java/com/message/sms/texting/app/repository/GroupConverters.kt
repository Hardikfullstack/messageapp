package com.message.sms.texting.app.repository

import androidx.room.TypeConverter
import com.message.sms.texting.app.model.GroupMember
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GroupConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromMemberList(members: List<GroupMember>): String {
        return json.encodeToString<List<GroupMember>>(members)
    }

    @TypeConverter
    fun toMemberList(data: String): List<GroupMember> {
        return if (data.isBlank()) emptyList() else json.decodeFromString<List<GroupMember>>(data)
    }
}
